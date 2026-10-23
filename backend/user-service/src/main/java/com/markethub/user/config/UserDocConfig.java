package com.markethub.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class UserDocConfig {
    @Bean
    @Primary
    public OpenAPI userOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MarketHub 用户服务 API")
                        .version("1.0.0")
                        .description("用户服务：注册登录、RBAC权限管理、用户管理\n分布式架构：Nacos + OpenFeign + Sentinel"));
    }
}
