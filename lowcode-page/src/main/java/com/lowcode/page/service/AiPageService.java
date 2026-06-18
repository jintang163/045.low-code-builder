package com.lowcode.page.service;

import com.lowcode.page.dto.AiPageGenerateDTO;
import com.lowcode.page.vo.AiPageGenerateVO;

public interface AiPageService {

    AiPageGenerateVO generatePage(AiPageGenerateDTO dto);

    String generateSessionId();

    void clearSession(String sessionId);
}
