package com.markethub.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiDocChunk {
    private Long id;
    private String source;
    private String kind;
    private String heading;
    private Integer chunkIndex;
    private String content;
    private String embedding;
    private LocalDateTime createTime;
}
