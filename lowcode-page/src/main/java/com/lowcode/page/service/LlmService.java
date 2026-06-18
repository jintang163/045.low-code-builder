package com.lowcode.page.service;

import com.lowcode.page.dto.AiChatMessage;

import java.util.List;

public interface LlmService {

    String chat(List<AiChatMessage> messages);

    String chatWithJson(List<AiChatMessage> messages, double temperature, int maxTokens);
}
