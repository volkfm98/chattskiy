CREATE KEYSPACE IF NOT EXISTS chattskiy WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : '1' };

CREATE TABLE IF NOT EXISTS chattskiy.message (
    chat_id UUID,
    message_id BIGINT,
    user_id UUID,
    content TEXT,
    created_at TIMESTAMP,
    PRIMARY KEY ( chat_id, message_id )
)
