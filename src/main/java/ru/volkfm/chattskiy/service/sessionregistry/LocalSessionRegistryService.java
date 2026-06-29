package ru.volkfm.chattskiy.service.sessionregistry;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalSessionRegistryService {
    private final Map<UUID, Map<String, WebSocketHandler>> sessionRegistry = new ConcurrentHashMap<>();

    public void register(UUID userId, WebSocketSession session, WebSocketHandler handler) {
        sessionRegistry.putIfAbsent(userId, new ConcurrentHashMap<>());
        sessionRegistry.get(userId).put(session.getId(), handler);
    }

    public void unregister(UUID userId, WebSocketSession session) {
        sessionRegistry.get(userId).remove(session.getId());
    }

    public WebSocketHandler getSession(UUID userId, WebSocketSession session) {
        return sessionRegistry.get(userId).get(session.getId());
    }

    public Map<String, WebSocketHandler> getSessions(UUID userId) {
        return sessionRegistry.get(userId);
    }
}
