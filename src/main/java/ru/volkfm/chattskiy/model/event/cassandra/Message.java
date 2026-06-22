package ru.volkfm.chattskiy.model.event.cassandra;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table
@Data
public class Message {
    @PrimaryKey
    private Key key;
    private UUID userId;
    private String content;
    @CreatedDate
    private Instant createdAt;

    @PrimaryKeyClass
    @Data
    public static class Key {
        @PrimaryKeyColumn(name = "chat_id", type= PrimaryKeyType.PARTITIONED)
        private UUID chatId;
        @PrimaryKeyColumn(name = "message_id", type= PrimaryKeyType.CLUSTERED)
        private Long messageId;
    }
}
