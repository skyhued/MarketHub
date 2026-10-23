package com.markethub.user.controller;

import com.markethub.common.constant.PermissionConstants;
import com.markethub.common.exception.BusinessException;
import com.markethub.common.exception.R;
import com.markethub.common.security.CurrentUserId;
import com.markethub.common.security.RequirePermission;
import com.markethub.user.entity.User;
import com.markethub.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理后台接口", description = "用户管理、数据统计、权限管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @Operation(summary = "获取统计数据（分布式）")
    @GetMapping("/stats")
    @RequirePermission(PermissionConstants.ADMIN_STATS)
    public R<Map<String, Long>> getStats() {
        return R.success(userService.getStats());
    }

    @Operation(summary = "获取所有用户")
    @GetMapping("/users")
    @RequirePermission(PermissionConstants.USER_LIST)
    public R<List<User>> getAllUsers() {
        return R.success(userService.findAllUsers());
    }

    @Operation(summary = "更新用户状态（启用/禁用）")
    @PostMapping("/user/status")
    @RequirePermission(PermissionConstants.USER_STATUS)
    public R<String> updateUserStatus(@RequestParam Integer userId, @RequestParam Integer status) {
        userService.updateUserStatus(userId, status);
        return R.success("状态已更新");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/users/{id}")
    @RequirePermission(PermissionConstants.USER_DELETE)
    public R<String> deleteUser(@PathVariable Integer id, @CurrentUserId Integer currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException("不能删除当前登录用户");
        }
        userService.deleteUser(id);
        return R.success("删除成功");
    }
}
