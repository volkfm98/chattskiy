package ru.volkfm.chattskiy.service.sessionregistry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionRegistryService {
    private final GlobalSessionRegistryService globalSessionRegistryService;
    private final LocalSessionRegistryService localSessionRegistryService;

    public Mono<String> register(UUID userId, WebSocketHandler handler) {
        var sessionId = UUID.randomUUID().toString();

        localSessionRegistryService.register(userId, sessionId, handler);
        return globalSessionRegistryService.register(userId, sessionId).thenReturn(sessionId);
    }

    public Mono<Long> unregister(UUID userId, String sessionId) {
        localSessionRegistryService.unregister(userId, sessionId);
        return globalSessionRegistryService.unregister(userId, sessionId);
    }

    public Flux<String> getNodes(UUID userId) {
        return globalSessionRegistryService.getNodes(userId);
    }

    public WebSocketHandler getSession(UUID userId, String sessionId) {
        return localSessionRegistryService.getSession(userId, sessionId);
    }

    public Map<String, WebSocketHandler> getSessions(UUID userId) {
        return localSessionRegistryService.getSessions(userId);
    }
}
