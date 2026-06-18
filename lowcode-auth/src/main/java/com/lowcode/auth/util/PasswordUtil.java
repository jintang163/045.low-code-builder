package com.lowcode.auth.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.core.util.RandomUtil;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    public String generateSalt() {
        return RandomUtil.randomString(16);
    }

    public String encryptPassword(String password, String salt) {
        return SecureUtil.md5(password + salt);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword, String salt) {
        return SecureUtil.md5(rawPassword + salt).equals(encodedPassword);
    }
}
