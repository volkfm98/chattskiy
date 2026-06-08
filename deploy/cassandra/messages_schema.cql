CREATE KEYSPACE IF NOT EXISTS chattskiy WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : '1' };

CREATE TABLE IF NOT EXISTS chattskiy.message (
    chat_id UUID,
    message_id BIGINT,
    sender UUID,
    content TEXT,
    batch_counter INT,
    created_at TIMESTAMP,
    PRIMARY KEY ( chat_id, message_id )
)
