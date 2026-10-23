package com.markethub.recommend.service;

public record TrainResult(boolean success, int exitCode, boolean timedOut, long elapsedMillis, String logFile) {
}
