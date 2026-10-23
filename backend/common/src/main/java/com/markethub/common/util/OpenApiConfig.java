package com.markethub.common.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MarketHub 二手交易平台 API")
                        .version("1.0.0")
                        .description("MarketHub 分布式二手交易平台 RESTful API 文档\n\n" +
                                "技术栈：Spring Boot 3.4 + Spring Cloud 2024 + MyBatis + Redis + RabbitMQ\n" +
                                "模块：用户服务、商品服务、订单服务、购物车服务、评价服务、推荐服务")
                        .contact(new Contact()
                                .name("MarketHub Team")
                                .email("contact@markethub.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
