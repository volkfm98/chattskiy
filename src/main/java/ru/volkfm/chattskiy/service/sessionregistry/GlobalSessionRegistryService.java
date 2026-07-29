package ru.volkfm.chattskiy.service.sessionregistry;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.config.ApplicationConfig;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import static ru.volkfm.chattskiy.constant.Redis.USER_KEY;

@Service
@RequiredArgsConstructor
public class GlobalSessionRegistryService {
    // Store in Redis for each user a hash with (session -> nodeId, TTL).
    // ToDo: Introduce interface, adjust return types

    private final ApplicationConfig appConfig;
    private final ReactiveStringRedisTemplate redisTemplate;

    // Add to redis hash entry with TTL
    public Mono<Void> register(UUID userId, String sessionId) {
        String key = getUserKey(userId);

        return redisTemplate.<String, String>opsForHash().put(key, sessionId, appConfig.nodeId)
                .flatMap(_ -> redisTemplate.<String, String>opsForHash()
                        .expire(key, Duration.ofSeconds(30), Collections.singleton(sessionId))
                .then()// ToDo: Adjust TTL
        );
    }

    public Mono<Long> unregister(UUID userId, String sessionId) {
        return redisTemplate.<String, String>opsForHash().remove(getUserKey(userId), sessionId);
    }

    public Flux<String> getNodes(UUID userId) {
        return redisTemplate.<String, String>opsForHash().values(getUserKey(userId)).distinct();
    }

    private String getUserKey(UUID userId) {
        return USER_KEY + ":" + userId.toString();
    }
}
