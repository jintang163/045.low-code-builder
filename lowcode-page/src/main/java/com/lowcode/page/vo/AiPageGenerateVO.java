package com.lowcode.page.vo;

import com.lowcode.page.dto.AiChatMessage;
import lombok.Data;

import java.util.List;

@Data
public class AiPageGenerateVO {

    private String sessionId;

    private String pageJson;

    private String pageName;

    private String replyMessage;

    private List<AiChatMessage> history;

    private Boolean success;

    private String errorMessage;
}
