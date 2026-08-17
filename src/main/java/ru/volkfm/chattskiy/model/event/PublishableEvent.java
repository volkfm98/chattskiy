package ru.volkfm.chattskiy.model.event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@SuperBuilder(toBuilder = true)
@Data
@NoArgsConstructor
public abstract class PublishableEvent extends Event {
    // Filled by web socket handler at the time of delegation
    protected UUID userId;
    @NotNull(message = "chatId must be present")
    protected UUID chatId;
    // Optional field. Filled by publisher service
    protected List<String> recipients;
    protected ZonedDateTime timestamp = ZonedDateTime.now();
}
