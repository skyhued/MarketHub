package com.markethub.user.config;

import com.markethub.common.feign.InternalAuthRequestInterceptor;
import com.markethub.common.feign.RErrorDecoder;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 为本服务的 Feign 调用注册内部认证拦截器。 */
@Configuration
public class FeignAuthConfig {

    @Value("${INTERNAL_AUTH_SECRET}")
    private String internalAuthSecret;

    @Bean
    public RequestInterceptor authHeaderForwarder() {
        return new InternalAuthRequestInterceptor(internalAuthSecret);
    }

    /** 把下游的 R 错误体还原成 BusinessException，保住原始的错误信息 */
    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new RErrorDecoder();
    }
}
