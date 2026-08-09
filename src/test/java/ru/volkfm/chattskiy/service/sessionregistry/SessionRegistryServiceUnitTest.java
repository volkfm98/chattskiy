package ru.volkfm.chattskiy.service.sessionregistry;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static ru.volkfm.chattskiy.Tags.TAG_UNIT;

@Tag(TAG_UNIT)
public class SessionRegistryServiceUnitTest {
    private GlobalSessionRegistryService globalSessionRegistryService = mock();
    private LocalSessionRegistryService localSessionRegistryService = mock();

    private SessionRegistryService registry = new SessionRegistryService(globalSessionRegistryService, localSessionRegistryService);

    @Test
    public void registerSession() {
        UUID userId = UUID.randomUUID();

        when(globalSessionRegistryService.register(Mockito.eq(userId), Mockito.anyString()))
                .thenReturn(Mono.empty());

        LocalSession session = registry.register(userId).block();

        verify(globalSessionRegistryService).register(userId, session.getSessionId());
        verify(localSessionRegistryService).register(session);
    }

    @Test
    public void unregisterSession() {
        UUID userId = UUID.randomUUID();
        String sessionId = UUID.randomUUID().toString();

        registry.unregister(userId, sessionId);

        verify(localSessionRegistryService).unregister(userId, sessionId);
        verify(globalSessionRegistryService).unregister(userId, sessionId);
    }

    @Test
    public void getNodes() {
        UUID userId = UUID.randomUUID();
        registry.getNodes(userId);
        verify(globalSessionRegistryService).getNodes(userId);
    }

    @Test
    public void getSession() {
        UUID userId = UUID.randomUUID();
        String sessionId = UUID.randomUUID().toString();
        registry.getSession(userId, sessionId);
        verify(localSessionRegistryService).getSession(userId, sessionId);
    }

    @Test
    public void getSessions() {
        UUID userId = UUID.randomUUID();
        registry.getSessions(userId);
        verify(localSessionRegistryService).getSessions(userId);
    }

    @Test
    public void renew() {
        UUID userId = UUID.randomUUID();
        String sessionId = UUID.randomUUID().toString();

        LocalSession localSession = new LocalSession(userId, sessionId);

        registry.renew(localSession);
        verify(globalSessionRegistryService).renew(localSession.getUserId(), localSession.getSessionId());
    }
}
