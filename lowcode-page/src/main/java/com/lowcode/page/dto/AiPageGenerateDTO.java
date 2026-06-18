package com.lowcode.page.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiPageGenerateDTO {

    private String sessionId;

    private String userMessage;

    private Long appId;

    private String currentPageJson;

    private List<AiChatMessage> history;
}
