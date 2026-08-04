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
import reactor.util.context.Context;
import ru.volkfm.chattskiy.config.ApplicationProperties;
import ru.volkfm.chattskiy.handler.event.EventHandler;
import ru.volkfm.chattskiy.model.event.ErrorEvent;
import ru.volkfm.chattskiy.model.event.Event;
import ru.volkfm.chattskiy.model.event.EventType;
import ru.volkfm.chattskiy.model.event.PublishableEvent;
import ru.volkfm.chattskiy.service.sessionregistry.LocalSession;
import ru.volkfm.chattskiy.service.sessionregistry.SessionRegistryService;
import ru.volkfm.chattskiy.util.logging.StructuredLog;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ru.volkfm.chattskiy.config.WebSocketConfig.ATTRIBUTE_USER_ID_KEY;
import static ru.volkfm.chattskiy.util.logging.StructuredLog.*;

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
                        Map<String, String> context = new HashMap<>();
                        context.put(SESSION_ID_KEY, userSession.getSessionId());
                        context.put(USER_ID_KEY, userId.toString());

                        log.atInfo()
                                .addKeyValue(SESSION_ID_KEY, userSession.getSessionId())
                                .addKeyValue(USER_ID_KEY, userId.toString())
                                .log("Session {} begins for user {}", userSession.getSessionId(), userId.toString());

                        Flux<WebSocketMessage> ping = getPingFlux(wsSession, appProps.redis.ttl.dividedBy(3));

                        Flux<WebSocketMessage> clientEvents = wsSession.receive()
                                .flatMap(m -> handleWsEvent(m, wsSession, userSession))
                                .onErrorContinue((t, o) -> {
                                    var logBuilder = log.atError()
                                            .setCause(t)
                                            .addKeyValue(OBJECT_KEY, StructuredLog.object(o))
                                            .addKeyValue(SESSION_ID_KEY, userSession.getSessionId())
                                            .addKeyValue(USER_ID_KEY, userId.toString());

                                    if (o instanceof Event event) {
                                        logBuilder = logBuilder.addKeyValue(TRACE_ID_KEY, event.getEventId().toString());
                                    }

                                    logBuilder.log("Exception happened during handling client data in session {} for user {}", userSession.getSessionId(), userId);
                                });

                        Flux<WebSocketMessage> outsideEvents = userSession.getOutsideSink().asFlux()
                                .map(event -> wsSession.textMessage(objectMapper.writeValueAsString(event)))
                                .onErrorContinue((t, o) -> {
                                    var logBuilder = log.atError()
                                            .setCause(t)
                                            .addKeyValue(OBJECT_KEY, StructuredLog.object(o))
                                            .addKeyValue(SESSION_ID_KEY, userSession.getSessionId())
                                            .addKeyValue(USER_ID_KEY, userId.toString());

                                    if (o instanceof Event event) {
                                        logBuilder = logBuilder.addKeyValue(TRACE_ID_KEY, event.getEventId().toString());
                                    }

                                    logBuilder.log("Exception happened during handling outside event in session {} for user {}", userSession.getSessionId(), userSession.getUserId());
                                });

                        return wsSession.send(Flux.merge(ping, clientEvents, outsideEvents)
                                .contextWrite(Context.of(context))
                                .onErrorContinue((t, o) -> log.atError()
                                        .setCause(t)
                                        .addKeyValue(OBJECT_KEY, StructuredLog.object(o))
                                        .addKeyValue(SESSION_ID_KEY, userSession.getSessionId())
                                        .addKeyValue(USER_ID_KEY, userId.toString())
                                        .log("Exception happened during handling session {} for user {}", userSession.getSessionId(), userId.toString()))
                                .doOnComplete(() -> log.atInfo()
                                        .addKeyValue(SESSION_ID_KEY, userSession.getSessionId())
                                        .addKeyValue(USER_ID_KEY, userId.toString())
                                        .log("Session {} ended for user {}", userSession.getSessionId(), userId.toString()))
                        );},
                localSession -> sessionRegistryService.unregister(userId, localSession.getSessionId()));
    }

    protected Event getEventFromWsMessage(WebSocketMessage wsMsg) {
            return objectMapper.readValue(wsMsg.getPayloadAsText(), Event.class);
    }

    protected Flux<WebSocketMessage> handleWsEvent(WebSocketMessage wsMsg, WebSocketSession wsSession, LocalSession userSession) {
        switch (wsMsg.getType()) {
            case WebSocketMessage.Type.TEXT -> {
                return handleEvent(wsMsg, wsSession, userSession)
                        .map(object -> wsSession.textMessage(objectMapper.writeValueAsString(object)));
            }
            case WebSocketMessage.Type.PONG -> {
                return sessionRegistryService.renew(userSession).thenMany(Flux.empty());
            }
        }

        return Flux.empty();
    }

    protected Flux<Event> handleEvent(WebSocketMessage wsMsg, WebSocketSession wsSession, LocalSession userSession) {
        try {
            Event event = getEventFromWsMessage(wsMsg);

            if (event instanceof PublishableEvent) {
                ((PublishableEvent) event).setUserId(userSession.getUserId());
            }

            log.atDebug()
                    .addKeyValue(TRACE_ID_KEY, event.getEventId().toString())
                    .addKeyValue(SESSION_ID_KEY, userSession.getSessionId())
                    .addKeyValue(USER_ID_KEY, userSession.getUserId().toString())
                    .addKeyValue(OBJECT_KEY, event)
                    .log("Incoming event {} during session {} for user {}", event.getEventId().toString(), userSession.getSessionId(), userSession.getUserId().toString());

            return delegateEventHandling(event);
        } catch (JacksonException e) {
            return Flux.just(new ErrorEvent(null, "400", "Error happened during user data parsing"));
        }
    }

    protected Flux<Event> delegateEventHandling(Event event) {
        return eventHandlerMap.get(event.getType()).handle(event);
    }

    protected Flux<WebSocketMessage> getPingFlux(WebSocketSession session, Duration pingInterval) {
        return Flux.interval(pingInterval)
                .map(_ -> session.pingMessage(f -> f.wrap(new byte[0])));
    }
}
