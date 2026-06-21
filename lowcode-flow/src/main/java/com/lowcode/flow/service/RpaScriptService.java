package com.lowcode.flow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.flow.dto.RpaScriptCreateDTO;
import com.lowcode.flow.entity.RpaScript;
import com.lowcode.flow.mapper.RpaScriptMapper;
import com.lowcode.flow.rpa.RpaExecutorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RpaScriptService extends ServiceImpl<RpaScriptMapper, RpaScript> {

    @Autowired
    private RpaExecutorClient rpaExecutorClient;

    public List<RpaScript> getScriptList(Long appId) {
        LambdaQueryWrapper<RpaScript> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpaScript::getAppId, appId);
        wrapper.orderByDesc(RpaScript::getCreatedTime);
        return list(wrapper);
    }

    public Page<RpaScript> getScriptPage(Long appId, Integer current, Integer size) {
        LambdaQueryWrapper<RpaScript> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpaScript::getAppId, appId);
        wrapper.orderByDesc(RpaScript::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    public RpaScript getScriptDetail(Long id) {
        RpaScript script = getById(id);
        if (script == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "RPA脚本不存在");
        }
        return script;
    }

    public RpaScript createScript(RpaScriptCreateDTO dto) {
        LambdaQueryWrapper<RpaScript> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpaScript::getScriptCode, dto.getScriptCode());
        wrapper.eq(RpaScript::getAppId, dto.getAppId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("脚本编码已存在");
        }

        RpaScript script = new RpaScript();
        BeanUtils.copyProperties(dto, script);
        script.setStatus("DRAFT");
        script.setVersion("1.0.0");
        save(script);

        return getScriptDetail(script.getId());
    }

    public RpaScript updateScript(Long id, RpaScriptCreateDTO dto) {
        RpaScript existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "RPA脚本不存在");
        }

        if (!existing.getScriptCode().equals(dto.getScriptCode())) {
            LambdaQueryWrapper<RpaScript> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RpaScript::getScriptCode, dto.getScriptCode());
            wrapper.eq(RpaScript::getAppId, dto.getAppId());
            Long count = count(wrapper);
            if (count > 0) {
                throw new BusinessException("脚本编码已存在");
            }
        }

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        return getScriptDetail(id);
    }

    public void deleteScript(Long id) {
        RpaScript existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "RPA脚本不存在");
        }
        removeById(id);
    }

    public String validateScript(Long id) {
        RpaScript script = getScriptDetail(id);
        return rpaExecutorClient.validateScript(script.getScriptContent());
    }

    public RpaScript publishScript(Long id) {
        RpaScript script = getScriptDetail(id);
        script.setStatus("PUBLISHED");
        script.setVersion(incrementVersion(script.getVersion()));
        updateById(script);
        return script;
    }

    private String incrementVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "1.0.0";
        }
        String[] parts = version.split("\\.");
        int patch = Integer.parseInt(parts[2]) + 1;
        return parts[0] + "." + parts[1] + "." + patch;
    }
}
