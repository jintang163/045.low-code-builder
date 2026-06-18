package com.lowcode.page.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessage {

    private String role;

    private String content;

    public static AiChatMessage user(String content) {
        return new AiChatMessage("user", content);
    }

    public static AiChatMessage assistant(String content) {
        return new AiChatMessage("assistant", content);
    }

    public static AiChatMessage system(String content) {
        return new AiChatMessage("system", content);
    }
}
