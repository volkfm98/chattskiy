package ru.volkfm.chattskiy.service.sessionregistry;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.config.ApplicationProperties;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static ru.volkfm.chattskiy.Tags.*;
import static ru.volkfm.chattskiy.constant.Redis.USER_KEY;

@DataRedisTest
@Import({GlobalSessionRegistryService.class})
@Tag(TAG_INTEGRATION)
@Tag(TAG_DOCKER)
public class GlobalSessionRegistryServiceIntegrationTest {
    private static final int REPETITIONS = 30;
    private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:latest"));

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private GlobalSessionRegistryService globalSessionRegistryService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getRedisHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
        registry.add("app.node-id", () -> UUID.randomUUID().toString());
    }

    @BeforeAll
    public static void beforeAll() {
        redis.start();
    }

    @AfterAll
    public static void afterAll() {
        redis.stop();
    }

    @RepeatedTest(REPETITIONS)
    @Execution(ExecutionMode.CONCURRENT)
    public void register() {
        UUID userId = UUID.randomUUID();
        String sessionId = UUID.randomUUID().toString();

        globalSessionRegistryService.register(userId, sessionId).block();

        String nodeId = redisTemplate.<String, String>opsForHash().get(USER_KEY + ":" + userId, sessionId);

        assertEquals(applicationProperties.nodeId, nodeId);

        Duration ttl = getSessionTtl(userId, sessionId);

        assertThat(ttl).isCloseTo(applicationProperties.redis.ttl, Duration.ofSeconds(2));
    }

    @RepeatedTest(REPETITIONS)
    @Execution(ExecutionMode.CONCURRENT)
    public void unregister() {
        UUID userId = UUID.randomUUID();
        String sessionId = UUID.randomUUID().toString();

        globalSessionRegistryService.unregister(userId, sessionId).block();

        String nodeId = redisTemplate.<String, String>opsForHash().get(USER_KEY + ":" + userId, sessionId);

        assertNull(nodeId);
    }

    @RepeatedTest(REPETITIONS)
    @Execution(ExecutionMode.CONCURRENT)
    public void getNodes() {
        List<String> nodeIds = generateNodeIds(10);

        generateUsers(5, 10, nodeIds);

        UUID userId = UUID.randomUUID();
        Map<String, String> sessions = generateUserSessions(userId, 10, nodeIds);
        Set<String> expectedNodes = new HashSet<>(sessions.values());

        List<String> nodes = globalSessionRegistryService.getNodes(userId).collectList().block();

        assertNotNull(nodes);
        assertEquals(expectedNodes.size(), nodes.size());

        for (String nodeId : expectedNodes) {
            assertTrue(nodes.contains(nodeId));
        }
    }

    @RepeatedTest(REPETITIONS)
    @Execution(ExecutionMode.CONCURRENT)
    @Tag(TAG_SLOW)
    public void renew() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        String sessionId = UUID.randomUUID().toString();

        Duration ttl = applicationProperties.redis.ttl;
        Duration delta = ttl.dividedBy(5);

        globalSessionRegistryService.register(userId, sessionId)
                .then(Mono.fromCallable(() -> {
                    Duration actual = getSessionTtl(userId, sessionId);
                    assertThat(actual).isCloseTo(ttl, delta);
                    return true;
                }))
                .delayElement(ttl.minus(delta))
                .then(globalSessionRegistryService.renew(userId, sessionId))
                .then(Mono.fromCallable(() -> {
                    Duration actual = getSessionTtl(userId, sessionId);
                    assertThat(actual).isCloseTo(ttl, delta);
                    return true;
                }))
                .delayElement(ttl)
                .then(Mono.fromCallable(() -> {
                    String expiredSession = redisTemplate.<String, String>opsForHash().get(USER_KEY + ":" + userId, sessionId);
                    assertNull(expiredSession);
                    return true;
                }))
        .block();
    }

    private Map<String, String> generateUserSessions(UUID userId, int sessionCount, List<String> nodeIds) {
        Map<String, String> userSessions = new HashMap<>();

        Random random = new Random();
        random.setSeed(System.currentTimeMillis());

        for (int i = 0; i < sessionCount; i++) {
            String nodeId = nodeIds.get(random.nextInt(nodeIds.size()));
            String sessionId = UUID.randomUUID().toString();

            redisTemplate.<String, String>opsForHash().put(USER_KEY + ":" + userId, sessionId, nodeId);

            userSessions.put(sessionId, nodeId);
        }

        return userSessions;
    }

    private List<String> generateNodeIds(int nodeCount) {
        List<String> nodeIds = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodeIds.add(UUID.randomUUID().toString());
        }

        return nodeIds;
    }

    private Map<UUID, Map<String, String>> generateUsers(int userCount, int sessionCount, List<String> nodeIds) {
        Map<UUID, Map<String, String>> userSessions = new HashMap<>();

        for (int i = 0; i < userCount; i++) {
            UUID userId = UUID.randomUUID();
            Map<String, String> sessions = generateUserSessions(userId, sessionCount, nodeIds);
            userSessions.put(userId, sessions);
        }

        return userSessions;
    }

    private Duration getSessionTtl(UUID userId, String sessionId) {
        return redisTemplate.<String, String>opsForHash()
                .getTimeToLive(USER_KEY + ":" + userId, Collections.singleton(sessionId))
                .ttlOf(sessionId);
    }
}
