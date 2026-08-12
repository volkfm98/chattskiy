package ru.volkfm.chattskiy.handler.event.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.generator.LongIdGenerator;
import ru.volkfm.chattskiy.handler.event.EventHandler;
import ru.volkfm.chattskiy.mapper.MessageEventToEntityMapper;
import ru.volkfm.chattskiy.model.event.*;
import ru.volkfm.chattskiy.model.data.cassandra.Message;
import ru.volkfm.chattskiy.repository.cassandra.MessageRepository;
import ru.volkfm.chattskiy.service.eventpublishing.RedisEventPublishingService;
import ru.volkfm.chattskiy.util.logging.StructuredLog;

import static ru.volkfm.chattskiy.util.logging.StructuredLog.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageEventHandler implements EventHandler {
    private final RedisEventPublishingService eventPublisher;
    private final MessageRepository messageRepository;
    private final LongIdGenerator longIdGenerator;
    private final MessageEventToEntityMapper messageMapper;

    @Override
    public Flux<Event> handle(Event event) {
        var messageEvent = (MessageEvent) event;

        // ToDo: validate event. Whether user actually can send messages this chat

        Message message = messageMapper.convert(messageEvent);

        if (message == null || message.getKey() == null) {
            // Normally this shouldn't be happening. That means messageMapper malfunctioned.
            throw new MessageEventConversionException();
        }

        message.getKey().setMessageId(longIdGenerator.generateId());

        return Flux.deferContextual(ctx -> {
            log.atDebug()
                    .addKeyValue(SESSION_ID_KEY, ctx.<String>get(SESSION_ID_KEY))
                    .addKeyValue(USER_ID_KEY, ctx.<String>get(USER_ID_KEY))
                    .addKeyValue(TRACE_ID_KEY, event.getEventId().toString())
                    .addKeyValue(OBJECT_KEY, StructuredLog.object(message))
                    .log("Saving message {} from chat {}", message.getKey().getMessageId(), message.getKey().getChatId());
            return messageRepository.save(message)
                .doOnNext(_ -> eventPublisher.publish(messageEvent))
                .thenReturn((Event) new AckEvent(event.getEventId()))
                .onErrorResume(t -> {
                    log.atError()
                            .setCause(t)
                            .addKeyValue(SESSION_ID_KEY, ctx.<String>get(SESSION_ID_KEY))
                            .addKeyValue(USER_ID_KEY, ctx.<String>get(USER_ID_KEY))
                            .addKeyValue(TRACE_ID_KEY, event.getEventId().toString())
                            .log("Could not persist message");

                    return Mono.just(new ErrorEvent(event.getEventId(), "500-1", "Couldn't persist the message"));
                });
        });
    }

    @Override
    public EventType getEventType() {
        return EventType.MESSAGE;
    }
}
