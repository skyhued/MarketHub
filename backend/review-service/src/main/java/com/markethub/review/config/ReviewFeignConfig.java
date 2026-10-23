package com.markethub.review.config;

import com.markethub.common.feign.InternalAuthRequestInterceptor;
import com.markethub.common.feign.RErrorDecoder;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReviewFeignConfig {

    @Value("${INTERNAL_AUTH_SECRET}")
    private String internalAuthSecret;

    @Bean
    public RequestInterceptor reviewAuthHeaderForwarder() {
        return new InternalAuthRequestInterceptor(internalAuthSecret);
    }

    @Bean
    public ErrorDecoder reviewFeignErrorDecoder() {
        return new RErrorDecoder();
    }
}
