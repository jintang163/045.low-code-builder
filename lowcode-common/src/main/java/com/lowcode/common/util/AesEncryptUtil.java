package com.lowcode.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
public class AesEncryptUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String DEFAULT_KEY = "LowcodePlatform2024SecureKey32B";

    private static SecretKeySpec getKeySpec() {
        byte[] keyBytes = DEFAULT_KEY.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
        return new SecretKeySpec(key, ALGORITHM);
    }

    public static String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = getKeySpec();
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return "AES(" + Base64.getEncoder().encodeToString(combined) + ")";
        } catch (Exception e) {
            log.error("AES加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密失败", e);
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            if (encryptedText == null || !encryptedText.startsWith("AES(") || !encryptedText.endsWith(")")) {
                return encryptedText;
            }

            String base64Content = encryptedText.substring(4, encryptedText.length() - 1);
            byte[] combined = Base64.getDecoder().decode(base64Content);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = getKeySpec();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("解密失败", e);
        }
    }

    public static boolean isEncrypted(String text) {
        return text != null && text.startsWith("AES(") && text.endsWith(")");
    }
}
