package com.markethub.recommend.service;

import com.markethub.common.constant.ApiConstants;
import com.markethub.common.exception.BusinessException;
import com.markethub.common.security.CurrentUser;
import com.markethub.recommend.entity.RecommendationCache;
import com.markethub.recommend.entity.UserBehavior;
import com.markethub.recommend.mapper.RecommendMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final RecommendMapper recommendMapper;
    private final RecommendTrainRunner recommendTrainRunner;

    public List<RecommendationCache> getRecommendForUser(Integer userId, int limit) {
        return recommendMapper.findRecommendByUserId(userId, limit);
    }

    public List<RecommendationCache> getHotProducts(int limit) {
        return recommendMapper.findHotProducts(limit);
    }

    public List<RecommendationCache> getRelatedProducts(Integer productId, int limit) {
        return recommendMapper.findRelatedProducts(productId, limit);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordBehavior(Integer userId, Integer productId, Integer behaviorType) {
        BigDecimal weight = switch (behaviorType) {
            case 1 -> BigDecimal.valueOf(0.5); // 浏览
            case 2 -> BigDecimal.valueOf(1.0); // 收藏
            case 3 -> BigDecimal.valueOf(0.8); // 加购
            case 4 -> BigDecimal.valueOf(2.0); // 购买
            default -> BigDecimal.ZERO;
        };

        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setProductId(productId);
        behavior.setBehaviorType(behaviorType);
        behavior.setWeight(weight);
        recommendMapper.insertBehavior(behavior);
        log.info("用户行为记录: userId={}, productId={}, type={}", userId, productId, behaviorType);
    }

    public TrainResult computeRecommendations() {
        log.info("开始计算推荐数据...");
        TrainResult result = recommendTrainRunner.train();
        if (!result.success()) {
            log.error("推荐数据计算未成功，超时={}，退出码={}，输出见 {}",
                    result.timedOut(), result.exitCode(), result.logFile());
        }
        return result;
    }

    public TrainResult triggerTraining() {
        if (!CurrentUser.isAdmin()) {
            throw new BusinessException(403, "只有管理员可以手动触发推荐训练");
        }
        return computeRecommendations();
    }
}
