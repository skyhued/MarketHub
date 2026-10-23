package com.markethub.recommend.task;

import com.markethub.common.constant.ApiConstants;
import com.markethub.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendScheduler {

    private final RecommendService recommendService;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 0 2 * * ?")
    public void computeRecommendations() {
        RLock lock = redissonClient.getLock(ApiConstants.REDIS_KEY_RECOMMEND_TRAIN_LOCK);
        if (!tryAcquire(lock)) {
            return;
        }
        try {
            recommendService.computeRecommendations();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private boolean tryAcquire(RLock lock) {
        try {
            if (lock.tryLock(0, TimeUnit.SECONDS)) {
                return true;
            }
            log.info("另一个实例正在训练推荐模型，本实例跳过");
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待推荐训练锁时被中断，本实例跳过");
            return false;
        }
    }
}
