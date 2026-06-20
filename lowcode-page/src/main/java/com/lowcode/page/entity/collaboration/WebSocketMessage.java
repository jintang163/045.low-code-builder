package com.lowcode.page.entity.collaboration;

import com.alibaba.fastjson.JSON;
import lombok.Data;

import java.util.UUID;

@Data
public class WebSocketMessage {

    private String type;

    private String payload;

    private String fromUserId;

    private String fromUsername;

    private long timestamp;

    private String messageId;

    public static WebSocketMessage join(String payload, String fromUserId, String fromUsername) {
        return build("JOIN", payload, fromUserId, fromUsername);
    }

    public static WebSocketMessage leave(String payload, String fromUserId, String fromUsername) {
        return build("LEAVE", payload, fromUserId, fromUsername);
    }

    public static WebSocketMessage operation(String payload, String fromUserId, String fromUsername) {
        return build("OPERATION", payload, fromUserId, fromUsername);
    }

    public static WebSocketMessage cursor(String payload, String fromUserId, String fromUsername) {
        return build("CURSOR", payload, fromUserId, fromUsername);
    }

    public static WebSocketMessage presence(String payload, String fromUserId, String fromUsername) {
        return build("PRESENCE", payload, fromUserId, fromUsername);
    }

    public static WebSocketMessage conflict(String payload, String fromUserId, String fromUsername) {
        return build("CONFLICT", payload, fromUserId, fromUsername);
    }

    public static WebSocketMessage resolve(String payload, String fromUserId, String fromUsername) {
        return build("RESOLVE", payload, fromUserId, fromUsername);
    }

    public static WebSocketMessage sync(String payload, String fromUserId, String fromUsername) {
        return build("SYNC", payload, fromUserId, fromUsername);
    }

    public static WebSocketMessage ack(String payload, String fromUserId, String fromUsername) {
        return build("ACK", payload, fromUserId, fromUsername);
    }

    public static WebSocketMessage error(String payload, String fromUserId, String fromUsername) {
        return build("ERROR", payload, fromUserId, fromUsername);
    }

    private static WebSocketMessage build(String type, String payload, String fromUserId, String fromUsername) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType(type);
        message.setPayload(payload);
        message.setFromUserId(fromUserId);
        message.setFromUsername(fromUsername);
        message.setTimestamp(System.currentTimeMillis());
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        return message;
    }

    public String toJson() {
        return JSON.toJSONString(this);
    }

    public static WebSocketMessage fromJson(String json) {
        return JSON.parseObject(json, WebSocketMessage.class);
    }

}
