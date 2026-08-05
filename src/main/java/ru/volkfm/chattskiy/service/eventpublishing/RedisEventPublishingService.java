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
import ru.volkfm.chattskiy.util.logging.StructuredLog;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static ru.volkfm.chattskiy.constant.Redis.*;
import static ru.volkfm.chattskiy.util.logging.StructuredLog.*;
import static ru.volkfm.chattskiy.util.logging.StructuredLog.OBJECT_KEY;


/**
 * Class, responsible for event publishing between nodes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
// ToDo: Multiple responsibilities. Split
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

        if (result.isSuccess()) {
            return PublishingStatus.SUCCESS;
        } else {
            log.atWarn()
                    .addKeyValue(TRACE_ID_KEY, event.getEventId())
                    .addKeyValue(USER_ID_KEY, event.getUserId())
                    .addKeyValue(OBJECT_KEY, result)
                    .log("Unable to emit event {} to publishing pipeline. Result is {}", event.getEventId(), result);

            return PublishingStatus.ERROR;
        }
    }

    private Mono<Void> process(Flux<PublishableEvent> incomingEvents) {
        // event -> map to ctx -> chatParticipantsList and map to ctx -> subsList(event) -> map ctx to enriched event -> publish
        return Mono.defer(() -> incomingEvents
                .flatMap(e ->
                        getUserIds(e)
                                .collectList()
                                .doOnNext(e::setRecipients)
                                .thenReturn(e)
                )
                .flatMap(e ->
                        getSubs(e.getRecipients())
                                .doOnNext(sub -> log.atDebug()
                                        .addKeyValue(USER_ID_KEY, e.getUserId())
                                        .addKeyValue(TRACE_ID_KEY, e.getEventId())
                                        .addKeyValue(OBJECT_KEY, StructuredLog.object(e))
                                        .addKeyValue(DEST_KEY, sub)
                                        .log("Sending event {} to destination {} via redis pub/sub", e.getEventId(), sub))
                                .flatMap(sub ->
                                        redisTemplate.convertAndSend(NODE_KEY + ":" + sub, objectMapper.writeValueAsString(e)))
                )
                .onErrorContinue((t, o) -> {
                    var logBuilder = log.atError()
                            .setCause(t)
                            .addKeyValue(OBJECT_KEY, StructuredLog.object(o));

                    if (o instanceof PublishableEvent event) {
                        logBuilder = logBuilder
                                .addKeyValue(TRACE_ID_KEY, event.getEventId())
                                .addKeyValue(USER_ID_KEY, event.getUserId());
                    }

                    logBuilder.log("Error during event publishing");
                })
                .then());
    }

    private Flux<String> getSubs(List<String> userIds) {
        return Flux.fromIterable(userIds)
                .flatMap(user -> registry.getNodes(UUID.fromString(user)))
                .distinct();
    }

    private Flux<String> getUserIds(PublishableEvent event) {
        UUID chatId = event.getChatId();

        return Flux.defer(() -> {
                    log.atDebug()
                            .addKeyValue(USER_ID_KEY, event.getUserId())
                            .addKeyValue(TRACE_ID_KEY, event.getEventId())
                            .log("Getting members of chat:{} from cache", chatId);

                    return redisTemplate.opsForSet().members(CHAT_KEY + ":" + chatId.toString())
                .switchIfEmpty(
                        Flux.defer(() -> {
                            log.atDebug()
                                    .addKeyValue(USER_ID_KEY, event.getUserId())
                                    .addKeyValue(TRACE_ID_KEY, event.getEventId())
                                    .log("Chat members of chat:{} not cached. Retrieving from DB", chatId);
                            return getUserIdsFromDbAndCache(chatId);
                        }));
        });
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
