package com.markethub.review.controller;

import com.markethub.common.constant.PermissionConstants;
import com.markethub.common.exception.R;
import com.markethub.common.security.CurrentUserId;
import com.markethub.common.security.RequirePermission;
import com.markethub.review.entity.Review;
import com.markethub.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "评价接口", description = "商品评价、评分管理")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "获取商品评价列表")
    @GetMapping("/{productId}")
    public R<List<Review>> getProductReviews(@PathVariable Integer productId) {
        return R.success(reviewService.findByProductId(productId));
    }

    @Operation(summary = "获取我对某件商品的评价（前端据此剔除已评过的订单）")
    @GetMapping("/mine/{productId}")
    @RequirePermission(PermissionConstants.REVIEW_LIST)
    public R<List<Review>> getMyReviews(@PathVariable Integer productId, @CurrentUserId Integer userId) {
        return R.success(reviewService.findMine(userId, productId));
    }

    @Operation(summary = "添加评价（只有确认收货的买家能评价，需带 orderId）")
    @PostMapping
    @RequirePermission(PermissionConstants.REVIEW_CREATE)
    public R<String> addReview(@RequestBody Review review, @CurrentUserId Integer userId) {
        review.setUserId(userId);
        reviewService.create(review);
        return R.success("评价成功");
    }
}
