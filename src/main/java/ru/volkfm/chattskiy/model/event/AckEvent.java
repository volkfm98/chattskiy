package ru.volkfm.chattskiy.model.event;

import java.util.UUID;

public class AckEvent extends Event {
    public AckEvent(UUID eventId) {
        this.eventId = eventId;
        this.type = EventType.ACK;
    }
}
