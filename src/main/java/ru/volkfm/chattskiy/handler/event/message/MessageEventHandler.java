package ru.volkfm.chattskiy.handler.event.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.generator.LongIdGenerator;
import ru.volkfm.chattskiy.handler.event.EventHandler;
import ru.volkfm.chattskiy.mapper.MessageEventToEntityMapper;
import ru.volkfm.chattskiy.model.event.AckEvent;
import ru.volkfm.chattskiy.model.event.Event;
import ru.volkfm.chattskiy.model.event.EventType;
import ru.volkfm.chattskiy.model.event.MessageEvent;
import ru.volkfm.chattskiy.model.data.cassandra.Message;
import ru.volkfm.chattskiy.repository.cassandra.MessageRepository;
import ru.volkfm.chattskiy.service.eventpublishing.RedisEventPublishingService;

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

        Message message = messageMapper.convert(messageEvent);

        if (message == null || message.getKey() == null) {
            // Normally this shouldn't be happening. That means messageMapper malfunctioned.
            throw new MessageEventConversionException();
        }

        message.getKey().setMessageId(longIdGenerator.generateId());

        return Mono.defer(() -> {
            log.debug("Saving message {}", message); // ToDo: log
            return messageRepository.save(message);
        })
                .doOnError(t -> log.error("Could not persist message", t))
                .doOnNext(_ -> eventPublisher.publish(messageEvent))
                .thenReturn((Event) new AckEvent(event.getEventId()))
                .flux();
    }

    @Override
    public EventType getEventType() {
        return EventType.MESSAGE;
    }
}
