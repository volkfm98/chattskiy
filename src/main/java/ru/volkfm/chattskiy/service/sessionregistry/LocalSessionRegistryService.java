package ru.volkfm.chattskiy.service.sessionregistry;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalSessionRegistryService {
    private final Map<UUID, Map<String, WebSocketHandler>> sessionRegistry = new ConcurrentHashMap<>();

    public void register(UUID userId, String sessionId, WebSocketHandler handler) {
        sessionRegistry.putIfAbsent(userId, new ConcurrentHashMap<>());
        sessionRegistry.get(userId).put(sessionId, handler);
    }

    public void unregister(UUID userId, String sessionId) {
        sessionRegistry.get(userId).remove(sessionId);
    }

    public WebSocketHandler getSession(UUID userId, String sessionId) {
        return sessionRegistry.get(userId).get(sessionId);
    }

    public Map<String, WebSocketHandler> getSessions(UUID userId) {
        return sessionRegistry.get(userId);
    }
}
