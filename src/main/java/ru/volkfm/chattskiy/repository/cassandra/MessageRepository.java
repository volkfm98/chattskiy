package ru.volkfm.chattskiy.repository.cassandra;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import ru.volkfm.chattskiy.model.event.cassandra.Message;


@Repository
public interface MessageRepository extends ReactiveCassandraRepository<Message, Message.Key> {
    
}
