package com.markethub.user.service;

import com.markethub.common.constant.ApiConstants;
import com.markethub.common.exception.BusinessException;
import com.markethub.common.util.JwtUtil;
import com.markethub.user.entity.Permission;
import com.markethub.user.entity.User;
import com.markethub.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public User findById(Integer id) {
        User user = userMapper.findById(id);
        if (user != null) user.setPassword(null);
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }
        user.setUsername(user.getUsername().trim());
        if (userMapper.findByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoleId(ApiConstants.ROLE_ID_USER);
        user.setStatus(ApiConstants.USER_STATUS_NORMAL);
        userMapper.insert(user);
        log.info("用户注册成功: username={}", user.getUsername());
    }

    public Map<String, Object> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("用户名和密码不能为空");
        }
        User user = userMapper.findByUsername(username);
        if (user == null) throw new BusinessException("用户不存在");
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        if (Integer.valueOf(ApiConstants.USER_STATUS_DISABLED).equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        String role = user.getRole() != null ? user.getRole().getRoleCode() : ApiConstants.ROLE_USER;
        List<String> perms = userMapper.findPermissionsByRoleId(user.getRoleId()).stream()
                .map(Permission::getPermCode)
                .toList();
        int version = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        redisTemplate.opsForValue().set(tokenVersionKey(user.getId()), String.valueOf(version));
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role, perms, version);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("role", role);
        result.put("permissions", perms);
        result.put("token", token);

        log.info("用户登录成功: userId={}, role={}", user.getId(), role);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        // 个人资料接口只允许修改昵称、头像，禁止客户端越权修改密码、状态和角色。
        user.setPassword(null);
        user.setStatus(null);
        user.setRoleId(null);
        userMapper.update(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new BusinessException("原密码不能为空");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        User user = userMapper.findById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(user);
        revokeTokens(userId);
    }

    /** 让该用户已签发的所有 token 立即失效（所有设备都需要重新登录）。 */
    @Transactional(rollbackFor = Exception.class)
    public void revokeTokens(Integer userId) {
        userMapper.incrementTokenVersion(userId);
        Integer version = userMapper.findTokenVersion(userId);
        if (version == null) throw new BusinessException(404, "用户不存在");
        redisTemplate.opsForValue().set(tokenVersionKey(userId), String.valueOf(version));
    }

    private static String tokenVersionKey(Integer userId) {
        return ApiConstants.REDIS_KEY_TOKEN_VERSION + userId;
    }


    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Integer userId, Integer status) {
        if (!Integer.valueOf(ApiConstants.USER_STATUS_DISABLED).equals(status)
                && !Integer.valueOf(ApiConstants.USER_STATUS_NORMAL).equals(status)) {
            throw new BusinessException("用户状态值不正确");
        }
        User user = userMapper.findById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        user.setStatus(status);
        userMapper.update(user);
        if (Integer.valueOf(ApiConstants.USER_STATUS_DISABLED).equals(status)) {
            revokeTokens(userId);
        }
        log.info("用户状态更新: userId={}, status={}", userId, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Integer userId) {
        User user = userMapper.findById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (user.getRole() != null && ApiConstants.ROLE_ADMIN.equals(user.getRole().getRoleCode())) {
            throw new BusinessException("不能删除管理员账号");
        }
        int affectedRows = userMapper.deleteById(userId);
        if (affectedRows == 0) throw new BusinessException(404, "用户不存在");
        redisTemplate.delete(tokenVersionKey(userId));
        log.info("用户删除成功: userId={}", userId);
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("users", (long) userMapper.countAll());
        return stats;
    }

    public List<User> findAllUsers() {
        List<User> users = userMapper.findAll();
        users.forEach(user -> user.setPassword(null));
        return users;
    }
}
