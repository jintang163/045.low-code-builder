package com.lowcode.common.util;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class UserContext {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USERNAME = "admin";

    public static Long getCurrentUserId() {
        try {
            String token = getTokenFromRequest();
            if (token == null) {
                return DEFAULT_USER_ID;
            }
            JwtUtil jwtUtil = new JwtUtil();
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userId != null ? userId : DEFAULT_USER_ID;
        } catch (Exception e) {
            log.warn("获取当前用户ID失败，使用默认值: {}", e.getMessage());
            return DEFAULT_USER_ID;
        }
    }

    public static String getCurrentUsername() {
        try {
            String token = getTokenFromRequest();
            if (token == null) {
                return DEFAULT_USERNAME;
            }
            JwtUtil jwtUtil = new JwtUtil();
            String username = jwtUtil.getUsernameFromToken(token);
            return username != null ? username : DEFAULT_USERNAME;
        } catch (Exception e) {
            log.warn("获取当前用户名失败，使用默认值: {}", e.getMessage());
            return DEFAULT_USERNAME;
        }
    }

    public static Long getCurrentUserId(Long defaultValue) {
        try {
            String token = getTokenFromRequest();
            if (token == null) {
                return defaultValue;
            }
            JwtUtil jwtUtil = new JwtUtil();
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userId != null ? userId : defaultValue;
        } catch (Exception e) {
            log.warn("获取当前用户ID失败: {}", e.getMessage());
            return defaultValue;
        }
    }

    public static String getCurrentUsername(String defaultValue) {
        try {
            String token = getTokenFromRequest();
            if (token == null) {
                return defaultValue;
            }
            JwtUtil jwtUtil = new JwtUtil();
            String username = jwtUtil.getUsernameFromToken(token);
            return username != null ? username : defaultValue;
        } catch (Exception e) {
            log.warn("获取当前用户名失败: {}", e.getMessage());
            return defaultValue;
        }
    }

    public static Claims getCurrentUserClaims() {
        try {
            String token = getTokenFromRequest();
            if (token == null) {
                return null;
            }
            JwtUtil jwtUtil = new JwtUtil();
            return jwtUtil.parseToken(token);
        } catch (Exception e) {
            log.warn("获取当前用户Claims失败: {}", e.getMessage());
            return null;
        }
    }

    private static String getTokenFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader(AUTHORIZATION_HEADER);
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            return token;
        } catch (Exception e) {
            log.warn("从请求中获取token失败: {}", e.getMessage());
            return null;
        }
    }
}
