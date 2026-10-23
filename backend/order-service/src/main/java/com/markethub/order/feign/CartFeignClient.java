package com.markethub.order.feign;

import com.markethub.common.exception.R;
import com.markethub.order.dto.CartItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "markethub-cart-service", path = "/api/cart")
public interface CartFeignClient {

    @GetMapping
    R<List<CartItemDTO>> getCart();

    @DeleteMapping("/{productId}")
    R<String> removeItem(@PathVariable("productId") Integer productId);
}