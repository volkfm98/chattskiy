package ru.volkfm.chattskiy.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.handler.WebSocketChatHandler;
import ru.volkfm.chattskiy.service.R2dbUserDetailsService;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig {
    public static final String INCOMING_MESSAGE_EVENT_ENDPOINT_KEY = "/chat";
    public static final String ATTRIBUTE_USER_ID_KEY = "principalUserId";

    public final WebSocketChatHandler webSocketChatHandler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put(INCOMING_MESSAGE_EVENT_ENDPOINT_KEY, webSocketChatHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);
        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        // A workaround to store principal data and use it later in WebSocketHandler
        var wsService = new HandshakeWebSocketService() {
            @Override
            public Mono<Void> handleRequest(
                    ServerWebExchange exchange,
                    WebSocketHandler handler) {

                return exchange.getPrincipal()
                        .cast(Authentication.class)
                        .map(Authentication::getPrincipal)
                        .cast(R2dbUserDetailsService.DbUser.class)
                        .map(principal -> principal.getDbData().getId())
                        .flatMap(p -> exchange.getSession()
                                .doOnNext(session -> session.getAttributes().put(ATTRIBUTE_USER_ID_KEY, p))
                        )
                        .then(super.handleRequest(exchange, handler));
            }
        };

        wsService.setSessionAttributePredicate(ATTRIBUTE_USER_ID_KEY::equals);

        return new WebSocketHandlerAdapter(wsService);
    }
}
