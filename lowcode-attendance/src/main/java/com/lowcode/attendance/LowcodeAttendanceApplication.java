package com.lowcode.attendance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages = {"com.lowcode"})
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.lowcode.attendance.mapper")
@EnableSwagger2
public class LowcodeAttendanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LowcodeAttendanceApplication.class, args);
    }
}
