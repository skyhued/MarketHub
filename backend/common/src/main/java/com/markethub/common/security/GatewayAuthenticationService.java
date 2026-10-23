package com.markethub.common.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 网关透传身份认证：校验内部认证头，并把权限码写入 Security 上下文。 */
@Slf4j
@Component
public class GatewayAuthenticationService {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String USER_PERMS_HEADER = "X-User-Perms";
    public static final String INTERNAL_AUTH_HEADER = "X-Internal-Auth";

    /** 服务间调用只凭内部认证密钥放行，没有用户身份。 */
    public static final String INTERNAL_ROLE = "INTERNAL";
    public static final String INTERNAL_AUTHORITY = "ROLE_" + INTERNAL_ROLE;

    @Value("${INTERNAL_AUTH_SECRET}")
    private String internalAuthSecret;

    /** /internal/** 只认内部认证密钥，其余路径走网关注入的用户身份。 */
    public boolean authenticateRequest(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/internal/")) {
            return authenticateInternal(request);
        }
        return authenticate(request);
    }

    /** 校验内部认证密钥；网关不转发 /internal/**。 */
    public boolean authenticateInternal(HttpServletRequest request) {
        String internalAuth = request.getHeader(INTERNAL_AUTH_HEADER);
        if (!internalAuthSecret.equals(internalAuth)) {
            return false;
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                INTERNAL_ROLE, null, List.of(new SimpleGrantedAuthority(INTERNAL_AUTHORITY)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return true;
    }

    public boolean authenticate(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);
        String perms = request.getHeader(USER_PERMS_HEADER);
        String internalAuth = request.getHeader(INTERNAL_AUTH_HEADER);

        if (!internalAuthSecret.equals(internalAuth) || userId == null || role == null) {
            return false;
        }

        try {
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            if (perms != null && !perms.isBlank()) {
                Arrays.stream(perms.split(","))
                        .map(String::trim)
                        .filter(p -> !p.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(Integer.valueOf(userId), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;
        } catch (NumberFormatException e) {
            log.warn("网关透传的用户ID格式不正确: {}", userId);
            return false;
        }
    }
}
