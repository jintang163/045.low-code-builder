package com.lowcode.collaboration.service.impl;

import com.lowcode.collaboration.dto.DesignHistoryCreateDTO;
import com.lowcode.collaboration.entity.DesignHistory;
import com.lowcode.collaboration.mapper.DesignHistoryMapper;
import com.lowcode.collaboration.service.DesignHistoryService;
import com.lowcode.collaboration.websocket.CollaborationWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DesignHistoryServiceImpl implements DesignHistoryService {

    @Autowired
    private DesignHistoryMapper historyMapper;

    @Autowired(required = false)
    private CollaborationWebSocketHandler webSocketHandler;

    @Override
    public DesignHistory createHistory(DesignHistoryCreateDTO dto) {
        DesignHistory history = new DesignHistory();
        BeanUtils.copyProperties(dto, history);
        historyMapper.insert(history);

        DesignHistory result = historyMapper.selectById(history.getId());

        if (webSocketHandler != null) {
            try {
                webSocketHandler.broadcastHistory(dto.getAppId(), dto.getTargetType(), dto.getTargetId(), result);
            } catch (Exception e) {
                log.warn("WebSocket推送历史记录消息失败", e);
            }
        }

        return result;
    }

    @Override
    public List<DesignHistory> getHistoryByTarget(Long appId, String targetType, Long targetId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 100;
        }
        return historyMapper.selectByTarget(appId, targetType, targetId, limit);
    }
}
