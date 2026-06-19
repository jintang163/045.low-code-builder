package com.lowcode.model.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.model.entity.VirtualView;
import com.lowcode.model.entity.VirtualViewJoin;
import com.lowcode.model.mapper.VirtualViewMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class VirtualViewService extends ServiceImpl<VirtualViewMapper, VirtualView> {

    public VirtualView saveVirtualView(VirtualView virtualView) {
        LambdaQueryWrapper<VirtualView> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VirtualView::getViewCode, virtualView.getViewCode());
        wrapper.eq(VirtualView::getAppId, virtualView.getAppId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.VIRTUAL_VIEW_EXISTS);
        }
        save(virtualView);
        return virtualView;
    }

    public VirtualView updateVirtualView(VirtualView virtualView) {
        VirtualView existing = getById(virtualView.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "虚拟视图不存在");
        }
        updateById(virtualView);
        return getById(virtualView.getId());
    }

    public void deleteVirtualView(Long id) {
        VirtualView existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "虚拟视图不存在");
        }
        removeById(id);
    }

    public List<VirtualView> listByAppId(Long appId) {
        LambdaQueryWrapper<VirtualView> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VirtualView::getAppId, appId);
        wrapper.orderByDesc(VirtualView::getCreatedTime);
        return list(wrapper);
    }

    public List<VirtualViewJoin> parseJoinConfig(String joinConfig) {
        if (joinConfig == null || joinConfig.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return JSON.parseObject(joinConfig, new TypeReference<List<VirtualViewJoin>>() {});
    }
}
