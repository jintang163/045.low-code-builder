package com.lowcode.flow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.flow.entity.AppExposedEvent;
import com.lowcode.flow.mapper.AppExposedEventMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AppExposedEventService extends ServiceImpl<AppExposedEventMapper, AppExposedEvent> {

    public AppExposedEvent getByCode(String eventCode) {
        LambdaQueryWrapper<AppExposedEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppExposedEvent::getEventCode, eventCode);
        return getOne(wrapper, false);
    }

    public List<AppExposedEvent> listByAppCode(String appCode) {
        LambdaQueryWrapper<AppExposedEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppExposedEvent::getAppCode, appCode)
                .eq(AppExposedEvent::getStatus, 1)
                .orderByDesc(AppExposedEvent::getCreatedTime);
        return list(wrapper);
    }

    public List<AppExposedEvent> listByApp(Long appId) {
        LambdaQueryWrapper<AppExposedEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppExposedEvent::getAppId, appId)
                .orderByDesc(AppExposedEvent::getCreatedTime);
        return list(wrapper);
    }

    public Page<AppExposedEvent> pageEvents(Integer current, Integer size, Long appId, String keyword) {
        LambdaQueryWrapper<AppExposedEvent> wrapper = new LambdaQueryWrapper<>();
        if (appId != null) {
            wrapper.eq(AppExposedEvent::getAppId, appId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AppExposedEvent::getEventName, keyword)
                    .or().like(AppExposedEvent::getEventCode, keyword);
        }
        wrapper.orderByDesc(AppExposedEvent::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    public AppExposedEvent saveEvent(AppExposedEvent event) {
        if (event.getId() == null) {
            if (event.getEventCode() == null || event.getEventCode().trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "事件编码不能为空");
            }
            AppExposedEvent exist = getByCode(event.getEventCode());
            if (exist != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "事件编码已存在：" + event.getEventCode());
            }
            if (event.getCreatedTime() == null) {
                event.setCreatedTime(LocalDateTime.now());
            }
            if (event.getStatus() == null) {
                event.setStatus(1);
            }
            save(event);
        } else {
            event.setUpdatedTime(LocalDateTime.now());
            updateById(event);
        }
        return getById(event.getId());
    }

    public void deleteEvent(Long id) {
        removeById(id);
    }
}
