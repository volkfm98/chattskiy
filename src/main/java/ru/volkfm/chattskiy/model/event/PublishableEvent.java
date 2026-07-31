package ru.volkfm.chattskiy.model.event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
public abstract class PublishableEvent extends Event {
    @NotNull(message = "userId must be present")
    protected UUID userId;
    @NotNull(message = "chatId must be present")
    protected UUID chatId;
    // Optional field. Filled by publisher service
    protected List<String> recipients;
    protected ZonedDateTime timestamp = ZonedDateTime.now();
}
