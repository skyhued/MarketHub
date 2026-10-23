package com.markethub.product.feign;

import com.markethub.common.exception.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "markethub-user-service", path = "/api/users")
public interface UserFeignClient {

    @GetMapping("/{id}")
    R<?> getUserById(@PathVariable("id") Integer userId);
}
