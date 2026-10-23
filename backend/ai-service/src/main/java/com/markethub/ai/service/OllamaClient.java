package com.markethub.ai.service;

import com.markethub.ai.config.AiProperties;
import com.markethub.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 只调 /api/embed 和 /api/chat；首次调用要加载模型，读超时放宽。 */
@Slf4j
@Component
public class OllamaClient {

    private final AiProperties properties;
    private final RestClient restClient;

    public OllamaClient(AiProperties properties) {
        this.properties = properties;
        AiProperties.Ollama ollama = properties.getOllama();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(ollama.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(ollama.getReadTimeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(ollama.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public float[] embed(String text) {
        EmbedResponse response;
        try {
            response = restClient.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", properties.getOllama().getEmbeddingModel(), "input", text))
                    .retrieve()
                    .body(EmbedResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(503, "调用 Ollama 向量化失败：" + hint(e));
        }
        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new BusinessException(503, "Ollama 没有返回向量，请检查模型 " + properties.getOllama().getEmbeddingModel());
        }
        List<Float> first = response.embeddings().get(0);
        float[] vector = new float[first.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = first.get(i);
        }
        return vector;
    }

    public String chat(String systemPrompt, String userPrompt) {
        ChatResponse response;
        try {
            response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", properties.getOllama().getChatModel(),
                            "stream", false,
                            "options", Map.of("temperature", 0.2),
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userPrompt))))
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(503, "调用 Ollama 生成回答失败：" + hint(e));
        }
        if (response == null || response.message() == null || response.message().content() == null) {
            throw new BusinessException(503, "Ollama 没有返回回答内容");
        }
        return response.message().content().trim();
    }

    public boolean isReachable() {
        try {
            restClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    public List<String> modelNames() {
        try {
            TagsResponse response = restClient.get().uri("/api/tags").retrieve().body(TagsResponse.class);
            if (response == null || response.models() == null) {
                return List.of();
            }
            return response.models().stream().map(TagModel::name).collect(Collectors.toList());
        } catch (RestClientException e) {
            return List.of();
        }
    }

    private String hint(RestClientException e) {
        String message = e.getMessage();
        if (message != null && message.contains("model") && message.contains("not found")) {
            return "Ollama 里没有这个模型，先在宿主机执行 ollama pull。" + message;
        }
        return message == null ? e.getClass().getSimpleName() : message;
    }

    private record EmbedResponse(List<List<Float>> embeddings) {}

    private record ChatResponse(ChatMessage message) {}

    private record ChatMessage(String role, String content) {}

    private record TagsResponse(List<TagModel> models) {}

    private record TagModel(String name, String model) {}
}
