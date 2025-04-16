package com.ph.phpictureback.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "spring.redis")
public class RedissonConfiguration {

    private String host;
    private String port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String format = String.format("redis://%s:%s", host, port);
        config.useSingleServer().setAddress(format);
        return Redisson.create(config);
    }
}
