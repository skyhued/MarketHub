package com.markethub.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class SeckillConfig {

    @Bean
    public DefaultRedisScript<Long> seckillGrabScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/seckill_grab.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
