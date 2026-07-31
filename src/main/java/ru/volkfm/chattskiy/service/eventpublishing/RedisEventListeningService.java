package ru.volkfm.chattskiy.service.eventpublishing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ru.volkfm.chattskiy.config.ApplicationProperties;
import ru.volkfm.chattskiy.model.event.PublishableEvent;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventListeningService implements EventListeningService {
    private final ApplicationProperties appProps;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Flux<PublishableEvent> getEvents() {
        return redisTemplate.listenToChannel(appProps.redis.channels)
                .doOnSubscribe(s -> log.info("Subscribed to Redis"))
                .doOnCancel(() -> log.info("Redis subscription cancelled"))
                .doOnComplete(() -> log.info("Redis subscription completed"))
                .doOnError(e -> log.error("Redis subscription error", e))
                .doFinally(sig -> log.info("Redis finally: {}", sig))
                .map(ReactiveSubscription.Message::getMessage)
                .doOnNext(message -> log.info("Raw message {}", message))
                .map(e -> objectMapper.readValue(e, PublishableEvent.class));
    }
}
