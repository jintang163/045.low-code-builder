package com.lowcode.gateway.config;

import com.lowcode.common.monitor.report.MonitorReportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(MonitorReportClient.class)
public class GatewayMonitorConfig {

    @Bean
    public MonitorReportClient monitorReportClient() {
        return new MonitorReportClient();
    }
}
