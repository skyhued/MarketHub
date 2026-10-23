package com.markethub.common.feign;

import com.markethub.common.security.GatewayAuthenticationService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Feign 调用统一带内部认证密钥，并透传当前用户身份。 */
public class InternalAuthRequestInterceptor implements RequestInterceptor {

    private static final String[] FORWARDED_HEADERS = {
            GatewayAuthenticationService.USER_ID_HEADER,
            GatewayAuthenticationService.USER_ROLE_HEADER,
            GatewayAuthenticationService.USER_PERMS_HEADER,
            "Authorization"
    };

    private final String internalAuthSecret;

    public InternalAuthRequestInterceptor(String internalAuthSecret) {
        this.internalAuthSecret = internalAuthSecret;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(GatewayAuthenticationService.INTERNAL_AUTH_HEADER, internalAuthSecret);

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;

        HttpServletRequest request = attributes.getRequest();
        for (String name : FORWARDED_HEADERS) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                template.header(name, value);
            }
        }
    }
}
