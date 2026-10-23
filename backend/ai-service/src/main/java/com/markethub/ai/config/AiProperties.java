package com.markethub.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private Ollama ollama = new Ollama();
    private Knowledge knowledge = new Knowledge();
    private Chat chat = new Chat();

    @Data
    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String chatModel = "qwen2.5:7b";
        private String embeddingModel = "bge-m3";
        private int connectTimeoutSeconds = 5;
        private int readTimeoutSeconds = 180;
    }

    @Data
    public static class Knowledge {
        private String helpDir = "docs/help";
        private String docsDir = "docs";
        private List<String> extraFiles = List.of();
        private int chunkSize = 500;
        private int chunkOverlap = 80;
        private int topK = 4;
        private double helpBoost = 0.05;
    }

    @Data
    public static class Chat {
        private int maxQuestionLength = 500;
    }
}
