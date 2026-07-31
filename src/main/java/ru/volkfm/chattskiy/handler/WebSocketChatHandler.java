package ru.volkfm.chattskiy.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.config.ApplicationProperties;
import ru.volkfm.chattskiy.handler.event.EventHandler;
import ru.volkfm.chattskiy.model.event.Event;
import ru.volkfm.chattskiy.model.event.EventType;
import ru.volkfm.chattskiy.service.sessionregistry.LocalSession;
import ru.volkfm.chattskiy.service.sessionregistry.SessionRegistryService;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static ru.volkfm.chattskiy.config.WebSocketConfig.ATTRIBUTE_USER_ID_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatHandler implements WebSocketHandler {
    public final static String BEAN_CHAT_WS_EVENT_HANDLER_MAP = "ChatWsEventHandlerMapBean";

    private final ObjectMapper objectMapper;
    private final ApplicationProperties appProps;
    private final SessionRegistryService sessionRegistryService;

    @Qualifier(BEAN_CHAT_WS_EVENT_HANDLER_MAP)
    private final Map<EventType, EventHandler> eventHandlerMap;

    @Override
    public Mono<Void> handle(WebSocketSession wsSession) {
        var userId = (UUID) wsSession.getHandshakeInfo().getAttributes().get(ATTRIBUTE_USER_ID_KEY);

        return Mono.usingWhen(sessionRegistryService.register(userId),
                userSession -> {
                        log.info("Session %s begins".formatted(userSession.getSessionId()));

                        Flux<WebSocketMessage> ping = getPingFlux(wsSession, appProps.redis.ttl.dividedBy(3));

                        Flux<WebSocketMessage> clientEvents = wsSession.receive()
                                .flatMap(m -> handleWsEvent(m, wsSession, userSession))
                                .doFinally(sig -> log.info("sig_type: %s, That's it folks!".formatted(sig.name())));

                        Flux<WebSocketMessage> outsideEvents = userSession.getOutsideSink().asFlux()
                                .doOnNext(message -> log.info("Got outside event:" + objectMapper.writeValueAsString(message)))
                                .map(event -> wsSession.textMessage(objectMapper.writeValueAsString(event)));

                        return wsSession.send(Flux.merge(ping, clientEvents, outsideEvents)
                                .doOnError(t -> log.info(t.toString()))
                                .doFinally(sig -> log.info("sig_type: %s, sess ended".formatted(sig.name())))
                        );},
                localSession -> sessionRegistryService.unregister(userId, localSession.getSessionId()));
    }

    protected Event getEventFromWsMessage(WebSocketMessage wsMsg) {
            return objectMapper.readValue(wsMsg.getPayloadAsText(), Event.class);
    }

    protected Flux<WebSocketMessage> handleWsEvent(WebSocketMessage wsMsg, WebSocketSession wsSession, LocalSession userSession) {
        log.info("ws_type: %s, raw_data: %s".formatted(wsMsg.getType().name(), wsMsg.getPayloadAsText()));

        switch (wsMsg.getType()) {
            case WebSocketMessage.Type.TEXT -> {
                Event event = getEventFromWsMessage(wsMsg);

                return delegateEventHandling(event)
                        .map(object -> wsSession.textMessage(objectMapper.writeValueAsString(object)));
            }
            case WebSocketMessage.Type.PONG -> {
                log.info("ws_type: %s, PING PONG".formatted(wsMsg.getType().name()));
                return sessionRegistryService.renew(userSession).thenMany(Flux.empty());
            }
        }

        return Flux.empty();
    }

    protected Flux<Event> delegateEventHandling(Event event) {
        return eventHandlerMap.get(event.getType()).handle(event);
    }

    protected Flux<WebSocketMessage> getPingFlux(WebSocketSession session, Duration pingInterval) {
        return Flux.interval(pingInterval)
                .map(_ -> session.pingMessage(f -> {
                    log.info("Sending ping");
                    return f.wrap(new byte[0]);
                }));
    }
}
