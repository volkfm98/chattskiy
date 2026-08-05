package ru.volkfm.chattskiy.model.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @Type(value = MessageEvent.class, name = "MESSAGE"),
        @Type(value = AckEvent.class, name = "ACK"),
        @Type(value = ErrorEvent.class, name = "ERROR")
})
public abstract class Event {
    protected UUID eventId = UUID.randomUUID();
    @NotNull(message = "event type must be present")
    protected EventType type;
}
