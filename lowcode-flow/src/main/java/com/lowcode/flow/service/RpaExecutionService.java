package com.lowcode.flow.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.flow.dto.RpaExecuteDTO;
import com.lowcode.flow.entity.RpaExecution;
import com.lowcode.flow.entity.RpaScript;
import com.lowcode.flow.mapper.RpaExecutionMapper;
import com.lowcode.flow.rpa.RpaExecutorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RpaExecutionService extends ServiceImpl<RpaExecutionMapper, RpaExecution> {

    @Autowired
    private RpaScriptService rpaScriptService;

    @Autowired
    private RpaExecutorClient rpaExecutorClient;

    @Transactional(rollbackFor = Exception.class)
    public RpaExecution executeScript(RpaExecuteDTO dto) {
        RpaScript script = rpaScriptService.getScriptDetail(dto.getScriptId());

        RpaExecution execution = new RpaExecution();
        execution.setScriptId(dto.getScriptId());
        execution.setExecutionNo(generateExecutionNo());
        execution.setTriggerType(dto.getTriggerType() != null ? dto.getTriggerType() : "MANUAL");
        execution.setTriggerLogicId(dto.getTriggerLogicId());
        execution.setTriggerNodeId(dto.getTriggerNodeId());
        execution.setInputParams(JSON.toJSONString(dto.getInputParams()));
        execution.setStatus("RUNNING");
        execution.setStartTime(LocalDateTime.now());
        save(execution);

        try {
            Map<String, Object> result = rpaExecutorClient.executeScript(
                    script.getScriptContent(),
                    dto.getInputParams(),
                    script.getTargetUrl()
            );

            execution.setStatus("SUCCESS");
            execution.setOutputResult(JSON.toJSONString(result));
            execution.setEndTime(LocalDateTime.now());
            execution.setDuration(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
            updateById(execution);

            log.info("RPA脚本执行成功, executionNo: {}", execution.getExecutionNo());
        } catch (Exception e) {
            log.error("RPA脚本执行失败, executionNo: {}", execution.getExecutionNo(), e);
            execution.setStatus("FAILED");
            execution.setErrorMessage(e.getMessage());
            execution.setEndTime(LocalDateTime.now());
            execution.setDuration(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
            updateById(execution);
            throw new BusinessException("RPA脚本执行失败: " + e.getMessage());
        }

        return execution;
    }

    public RpaExecution getExecutionDetail(Long id) {
        RpaExecution execution = getById(id);
        if (execution == null) {
            throw new BusinessException("执行记录不存在");
        }
        return execution;
    }

    public Page<RpaExecution> getExecutionPage(Long scriptId, Integer current, Integer size) {
        LambdaQueryWrapper<RpaExecution> wrapper = new LambdaQueryWrapper<>();
        if (scriptId != null) {
            wrapper.eq(RpaExecution::getScriptId, scriptId);
        }
        wrapper.orderByDesc(RpaExecution::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    private String generateExecutionNo() {
        return "RPA" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
