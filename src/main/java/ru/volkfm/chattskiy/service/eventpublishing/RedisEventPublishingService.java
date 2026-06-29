package ru.volkfm.chattskiy.service.eventpublishing;

import lombok.*;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.model.event.PublishableEvent;
import ru.volkfm.chattskiy.service.sessionregistry.GlobalSessionRegistryService;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;


/**
 * Class, responsible for event publishing between nodes.
 */
@Service
@RequiredArgsConstructor
public class RedisEventPublishingService implements EventPublishingService {
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final GlobalSessionRegistryService globalRegistry;
    //    private final ChatRepository chatRepo;

    @Getter
    @Setter
    @NoArgsConstructor
    private class PipelineContext {
        private PublishableEvent event;
        private List<String> chatParticipants;
    }


    @Override
    public Mono<Void> publish(PublishableEvent event) {
        // event -> map to ctx -> chatParticipantsList and map to ctx -> subsList(event) -> map ctx to enriched event -> publish

        var context = new PipelineContext();
        context.setEvent(event);

        return Mono.just(context)
                .flatMap(ctx ->
                        getUserIds(ctx.getEvent().getChatId())
                                .collectList()
                                .doOnNext(ctx::setChatParticipants)
                                .thenReturn(ctx)
                )
                .flatMap(ctx ->
                        getSubs(ctx.getChatParticipants())
                                .flatMap(sub ->
                                    redisTemplate.convertAndSend(sub, objectMapper.writeValueAsString(event))
                                )
                                .then()
                );
    }

    private Flux<String> getSubs(List<String> userIds) {
        return Flux.fromIterable(userIds)
                .flatMap(user -> globalRegistry.getNodes(UUID.fromString(user)))
                .distinct();
    }

    private Flux<String> getUserIds(UUID chatId) {
        return redisTemplate.opsForSet().members(chatId.toString())
                .switchIfEmpty(
                        chatRepo.findById(chatId).flux()
                                .flatMap(chat ->
                                    redisTemplate.opsForSet()
                                            .add(chatId.toString(), chat.getUsers().toArray(new String[0]))
                                            .doOnNext(_ -> redisTemplate.expire(chatId.toString(), Duration.ofMinutes(30)))
                                            .then(chat.getUsers())
                                )
                                .map(UUID::toString)
                );

        return Flux.empty();
    }
}
