package ru.volkfm.chattskiy.listener;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ru.volkfm.chattskiy.model.event.PublishableEvent;
import ru.volkfm.chattskiy.service.eventpublishing.EventListeningService;
import ru.volkfm.chattskiy.service.sessionregistry.LocalSession;
import ru.volkfm.chattskiy.service.sessionregistry.SessionRegistryService;
import ru.volkfm.chattskiy.util.logging.StructuredLog;

import java.util.UUID;
import java.util.stream.Stream;

import static ru.volkfm.chattskiy.util.logging.StructuredLog.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventListener {
    private final EventListeningService eventListeningService;
    private final SessionRegistryService sessionRegistry;

    @PostConstruct
    void startListening() {
        handleEvents()
                .onErrorContinue((t, o) -> {
                    var logEventBuilder = log.atError();

                    if (o instanceof PublishableEvent event) {
                        logEventBuilder = logEventBuilder
                                .addKeyValue(TRACE_ID_KEY, event.getEventId().toString())
                                .addKeyValue(USER_ID_KEY, event.getUserId().toString());
                    }

                    logEventBuilder
                            .addKeyValue(OBJECT_KEY, StructuredLog.object(o))
                            .setCause(t)
                            .log("Error occurred while handling outside event from redis");
                })
                .subscribe();
    }

    Flux<PublishableEvent> handleEvents() {
        return eventListeningService.getEvents()
                .flatMap(event ->
                        switch (event.getType()) {
                            case MESSAGE -> Flux.just(event); // Some EventType specific processing, which can result in new events
                            default -> Flux.just(event);
                })
                .doOnNext(event -> {
                    Stream<LocalSession> sessions = event.getRecipients().stream()
                            .map(UUID::fromString)
                            .flatMap(recipientId -> sessionRegistry.getSessions(recipientId).values().stream());

                    sessions.forEach(session -> {
                        log.atDebug()
                                .addKeyValue(TRACE_ID_KEY, event.getEventId().toString())
                                .addKeyValue(USER_ID_KEY, event.getUserId().toString())
                                .addKeyValue(SESSION_ID_KEY, session.getSessionId())
                                .addKeyValue(OBJECT_KEY, StructuredLog.object(event))
                                .log("Emitting outside event {}", event.getEventId());

                        var sink = session.getOutsideSink();
                        sink.tryEmitNext(event.copy());
                    });
                });
    }
}
