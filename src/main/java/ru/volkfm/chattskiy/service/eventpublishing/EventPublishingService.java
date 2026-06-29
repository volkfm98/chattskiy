package ru.volkfm.chattskiy.service.eventpublishing;

import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.model.event.PublishableEvent;

public interface EventPublishingService {
    Mono<Void> publish(PublishableEvent event);
}
