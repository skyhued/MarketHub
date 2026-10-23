package com.markethub.gateway.filter;

import com.markethub.common.constant.ApiConstants;
import com.markethub.common.security.AuthCookies;
import com.markethub.common.security.GatewayAuthenticationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 网关全局 JWT 鉴权过滤器：校验 token 与版本号，并向下游注入身份和权限码。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${INTERNAL_AUTH_SECRET}")
    private String internalAuthSecret;

    @Value("${auth.cookie.secure:false}")
    private boolean cookieSecure;

    /** 服务间调用的内部接口，网关一律不转发 */
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    /** 不需要认证、也不会注入身份（没有"本人"的概念）的路径 */
    private static final String[] WHITE_LIST = {
            "/api/users/login",
            "/api/users/register",
            "/api/uploads/"
    };

    /** 可选登录的 GET 接口；用正则匹配，避免把 /api/products/my 一起放行。 */
    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            new PublicEndpoint(HttpMethod.GET, "^/api/products/on-sale/?$"),
            new PublicEndpoint(HttpMethod.GET, "^/api/products/search/?$"),
            new PublicEndpoint(HttpMethod.GET, "^/api/products/\\d+$"),
            new PublicEndpoint(HttpMethod.GET, "^/api/categories/?$"),
            new PublicEndpoint(HttpMethod.GET, "^/api/reviews/\\d+$"),
            new PublicEndpoint(HttpMethod.GET, "^/api/recommend/me/?$"),
            new PublicEndpoint(HttpMethod.GET, "^/api/recommend/hot/?$"),
            new PublicEndpoint(HttpMethod.GET, "^/api/recommend/related/\\d+$"),
            new PublicEndpoint(HttpMethod.GET, "^/api/seckill/\\d+$"),
            new PublicEndpoint(HttpMethod.GET, "^/api/seckill/\\d+/stock$")
    );

    private record PublicEndpoint(HttpMethod method, String pathRegex) {
        boolean matches(HttpMethod requestMethod, String path) {
            return method.equals(requestMethod) && path.matches(pathRegex);
        }
    }

    private record ExtractedToken(String value, boolean fromCookie) {}

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 身份头只能由网关写入，客户端自带的一律丢弃（匿名路径也一样）
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GatewayAuthenticationService.USER_ID_HEADER);
                    headers.remove(GatewayAuthenticationService.USER_ROLE_HEADER);
                    headers.remove(GatewayAuthenticationService.USER_PERMS_HEADER);
                    headers.remove(GatewayAuthenticationService.INTERNAL_AUTH_HEADER);
                })
                .build();
        ServerWebExchange cleanExchange = exchange.mutate().request(stripped).build();

        String path = stripped.getURI().getPath();
        log.debug("网关请求: {} {}", stripped.getMethod(), path);

        if (path.startsWith(INTERNAL_PATH_PREFIX)) {
            log.warn("拒绝外部访问内部接口: {}", path);
            return notFound(cleanExchange, false);
        }

        for (String whitePath : WHITE_LIST) {
            if (path.startsWith(whitePath)) {
                return chain.filter(cleanExchange);
            }
        }

        boolean anonymousAllowed = isPublicEndpoint(stripped.getMethod(), path);

        ExtractedToken token = extractToken(stripped);
        if (token == null) {
            if (anonymousAllowed) return chain.filter(cleanExchange);
            log.debug("请求缺少Token: {}", path);
            return reject(cleanExchange, false);
        }

        Claims claims;
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token.value()).getPayload();
        } catch (Exception e) {
            log.warn("Token校验失败: {}", e.getMessage());
            return anonymousAllowed ? anonymousButClearCookie(cleanExchange, chain, token) : reject(cleanExchange, token.fromCookie());
        }

        String userId = claims.getSubject();
        Integer version = claims.get("ver", Integer.class);
        if (version == null) {
            return anonymousAllowed ? anonymousButClearCookie(cleanExchange, chain, token) : reject(cleanExchange, token.fromCookie());
        }

        return redisTemplate.opsForValue().get(ApiConstants.REDIS_KEY_TOKEN_VERSION + userId)
                .map(current -> current.equals(String.valueOf(version)))
                // 缓存里没有版本号：让用户重新登录一次，登录时会从数据库写回
                .defaultIfEmpty(false)
                // Redis 不可用时宁可拒绝，也不放行可能已作废的 token
                .onErrorResume(e -> {
                    log.error("Redis 不可用，拒绝请求: {}", e.getMessage());
                    return Mono.just(false);
                })
                .flatMap(valid -> {
                    if (valid) {
                        return chain.filter(withIdentity(cleanExchange, claims));
                    }
                    if (anonymousAllowed) {
                        return anonymousButClearCookie(cleanExchange, chain, token);
                    }
                    log.debug("Token已作废: userId={}, ver={}", userId, version);
                    return reject(cleanExchange, token.fromCookie());
                });
    }

    private boolean isPublicEndpoint(HttpMethod method, String path) {
        if (method == null) return false;
        return PUBLIC_ENDPOINTS.stream().anyMatch(endpoint -> endpoint.matches(method, path));
    }

    /** 可选登录路径上的无效 token 降级为匿名，清掉 Cookie 并返回失效响应头。 */
    private Mono<Void> anonymousButClearCookie(ServerWebExchange exchange, GatewayFilterChain chain, ExtractedToken token) {
        exchange.getResponse().getHeaders().set(ApiConstants.TOKEN_EXPIRED_HEADER, "1");
        if (token.fromCookie()) {
            exchange.getResponse().addCookie(AuthCookies.clear(cookieSecure));
        }
        return chain.filter(exchange);
    }

    private ServerWebExchange withIdentity(ServerWebExchange exchange, Claims claims) {
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        List<?> perms = claims.get("perms", List.class);
        String permsHeader = perms == null ? "" : perms.stream().map(String::valueOf).collect(Collectors.joining(","));

        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.set(GatewayAuthenticationService.USER_ID_HEADER, userId);
                    headers.set(GatewayAuthenticationService.USER_ROLE_HEADER, role);
                    headers.set(GatewayAuthenticationService.USER_PERMS_HEADER, permsHeader);
                    headers.set(GatewayAuthenticationService.INTERNAL_AUTH_HEADER, internalAuthSecret);
                }))
                .build();
    }

    /** 失效的 Cookie 顺手清掉，免得浏览器每次都带着它来 */
    private Mono<Void> reject(ServerWebExchange exchange, boolean clearCookie) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        if (clearCookie) {
            exchange.getResponse().addCookie(AuthCookies.clear(cookieSecure));
        }
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> notFound(ServerWebExchange exchange, boolean clearCookie) {
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        if (clearCookie) {
            exchange.getResponse().addCookie(AuthCookies.clear(cookieSecure));
        }
        return exchange.getResponse().setComplete();
    }

    private ExtractedToken extractToken(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst(ApiConstants.AUTH_COOKIE_NAME);
        if (cookie != null && !cookie.getValue().isBlank()) {
            return new ExtractedToken(cookie.getValue(), true);
        }
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return new ExtractedToken(bearerToken.substring(7), false);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
