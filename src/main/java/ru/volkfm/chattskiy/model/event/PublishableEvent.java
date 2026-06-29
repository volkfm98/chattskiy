package ru.volkfm.chattskiy.model.event;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
public abstract class PublishableEvent extends Event {
    protected UUID userId;
    protected UUID chatId;
    protected ZonedDateTime timestamp;
}
