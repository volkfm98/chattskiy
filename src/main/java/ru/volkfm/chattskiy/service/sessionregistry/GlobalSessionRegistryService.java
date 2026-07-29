package ru.volkfm.chattskiy.service.sessionregistry;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GlobalSessionRegistryService {
    // Store in Redis for each user a hash with (session -> nodeId, TTL).
    // ToDo: Introduce interface, adjust return types

    public final ReactiveStringRedisTemplate redisTemplate;

    public static final String nodeId = "1"; // ToDo: Refactor

    // Add to redis hash entry with TTL
    public Mono<Void> register(UUID userId, String sessionId) {
        String key = userId.toString(); // ToDo: Adjust key

        return redisTemplate.<String, String>opsForHash().put(key, sessionId, nodeId)
                .flatMap(_ -> redisTemplate.<String, String>opsForHash()
                        .expire(key, Duration.ofSeconds(30), Collections.singleton(sessionId))
                .then()// ToDo: Adjust TTL
        );
    }

    public Mono<Long> unregister(UUID userId, String sessionId) {
        return redisTemplate.<String, String>opsForHash().remove(userId.toString(), sessionId);
    }

    public Flux<String> getNodes(UUID userId) {
        return redisTemplate.<String, String>opsForHash().values(userId.toString()).distinct();
    }
}
