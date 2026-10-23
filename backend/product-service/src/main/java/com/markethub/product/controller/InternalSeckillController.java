package com.markethub.product.controller;

import com.markethub.common.dto.SeckillSnapshotDTO;
import com.markethub.common.exception.R;
import com.markethub.product.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 服务间内部秒杀接口，只给 order-service 的异步建单消费者调用。 */
@Tag(name = "秒杀内部接口", description = "异步建单消费者获取活动信息、回填订单号、失败释放名额")
@RestController
@RequestMapping("/internal/seckill")
@RequiredArgsConstructor
public class InternalSeckillController {

    private final SeckillService seckillService;

    @Operation(summary = "获取秒杀活动商品信息")
    @GetMapping("/{activityId}/snapshot")
    public R<SeckillSnapshotDTO> snapshot(@PathVariable Integer activityId) {
        return R.success(seckillService.getSnapshot(activityId));
    }

    @Operation(summary = "查询该用户这场秒杀是否已经建过订单")
    @GetMapping("/{activityId}/order-no")
    public R<String> orderNo(@PathVariable Integer activityId, @RequestParam Integer userId) {
        return R.success(seckillService.findOrderNo(activityId, userId));
    }

    @Operation(summary = "建单成功后回填订单号")
    @PostMapping("/{activityId}/attach-order")
    public R<String> attachOrder(@PathVariable Integer activityId,
                                 @RequestParam Integer userId,
                                 @RequestParam String orderNo) {
        seckillService.attachOrderNo(activityId, userId, orderNo);
        return R.success("已回填订单号");
    }

    @Operation(summary = "建单最终失败，释放抢到的名额")
    @PostMapping("/{activityId}/release")
    public R<String> release(@PathVariable Integer activityId, @RequestParam Integer userId) {
        seckillService.releaseReservation(activityId, userId);
        return R.success("名额已释放");
    }
}
