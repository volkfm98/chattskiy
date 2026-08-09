package ru.volkfm.chattskiy.service.sessionregistry;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.volkfm.chattskiy.config.ApplicationProperties;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
public class TestConfig {
}
