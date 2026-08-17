package ru.volkfm.chattskiy.service.sessionregistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.config.ApplicationProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import static ru.volkfm.chattskiy.constant.Redis.USER_KEY;
import static ru.volkfm.chattskiy.util.logging.StructuredLog.SESSION_ID_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalSessionRegistryService {
    // Store in Redis for each user a hash with (session -> nodeId, TTL).

    private final ApplicationProperties appProps;
    private final ReactiveStringRedisTemplate redisTemplate;

    // Add to redis hash entry with TTL
    public Mono<Void> register(UUID userId, String sessionId) {
        String key = getUserKey(userId);

        log.atDebug()
                .addKeyValue(USER_KEY, userId)
                .addKeyValue(SESSION_ID_KEY, sessionId)
                .log("Registering session {} for user {} in global registry", sessionId, userId);

        return redisTemplate.<String, String>opsForHash().put(key, sessionId, appProps.nodeId)
                .flatMap(_ -> redisTemplate.<String, String>opsForHash()
                        .expire(key, appProps.redis.ttl, Collections.singleton(sessionId)))
                .then();
    }

    public Mono<Long> unregister(UUID userId, String sessionId) {
        log.atDebug()
                .addKeyValue(USER_KEY, userId)
                .addKeyValue(SESSION_ID_KEY, sessionId)
                .log("Unregistering session {} for user {} in global registry", sessionId, userId);

        return redisTemplate.<String, String>opsForHash().remove(getUserKey(userId), sessionId);
    }

    public Flux<String> getNodes(UUID userId) {
        return redisTemplate.<String, String>opsForHash().values(getUserKey(userId)).distinct();
    }

    public Mono<Void> renew(UUID userId, String sessionId) {
        return redisTemplate.<String, String>opsForHash()
                .getTimeToLive(getUserKey(userId), Collections.singleton(sessionId))
                .filter(exp -> {
                    Duration remainingTtl = exp.ttlOf(sessionId);
                    return remainingTtl != null && appProps.redis.ttl.dividedBy(3).compareTo(remainingTtl) >= 0;
                })
                .doOnNext(_ -> log.atDebug()
                        .addKeyValue(USER_KEY, userId)
                        .addKeyValue(SESSION_ID_KEY, sessionId)
                        .log("Renewing session id {}", sessionId))
                .flatMap(_ -> redisTemplate.opsForHash()
                        .expire(getUserKey(userId), appProps.redis.ttl, Collections.singleton(sessionId)))
                .then();
    }

    private String getUserKey(UUID userId) {
        return USER_KEY + ":" + userId.toString();
    }
}
