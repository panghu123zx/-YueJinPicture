package com.ph.phpictureback.config;


import io.github.briqt.spark4j.SparkClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 星火AI的配置
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "xun-fei.client")
public class XingHuoConfig {
    private String appId;
    private String apiSecret;
    private String apiKey;


    @Bean
    public SparkClient sparkClient(){
        SparkClient sparkClient = new SparkClient();
        sparkClient.apiKey=apiKey;
        sparkClient.apiSecret=apiSecret;
        sparkClient.appid=appId;
        return sparkClient;
    }
}
