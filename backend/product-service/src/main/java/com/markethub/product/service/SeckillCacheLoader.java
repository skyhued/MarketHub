package com.markethub.product.service;

import com.markethub.common.constant.ApiConstants;
import com.markethub.common.exception.BusinessException;
import com.markethub.product.entity.SeckillActivity;
import com.markethub.product.mapper.SeckillActivityMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeckillCacheLoader {

    private final SeckillActivityMapper seckillActivityMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    public void warmUp(Integer activityId) {
        String stockKey = ApiConstants.REDIS_KEY_SECKILL_STOCK + activityId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
            return;
        }

        RLock lock = redissonClient.getLock(ApiConstants.REDIS_KEY_SECKILL_INIT_LOCK + activityId);
        lock.lock();
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
                return;
            }
            SeckillActivity activity = seckillActivityMapper.findById(activityId);
            if (activity == null) {
                throw new BusinessException("秒杀活动不存在");
            }
            redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(activity.getStock()));
        } finally {
            lock.unlock();
        }
    }
}
