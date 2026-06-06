package ru.volkfm.chattskiy.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.handler.event.EventHandler;
import ru.volkfm.chattskiy.model.event.Event;
import ru.volkfm.chattskiy.model.event.EventType;
import ru.volkfm.chattskiy.model.event.MessageEvent;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatWsHandler implements WebSocketHandler {
    private final ObjectMapper objectMapper;
    private final Map<EventType, EventHandler> eventHandlerMap = new HashMap<>(); // ToDo: make it more configurable

    @Override
    public Mono<Void> handle(WebSocketSession session) {
         var output = session.receive().doOnNext(this::handleWsEvent);
         return session.send(output);
    }

    protected Event getEventFromWsMessage(WebSocketMessage wsMsg) throws JsonProcessingException {
            return objectMapper.readValue(wsMsg.getPayloadAsText(), Event.class);
    }

    protected void handleWsEvent(WebSocketMessage wsMsg) {
        try {
            switch (wsMsg.getType()) {
                case WebSocketMessage.Type.TEXT -> {
                    Event event = getEventFromWsMessage(wsMsg);

                    eventHandlerMap.get(event.getType()).handle(event);
                }
                case WebSocketMessage.Type.PING, WebSocketMessage.Type.PONG -> {
                    // ToDo: Add ping pong handling
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e); // ToDo: Fix this dirty hack.
        }
    }
}
