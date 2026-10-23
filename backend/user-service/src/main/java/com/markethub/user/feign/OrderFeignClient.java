package com.markethub.user.feign;

import com.markethub.common.exception.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 收回商家资格前确认该卖家没有未完成订单。 */
@FeignClient(name = "markethub-order-service", path = "/internal/orders")
public interface OrderFeignClient {

    @GetMapping("/seller/{sellerId}/unfinished-count")
    R<Integer> countUnfinishedBySeller(@PathVariable("sellerId") Integer sellerId);
}
