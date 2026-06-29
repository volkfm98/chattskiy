package ru.volkfm.chattskiy.model.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EventType {
    MESSAGE, ACK;
}
