package ru.volkfm.chattskiy.model.event;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
public class MessageEvent extends Event {
    private String userId;
    private String chatId;
    private String message;
    private ZonedDateTime timestamp;
}
