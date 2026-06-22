package ru.volkfm.chattskiy.handler.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import ru.volkfm.chattskiy.generator.LongIdGenerator;
import ru.volkfm.chattskiy.mapper.MessageEventToEntityMapper;
import ru.volkfm.chattskiy.model.event.Event;
import ru.volkfm.chattskiy.model.event.MessageEvent;
import ru.volkfm.chattskiy.model.event.cassandra.Message;
import ru.volkfm.chattskiy.repository.cassandra.MessageRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class MessageEventHandler implements EventHandler {
    public final String KAFKA_TOPIC = "events";

    private final MessageRepository messageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
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

        messageRepository.save(message)
                .map(objectMapper::writeValueAsString)
                .map(messageString -> {
                    Set<Integer> partitions = new HashSet<>(); // ToDo: get from redis
                    kafkaTemplate.send(KAFKA_TOPIC, messageString);
                });

        return Flux.empty();
    }
}
