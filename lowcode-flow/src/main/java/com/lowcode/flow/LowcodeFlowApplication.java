package com.lowcode.flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.lowcode"}, exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableFeignClients
public class LowcodeFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(LowcodeFlowApplication.class, args);
    }
}
