package ru.volkfm.chattskiy.service.sessionregistry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Sinks;
import ru.volkfm.chattskiy.model.event.Event;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class LocalSession {
    private final UUID userId;
    private final String sessionId;
    private final Sinks.Many<Event> outsideSink = Sinks.many().multicast().onBackpressureBuffer();
}
