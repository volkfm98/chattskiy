package ru.volkfm.chattskiy.model.data.cassandra;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.time.Instant;
import java.util.UUID;

@Table
@Data
public class Message {
    @PrimaryKey
    private Key key;
    @Column("user_id")
    private UUID userId;
    private String content;
    @CreatedDate
    @Column("created_at")
    private Instant createdAt = Instant.now();

    @PrimaryKeyClass
    @Data
    public static class Key {
        @PrimaryKeyColumn(name = "chat_id", type= PrimaryKeyType.PARTITIONED)
        private UUID chatId;
        @PrimaryKeyColumn(name = "message_id", type= PrimaryKeyType.CLUSTERED)
        private Long messageId;
    }
}
