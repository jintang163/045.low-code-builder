package com.lowcode.flow.schedule;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.flow.dto.RpaExecuteDTO;
import com.lowcode.flow.entity.RpaExecution;
import com.lowcode.flow.entity.RpaScript;
import com.lowcode.flow.mapper.RpaScriptMapper;
import com.lowcode.flow.service.RpaExecutionService;
import com.lowcode.flow.service.RpaScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RpaScheduleService {

    @Autowired
    private RpaScriptMapper rpaScriptMapper;

    @Autowired
    private RpaScriptService rpaScriptService;

    @Autowired
    private RpaExecutionService rpaExecutionService;

    @Scheduled(cron = "0 * * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void executeScheduledScripts() {
        log.debug("开始检查定时RPA脚本...");

        try {
            LambdaQueryWrapper<RpaScript> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RpaScript::getScheduleEnabled, 1);
            wrapper.eq(RpaScript::getStatus, "PUBLISHED");
            wrapper.isNotNull(RpaScript::getCronExpression);
            List<RpaScript> scripts = rpaScriptMapper.selectList(wrapper);

            if (scripts.isEmpty()) {
                log.debug("没有需要执行的定时RPA脚本");
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            int executedCount = 0;

            for (RpaScript script : scripts) {
                try {
                    if (shouldExecute(script, now)) {
                        log.info("执行定时RPA脚本: {} [{}]", script.getScriptName(), script.getScriptCode());
                        executeScript(script);
                        executedCount++;
                    }
                } catch (Exception e) {
                    log.error("执行定时RPA脚本失败: {} [{}]", script.getScriptName(), script.getScriptCode(), e);
                    updateScriptStats(script, false, e.getMessage());
                }
            }

            if (executedCount > 0) {
                log.info("定时RPA脚本检查完成，执行了{}个脚本", executedCount);
            }

        } catch (Exception e) {
            log.error("定时RPA脚本调度异常", e);
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void updateNextExecuteTimes() {
        log.debug("更新定时RPA脚本下次执行时间...");

        try {
            LambdaQueryWrapper<RpaScript> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RpaScript::getScheduleEnabled, 1);
            wrapper.isNotNull(RpaScript::getCronExpression);
            List<RpaScript> scripts = rpaScriptMapper.selectList(wrapper);

            for (RpaScript script : scripts) {
                try {
                    LocalDateTime nextTime = calculateNextExecuteTime(script.getCronExpression());
                    if (nextTime != null) {
                        script.setNextExecuteTime(nextTime);
                        rpaScriptMapper.updateById(script);
                    }
                } catch (Exception e) {
                    log.warn("更新脚本下次执行时间失败: {}", script.getScriptCode(), e);
                }
            }
        } catch (Exception e) {
            log.error("更新定时RPA脚本下次执行时间异常", e);
        }
    }

    private boolean shouldExecute(RpaScript script, LocalDateTime now) {
        if (script.getCronExpression() == null || script.getCronExpression().isEmpty()) {
            return false;
        }

        try {
            CronExpression cronExpression = CronExpression.parse(script.getCronExpression());

            LocalDateTime lastExecuteTime = script.getLastExecuteTime();
            if (lastExecuteTime == null) {
                LocalDateTime nextTime = cronExpression.next(LocalDateTime.now().minusMinutes(1));
                return nextTime != null && nextTime.isBefore(now.plusSeconds(30));
            }

            LocalDateTime nextAfterLast = cronExpression.next(lastExecuteTime);
            if (nextAfterLast == null) {
                return false;
            }

            return nextAfterLast.isBefore(now.plusSeconds(30)) && !nextAfterLast.isAfter(now.plusMinutes(1));

        } catch (Exception e) {
            log.warn("解析Cron表达式失败: {} - {}", script.getScriptCode(), script.getCronExpression(), e);
            return false;
        }
    }

    private void executeScript(RpaScript script) {
        Map<String, Object> params = null;
        if (script.getScheduleParams() != null && !script.getScheduleParams().isEmpty()) {
            try {
                params = JSON.parseObject(script.getScheduleParams(), Map.class);
            } catch (Exception e) {
                log.warn("解析定时参数失败，使用空参数: {}", script.getScheduleParams(), e);
            }
        }

        RpaExecuteDTO executeDTO = new RpaExecuteDTO();
        executeDTO.setScriptId(script.getId());
        executeDTO.setTriggerType("SCHEDULE");
        executeDTO.setInputParams(params);

        try {
            RpaExecution execution = rpaExecutionService.executeScript(executeDTO);
            updateScriptStats(script, "SUCCESS".equals(execution.getStatus()), execution.getErrorMessage());
            log.info("定时RPA脚本执行成功: {} [{}], executionNo={}",
                    script.getScriptName(), script.getScriptCode(), execution.getExecutionNo());
        } catch (Exception e) {
            updateScriptStats(script, false, e.getMessage());
            throw new BusinessException("定时RPA脚本执行失败: " + e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateScriptStats(RpaScript script, boolean success, String errorMessage) {
        RpaScript updateScript = rpaScriptService.getById(script.getId());
        if (updateScript == null) return;

        updateScript.setLastExecuteTime(LocalDateTime.now());
        updateScript.setExecuteCount((updateScript.getExecuteCount() == null ? 0 : updateScript.getExecuteCount()) + 1);

        if (success) {
            updateScript.setSuccessCount((updateScript.getSuccessCount() == null ? 0 : updateScript.getSuccessCount()) + 1);
        } else {
            updateScript.setFailCount((updateScript.getFailCount() == null ? 0 : updateScript.getFailCount()) + 1);
        }

        if (updateScript.getCronExpression() != null && !updateScript.getCronExpression().isEmpty()) {
            LocalDateTime nextTime = calculateNextExecuteTime(updateScript.getCronExpression());
            updateScript.setNextExecuteTime(nextTime);
        }

        rpaScriptMapper.updateById(updateScript);
    }

    public LocalDateTime calculateNextExecuteTime(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            return cron.next(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("计算下次执行时间失败: {}", cronExpression, e);
            return null;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public RpaScript enableSchedule(Long scriptId, String cronExpression, String scheduleParams) {
        RpaScript script = rpaScriptService.getScriptDetail(scriptId);

        if (!"PUBLISHED".equals(script.getStatus())) {
            throw new BusinessException("请先发布脚本再启用定时调度");
        }

        try {
            CronExpression.parse(cronExpression);
        } catch (Exception e) {
            throw new BusinessException("Cron表达式格式无效: " + e.getMessage());
        }

        script.setScheduleEnabled(1);
        script.setCronExpression(cronExpression);
        script.setScheduleParams(scheduleParams);
        script.setNextExecuteTime(calculateNextExecuteTime(cronExpression));

        rpaScriptMapper.updateById(script);
        log.info("已启用RPA脚本定时调度: {} [{}], cron={}", script.getScriptName(), script.getScriptCode(), cronExpression);

        return script;
    }

    @Transactional(rollbackFor = Exception.class)
    public RpaScript disableSchedule(Long scriptId) {
        RpaScript script = rpaScriptService.getScriptDetail(scriptId);
        script.setScheduleEnabled(0);
        script.setNextExecuteTime(null);
        rpaScriptMapper.updateById(script);
        log.info("已禁用RPA脚本定时调度: {} [{}]", script.getScriptName(), script.getScriptCode());
        return script;
    }

    public List<RpaScript> getScheduledScripts() {
        LambdaQueryWrapper<RpaScript> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpaScript::getScheduleEnabled, 1);
        wrapper.orderByAsc(RpaScript::getNextExecuteTime);
        return rpaScriptMapper.selectList(wrapper);
    }
}
