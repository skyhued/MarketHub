package com.markethub.recommend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RecommendTrainRunner {

    private static final DateTimeFormatter LOG_FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final long DESTROY_WAIT_SECONDS = 10L;

    private final String pythonExecutable;
    private final String scriptPath;
    private final long timeoutSeconds;
    private final String logDir;

    public RecommendTrainRunner(
            @Value("${recommend.train.python-executable:python}") String pythonExecutable,
            @Value("${recommend.train.script-path:recommend/engine/train.py}") String scriptPath,
            @Value("${recommend.train.timeout-seconds:600}") long timeoutSeconds,
            @Value("${recommend.train.log-dir:recommend/logs}") String logDir) {
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
        this.timeoutSeconds = timeoutSeconds;
        this.logDir = logDir;
    }

    public TrainResult train() {
        long startedAt = System.currentTimeMillis();
        Path logFile;
        try {
            logFile = prepareLogFile();
        } catch (IOException e) {
            log.error("推荐训练日志文件创建失败: {}", e.getMessage());
            return new TrainResult(false, -1, false, System.currentTimeMillis() - startedAt, null);
        }

        Process process = null;
        try {
            process = buildProcess(logFile).start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS);
                log.error("推荐训练超过 {} 秒未结束，已强制终止，完整输出见 {}", timeoutSeconds, logFile);
                return new TrainResult(false, -1, true, System.currentTimeMillis() - startedAt, logFile.toString());
            }
            int exitCode = process.exitValue();
            long elapsed = System.currentTimeMillis() - startedAt;
            if (exitCode == 0) {
                log.info("推荐训练成功，耗时 {} ms，完整输出见 {}", elapsed, logFile);
            } else {
                log.error("推荐训练退出码 {}，完整输出见 {}", exitCode, logFile);
            }
            return new TrainResult(exitCode == 0, exitCode, false, elapsed, logFile.toString());
        } catch (IOException e) {
            log.error("推荐训练进程启动失败，请检查 {} 是否可用、脚本 {} 是否存在: {}",
                    pythonExecutable, scriptPath, e.getMessage());
            return new TrainResult(false, -1, false, System.currentTimeMillis() - startedAt, logFile.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            log.error("推荐训练等待被中断: {}", e.getMessage());
            return new TrainResult(false, -1, true, System.currentTimeMillis() - startedAt, logFile.toString());
        }
    }

    ProcessBuilder buildProcess(Path logFile) {
        ProcessBuilder builder = new ProcessBuilder(command());
        builder.redirectErrorStream(true);
        builder.redirectOutput(logFile.toFile());
        return builder;
    }

    List<String> command() {
        return List.of(pythonExecutable, scriptPath);
    }

    private Path prepareLogFile() throws IOException {
        Path dir = Paths.get(logDir);
        Files.createDirectories(dir);
        return dir.resolve("train-" + LocalDateTime.now().format(LOG_FILE_FORMAT) + ".log");
    }
}
