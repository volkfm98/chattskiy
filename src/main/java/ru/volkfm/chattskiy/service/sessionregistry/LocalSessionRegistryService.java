package ru.volkfm.chattskiy.service.sessionregistry;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalSessionRegistryService {
    private final Map<UUID, Map<String, LocalSession>> sessionRegistry = new ConcurrentHashMap<>();

    public void register(UUID userId, String sessionId, LocalSession session) {
        sessionRegistry.computeIfAbsent(userId, _ -> new ConcurrentHashMap<>()).put(sessionId, session);
    }

    public void unregister(UUID userId, String sessionId) {
        sessionRegistry.get(userId).remove(sessionId);
    }

    public LocalSession getSession(UUID userId, String sessionId) {
        return sessionRegistry.getOrDefault(userId, new ConcurrentHashMap<>()).get(sessionId);
    }

    public Map<String, LocalSession> getSessions(UUID userId) {
        return sessionRegistry.getOrDefault(userId, new ConcurrentHashMap<>());
    }
}
