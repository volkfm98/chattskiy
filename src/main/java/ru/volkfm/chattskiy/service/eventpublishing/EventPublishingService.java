package ru.volkfm.chattskiy.service.eventpublishing;

import ru.volkfm.chattskiy.model.event.PublishableEvent;

public interface EventPublishingService {
    PublishingStatus publish(PublishableEvent event);
}
