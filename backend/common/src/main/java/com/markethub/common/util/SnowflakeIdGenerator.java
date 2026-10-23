package com.markethub.common.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** 雪花 ID：41 位时间戳 + 10 位机器 ID + 12 位序列号。 */
public class SnowflakeIdGenerator {

    private final long twepoch = 1704067200000L;

    private final long workerIdBits = 10L;

    private final long maxWorkerId = ~(-1L << workerIdBits);

    private final long sequenceBits = 12L;

    private final long workerIdShift = sequenceBits;

    private final long timestampLeftShift = sequenceBits + workerIdBits;

    private final long sequenceMask = ~(-1L << sequenceBits);

    private long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker ID must be between 0 and %d", maxWorkerId));
        }
        this.workerId = workerId;
    }

    public static SnowflakeIdGenerator fromLocalAddress() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            byte[] ipBytes = addr.getAddress();
            long workerId = (ipBytes[ipBytes.length - 1] & 0xFF) % 1024;
            return new SnowflakeIdGenerator(workerId);
        } catch (UnknownHostException e) {
            return new SnowflakeIdGenerator(1L);
        }
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards, refusing to generate id for %d ms", lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}
