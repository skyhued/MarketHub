package com.markethub.common.security;

import com.markethub.common.constant.ApiConstants;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class AuthCookies {

    private AuthCookies() {}

    public static ResponseCookie issue(String token, long maxAgeMillis, boolean secure) {
        return base(token, secure).maxAge(Duration.ofMillis(maxAgeMillis)).build();
    }

    public static ResponseCookie clear(boolean secure) {
        return base("", secure).maxAge(Duration.ZERO).build();
    }

    // 这个 SPA 用 Strict 没有副作用：Cookie 只在同站 XHR 里需要，首屏加载用不到。
    private static ResponseCookie.ResponseCookieBuilder base(String value, boolean secure) {
        return ResponseCookie.from(ApiConstants.AUTH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/");
    }
}
