package ru.volkfm.chattskiy.model.event;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class AckEvent extends Event {
    public AckEvent(UUID eventId) {
        this.eventId = eventId;
        this.type = EventType.ACK;
    }

    @Override
    public Event copy() {
        return this.toBuilder().build();
    }
}
