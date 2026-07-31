package ru.volkfm.chattskiy.model.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageEvent extends PublishableEvent {
    private String content;
}
