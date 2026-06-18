package ru.volkfm.chattskiy.handler.event;

import reactor.core.publisher.Flux;
import ru.volkfm.chattskiy.model.event.Event;

public interface EventHandler {
    Flux<Event> handle(Event e);
}
