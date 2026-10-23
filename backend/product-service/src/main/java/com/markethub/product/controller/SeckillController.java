package com.markethub.product.controller;

import com.markethub.common.dto.SeckillSnapshotDTO;
import com.markethub.common.exception.R;
import com.markethub.common.security.CurrentUserId;
import com.markethub.product.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @GetMapping("/{activityId}")
    public R<SeckillSnapshotDTO> snapshot(@PathVariable Integer activityId) {
        return R.success(seckillService.getSnapshot(activityId));
    }

    @GetMapping("/{activityId}/stock")
    public R<Integer> stock(@PathVariable Integer activityId) {
        return R.success(seckillService.remainingStock(activityId));
    }

    @PostMapping("/{activityId}/grab")
    public R<String> grab(@PathVariable Integer activityId, @CurrentUserId Integer userId) {
        return R.success(seckillService.grab(activityId, userId));
    }

    @PostMapping("/{activityId}/grab-naive")
    public R<String> grabNaive(@PathVariable Integer activityId, @CurrentUserId Integer userId) {
        return R.success(seckillService.grabNaive(activityId, userId));
    }
}
