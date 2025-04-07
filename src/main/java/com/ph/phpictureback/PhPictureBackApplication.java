package com.ph.phpictureback;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@SpringBootApplication
@MapperScan("com.ph.phpictureback.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling
public class PhPictureBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhPictureBackApplication.class, args);
    }

}
