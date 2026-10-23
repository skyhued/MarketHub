package com.markethub.ai.service;

import com.markethub.ai.entity.AiDocChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 内存向量索引，entries 是一次性替换的不可变快照，检索无需加锁。 */
@Slf4j
@Component
public class VectorIndex {

    public record Scored(AiDocChunk chunk, double score) {}

    private record Entry(AiDocChunk chunk, float[] vector) {}

    private volatile List<Entry> entries = List.of();

    public void load(List<AiDocChunk> chunks) {
        List<Entry> loaded = new ArrayList<>(chunks.size());
        for (AiDocChunk chunk : chunks) {
            float[] vector = parseVector(chunk.getEmbedding());
            if (vector.length > 0) {
                loaded.add(new Entry(chunk, vector));
            }
        }
        this.entries = List.copyOf(loaded);
        log.info("向量索引已加载 {} 个片段", loaded.size());
    }

    public int size() {
        return entries.size();
    }

    public List<Scored> search(float[] query, int topK, double helpBoost) {
        List<Entry> snapshot = entries;
        if (snapshot.isEmpty() || query.length == 0) {
            return List.of();
        }
        List<Scored> scored = new ArrayList<>(snapshot.size());
        for (Entry entry : snapshot) {
            if (entry.vector().length != query.length) {
                continue;
            }
            double score = cosine(query, entry.vector());
            if (helpBoost > 0 && "HELP".equals(entry.chunk().getKind())) {
                score += helpBoost;
            }
            scored.add(new Scored(entry.chunk(), score));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return scored.size() <= topK ? scored : new ArrayList<>(scored.subList(0, topK));
    }

    static double cosine(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static String toText(float[] vector) {
        StringBuilder text = new StringBuilder(vector.length * 10);
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append(vector[i]);
        }
        return text.toString();
    }

    static float[] parseVector(String text) {
        if (text == null || text.isBlank()) {
            return new float[0];
        }
        String[] parts = text.split(",");
        float[] vector = new float[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                vector[i] = Float.parseFloat(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            log.warn("向量文本解析失败，跳过该片段");
            return new float[0];
        }
        return vector;
    }
}
