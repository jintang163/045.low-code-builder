package com.lowcode.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.lowcode.generator", "com.lowcode.model"})
@EnableDiscoveryClient
@EnableFeignClients
public class LowcodeGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(LowcodeGeneratorApplication.class, args);
    }
}
