package ru.volkfm.chattskiy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
@RequiredArgsConstructor
public class ApplicationProperties {
    public final String nodeId;
    public final RedisConfig redis;

    @RequiredArgsConstructor
    public static class RedisConfig {
        public final String[] channels;
        public final Duration ttl;
    }
}
