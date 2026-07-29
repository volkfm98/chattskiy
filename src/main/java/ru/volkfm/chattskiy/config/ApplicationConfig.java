package ru.volkfm.chattskiy.config;

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
public class ApplicationConfig {
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(1L);
    }

    @Bean(BEAN_CHAT_WS_EVENT_HANDLER_MAP)
    public Map<EventType, EventHandler> eventHandlerMap(MessageEventHandler messageEventHandler) {
        var eventHandlerMap = new HashMap<EventType, EventHandler>();

        return eventHandlerMap;
    }

}
