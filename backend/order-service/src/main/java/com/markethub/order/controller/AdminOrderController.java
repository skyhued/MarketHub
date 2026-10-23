package com.markethub.order.controller;

import com.markethub.common.constant.PermissionConstants;
import com.markethub.common.exception.R;
import com.markethub.common.security.RequirePermission;
import com.markethub.order.entity.Order;
import com.markethub.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @RequirePermission(PermissionConstants.ADMIN_STATS)
    public R<List<Order>> getAllOrders() {
        return R.success(orderService.findAll());
    }
}