package ru.volkfm.chattskiy.service.sessionregistry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionRegistryService {
    private final GlobalSessionRegistryService globalSessionRegistryService;
    private final LocalSessionRegistryService localSessionRegistryService;

    public Mono<LocalSession> register(UUID userId) {
        var sessionId = UUID.randomUUID().toString();

        var session = new LocalSession(userId, sessionId);

        localSessionRegistryService.register(userId, sessionId, session);
        return globalSessionRegistryService.register(userId, sessionId).thenReturn(session);
    }

    public Mono<Long> unregister(UUID userId, String sessionId) {
        localSessionRegistryService.unregister(userId, sessionId);
        return globalSessionRegistryService.unregister(userId, sessionId);
    }

    public Flux<String> getNodes(UUID userId) {
        return globalSessionRegistryService.getNodes(userId);
    }

    public LocalSession getSession(UUID userId, String sessionId) {
        return localSessionRegistryService.getSession(userId, sessionId);
    }

    public Map<String, LocalSession> getSessions(UUID userId) {
        return localSessionRegistryService.getSessions(userId);
    }

    public Mono<Void> renew(LocalSession localSession) {
        return globalSessionRegistryService.renew(localSession.getUserId(), localSession.getSessionId());
    }
}
