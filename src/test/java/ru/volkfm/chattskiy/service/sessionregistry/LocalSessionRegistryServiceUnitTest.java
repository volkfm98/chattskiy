package ru.volkfm.chattskiy.service.sessionregistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.volkfm.chattskiy.Tags.TAG_UNIT;

@Tag(TAG_UNIT)
public class LocalSessionRegistryServiceUnitTest {
    private LocalSessionRegistryService registry;

    @BeforeEach
    public void setup() {
        registry = new LocalSessionRegistryService();
    }

    @Test
    @DisplayName("Simple insertion and retrieval")
    public void simpleInsertGet() {
        UUID userId = UUID.randomUUID();
        LocalSession session = new LocalSession(userId, "test");

        registry.register(session);
        assertEquals(session, registry.getSession(userId, "test"));
    }

    @Test
    public void multipleUsers() {
        // ToDo: Replace all the actual values with random ones
        List<LocalSession> sessions = populateRegistry(100, 1);

        sessions.forEach(session -> assertEquals(session,
                registry.getSession(session.getUserId(), session.getSessionId())));
    }

    @Test
    public void multipleSessions() {
        List<LocalSession> sessions = populateRegistry(1, 100);

        sessions.forEach(session -> assertEquals(session,
                registry.getSession(session.getUserId(), session.getSessionId())));
    }

    @Test
    public void multipleUsersWithMultipleSessions() {
        List<LocalSession> sessions = populateRegistry(100, 100);

        sessions.forEach(session -> assertEquals(session,
                registry.getSession(session.getUserId(), session.getSessionId())));
    }

    @Test
    public void getAllSessionsForUser() {
        populateRegistry(5, 30);
        List<LocalSession> sessions = populateRegistry(1, 100);
        populateRegistry(10, 100);

        Map<String, LocalSession> registeredSessions = registry.getSessions(sessions.get(0).getUserId());

        sessions.forEach(session ->
                assertEquals(session, registeredSessions.get(session.getSessionId())));

        assertEquals(sessions.size(), registeredSessions.size());
    }

    @Test
    public void unregisterSession() {
        populateRegistry(3, 27);
        List<LocalSession> sessions = populateRegistry(1, 100);
        populateRegistry(7, 18);

        LocalSession session = sessions.get(17);

        registry.unregister(session.getUserId(), session.getSessionId());
        assertNull(registry.getSession(session.getUserId(), session.getSessionId()));
        assertEquals(sessions.size() - 1, registry.getSessions(session.getUserId()).size());
    }

    private List<LocalSession> populateRegistry(int userCount, int sessionCount) {
        List<LocalSession> sessions = new LinkedList<>();

        for (int i = 0; i < userCount; i++) {
            UUID userId = UUID.randomUUID();
            sessions.addAll(populateUser(userId, sessionCount));
        }

        return sessions;
    }

    private List<LocalSession> populateUser(UUID userId, int sessionCount) {
        List<LocalSession> sessions = new LinkedList<>();

        for (int i = 0; i < sessionCount; i++) {
            LocalSession session = new LocalSession(userId, UUID.randomUUID().toString());
            sessions.add(session);

            registry.register(session);
        }

        return sessions;
    }
}
