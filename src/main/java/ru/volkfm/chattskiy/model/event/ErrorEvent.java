package ru.volkfm.chattskiy.model.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class ErrorEvent extends Event {
    private String code;
    private String message;

    public ErrorEvent(UUID eventId, String code, String message) {
        this.eventId = eventId;
        this.type = EventType.ERROR;
        this.code = code;
        this.message = message;
    }

    @Override
    public Event copy() {
        return this.toBuilder().build();
    }
}
