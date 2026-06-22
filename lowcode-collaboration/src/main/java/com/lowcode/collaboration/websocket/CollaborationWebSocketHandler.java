package com.lowcode.collaboration.websocket;

import com.alibaba.fastjson2.JSON;
import com.lowcode.collaboration.entity.Comment;
import com.lowcode.collaboration.entity.DesignHistory;
import com.lowcode.collaboration.entity.TaskAssignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class CollaborationWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> appSessionMap = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> targetSessionMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> sessionUserMap = new ConcurrentHashMap<>();

    private static final UriTemplate URI_TEMPLATE = new UriTemplate("/ws/collaboration/{appId}");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String appId = extractAppId(session);
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("WebSocket连接未携带appId，关闭连接");
            session.close(CloseStatus.BAD_DATA.withReason("appId is required"));
            return;
        }

        Map<String, Object> attrs = session.getAttributes();
        String userId = (String) attrs.getOrDefault("userId", "");
        String username = (String) attrs.getOrDefault("username", "匿名用户");
        String avatar = (String) attrs.getOrDefault("avatar", "");
        String targetType = (String) attrs.getOrDefault("targetType", "");
        String targetId = (String) attrs.getOrDefault("targetId", "");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", username);
        userInfo.put("avatar", avatar);
        userInfo.put("targetType", targetType);
        userInfo.put("targetId", targetId);
        sessionUserMap.put(session.getId(), userInfo);

        appSessionMap.computeIfAbsent(appId, k -> new CopyOnWriteArraySet<>()).add(session);

        if (!targetType.isEmpty() && !targetId.isEmpty()) {
            String targetKey = buildTargetKey(appId, targetType, targetId);
            targetSessionMap.computeIfAbsent(targetKey, k -> new CopyOnWriteArraySet<>()).add(session);
        }

        log.info("WebSocket连接建立: appId={}, userId={}, sessionId={}", appId, userId, session.getId());

        Map<String, Object> welcome = buildMessage("WELCOME", new HashMap<String, Object>() {{
            put("message", "连接成功");
            put("appId", appId);
        }});
        sendMessage(session, welcome);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String appId = extractAppId(session);
        Map<String, Object> userInfo = sessionUserMap.remove(session.getId());

        if (appId != null) {
            Set<WebSocketSession> sessions = appSessionMap.get(appId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    appSessionMap.remove(appId);
                }
            }
        }

        if (userInfo != null && appId != null) {
            String targetType = (String) userInfo.get("targetType");
            String targetId = (String) userInfo.get("targetId");
            if (targetType != null && !targetType.isEmpty() && targetId != null && !targetId.isEmpty()) {
                String targetKey = buildTargetKey(appId, targetType, targetId);
                Set<WebSocketSession> targetSessions = targetSessionMap.get(targetKey);
                if (targetSessions != null) {
                    targetSessions.remove(session);
                    if (targetSessions.isEmpty()) {
                        targetSessionMap.remove(targetKey);
                    }
                }
            }
        }

        log.info("WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            Map<String, Object> msg = JSON.parseObject(payload, Map.class);
            String type = (String) msg.get("type");

            if ("PING".equalsIgnoreCase(type) || "HEARTBEAT".equalsIgnoreCase(type)) {
                Map<String, Object> pong = buildMessage("PONG", new HashMap<String, Object>() {{
                    put("timestamp", System.currentTimeMillis());
                }});
                sendMessage(session, pong);
                return;
            }

            log.debug("收到WebSocket消息: type={}, sessionId={}", type, session.getId());
        } catch (Exception e) {
            log.warn("解析WebSocket消息失败: payload={}", payload, e);
        }
    }

    public void broadcastComment(Long appId, String targetType, Long targetId, Comment comment) {
        Map<String, Object> data = new HashMap<>();
        data.put("comment", comment);
        Map<String, Object> msg = buildMessage("NEW_COMMENT", data);
        broadcastToTarget(appId, targetType, String.valueOf(targetId), msg);
        broadcastToApp(appId, msg);
    }

    public void broadcastHistory(Long appId, String targetType, Long targetId, DesignHistory history) {
        Map<String, Object> data = new HashMap<>();
        data.put("history", history);
        Map<String, Object> msg = buildMessage("NEW_HISTORY", data);
        broadcastToTarget(appId, targetType, String.valueOf(targetId), msg);
    }

    public void broadcastTask(Long appId, String targetType, Long targetId, TaskAssignment task) {
        Map<String, Object> data = new HashMap<>();
        data.put("task", task);
        Map<String, Object> msg = buildMessage("TASK_UPDATE", data);
        broadcastToTarget(appId, targetType, String.valueOf(targetId), msg);
        if (task.getAssigneeId() != null) {
            broadcastToUser(String.valueOf(task.getAssigneeId()), msg);
        }
    }

    public void broadcastNotification(Long userId, String notificationType, Object content) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", notificationType);
        data.put("content", content);
        Map<String, Object> msg = buildMessage("NOTIFICATION", data);
        broadcastToUser(String.valueOf(userId), msg);
    }

    private void broadcastToApp(Long appId, Map<String, Object> message) {
        Set<WebSocketSession> sessions = appSessionMap.get(String.valueOf(appId));
        sendToSessions(sessions, message);
    }

    private void broadcastToTarget(Long appId, String targetType, String targetId, Map<String, Object> message) {
        String targetKey = buildTargetKey(String.valueOf(appId), targetType, targetId);
        Set<WebSocketSession> sessions = targetSessionMap.get(targetKey);
        sendToSessions(sessions, message);
    }

    private void broadcastToUser(String userId, Map<String, Object> message) {
        if (userId == null || userId.isEmpty()) return;
        for (Map.Entry<String, Map<String, Object>> entry : sessionUserMap.entrySet()) {
            if (userId.equals(entry.getValue().get("userId"))) {
                for (Set<WebSocketSession> sessions : appSessionMap.values()) {
                    for (WebSocketSession session : sessions) {
                        if (session.getId().equals(entry.getKey())) {
                            sendMessage(session, message);
                        }
                    }
                }
            }
        }
    }

    private void sendToSessions(Set<WebSocketSession> sessions, Map<String, Object> message) {
        if (sessions == null || sessions.isEmpty()) return;
        String json = JSON.toJSONString(message);
        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.warn("发送WebSocket消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
                }
            }
        }
    }

    private Map<String, Object> buildMessage(String type, Object data) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        msg.put("data", data != null ? data : new HashMap<>());
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        if (session != null && session.isOpen()) {
            try {
                String json = JSON.toJSONString(message);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.warn("发送WebSocket消息失败: sessionId={}", session.getId(), e);
            }
        }
    }

    private String buildTargetKey(String appId, String targetType, String targetId) {
        return appId + ":" + targetType + ":" + targetId;
    }

    private String extractAppId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String path = uri.getPath();
        try {
            Map<String, String> variables = URI_TEMPLATE.match(path);
            return variables.get("appId");
        } catch (Exception e) {
            log.warn("解析WebSocket路径失败: path={}, error={}", path, e.getMessage());
            return null;
        }
    }
}
