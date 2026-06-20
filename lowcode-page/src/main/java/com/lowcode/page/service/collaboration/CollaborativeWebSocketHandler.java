package com.lowcode.page.service.collaboration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CollaborativeWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private CollaborationService collaborationService;

    @Autowired
    private CRDTEngine crdtEngine;

    private static final String ATTR_PAGE_ID = "pageId";
    private static final String ATTR_SESSION_ID = "sessionId";
    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_USERNAME = "username";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: sessionId={}", session.getId());
        try {
            Map<String, Object> attributes = session.getAttributes();
            String pageIdStr = (String) attributes.get("pageId");
            String userId = (String) attributes.get("userId");
            String username = (String) attributes.get("username");
            String avatar = (String) attributes.get("avatar");

            if (pageIdStr == null || pageIdStr.isEmpty()) {
                log.warn("No pageId in WebSocket session attributes");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            Long pageId = Long.parseLong(pageIdStr);

            session.getAttributes().put(ATTR_PAGE_ID, pageId);
            session.getAttributes().put(ATTR_SESSION_ID, session.getId());
            session.getAttributes().put(ATTR_USER_ID, userId);
            session.getAttributes().put(ATTR_USERNAME, username);

            collaborationService.registerSession(pageId, session.getId(), session);

            Collaborator collaborator = collaborationService.joinPage(
                    pageId, session.getId(), userId, username, avatar);

            sendSyncMessage(session, pageId, session.getId());

            broadcastPresence(pageId, session.getId());

            log.info("User joined via WebSocket: pageId={}, userId={}, sessionId={}",
                    pageId, userId, session.getId());

        } catch (Exception e) {
            log.error("Error in afterConnectionEstablished", e);
            try {
                sendMessage(session, buildErrorMessage("CONNECTION_ERROR", e.getMessage()));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received WebSocket message: sessionId={}", session.getId());

        try {
            String payload = message.getPayload();
            JSONObject msg = JSON.parseObject(payload);
            String type = msg.getString("type");

            if (type == null || type.isEmpty()) {
                sendMessage(session, buildErrorMessage("INVALID_MESSAGE", "Message type is required"));
                return;
            }

            Long pageId = (Long) session.getAttributes().get(ATTR_PAGE_ID);
            String sessionId = (String) session.getAttributes().get(ATTR_SESSION_ID);

            switch (type.toUpperCase()) {
                case "JOIN":
                case "LEAVE":
                    handleJoin(session, msg, pageId, sessionId);
                    break;
                case "OPERATION":
                    handleOperation(session, msg, pageId, sessionId);
                    break;
                case "CURSOR":
                    handleCursor(session, msg, pageId, sessionId);
                    break;
                case "PRESENCE":
                    handlePresence(session, msg, pageId, sessionId);
                    break;
                case "CONFLICT":
                case "RESOLVE":
                    handleResolve(session, msg, pageId, sessionId);
                    break;
                case "SYNC":
                    handleSync(session, msg, pageId, sessionId);
                    break;
                case "HEARTBEAT":
                case "PING":
                    handlePing(session, msg);
                    break;
                case "PONG":
                    log.debug("Received PONG from session: {}", sessionId);
                    break;
                default:
                    log.warn("Unknown message type: {}", type);
                    sendMessage(session, buildErrorMessage("UNKNOWN_TYPE", "Unknown message type: " + type));
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            try {
                sendMessage(session, buildErrorMessage("MESSAGE_ERROR", e.getMessage()));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }

    private void handleJoin(WebSocketSession session, JSONObject msg, Long pageId, String sessionId) throws Exception {
        log.info("Handling JOIN message: sessionId={}", sessionId);

        JSONObject data = msg.getJSONObject("data");
        String userId;
        String username;
        String avatar;

        if (data != null) {
            userId = data.getString("userId");
            username = data.getString("username");
            avatar = data.getString("avatar");
        } else {
            userId = msg.getString("userId");
            username = msg.getString("username");
            avatar = msg.getString("avatar");
        }

        if (userId != null) {
            session.getAttributes().put(ATTR_USER_ID, userId);
        }
        if (username != null) {
            session.getAttributes().put(ATTR_USERNAME, username);
        }

        Collaborator collaborator = collaborationService.joinPage(
                pageId, sessionId,
                userId != null ? userId : (String) session.getAttributes().get(ATTR_USER_ID),
                username != null ? username : (String) session.getAttributes().get(ATTR_USERNAME),
                avatar);

        sendSyncMessage(session, pageId, sessionId);
        broadcastPresence(pageId, sessionId);
    }

    private void handleOperation(WebSocketSession session, JSONObject msg, Long pageId, String sessionId) throws Exception {
        log.debug("Handling OPERATION message: sessionId={}", sessionId);

        JSONObject operationJson = msg.getJSONObject("data");
        if (operationJson == null) {
            operationJson = msg.getJSONObject("operation");
        }
        if (operationJson == null) {
            sendMessage(session, buildErrorMessage("INVALID_OPERATION", "Operation data is required"));
            return;
        }

        CRDTOperation operation = JSON.toJavaObject(operationJson, CRDTOperation.class);

        if (operation.getUserId() == null || operation.getUserId().isEmpty()) {
            operation.setUserId((String) session.getAttributes().get(ATTR_USER_ID));
        }
        if (operation.getUsername() == null || operation.getUsername().isEmpty()) {
            operation.setUsername((String) session.getAttributes().get(ATTR_USERNAME));
        }
        operation.setSessionId(sessionId);

        if (operation.getId() == null || operation.getId().isEmpty()) {
            operation.setId(crdtEngine.generateOperationId());
        }

        collaborationService.broadcastOperation(pageId, operation);

        sendMessage(session, buildOperationAckMessage(operation));

        broadcastOperationToOthers(pageId, sessionId, operation);

        List<ConflictInfo> pendingConflicts = collaborationService.getPendingConflicts(pageId);
        if (!pendingConflicts.isEmpty()) {
            broadcastConflicts(pageId);
        }
    }

    private void handleCursor(WebSocketSession session, JSONObject msg, Long pageId, String sessionId) throws Exception {
        log.debug("Handling CURSOR message: sessionId={}", sessionId);

        JSONObject data = msg.getJSONObject("data");
        Object cursorPositionObj = null;
        String userId = null;

        if (data != null) {
            if (data.containsKey("cursorPosition")) {
                cursorPositionObj = data.get("cursorPosition");
            } else if (data.containsKey("position")) {
                cursorPositionObj = data.get("position");
            }
            userId = data.getString("userId");
        } else {
            if (msg.containsKey("cursorPosition")) {
                cursorPositionObj = msg.get("cursorPosition");
            } else if (msg.containsKey("position")) {
                cursorPositionObj = msg.get("position");
            }
            userId = msg.getString("userId");
        }

        if (cursorPositionObj == null) {
            return;
        }

        collaborationService.broadcastCursor(pageId, sessionId, cursorPositionObj);

        broadcastCursorUpdate(pageId, sessionId, userId, cursorPositionObj);
    }

    private void handlePresence(WebSocketSession session, JSONObject msg, Long pageId, String sessionId) throws Exception {
        log.debug("Handling PRESENCE message: sessionId={}", sessionId);

        List<Collaborator> collaborators = collaborationService.getCollaborators(pageId);
        sendMessage(session, buildPresenceMessage(collaborators));
    }

    private void handleResolve(WebSocketSession session, JSONObject msg, Long pageId, String sessionId) throws Exception {
        log.info("Handling RESOLVE message: sessionId={}", sessionId);

        JSONObject data = msg.getJSONObject("data");
        String conflictId;
        String resolution;
        String chosenUserId;

        if (data != null) {
            conflictId = data.getString("conflictId");
            resolution = data.getString("resolution");
            chosenUserId = data.getString("chosenUserId");
        } else {
            conflictId = msg.getString("conflictId");
            resolution = msg.getString("resolution");
            chosenUserId = msg.getString("chosenUserId");
        }

        if (conflictId == null || conflictId.isEmpty()) {
            sendMessage(session, buildErrorMessage("INVALID_RESOLVE", "Conflict ID is required"));
            return;
        }

        boolean success = collaborationService.resolveConflict(pageId, conflictId, resolution, chosenUserId);

        if (success) {
            sendMessage(session, buildResolveAckMessage(conflictId, true));
            broadcastConflicts(pageId);
        } else {
            sendMessage(session, buildErrorMessage("RESOLVE_FAILED", "Failed to resolve conflict"));
        }
    }

    private void handleSync(WebSocketSession session, JSONObject msg, Long pageId, String sessionId) throws Exception {
        log.info("Handling SYNC message: sessionId={}", sessionId);
        sendSyncMessage(session, pageId, sessionId);
    }

    private void sendSyncMessage(WebSocketSession session, Long pageId, String sessionId) throws Exception {
        DocumentState state = collaborationService.syncDocument(pageId, sessionId);
        List<Collaborator> collaborators = collaborationService.getCollaborators(pageId);
        List<ConflictInfo> conflicts = collaborationService.getPendingConflicts(pageId);

        sendMessage(session, buildSyncMessage(state, collaborators, conflicts));
    }

    private void handlePing(WebSocketSession session, JSONObject msg) throws Exception {
        log.debug("Handling PING/HEARTBEAT message: sessionId={}", session.getId());

        Map<String, Object> data = new HashMap<>();
        if (msg.containsKey("timestamp")) {
            data.put("clientTimestamp", msg.getLong("timestamp"));
        }

        Map<String, Object> pongMsg = buildMessage("PONG", data);
        sendMessage(session, pongMsg);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: sessionId={}, status={}", session.getId(), status);

        try {
            Long pageId = (Long) session.getAttributes().get(ATTR_PAGE_ID);
            String sessionId = (String) session.getAttributes().get(ATTR_SESSION_ID);

            if (pageId != null && sessionId != null) {
                collaborationService.leavePage(pageId, sessionId);
                broadcastPresence(pageId, sessionId);
            }
        } catch (Exception e) {
            log.error("Error in afterConnectionClosed", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: sessionId={}", session.getId(), exception);

        try {
            Long pageId = (Long) session.getAttributes().get(ATTR_PAGE_ID);
            String sessionId = (String) session.getAttributes().get(ATTR_SESSION_ID);

            if (pageId != null && sessionId != null) {
                collaborationService.leavePage(pageId, sessionId);
                broadcastPresence(pageId, sessionId);
            }
        } catch (Exception e) {
            log.error("Error handling transport error cleanup", e);
        }
    }

    private Map<String, Object> buildMessage(String type, Object data) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        msg.put("data", data != null ? data : new HashMap<>());
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws Exception {
        if (session != null && session.isOpen()) {
            String json = JSON.toJSONString(message);
            session.sendMessage(new TextMessage(json));
        }
    }

    private void broadcastOperationToOthers(Long pageId, String excludeSessionId, CRDTOperation operation) {
        Map<String, WebSocketSession> pageSessions = collaborationService.getSessions(pageId);
        if (pageSessions == null || pageSessions.isEmpty()) {
            return;
        }

        Map<String, Object> msg = buildMessage("OPERATION", operation);
        String msgJson = JSON.toJSONString(msg);

        for (Map.Entry<String, WebSocketSession> entry : pageSessions.entrySet()) {
            String sessionId = entry.getKey();
            if (sessionId.equals(excludeSessionId)) {
                continue;
            }

            WebSocketSession session = entry.getValue();
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(msgJson));
                }
            } catch (Exception e) {
                log.error("Failed to broadcast operation to session: {}", sessionId, e);
            }
        }
    }

    private void broadcastPresence(Long pageId, String excludeSessionId) {
        List<Collaborator> collaborators = collaborationService.getCollaborators(pageId);
        Map<String, WebSocketSession> pageSessions = collaborationService.getSessions(pageId);

        if (pageSessions == null || pageSessions.isEmpty()) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("collaborators", collaborators);
        Map<String, Object> msg = buildMessage("PRESENCE", data);
        String msgJson = JSON.toJSONString(msg);

        for (Map.Entry<String, WebSocketSession> entry : pageSessions.entrySet()) {
            String sessionId = entry.getKey();
            if (excludeSessionId != null && sessionId.equals(excludeSessionId)) {
                continue;
            }

            WebSocketSession session = entry.getValue();
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(msgJson));
                }
            } catch (Exception e) {
                log.error("Failed to broadcast collaborator update to session: {}", sessionId, e);
            }
        }
    }

    private void broadcastCursorUpdate(Long pageId, String excludeSessionId, String userId, Object cursorPosition) {
        Map<String, WebSocketSession> pageSessions = collaborationService.getSessions(pageId);
        if (pageSessions == null || pageSessions.isEmpty()) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("sessionId", excludeSessionId);
        data.put("cursorPosition", cursorPosition);
        Map<String, Object> msg = buildMessage("CURSOR", data);
        String msgJson = JSON.toJSONString(msg);

        for (Map.Entry<String, WebSocketSession> entry : pageSessions.entrySet()) {
            String sessionId = entry.getKey();
            if (sessionId.equals(excludeSessionId)) {
                continue;
            }

            WebSocketSession session = entry.getValue();
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(msgJson));
                }
            } catch (Exception e) {
                log.error("Failed to broadcast cursor to session: {}", sessionId, e);
            }
        }
    }

    private void broadcastConflicts(Long pageId) {
        List<ConflictInfo> conflicts = collaborationService.getPendingConflicts(pageId);
        Map<String, WebSocketSession> pageSessions = collaborationService.getSessions(pageId);

        if (pageSessions == null || pageSessions.isEmpty()) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("conflicts", conflicts);
        Map<String, Object> msg = buildMessage("CONFLICT", data);
        String msgJson = JSON.toJSONString(msg);

        for (Map.Entry<String, WebSocketSession> entry : pageSessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(msgJson));
                }
            } catch (Exception e) {
                log.error("Failed to broadcast conflicts to session: {}", entry.getKey(), e);
            }
        }
    }

    private Map<String, Object> buildOperationAckMessage(CRDTOperation operation) {
        Map<String, Object> data = new HashMap<>();
        data.put("operationId", operation.getId());
        data.put("lamportClock", operation.getLamportClock());
        return buildMessage("ACK", data);
    }

    private Map<String, Object> buildPresenceMessage(List<Collaborator> collaborators) {
        Map<String, Object> data = new HashMap<>();
        data.put("collaborators", collaborators);
        return buildMessage("PRESENCE", data);
    }

    private Map<String, Object> buildResolveAckMessage(String conflictId, boolean success) {
        Map<String, Object> data = new HashMap<>();
        data.put("conflictId", conflictId);
        data.put("success", success);
        return buildMessage("ACK", data);
    }

    private Map<String, Object> buildSyncMessage(DocumentState state, List<Collaborator> collaborators,
                                                  List<ConflictInfo> conflicts) {
        Map<String, Object> data = new HashMap<>();
        data.put("documentState", state);
        data.put("collaborators", collaborators);
        data.put("conflicts", conflicts);
        data.put("documentVersion", state != null ? state.getVersion() : 0);
        return buildMessage("SYNC", data);
    }

    private Map<String, Object> buildErrorMessage(String code, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("message", message);
        return buildMessage("ERROR", data);
    }
}
