package com.markethub.user.feign;

import com.markethub.common.exception.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "markethub-product-service", path = "/api/products")
public interface ProductFeignClient {

    @GetMapping("/{id}")
    R<?> getProductById(@PathVariable("id") Integer productId);
}
