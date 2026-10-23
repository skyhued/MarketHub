package com.markethub.order.config;

import com.markethub.common.feign.InternalAuthRequestInterceptor;
import com.markethub.common.feign.RErrorDecoder;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignAuthConfig {

    @Value("${INTERNAL_AUTH_SECRET}")
    private String internalAuthSecret;

    @Bean
    public RequestInterceptor authHeaderForwarder() {
        return new InternalAuthRequestInterceptor(internalAuthSecret);
    }

    /** 把下游 R 错误体还原成 BusinessException，保住原始提示。 */
    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new RErrorDecoder();
    }
}
