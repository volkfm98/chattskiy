package ru.volkfm.chattskiy.handler.event;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import ru.volkfm.chattskiy.generator.LongIdGenerator;
import ru.volkfm.chattskiy.mapper.MessageEventToEntityMapper;
import ru.volkfm.chattskiy.model.event.AckEvent;
import ru.volkfm.chattskiy.model.event.Event;
import ru.volkfm.chattskiy.model.event.MessageEvent;
import ru.volkfm.chattskiy.model.event.cassandra.Message;
import ru.volkfm.chattskiy.repository.cassandra.MessageRepository;
import ru.volkfm.chattskiy.service.eventpublishing.RedisEventPublishingService;

@RequiredArgsConstructor
public class MessageEventHandler implements EventHandler {
    private final RedisEventPublishingService eventPublisher;
    private final MessageRepository messageRepository;
    private final LongIdGenerator longIdGenerator;
    private final MessageEventToEntityMapper messageMapper;

    @Override
    public Flux<Event> handle(Event event) {
        // ToDo: basic flow - store to cassandra, return ack
        var messageEvent = (MessageEvent) event;
        Message message = messageMapper.convert(messageEvent);

        if (message == null || message.getKey() == null) {
            // ToDo: throw error
            throw new RuntimeException();
        }

        message.getKey().setMessageId(longIdGenerator.generateId());

        return messageRepository.save(message)
                .flatMap(_ -> eventPublisher.publish(messageEvent))
                .thenReturn((Event) new AckEvent(event.getEventId()))
                .flux();
    }
}
