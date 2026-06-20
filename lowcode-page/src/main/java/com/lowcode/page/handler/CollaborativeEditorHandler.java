package com.lowcode.page.handler;

import com.alibaba.fastjson.JSON;
import com.lowcode.page.entity.collaboration.Collaborator;
import com.lowcode.page.entity.collaboration.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Date;
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

        WebSocketMessage joinMessage = WebSocketMessage.join(
                JSON.toJSONString(collaborator),
                null,
                null
        );
        broadcastToPage(pageId, joinMessage, sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String pageId = getPageId(session);
        String sessionId = session.getId();

        try {
            WebSocketMessage wsMessage = WebSocketMessage.fromJson(message.getPayload());

            Collaborator collaborator = collaborators.get(sessionId);
            if (collaborator != null) {
                collaborator.setLastActiveTime(new Date());
                if (wsMessage.getFromUserId() != null) {
                    collaborator.setUserId(Long.parseLong(wsMessage.getFromUserId()));
                }
                if (wsMessage.getFromUsername() != null) {
                    collaborator.setUsername(wsMessage.getFromUsername());
                }
            }

            if ("CURSOR".equals(wsMessage.getType()) || "OPERATION".equals(wsMessage.getType())
                    || "PRESENCE".equals(wsMessage.getType())) {
                broadcastToPage(pageId, wsMessage, sessionId);
            } else if ("SYNC".equals(wsMessage.getType())) {
                session.sendMessage(new TextMessage(wsMessage.toJson()));
            }

        } catch (Exception e) {
            log.error("Failed to handle message, sessionId: {}", sessionId, e);
            WebSocketMessage errorMessage = WebSocketMessage.error(
                    "Failed to handle message: " + e.getMessage(),
                    null,
                    null
            );
            session.sendMessage(new TextMessage(errorMessage.toJson()));
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
            WebSocketMessage leaveMessage = WebSocketMessage.leave(
                    JSON.toJSONString(collaborator),
                    String.valueOf(collaborator.getUserId()),
                    collaborator.getUsername()
            );
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

    private void broadcastToPage(String pageId, WebSocketMessage message, String excludeSessionId) {
        Map<String, WebSocketSession> sessions = pageSessions.get(pageId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage textMessage = new TextMessage(message.toJson());
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
