package com.markethub.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AskResponse {
    private String answer;
    private long latencyMs;
}
