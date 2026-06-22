package com.lowcode.collaboration.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
public class CollaborationWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private CollaborationWebSocketHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/collaboration/{appId}")
                .addInterceptors(new CollaborationHandshakeInterceptor())
                .setAllowedOrigins("*");
        log.info("WebSocket collaboration endpoint registered: /ws/collaboration/{{appId}}");
    }

    private static class CollaborationHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                HttpServletRequest httpRequest = servletRequest.getServletRequest();

                String userId = httpRequest.getParameter("userId");
                String username = httpRequest.getParameter("username");
                String avatar = httpRequest.getParameter("avatar");
                String targetType = httpRequest.getParameter("targetType");
                String targetId = httpRequest.getParameter("targetId");

                if (userId == null || userId.isEmpty()) {
                    log.warn("WebSocket handshake failed: userId is required");
                    return false;
                }

                attributes.put("userId", userId);
                attributes.put("username", username != null ? username : "匿名用户");
                attributes.put("avatar", avatar != null ? avatar : "");
                attributes.put("targetType", targetType != null ? targetType : "");
                attributes.put("targetId", targetId != null ? targetId : "");

                log.debug("WebSocket handshake success: userId={}, targetType={}, targetId={}", userId, targetType, targetId);
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Exception exception) {
            if (exception != null) {
                log.error("WebSocket handshake error", exception);
            }
        }
    }
}
