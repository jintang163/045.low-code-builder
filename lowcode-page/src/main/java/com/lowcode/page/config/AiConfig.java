package com.lowcode.page.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.llm")
public class AiConfig {

    private Boolean enabled = true;

    private String provider = "openai";

    private String apiKey;

    private String endpoint = "https://api.openai.com/v1";

    private String model = "gpt-4o-mini";

    private Double temperature = 0.3;

    private Integer maxTokens = 4096;

    private Integer timeout = 60;
}
