package com.markethub.recommend.config;

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
public class RecommendSecurityConfig {

    @Bean
    public SecurityFilterChain recommendSecurityFilterChain(HttpSecurity http, GatewayAuthenticationService gatewayAuthenticationService) throws Exception {
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 未登录时 /me 退化成热门推荐，这几个接口不要求登录
                        .requestMatchers("/api/recommend/me", "/api/recommend/hot", "/api/recommend/related/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/doc.html").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore((request, response, chain) -> {
                    HttpServletRequest httpRequest = (HttpServletRequest) request;
                    gatewayAuthenticationService.authenticate(httpRequest);
                    chain.doFilter(request, response);
                }, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
