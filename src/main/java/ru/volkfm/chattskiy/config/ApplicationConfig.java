package ru.volkfm.chattskiy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.volkfm.chattskiy.generator.SnowflakeIdGenerator;

@Configuration
public class ApplicationConfig {
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(1L);
    }
}
