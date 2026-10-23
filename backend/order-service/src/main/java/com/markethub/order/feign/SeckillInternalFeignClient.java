package com.markethub.order.feign;

import com.markethub.common.dto.SeckillSnapshotDTO;
import com.markethub.common.exception.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** product-service 秒杀内部接口；contextId 必须与 ProductInternalFeignClient 区分。 */
@FeignClient(name = "markethub-product-service", contextId = "seckillInternal", path = "/internal/seckill")
public interface SeckillInternalFeignClient {

    @GetMapping("/{activityId}/snapshot")
    R<SeckillSnapshotDTO> snapshot(@PathVariable("activityId") Integer activityId);

    @GetMapping("/{activityId}/order-no")
    R<String> orderNo(@PathVariable("activityId") Integer activityId, @RequestParam("userId") Integer userId);

    @PostMapping("/{activityId}/attach-order")
    R<String> attachOrder(@PathVariable("activityId") Integer activityId,
                          @RequestParam("userId") Integer userId,
                          @RequestParam("orderNo") String orderNo);

    @PostMapping("/{activityId}/release")
    R<String> release(@PathVariable("activityId") Integer activityId, @RequestParam("userId") Integer userId);
}
