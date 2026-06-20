package com.lowcode.deploy.service;

import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.deploy.entity.DeployProgressEvent;
import com.lowcode.deploy.entity.DeployStatus;
import com.lowcode.deploy.entity.DeployTask;
import com.lowcode.deploy.event.DeployProgressApplicationEvent;
import com.lowcode.deploy.mapper.DeployTaskMapper;
import com.lowcode.deploy.websocket.DeployWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class DeployProgressTracker {

    private final ConcurrentHashMap<String, DeployTask> taskStore = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<DeployProgressEvent>> eventHistory = new ConcurrentHashMap<>();

    @Autowired
    private DeployTaskMapper deployTaskMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private DeployWebSocketHandler webSocketHandler;

    @Transactional(rollbackFor = Exception.class)
    public void registerTask(DeployTask task) {
        try {
            if (task == null || task.getTaskId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "任务信息不能为空");
            }
            deployTaskMapper.insert(task);
            taskStore.put(task.getTaskId(), task);
            eventHistory.putIfAbsent(task.getTaskId(), new CopyOnWriteArrayList<>());
            log.info("注册部署任务: taskId={}, serviceId={}", task.getTaskId(), task.getServiceId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("注册部署任务失败: taskId={}", task != null ? task.getTaskId() : null, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册部署任务失败: " + e.getMessage());
        }
    }

    public void updateProgress(String taskId, Integer progress, String currentStep,
                               DeployStatus status, String logLevel, String message) {
        try {
            DeployTask task = taskStore.get(taskId);
            if (task == null) {
                task = deployTaskMapper.selectByTaskId(taskId);
                if (task == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "部署任务不存在: " + taskId);
                }
                taskStore.put(taskId, task);
                eventHistory.putIfAbsent(taskId, new CopyOnWriteArrayList<>());
            }

            if (progress != null) {
                task.setProgress(progress);
            }
            if (currentStep != null) {
                task.setCurrentStep(currentStep);
            }
            if (status != null) {
                task.setStatus(status);
            }
            if (DeployStatus.SUCCESS.equals(status) || DeployStatus.FAILED.equals(status)
                    || DeployStatus.ROLLED_BACK.equals(status)) {
                task.setFinishedAt(new Date());
                if (DeployStatus.FAILED.equals(status) && message != null) {
                    task.setErrorMessage(message);
                }
            }

            asyncSaveOrUpdate(task);

            DeployProgressEvent event = DeployProgressEvent.builder()
                    .taskId(taskId)
                    .step(currentStep)
                    .message(message)
                    .timestamp(new Date())
                    .progress(progress)
                    .status(status != null ? status.getCode() : null)
                    .logLevel(logLevel != null ? logLevel : "INFO")
                    .build();

            eventHistory.get(taskId).add(event);
            eventPublisher.publishEvent(new DeployProgressApplicationEvent(this, event));
            webSocketHandler.pushProgress(event);

            log.info("更新部署进度: taskId={}, progress={}, step={}, status={}, message={}",
                    taskId, progress, currentStep, status, message);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新部署进度失败: taskId={}", taskId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新部署进度失败: " + e.getMessage());
        }
    }

    @Async
    @Transactional(rollbackFor = Exception.class)
    public void asyncSaveOrUpdate(DeployTask task) {
        try {
            if (task.getId() == null) {
                deployTaskMapper.insert(task);
            } else {
                deployTaskMapper.updateById(task);
            }
        } catch (Exception e) {
            log.error("异步持久化部署任务失败: taskId={}", task.getTaskId(), e);
        }
    }

    public DeployTask getTask(String taskId) {
        try {
            if (taskId == null || taskId.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
            }
            DeployTask task = taskStore.get(taskId);
            if (task == null) {
                task = deployTaskMapper.selectByTaskId(taskId);
                if (task != null) {
                    taskStore.put(taskId, task);
                }
            }
            return task;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取部署任务失败: taskId={}", taskId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取部署任务失败: " + e.getMessage());
        }
    }

    public List<DeployProgressEvent> getEventHistory(String taskId) {
        try {
            if (taskId == null || taskId.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
            }
            List<DeployProgressEvent> events = eventHistory.get(taskId);
            if (events == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>(events);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取任务事件历史失败: taskId={}", taskId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取任务事件历史失败: " + e.getMessage());
        }
    }

    public void appendLog(String taskId, String logLevel, String message) {
        try {
            if (taskId == null || taskId.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
            }
            DeployProgressEvent event = DeployProgressEvent.builder()
                    .taskId(taskId)
                    .message(message)
                    .timestamp(new Date())
                    .logLevel(logLevel != null ? logLevel : "INFO")
                    .build();

            eventHistory.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>()).add(event);

            DeployTask task = taskStore.get(taskId);
            if (task != null && task.getProgress() != null) {
                event.setProgress(task.getProgress());
                event.setStep(task.getCurrentStep());
                event.setStatus(task.getStatus() != null ? task.getStatus().getCode() : null);
            }

            eventPublisher.publishEvent(new DeployProgressApplicationEvent(this, event));
            webSocketHandler.pushProgress(event);

            if ("ERROR".equalsIgnoreCase(logLevel)) {
                log.error("部署任务日志: taskId={}, level={}, message={}", taskId, logLevel, message);
            } else if ("WARN".equalsIgnoreCase(logLevel)) {
                log.warn("部署任务日志: taskId={}, level={}, message={}", taskId, logLevel, message);
            } else {
                log.info("部署任务日志: taskId={}, level={}, message={}", taskId, logLevel, message);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("追加日志失败: taskId={}", taskId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "追加日志失败: " + e.getMessage());
        }
    }
}
