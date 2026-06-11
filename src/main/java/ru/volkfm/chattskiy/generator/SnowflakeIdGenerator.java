package ru.volkfm.chattskiy.generator;


import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class SnowflakeIdGenerator {
    protected static final int TIMESTAMP_LENGTH = 41;
    protected static final int NODE_ID_LENGTH = 10;
    protected static final int SEQUENCE_ID_LENGTH = 12;

    private final Long nodeId;
    private final AtomicLong sequenceId = new AtomicLong(0);

    private Long lastTimestamp = System.currentTimeMillis();

    public synchronized Long generateId() {
        Long timestamp = System.currentTimeMillis();

        if (lastTimestamp < timestamp) {
            sequenceId.set(0L);
            lastTimestamp = timestamp;
        }

        Long normalizedTimestamp = fitIntoNBits(timestamp, TIMESTAMP_LENGTH);
        Long normalizedNodeId = fitIntoNBits(nodeId, NODE_ID_LENGTH);
        Long normalizedSequenceId = fitIntoNBits(sequenceId.getAndIncrement(), SEQUENCE_ID_LENGTH);

        return normalizedTimestamp << NODE_ID_LENGTH | normalizedNodeId << SEQUENCE_ID_LENGTH | normalizedSequenceId;
    }

    protected Long trimFirstNBits(Long value, int n) {
        return value << n >> n;
    }

    protected Long fitIntoNBits(Long value, int n) {
        return trimFirstNBits(value, Long.SIZE - n);
    }
}
