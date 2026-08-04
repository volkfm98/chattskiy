package ru.volkfm.chattskiy.service.eventpublishing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ru.volkfm.chattskiy.config.ApplicationProperties;
import ru.volkfm.chattskiy.model.event.PublishableEvent;
import ru.volkfm.chattskiy.util.logging.StructuredLog;
import tools.jackson.databind.ObjectMapper;

import static ru.volkfm.chattskiy.util.logging.StructuredLog.*;

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
                .map(ReactiveSubscription.Message::getMessage)
                .map(e -> objectMapper.readValue(e, PublishableEvent.class))
                .doOnNext(event -> log.atDebug()
                        .addKeyValue(TRACE_ID_KEY, event.getEventId())
                        .addKeyValue(USER_ID_KEY, event.getUserId())
                        .addKeyValue(OBJECT_KEY, event)
                        .log("Received outside event {}", event.getEventId()))
                .onErrorContinue((t, o) -> {
                    var logEventBuilder = log.atError();

                    if (o instanceof PublishableEvent event) {
                        logEventBuilder = logEventBuilder
                                .addKeyValue(TRACE_ID_KEY, event.getEventId().toString())
                                .addKeyValue(USER_ID_KEY, event.getUserId().toString());
                    }

                    logEventBuilder
                            .setCause(t)
                            .addKeyValue(OBJECT_KEY, StructuredLog.object(o))
                            .log("Error occurred listening outside event from redis");
                });
    }
}
