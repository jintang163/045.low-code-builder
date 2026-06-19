package com.lowcode.model;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.lowcode"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class LowcodeModelApplication {

    public static void main(String[] args) {
        SpringApplication.run(LowcodeModelApplication.class, args);
    }
}
