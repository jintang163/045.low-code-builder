package com.lowcode.page.handler;

import com.alibaba.fastjson.JSON;
import com.lowcode.page.service.collaboration.Collaborator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CollaborativeEditorHandler extends TextWebSocketHandler {

    private final Map<String, Map<String, WebSocketSession>> pageSessions = new ConcurrentHashMap<>();

    private final Map<String, Collaborator> collaborators = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String pageId = getPageId(session);
        String sessionId = session.getId();

        log.info("Collaborator connected, pageId: {}, sessionId: {}", pageId, sessionId);

        pageSessions.computeIfAbsent(pageId, k -> new ConcurrentHashMap<>()).put(sessionId, session);

        Collaborator collaborator = new Collaborator();
        collaborator.setSessionId(sessionId);
        collaborator.setPageId(Long.parseLong(pageId));
        collaborator.setJoinTime(new Date());
        collaborator.setLastActiveTime(new Date());
        collaborator.setOnline(true);
        collaborators.put(sessionId, collaborator);

        Map<String, Object> data = new HashMap<>();
        data.put("collaborator", collaborator);
        Map<String, Object> joinMessage = buildMessage("JOIN", data);
        broadcastToPage(pageId, joinMessage, sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String pageId = getPageId(session);
        String sessionId = session.getId();

        try {
            com.alibaba.fastjson.JSONObject wsMessage = JSON.parseObject(message.getPayload());
            String type = wsMessage.getString("type");

            Collaborator collaborator = collaborators.get(sessionId);
            if (collaborator != null) {
                collaborator.setLastActiveTime(new Date());
                com.alibaba.fastjson.JSONObject data = wsMessage.getJSONObject("data");
                String fromUserId = null;
                String fromUsername = null;
                if (data != null) {
                    if (data.containsKey("userId")) {
                        fromUserId = data.getString("userId");
                    }
                    if (data.containsKey("username")) {
                        fromUsername = data.getString("username");
                    }
                } else {
                    if (wsMessage.containsKey("fromUserId")) {
                        fromUserId = wsMessage.getString("fromUserId");
                    }
                    if (wsMessage.containsKey("fromUsername")) {
                        fromUsername = wsMessage.getString("fromUsername");
                    }
                }
                if (fromUserId != null) {
                    collaborator.setUserId(fromUserId);
                }
                if (fromUsername != null) {
                    collaborator.setUsername(fromUsername);
                }
            }

            if ("CURSOR".equalsIgnoreCase(type) || "OPERATION".equalsIgnoreCase(type)
                    || "PRESENCE".equalsIgnoreCase(type)) {
                broadcastToPage(pageId, wsMessage, sessionId);
            } else if ("SYNC".equalsIgnoreCase(type)) {
                session.sendMessage(new TextMessage(JSON.toJSONString(wsMessage)));
            } else if ("PING".equalsIgnoreCase(type) || "HEARTBEAT".equalsIgnoreCase(type)) {
                Map<String, Object> pongData = new HashMap<>();
                if (wsMessage.containsKey("timestamp")) {
                    pongData.put("clientTimestamp", wsMessage.getLong("timestamp"));
                }
                Map<String, Object> pongMsg = buildMessage("PONG", pongData);
                session.sendMessage(new TextMessage(JSON.toJSONString(pongMsg)));
            }

        } catch (Exception e) {
            log.error("Failed to handle message, sessionId: {}", sessionId, e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("code", "MESSAGE_ERROR");
            errorData.put("message", "Failed to handle message: " + e.getMessage());
            Map<String, Object> errorMessage = buildMessage("ERROR", errorData);
            session.sendMessage(new TextMessage(JSON.toJSONString(errorMessage)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String pageId = getPageId(session);
        String sessionId = session.getId();

        log.info("Collaborator disconnected, pageId: {}, sessionId: {}", pageId, sessionId);

        Map<String, WebSocketSession> sessions = pageSessions.get(pageId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                pageSessions.remove(pageId);
            }
        }

        Collaborator collaborator = collaborators.remove(sessionId);

        if (collaborator != null) {
            collaborator.setOnline(false);
            Map<String, Object> data = new HashMap<>();
            data.put("collaborator", collaborator);
            Map<String, Object> leaveMessage = buildMessage("LEAVE", data);
            broadcastToPage(pageId, leaveMessage, null);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error, sessionId: {}", session.getId(), exception);
    }

    private String getPageId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private Map<String, Object> buildMessage(String type, Object data) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        msg.put("data", data != null ? data : new HashMap<>());
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }

    private void broadcastToPage(String pageId, Object message, String excludeSessionId) {
        Map<String, WebSocketSession> sessions = pageSessions.get(pageId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage textMessage = new TextMessage(JSON.toJSONString(message));
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (excludeSessionId != null && excludeSessionId.equals(entry.getKey())) {
                continue;
            }
            WebSocketSession session = entry.getValue();
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (Exception e) {
                    log.error("Failed to send message to session: {}", entry.getKey(), e);
                }
            }
        }
    }

}
