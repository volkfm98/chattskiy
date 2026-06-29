package ru.volkfm.chattskiy.model.event;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
public class MessageEvent extends PublishableEvent {
    private String content;
}
