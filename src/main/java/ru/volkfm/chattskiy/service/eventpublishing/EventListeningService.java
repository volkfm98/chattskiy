package ru.volkfm.chattskiy.service.eventpublishing;

import reactor.core.publisher.Flux;
import ru.volkfm.chattskiy.model.event.PublishableEvent;

public interface EventListeningService {
    Flux<PublishableEvent> getEvents();
}
