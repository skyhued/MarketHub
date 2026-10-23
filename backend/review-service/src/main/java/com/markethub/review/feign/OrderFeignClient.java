package com.markethub.review.feign;

import com.markethub.common.exception.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** order-service 内部接口：校验评价资格。 */
@FeignClient(name = "markethub-order-service", path = "/internal/orders")
public interface OrderFeignClient {

    @GetMapping("/{id}/review-eligibility")
    R<Boolean> reviewEligibility(@PathVariable("id") Integer orderId,
                                 @RequestParam("userId") Integer userId,
                                 @RequestParam("productId") Integer productId);
}
