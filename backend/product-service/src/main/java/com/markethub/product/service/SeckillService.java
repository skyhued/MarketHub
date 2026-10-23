package com.markethub.product.service;

import com.markethub.common.constant.ApiConstants;
import com.markethub.common.dto.SeckillGrabMessage;
import com.markethub.common.dto.SeckillSnapshotDTO;
import com.markethub.common.exception.BusinessException;
import com.markethub.product.entity.Product;
import com.markethub.product.entity.SeckillActivity;
import com.markethub.product.mapper.ProductMapper;
import com.markethub.product.mapper.SeckillActivityMapper;
import com.markethub.product.mapper.SeckillGrabLedgerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillGrabLedgerMapper seckillGrabLedgerMapper;
    private final ProductMapper productMapper;
    private final SeckillCacheLoader seckillCacheLoader;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final DefaultRedisScript<Long> seckillGrabScript;

    public SeckillSnapshotDTO getSnapshot(Integer activityId) {
        SeckillActivity activity = requireActivity(activityId);
        Product product = productMapper.findById(activity.getProductId());
        if (product == null) {
            throw new BusinessException("秒杀商品不存在");
        }
        SeckillSnapshotDTO dto = new SeckillSnapshotDTO();
        dto.setActivityId(activity.getId());
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setProductImage(product.getImageUrl());
        dto.setSellerId(product.getUserId());
        dto.setSeckillPrice(activity.getSeckillPrice());
        return dto;
    }

    public Integer remainingStock(Integer activityId) {
        requireActivity(activityId);
        String stockKey = ApiConstants.REDIS_KEY_SECKILL_STOCK + activityId;
        String cached = redisTemplate.opsForValue().get(stockKey);
        if (cached != null) {
            return Integer.valueOf(cached);
        }
        return requireActivity(activityId).getStock();
    }

    public String grab(Integer activityId, Integer userId) {
        SeckillActivity activity = requireActivity(activityId);
        requireOnSale(activity);
        seckillCacheLoader.warmUp(activityId);

        String stockKey = ApiConstants.REDIS_KEY_SECKILL_STOCK + activityId;
        String userKey = ApiConstants.REDIS_KEY_SECKILL_USER + activityId + ":" + userId;
        Long result = redisTemplate.execute(seckillGrabScript, List.of(stockKey, userKey));

        if (result == null || result == -2) {
            throw new BusinessException("秒杀活动尚未准备好，请稍后重试");
        }
        if (result == 0) {
            throw new BusinessException("你已经抢过这场秒杀了");
        }
        if (result == -1) {
            throw new BusinessException("很遗憾，名额已抢完");
        }

        if (seckillGrabLedgerMapper.tryRecord(activityId, userId) == 0) {
            rollbackRedisGrab(stockKey, userKey);
            throw new BusinessException("你已经抢过这场秒杀了");
        }

        if (seckillActivityMapper.deductStock(activityId) == 0) {
            rollbackRedisGrab(stockKey, userKey);
            seckillGrabLedgerMapper.deleteIfNoOrder(activityId, userId);
            throw new BusinessException("很遗憾，名额已抢完");
        }

        publishGrabMessage(activityId, userId);
        return "抢购成功，订单正在生成，请稍后在订单列表查看";
    }

    /** 跳过 Redis 预扣，直接打 DB 条件更新，仅用于和 grab 做压测对比 */
    public String grabNaive(Integer activityId, Integer userId) {
        requireOnSale(requireActivity(activityId));

        if (seckillGrabLedgerMapper.exists(activityId, userId) > 0) {
            throw new BusinessException("你已经抢过这场秒杀了");
        }
        if (seckillActivityMapper.deductStock(activityId) == 0) {
            throw new BusinessException("很遗憾，名额已抢完");
        }
        if (seckillGrabLedgerMapper.tryRecord(activityId, userId) == 0) {
            seckillActivityMapper.restoreStock(activityId);
            throw new BusinessException("你已经抢过这场秒杀了");
        }

        publishGrabMessage(activityId, userId);
        return "抢购成功，订单正在生成，请稍后在订单列表查看";
    }

    public String findOrderNo(Integer activityId, Integer userId) {
        return seckillGrabLedgerMapper.findOrderNo(activityId, userId);
    }

    public void attachOrderNo(Integer activityId, Integer userId, String orderNo) {
        seckillGrabLedgerMapper.attachOrderNo(activityId, userId, orderNo);
    }

    /** 异步建单最终失败时释放名额：DB 账本、DB 活动库存、Redis 库存与用户标记一起还原 */
    public void releaseReservation(Integer activityId, Integer userId) {
        if (seckillGrabLedgerMapper.deleteIfNoOrder(activityId, userId) == 0) {
            return;
        }
        seckillActivityMapper.restoreStock(activityId);
        rollbackRedisGrab(ApiConstants.REDIS_KEY_SECKILL_STOCK + activityId,
                ApiConstants.REDIS_KEY_SECKILL_USER + activityId + ":" + userId);
    }

    private void publishGrabMessage(Integer activityId, Integer userId) {
        rabbitTemplate.convertAndSend(ApiConstants.EXCHANGE_SECKILL, ApiConstants.ROUTING_SECKILL_CREATE,
                new SeckillGrabMessage(activityId, userId));
    }

    private void rollbackRedisGrab(String stockKey, String userKey) {
        redisTemplate.delete(userKey);
        redisTemplate.opsForValue().increment(stockKey);
    }

    private SeckillActivity requireActivity(Integer activityId) {
        SeckillActivity activity = seckillActivityMapper.findById(activityId);
        if (activity == null) {
            throw new BusinessException("秒杀活动不存在");
        }
        return activity;
    }

    private void requireOnSale(SeckillActivity activity) {
        if (activity.getStatus() == null || activity.getStatus() != 1) {
            throw new BusinessException("秒杀活动未上线");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException("不在秒杀时间范围内");
        }
    }
}
