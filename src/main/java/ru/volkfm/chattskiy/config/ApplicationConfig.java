package ru.volkfm.chattskiy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.volkfm.chattskiy.generator.SnowflakeIdGenerator;
import ru.volkfm.chattskiy.handler.event.EventHandler;
import ru.volkfm.chattskiy.model.event.EventType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.volkfm.chattskiy.handler.WebSocketChatHandler.BEAN_CHAT_WS_EVENT_HANDLER_MAP;

@Configuration
public class ApplicationConfig {
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(ApplicationProperties props) {
        return new SnowflakeIdGenerator((long) props.nodeId.hashCode());
    }

    @Bean(BEAN_CHAT_WS_EVENT_HANDLER_MAP)
    public Map<EventType, EventHandler> eventHandlerMap(List<EventHandler> messageEventHandler) {
        return messageEventHandler.stream()
                .collect(Collectors.toMap(EventHandler::getEventType, eventHandler -> eventHandler));
    }
}
