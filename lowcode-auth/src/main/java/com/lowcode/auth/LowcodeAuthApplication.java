package com.lowcode.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.lowcode.auth.mapper")
public class LowcodeAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(LowcodeAuthApplication.class, args);
    }
}
