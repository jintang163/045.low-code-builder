package com.lowcode.collaboration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.lowcode"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@MapperScan("com.lowcode.collaboration.mapper")
public class LowcodeCollaborationApplication {

    public static void main(String[] args) {
        SpringApplication.run(LowcodeCollaborationApplication.class, args);
    }
}
