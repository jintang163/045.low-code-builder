package com.lowcode.page.config;

import com.lowcode.page.service.collaboration.CollaborativeWebSocketHandler;
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
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private CollaborativeWebSocketHandler collaborativeWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(collaborativeWebSocketHandler, "/ws/collaboration")
                .addInterceptors(new CollaborationHandshakeInterceptor())
                .setAllowedOrigins("*");

        log.info("WebSocket collaboration endpoint registered: /ws/collaboration");
    }

    private static class CollaborationHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                HttpServletRequest httpRequest = servletRequest.getServletRequest();

                String pageId = httpRequest.getParameter("pageId");
                String userId = httpRequest.getParameter("userId");
                String username = httpRequest.getParameter("username");
                String avatar = httpRequest.getParameter("avatar");

                if (pageId == null || pageId.isEmpty()) {
                    log.warn("WebSocket handshake failed: pageId is required");
                    return false;
                }

                attributes.put("pageId", pageId);
                attributes.put("userId", userId != null ? userId : "anonymous");
                attributes.put("username", username != null ? username : "匿名用户");
                attributes.put("avatar", avatar);

                log.debug("WebSocket handshake success: pageId={}, userId={}", pageId, userId);
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
