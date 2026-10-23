package com.markethub.cart.controller;

import com.markethub.common.constant.PermissionConstants;
import com.markethub.common.exception.R;
import com.markethub.common.security.CurrentUserId;
import com.markethub.common.security.RequirePermission;
import com.markethub.cart.entity.Cart;
import com.markethub.cart.mapper.CartMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "购物车接口", description = "购物车CRUD、勾选、数量修改")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartMapper cartMapper;

    @Operation(summary = "获取购物车列表")
    @GetMapping
    @RequirePermission(PermissionConstants.CART_LIST)
    public R<List<Cart>> getCartList(@CurrentUserId Integer userId) {
        return R.success(cartMapper.findByUserId(userId));
    }

    @Operation(summary = "添加到购物车")
    @PostMapping
    @RequirePermission(PermissionConstants.CART_OPERATE)
    public R<String> addToCart(@RequestBody Cart cart, @CurrentUserId Integer userId) {
        cart.setUserId(userId);
        Cart existing = cartMapper.findByUserAndProduct(userId, cart.getProductId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + (cart.getQuantity() != null ? cart.getQuantity() : 1));
            cartMapper.update(existing);
        } else {
            if (cart.getQuantity() == null) cart.setQuantity(1);
            if (cart.getChecked() == null) cart.setChecked(1);
            cartMapper.insert(cart);
        }
        return R.success("已加入购物车");
    }

    @Operation(summary = "修改购物车数量")
    @PutMapping("/quantity")
    @RequirePermission(PermissionConstants.CART_OPERATE)
    public R<String> updateQuantity(@RequestBody Cart cart, @CurrentUserId Integer userId) {
        cart.setUserId(userId);
        cartMapper.update(cart);
        return R.success("数量已更新");
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/{productId}")
    @RequirePermission(PermissionConstants.CART_OPERATE)
    public R<String> removeFromCart(@PathVariable Integer productId, @CurrentUserId Integer userId) {
        cartMapper.deleteByUserAndProduct(userId, productId);
        return R.success("已删除");
    }

    @Operation(summary = "更新勾选状态")
    @PutMapping("/checked")
    @RequirePermission(PermissionConstants.CART_OPERATE)
    public R<String> updateChecked(@RequestBody Cart cart, @CurrentUserId Integer userId) {
        cart.setUserId(userId);
        cartMapper.update(cart);
        return R.success("状态已更新");
    }
}
