package com.markethub.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** 订单号：14 位时间戳 + 3 位机器 ID + 6 位随机码。 */
public class OrderNoGenerator {

    private static final SnowflakeIdGenerator idGenerator = SnowflakeIdGenerator.fromLocalAddress();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generate() {
        return String.valueOf(idGenerator.nextId());
    }

    /** 可读订单号：14 位日期 + 3 位机器 + 8 位序列。 */
    public static String generateReadable() {
        String datePart = LocalDateTime.now().format(formatter);
        String seqPart = String.valueOf(idGenerator.nextId()).substring(5, 13);
        return datePart + seqPart;
    }

    public static String generateTransNo() {
        return "TXN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
