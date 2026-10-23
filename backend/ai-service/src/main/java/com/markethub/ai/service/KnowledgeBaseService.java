package com.markethub.ai.service;

import com.markethub.ai.config.AiProperties;
import com.markethub.ai.entity.AiDocChunk;
import com.markethub.ai.mapper.AiMapper;
import com.markethub.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/** 把 docs 下的 Markdown 切块、向量化、落库，并装载内存索引。 */
@Slf4j
@Service
public class KnowledgeBaseService {

    private static final String KIND_HELP = "HELP";
    private static final String KIND_DEV = "DEV";
    private static final int INSERT_BATCH = 20;

    public record CorpusFile(String source, String kind, Path path) {}

    public record ReindexResult(int files, int chunks, long durationMs, List<String> models) {}

    public record Status(int chunkCount, int indexedCount, boolean ollamaReachable, List<String> models,
                         String chatModel, String embeddingModel, String corpusRoot) {}

    private final AiMapper aiMapper;
    private final OllamaClient ollamaClient;
    private final VectorIndex vectorIndex;
    private final AiProperties properties;
    private final TransactionTemplate transactionTemplate;

    private final ReentrantLock reindexLock = new ReentrantLock();

    public KnowledgeBaseService(AiMapper aiMapper, OllamaClient ollamaClient, VectorIndex vectorIndex,
                                AiProperties properties, TransactionTemplate transactionTemplate) {
        this.aiMapper = aiMapper;
        this.ollamaClient = ollamaClient;
        this.vectorIndex = vectorIndex;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    /** 启动时用已有向量装载索引；表为空才调 Ollama 首次索引，失败不影响服务启动。 */
    @EventListener(ApplicationReadyEvent.class)
    public void loadIndexOnStartup() {
        try {
            if (aiMapper.countChunks() == 0) {
                log.info("知识库为空，开始首次索引");
                reindex();
                return;
            }
            vectorIndex.load(aiMapper.findAllChunks());
        } catch (Exception e) {
            log.warn("知识库初始化失败，AI 助手暂时不可用：{}", e.getMessage());
        }
    }

    public ReindexResult reindex() {
        if (!reindexLock.tryLock()) {
            throw new BusinessException(409, "已有一次索引重建在进行中，请稍后再试");
        }
        try {
            long startedAt = System.currentTimeMillis();
            List<CorpusFile> files = readCorpus();
            if (files.isEmpty()) {
                throw new BusinessException(500, "没有读到任何语料文件，检查 ai.knowledge 配置与挂载目录");
            }

            List<AiDocChunk> rows = new ArrayList<>();
            for (CorpusFile file : files) {
                String markdown = Files.readString(file.path(), StandardCharsets.UTF_8);
                List<MarkdownChunker.Chunk> chunks = MarkdownChunker.split(
                        markdown, properties.getKnowledge().getChunkSize(), properties.getKnowledge().getChunkOverlap());
                for (MarkdownChunker.Chunk chunk : chunks) {
                    AiDocChunk row = new AiDocChunk();
                    row.setSource(file.source());
                    row.setKind(file.kind());
                    row.setHeading(chunk.heading());
                    row.setChunkIndex(chunk.index());
                    row.setContent(chunk.content());
                    row.setEmbedding(VectorIndex.toText(ollamaClient.embed(embeddingInput(chunk))));
                    rows.add(row);
                }
                log.info("已切块 {} -> {} 段", file.source(), chunks.size());
            }

            replaceAllChunks(rows);
            vectorIndex.load(aiMapper.findAllChunks());

            long duration = System.currentTimeMillis() - startedAt;
            log.info("索引重建完成：{} 个文件 / {} 个片段 / 耗时 {} ms", files.size(), rows.size(), duration);
            return new ReindexResult(files.size(), rows.size(), duration, ollamaClient.modelNames());
        } catch (IOException e) {
            throw new BusinessException(500, "读取语料文件失败：" + e.getMessage());
        } finally {
            reindexLock.unlock();
        }
    }

    public Status status() {
        return new Status(aiMapper.countChunks(), vectorIndex.size(), ollamaClient.isReachable(),
                ollamaClient.modelNames(), properties.getOllama().getChatModel(),
                properties.getOllama().getEmbeddingModel(), corpusRoot().toString());
    }

    /** 先算完向量再开事务，事务只包删旧和分批写新。 */
    private void replaceAllChunks(List<AiDocChunk> rows) {
        transactionTemplate.executeWithoutResult(status -> {
            aiMapper.deleteAllChunks();
            for (int from = 0; from < rows.size(); from += INSERT_BATCH) {
                int to = Math.min(rows.size(), from + INSERT_BATCH);
                aiMapper.batchInsertChunks(rows.subList(from, to));
            }
        });
    }

    private String embeddingInput(MarkdownChunker.Chunk chunk) {
        return chunk.heading().isEmpty() ? chunk.content() : chunk.heading() + "\n" + chunk.content();
    }

    private List<CorpusFile> readCorpus() {
        Path root = corpusRoot();
        AiProperties.Knowledge knowledge = properties.getKnowledge();
        Path helpDir = root.resolve(knowledge.getHelpDir()).normalize();
        Path docsDir = root.resolve(knowledge.getDocsDir()).normalize();

        List<CorpusFile> files = new ArrayList<>();
        collectMarkdown(root, helpDir, null, KIND_HELP, files);
        collectMarkdown(root, docsDir, helpDir, KIND_DEV, files);
        for (String extra : knowledge.getExtraFiles()) {
            Path path = root.resolve(extra).normalize();
            if (Files.isRegularFile(path)) {
                files.add(new CorpusFile(relative(root, path), KIND_DEV, path));
            } else {
                log.warn("语料文件不存在，已跳过：{}", path);
            }
        }
        files.sort(Comparator.comparing(CorpusFile::source));
        return files;
    }

    private void collectMarkdown(Path root, Path dir, Path excludeDir, String kind, List<CorpusFile> files) {
        if (!Files.isDirectory(dir)) {
            log.warn("语料目录不存在，已跳过：{}", dir);
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .filter(path -> excludeDir == null || !path.normalize().startsWith(excludeDir))
                    .forEach(path -> files.add(new CorpusFile(relative(root, path), kind, path)));
        } catch (IOException e) {
            log.warn("遍历语料目录失败 {}：{}", dir, e.getMessage());
        }
    }

    private String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    /** 语料目录从当前工作目录往上找，兼容容器和 IDEA 两种启动方式。 */
    private Path corpusRoot() {
        Path current = Path.of("").toAbsolutePath();
        for (int up = 0; up < 4 && current != null; up++) {
            if (Files.isDirectory(current.resolve(properties.getKnowledge().getDocsDir()))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of("").toAbsolutePath();
    }
}
