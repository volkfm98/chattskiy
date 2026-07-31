package ru.volkfm.chattskiy.service.eventpublishing;

import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import ru.volkfm.chattskiy.model.event.PublishableEvent;
import ru.volkfm.chattskiy.repository.postgres.ChatRepository;
import ru.volkfm.chattskiy.service.sessionregistry.SessionRegistryService;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static ru.volkfm.chattskiy.constant.Redis.*;


/**
 * Class, responsible for event publishing between nodes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublishingService implements EventPublishingService {
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final SessionRegistryService registry;
    private final ChatRepository chatRepo;
    private final Sinks.Many<PublishableEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    @PostConstruct
    protected void start() {
        process(sink.asFlux())
                .subscribe();
    }

    @Override
    public PublishingStatus publish(PublishableEvent event) {
        Sinks.EmitResult result = sink.tryEmitNext(event);
        return result.isSuccess() ? PublishingStatus.SUCCESS : PublishingStatus.ERROR;
    }

    private Mono<Void> process(Flux<PublishableEvent> incomingEvents) {
        // event -> map to ctx -> chatParticipantsList and map to ctx -> subsList(event) -> map ctx to enriched event -> publish
        return incomingEvents
                .flatMap(e ->
                        getUserIds(e.getChatId())
                                .collectList()
                                .doOnNext(e::setRecipients)
                                .thenReturn(e)
                )
                .flatMap(e ->
                        getSubs(e.getRecipients())
                                .doOnNext(sub -> log.debug("Sending to node {}", sub)) // ToDo: log
                                .flatMap(sub ->
                                        redisTemplate.convertAndSend(NODE_KEY + ":" + sub, objectMapper.writeValueAsString(e)))
                )
                .onErrorContinue((e, _) -> log.error("Error during event publishing", e)) // ToDo: log
                .then();
    }

    private Flux<String> getSubs(List<String> userIds) {
        return Flux.fromIterable(userIds)
                .flatMap(user -> registry.getNodes(UUID.fromString(user)))
                .distinct();
    }

    private Flux<String> getUserIds(UUID chatId) {
        return Flux.defer(() -> {
                    log.debug("Getting members of chat:{} from cache", chatId); // ToDo: log
                    return redisTemplate.opsForSet().members(CHAT_KEY + ":" + chatId.toString());
                })
                .switchIfEmpty(
                        Flux.defer(() -> {
                            log.debug("Chat members of chat:{} not cached. Retrieving from DB", chatId); // ToDo: log
                            return getUserIdsFromDbAndCache(chatId);
                        })
                );
    }

    private Flux<String> getUserIdsFromDbAndCache(UUID chatId) {
        return chatRepo.getUsers(chatId)
                .map(UUID::toString)
                .collectList()
                .flatMap(users ->
                        redisTemplate.opsForSet()
                                .add(CHAT_KEY + ":" + chatId, users.toArray(String[]::new))
                                .flatMap(_ -> redisTemplate.expire(CHAT_KEY + ":" + chatId, Duration.ofMinutes(30)))
                                .thenReturn(users)
                )
                .flatMapIterable(l -> l);
    }
}
