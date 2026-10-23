package com.markethub.common.security;

import com.markethub.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(com.markethub.common.security.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            return joinPoint.proceed();
        }

        // 权限码由网关从 JWT（登录时查询 role_permission 表生成）透传，已写入 authorities
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(403, "未登录或登录已过期");
        }

        Set<String> permissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String[] requiredPermissions = requirePermission.value();
        RequirePermission.Logical logical = requirePermission.logical();

        boolean hasPermission;
        if (logical == RequirePermission.Logical.AND) {
            hasPermission = Arrays.stream(requiredPermissions).allMatch(permissions::contains);
        } else {
            hasPermission = Arrays.stream(requiredPermissions).anyMatch(permissions::contains);
        }

        if (!hasPermission) {
            log.warn("用户[{}]缺少权限: {}", authentication.getPrincipal(), Arrays.toString(requiredPermissions));
            throw new BusinessException(403, "权限不足");
        }

        return joinPoint.proceed();
    }
}
