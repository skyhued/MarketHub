package com.markethub.order.feign;

import com.markethub.common.exception.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** product-service 内部库存/销量接口；每次调用带 orderNo 做幂等；contextId 必须与 SeckillInternalFeignClient 区分。 */
@FeignClient(name = "markethub-product-service", contextId = "productInternal", path = "/internal/products")
public interface ProductInternalFeignClient {

    @PostMapping("/{id}/stock/deduct")
    R<String> deductStock(@PathVariable("id") Integer productId,
                          @RequestParam("orderNo") String orderNo,
                          @RequestParam("quantity") Integer quantity);

    @PostMapping("/{id}/stock/restore")
    R<String> restoreStock(@PathVariable("id") Integer productId,
                           @RequestParam("orderNo") String orderNo,
                           @RequestParam("quantity") Integer quantity);

    @PostMapping("/{id}/sales/increase")
    R<String> increaseSales(@PathVariable("id") Integer productId,
                            @RequestParam("orderNo") String orderNo,
                            @RequestParam("quantity") Integer quantity);

    @PostMapping("/{id}/sales/decrease")
    R<String> decreaseSales(@PathVariable("id") Integer productId,
                            @RequestParam("orderNo") String orderNo,
                            @RequestParam("quantity") Integer quantity);
}
