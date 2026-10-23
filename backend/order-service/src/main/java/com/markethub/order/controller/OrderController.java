package com.markethub.order.controller;

import com.markethub.common.constant.PermissionConstants;
import com.markethub.common.exception.R;
import com.markethub.common.security.CurrentUserId;
import com.markethub.common.security.RequirePermission;
import com.markethub.order.entity.Order;
import com.markethub.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@Tag(name = "订单接口", description = "订单CRUD、支付、发货、收货、取消")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "获取我的订单列表")
    @GetMapping("/my-orders")
    @RequirePermission(PermissionConstants.ORDER_LIST)
    public R<List<Order>> getMyOrders(@CurrentUserId Integer userId) {
        return R.success(orderService.findByUserId(userId));
    }

    @Operation(summary = "获取我卖出的订单")
    @GetMapping("/my-sold")
    @RequirePermission(PermissionConstants.ORDER_LIST)
    public R<List<Order>> getMySoldOrders(@CurrentUserId Integer userId) {
        return R.success(orderService.findBySellerId(userId));
    }

    @Operation(summary = "从购物车创建订单")
    @PostMapping("/from-cart")
    @RequirePermission(PermissionConstants.ORDER_CREATE)
    public R<Order> createOrderFromCart(@RequestBody Map<String, Object> request,
                                        @CurrentUserId Integer userId) {
        Object rawIds = request.get("cartIds");
        if (!(rawIds instanceof List<?> ids)) {
            return R.error("cartIds 不能为空");
        }
        List<Integer> cartIds = ids.stream()
                .map(value -> Integer.valueOf(String.valueOf(value)))
                .collect(Collectors.toList());
        return R.success(orderService.createOrderFromCart(cartIds));
    }
    @Operation(summary = "通过ID获取订单详情")
    @GetMapping("/{id}")
    @RequirePermission(PermissionConstants.ORDER_LIST)
    public R<Order> getOrderById(@PathVariable Integer id, @CurrentUserId Integer userId) {
        return R.success(orderService.findById(id, userId));
    }

    @Operation(summary = "通过订单号获取订单")
    @GetMapping("/no/{orderNo}")
    @RequirePermission(PermissionConstants.ORDER_LIST)
    public R<Order> getOrderByNo(@PathVariable String orderNo, @CurrentUserId Integer userId) {
        return R.success(orderService.findByOrderNo(orderNo, userId));
    }

    @Operation(summary = "支付订单")
    @PostMapping("/{id}/pay")
    @RequirePermission(PermissionConstants.ORDER_PAY)
    public R<String> payOrder(@PathVariable Integer id, @CurrentUserId Integer userId) {
        orderService.pay(id, userId);
        return R.success("支付成功");
    }

    @Operation(summary = "发货（卖家操作）")
    @PostMapping("/{id}/ship")
    @RequirePermission(PermissionConstants.ORDER_SHIP)
    public R<String> shipOrder(@PathVariable Integer id, @CurrentUserId Integer userId) {
        orderService.ship(id, userId);
        return R.success("发货成功");
    }

    @Operation(summary = "确认收货（买家操作）")
    @PostMapping("/{id}/confirm")
    @RequirePermission(PermissionConstants.ORDER_CONFIRM)
    public R<String> confirmReceipt(@PathVariable Integer id, @CurrentUserId Integer userId) {
        orderService.confirmReceipt(id, userId);
        return R.success("确认收货成功");
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    @RequirePermission(PermissionConstants.ORDER_CANCEL)
    public R<String> cancelOrder(@PathVariable Integer id, @CurrentUserId Integer userId,
                                 @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "";
        orderService.cancel(id, userId, reason);
        return R.success("订单已取消");
    }
}
