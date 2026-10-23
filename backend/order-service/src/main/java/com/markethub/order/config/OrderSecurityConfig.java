package com.markethub.order.config;

import com.markethub.common.security.GatewayAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class OrderSecurityConfig {

    @Bean
    public SecurityFilterChain orderSecurityFilterChain(HttpSecurity http, GatewayAuthenticationService gatewayAuthenticationService) throws Exception {
        http.securityMatcher("/api/**", "/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 服务间接口只认内部认证密钥，没有用户身份，也不对外暴露
                        .requestMatchers("/internal/**").hasAuthority(GatewayAuthenticationService.INTERNAL_AUTHORITY)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/doc.html").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore((request, response, chain) -> {
                    HttpServletRequest httpRequest = (HttpServletRequest) request;
                    gatewayAuthenticationService.authenticateRequest(httpRequest);
                    chain.doFilter(request, response);
                }, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
