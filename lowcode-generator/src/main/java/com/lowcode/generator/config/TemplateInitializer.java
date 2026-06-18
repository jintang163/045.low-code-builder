package com.lowcode.generator.config;

import com.lowcode.generator.service.AppTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(100)
public class TemplateInitializer implements ApplicationRunner {

    @Autowired
    private AppTemplateService appTemplateService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("开始初始化内置应用模板...");
            appTemplateService.initBuiltinTemplates();
            log.info("内置应用模板初始化完成");
        } catch (Exception e) {
            log.error("内置应用模板初始化失败", e);
        }
    }
}
