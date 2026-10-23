package com.markethub.recommend.controller;

import com.markethub.common.constant.ApiConstants;
import com.markethub.common.exception.R;
import com.markethub.common.security.CurrentUserId;
import com.markethub.recommend.entity.RecommendationCache;
import com.markethub.recommend.service.RecommendService;
import com.markethub.recommend.service.TrainResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "推荐接口", description = "个性化推荐、热门商品、相关推荐")
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @Operation(summary = "获取个性化推荐（基于用户行为）")
    @GetMapping("/me")
    public R<List<RecommendationCache>> getRecommendForMe(@CurrentUserId(required = false) Integer userId) {
        if (userId == null) {
            return R.success(recommendService.getHotProducts(20));
        }
        return R.success(recommendService.getRecommendForUser(userId, 20));
    }

    @Operation(summary = "获取热门推荐")
    @GetMapping("/hot")
    public R<List<RecommendationCache>> getHotProducts() {
        return R.success(recommendService.getHotProducts(20));
    }

    @Operation(summary = "获取相关推荐")
    @GetMapping("/related/{productId}")
    public R<List<RecommendationCache>> getRelatedProducts(@PathVariable Integer productId) {
        return R.success(recommendService.getRelatedProducts(productId, 10));
    }

    @Operation(summary = "手动触发推荐训练（管理员，同步等待训练与评估跑完）")
    @PostMapping("/admin/train")
    public R<TrainResult> triggerTraining() {
        return R.success(recommendService.triggerTraining());
    }

    @Operation(summary = "记录用户行为（用于推荐算法）")
    @PostMapping("/behavior")
    public R<String> recordBehavior(@RequestBody UserBehaviorRequest request,
                                     @CurrentUserId Integer userId) {
        recommendService.recordBehavior(userId, request.getProductId(), request.getBehaviorType());
        return R.success("记录成功");
    }
}

@Data
class UserBehaviorRequest {
    private Integer productId;
    private Integer behaviorType;
}
