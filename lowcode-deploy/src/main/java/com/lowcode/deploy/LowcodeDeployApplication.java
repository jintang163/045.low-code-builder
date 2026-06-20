package com.lowcode.deploy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"com.lowcode"})
@EnableDiscoveryClient
@EnableAsync
@MapperScan("com.lowcode.deploy.mapper")
public class LowcodeDeployApplication {

    public static void main(String[] args) {
        SpringApplication.run(LowcodeDeployApplication.class, args);
    }
}
