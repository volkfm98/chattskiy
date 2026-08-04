package ru.volkfm.chattskiy.service.sessionregistry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static ru.volkfm.chattskiy.util.logging.StructuredLog.SESSION_ID_KEY;
import static ru.volkfm.chattskiy.util.logging.StructuredLog.USER_ID_KEY;

@Service
@Slf4j
public class LocalSessionRegistryService {
    private final Map<UUID, Map<String, LocalSession>> sessionRegistry = new ConcurrentHashMap<>();

    public void register(LocalSession session) {
        log.atDebug()
                .addKeyValue(USER_ID_KEY, session.getUserId())
                .addKeyValue(SESSION_ID_KEY, session.getSessionId())
                .log("Registering session {} for user {} in local registry", session.getSessionId(), session.getUserId());

        sessionRegistry.computeIfAbsent(session.getUserId(), _ -> new ConcurrentHashMap<>()).put(session.getSessionId(), session);
    }

    public void unregister(UUID userId, String sessionId) {
        log.atDebug()
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue(SESSION_ID_KEY, sessionId)
                .log("Unregistering session {} for user {} in local registry", sessionId, userId);

        sessionRegistry.get(userId).remove(sessionId);
    }

    public LocalSession getSession(UUID userId, String sessionId) {
        return sessionRegistry.getOrDefault(userId, new ConcurrentHashMap<>()).get(sessionId);
    }

    public Map<String, LocalSession> getSessions(UUID userId) {
        return sessionRegistry.getOrDefault(userId, new ConcurrentHashMap<>());
    }
}
