package com.lowcode.model;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.lowcode"})
@EnableDiscoveryClient
@EnableFeignClients
public class LowcodeModelApplication {

    public static void main(String[] args) {
        SpringApplication.run(LowcodeModelApplication.class, args);
    }
}
