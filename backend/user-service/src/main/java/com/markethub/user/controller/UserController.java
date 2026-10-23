package com.markethub.user.controller;

import com.markethub.common.exception.BusinessException;
import com.markethub.common.exception.R;
import com.markethub.common.security.AuthCookies;
import com.markethub.common.security.CurrentUserId;
import com.markethub.common.util.FileUploadUtil;
import com.markethub.common.util.JwtUtil;
import com.markethub.user.entity.User;
import com.markethub.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "用户接口", description = "用户注册登录、个人信息管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileUploadUtil fileUploadUtil;
    private final JwtUtil jwtUtil;

    @Value("${auth.cookie.secure:false}")
    private boolean cookieSecure;

    @Operation(summary = "获取用户信息")
    @GetMapping("/{id}")
    public R<User> getUserById(@PathVariable Integer id) {
        return R.success(userService.findById(id));
    }

    @Operation(summary = "获取当前登录用户（含角色和 token 中的权限码）")
    @GetMapping("/me")
    public R<Map<String, Object>> getCurrentUser(@CurrentUserId Integer userId) {
        User user = userService.findById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = null;
        List<String> permissions = new ArrayList<>();
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String name = authority.getAuthority();
            if (name.startsWith("ROLE_")) role = name.substring(5);
            else permissions.add(name);
        }
        Map<String, Object> me = new HashMap<>();
        me.put("id", user.getId());
        me.put("username", user.getUsername());
        me.put("nickname", user.getNickname());
        me.put("avatarUrl", user.getAvatarUrl());
        me.put("shopName", user.getShopName());
        me.put("role", role);
        me.put("permissions", permissions);
        return R.success(me);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<String> register(@RequestBody User user) {
        userService.register(user);
        return R.success("注册成功");
    }

    @Operation(summary = "用户登录（token 以 httpOnly Cookie 下发，响应体中的 token 仅供 Swagger/Postman 调试）")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> request, HttpServletResponse response) {
        Map<String, Object> result = userService.login(request.get("username"), request.get("password"));
        response.addHeader(HttpHeaders.SET_COOKIE,
                AuthCookies.issue((String) result.get("token"), jwtUtil.getExpiration(), cookieSecure).toString());
        return R.success(result);
    }

    @Operation(summary = "退出登录（该用户所有设备上的 token 同时失效）")
    @PostMapping("/logout")
    public R<String> logout(@CurrentUserId Integer userId, HttpServletResponse response) {
        userService.revokeTokens(userId);
        response.addHeader(HttpHeaders.SET_COOKIE, AuthCookies.clear(cookieSecure).toString());
        return R.success("已退出登录");
    }

    @Operation(summary = "更新当前用户信息")
    @PutMapping("/update")
    public R<String> updateUserInfo(@RequestBody User user, @CurrentUserId Integer userId) {
        user.setId(userId);
        userService.updateUserInfo(user);
        return R.success("更新成功");
    }

    @Operation(summary = "上传头像")
    @PostMapping("/upload/avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                   @CurrentUserId Integer userId) {
        try {
            String avatarPath = fileUploadUtil.uploadFile(file, "avatars");
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setAvatarUrl(avatarPath);
            userService.updateUserInfo(updateUser);
            return R.success(avatarPath);
        } catch (Exception e) {
            return R.error("上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "修改密码（成功后所有设备需要重新登录）")
    @PostMapping("/change-password")
    public R<String> changePassword(@RequestBody Map<String, String> request,
                                     @CurrentUserId Integer userId, HttpServletResponse response) {
        userService.changePassword(userId, request.get("oldPassword"), request.get("newPassword"));
        response.addHeader(HttpHeaders.SET_COOKIE, AuthCookies.clear(cookieSecure).toString());
        return R.success("密码修改成功");
    }
}
