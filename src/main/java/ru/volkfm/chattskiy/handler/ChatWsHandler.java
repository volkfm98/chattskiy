package ru.volkfm.chattskiy.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.handler.event.EventHandler;
import ru.volkfm.chattskiy.model.event.Event;
import ru.volkfm.chattskiy.model.event.EventType;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log
public class ChatWsHandler implements WebSocketHandler {
    private final ObjectMapper objectMapper;
    private final Map<EventType, EventHandler> eventHandlerMap = new HashMap<>(); // ToDo: make it more configurable

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("New session begins");
        var eventPipeline = session.receive()
                .flatMap(this::handleWsEvent)
                .map(object -> session.textMessage(objectMapper.writeValueAsString(object)))
                .doFinally(sig -> log.info("sig_type: %s, That's it folks!".formatted(sig.name()))); // ToDo: basic flow - store to cassandra, return ack

        Flux<WebSocketMessage> ping = getPingFlux(session, Duration.ofSeconds(30));

        // Stub for now
        Flux<WebSocketMessage> outsideEvents = Flux.range(0, 10).delayElements(Duration.ofSeconds(2))
                .map(i -> session.textMessage("side event " + i));

        return session.send(Flux.merge(ping, eventPipeline, outsideEvents)
                .doOnError(t -> log.info(t.toString()))
                .doFinally(sig -> log.info("sig_type: %s, sess ended".formatted(sig.name()))));
    }

    protected Event getEventFromWsMessage(WebSocketMessage wsMsg) {
            return objectMapper.readValue(wsMsg.getPayloadAsText(), Event.class);
    }

    protected Flux<Object> handleWsEvent(WebSocketMessage wsMsg) {
        log.info("ws_type: %s, raw_data: %s".formatted(wsMsg.getType().name(), wsMsg.getPayloadAsText()));

        switch (wsMsg.getType()) {
            case WebSocketMessage.Type.TEXT -> {
                Event event = getEventFromWsMessage(wsMsg);
                // eventHandlerMap.get(event.getType()).handle(event);
                return Flux.just(event);
            }
            case WebSocketMessage.Type.PONG -> {
                log.info("ws_type: %s, PING PONG".formatted(wsMsg.getType().name()));
                // ToDo: Add ping pong handling
            }
        }

        return Flux.empty(); // Stub
    }

    protected Flux<WebSocketMessage> getPingFlux(WebSocketSession session, Duration pingInterval) {
        return Flux.interval(pingInterval)
                .map(i -> session.pingMessage(f -> {
                    log.info("Sending ping");
                    return f.wrap(new byte[0]);
                }));
    }
}
