package com.lowcode.deploy.websocket;

import com.alibaba.fastjson2.JSON;
import com.lowcode.deploy.entity.DeployProgressEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class DeployWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();

    private static final UriTemplate URI_TEMPLATE = new UriTemplate("/ws/deploy/{taskId}");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String taskId = extractTaskId(session);
        if (taskId == null || taskId.trim().isEmpty()) {
            log.warn("WebSocket连接未携带taskId，关闭连接");
            session.close(CloseStatus.BAD_DATA.withReason("taskId is required"));
            return;
        }
        sessionMap.computeIfAbsent(taskId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("WebSocket连接建立: taskId={}, sessionId={}, 当前连接数={}",
                taskId, session.getId(), sessionMap.get(taskId).size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String taskId = extractTaskId(session);
        if (taskId != null) {
            Set<WebSocketSession> sessions = sessionMap.get(taskId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionMap.remove(taskId);
                }
                log.info("WebSocket连接关闭: taskId={}, sessionId={}, 剩余连接数={}",
                        taskId, session.getId(), sessions.size());
            }
        }
    }

    public void pushProgress(DeployProgressEvent event) {
        if (event == null || event.getTaskId() == null) {
            return;
        }
        String json = JSON.toJSONString(event);
        sendToTaskSessions(event.getTaskId(), json);
    }

    public void pushLog(String taskId, String message) {
        if (taskId == null || message == null) {
            return;
        }
        DeployProgressEvent event = DeployProgressEvent.builder()
                .taskId(taskId)
                .message(message)
                .logLevel("INFO")
                .build();
        String json = JSON.toJSONString(event);
        sendToTaskSessions(taskId, json);
    }

    public void sendError(String taskId, String msg) {
        if (taskId == null || msg == null) {
            return;
        }
        DeployProgressEvent event = DeployProgressEvent.builder()
                .taskId(taskId)
                .message(msg)
                .logLevel("ERROR")
                .build();
        String json = JSON.toJSONString(event);
        sendToTaskSessions(taskId, json);
    }

    private void sendToTaskSessions(String taskId, String message) {
        Set<WebSocketSession> sessions = sessionMap.get(taskId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.warn("发送WebSocket消息失败: taskId={}, sessionId={}, error={}",
                            taskId, session.getId(), e.getMessage());
                }
            }
        }
    }

    private String extractTaskId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String path = uri.getPath();
        try {
            Map<String, String> variables = URI_TEMPLATE.match(path);
            return variables.get("taskId");
        } catch (Exception e) {
            log.warn("解析WebSocket路径失败: path={}, error={}", path, e.getMessage());
            return null;
        }
    }
}
