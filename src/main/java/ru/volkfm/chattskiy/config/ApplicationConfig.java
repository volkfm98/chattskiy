package ru.volkfm.chattskiy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.volkfm.chattskiy.generator.SnowflakeIdGenerator;
import ru.volkfm.chattskiy.handler.event.EventHandler;
import ru.volkfm.chattskiy.handler.event.MessageEventHandler;
import ru.volkfm.chattskiy.model.event.EventType;

import java.util.HashMap;
import java.util.Map;

import static ru.volkfm.chattskiy.handler.ChatWsHandler.BEAN_CHAT_WS_EVENT_HANDLER_MAP;

@Configuration
@ConfigurationProperties(prefix = "app")
@RequiredArgsConstructor
public class ApplicationConfig {
    public final String nodeId;

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator((long) nodeId.hashCode());
    }

    @Bean(BEAN_CHAT_WS_EVENT_HANDLER_MAP)
    public Map<EventType, EventHandler> eventHandlerMap(MessageEventHandler messageEventHandler) {
        var eventHandlerMap = new HashMap<EventType, EventHandler>();
        eventHandlerMap.put(EventType.MESSAGE, messageEventHandler);

        return eventHandlerMap;
    }

}
