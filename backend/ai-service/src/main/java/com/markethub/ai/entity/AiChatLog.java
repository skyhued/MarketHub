package com.markethub.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatLog {
    private Long id;
    private Integer userId;
    private String question;
    private String answer;
    private String sources;
    private Integer latencyMs;
    private LocalDateTime createTime;
}
