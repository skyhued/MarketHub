package com.markethub.order.controller;

import com.markethub.common.exception.R;
import com.markethub.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 服务间内部接口，只给 review-service 和 user-service 调用。 */
@Tag(name = "订单内部接口", description = "评价资格校验、卖家未完成订单数，仅供服务间调用")
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @Operation(summary = "校验评价资格：订单属于该用户、已完成、且含该商品")
    @GetMapping("/{id}/review-eligibility")
    public R<Boolean> reviewEligibility(@PathVariable Integer id,
                                        @RequestParam Integer userId,
                                        @RequestParam Integer productId) {
        return R.success(orderService.isReviewable(id, userId, productId));
    }

    @Operation(summary = "该卖家名下未完成订单数（已付款待发货 + 已发货）")
    @GetMapping("/seller/{sellerId}/unfinished-count")
    public R<Integer> unfinishedCount(@PathVariable Integer sellerId) {
        return R.success(orderService.countUnfinishedBySeller(sellerId));
    }
}
