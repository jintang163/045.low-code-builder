package com.lowcode.common.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class EncryptConfig {

    @Value("${lowcode.encrypt.aes.key:LowcodePlatform2024SecureKey32B}")
    private String aesKey;
}
