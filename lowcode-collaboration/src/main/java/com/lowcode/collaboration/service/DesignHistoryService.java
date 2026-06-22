package com.lowcode.collaboration.service;

import com.lowcode.collaboration.dto.DesignHistoryCreateDTO;
import com.lowcode.collaboration.entity.DesignHistory;

import java.util.List;

public interface DesignHistoryService {

    DesignHistory createHistory(DesignHistoryCreateDTO dto);

    List<DesignHistory> getHistoryByTarget(Long appId, String targetType, Long targetId, Integer limit);
}
