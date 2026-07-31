package ru.volkfm.chattskiy.listener;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import ru.volkfm.chattskiy.model.event.Event;
import ru.volkfm.chattskiy.model.event.PublishableEvent;
import ru.volkfm.chattskiy.service.eventpublishing.EventListeningService;
import ru.volkfm.chattskiy.service.sessionregistry.LocalSession;
import ru.volkfm.chattskiy.service.sessionregistry.SessionRegistryService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventListener {
    private final EventListeningService eventListeningService;
    private final SessionRegistryService sessionRegistry;

    @PostConstruct
    void startListening() {
        handleEvents()
                .onErrorContinue((t, object) -> log.error("Error occurred while listening events from redis", t)) // ToDo: log
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
                    List<Sinks.Many<Event>> sinks = event.getRecipients().stream()
                            .map(UUID::fromString)
                            .flatMap(recipientId -> sessionRegistry.getSessions(recipientId).values().stream())
                            .map(LocalSession::getOutsideSink)
                            .toList();

                    List<Sinks.EmitResult> results = sinks.stream().map(sink -> sink.tryEmitNext(event)).toList(); // ToDo: log each emit

                    log.info("Emitted events with statuses: {}", results);
                });
    }
}
