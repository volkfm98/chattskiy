package ru.volkfm.chattskiy.handler.event;

import ru.volkfm.chattskiy.model.event.Event;

public interface EventHandler {
    void handle(Event e) throws Exception;
}
