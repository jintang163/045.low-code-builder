package com.lowcode.deploy.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.deploy.controller.dto.DeployRequest;
import com.lowcode.deploy.entity.AppService;
import com.lowcode.deploy.entity.CloudConfig;
import com.lowcode.deploy.entity.DeployProgressEvent;
import com.lowcode.deploy.entity.DeploySpec;
import com.lowcode.deploy.entity.DeployStatus;
import com.lowcode.deploy.entity.DeployTask;
import com.lowcode.deploy.event.DeployProgressApplicationEvent;
import com.lowcode.deploy.mapper.DeployTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
public class DeployOrchestrationService extends ServiceImpl<DeployTaskMapper, DeployTask> {

    @Autowired
    private AppServiceRegistry appRegistry;

    @Autowired
    private CloudConfigService configService;

    @Autowired
    private CloudClientFactory clientFactory;

    @Autowired
    private DockerBuildService dockerService;

    @Autowired
    private KubernetesDeployService k8sService;

    @Autowired
    private DeployProgressTracker tracker;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public DeployTask submitDeploy(DeployRequest request) {
        log.info("收到部署请求: serviceName={}, cloudConfigId={}, version={}",
                request.getServiceName(), request.getCloudConfigId(), request.getVersion());

        validateDeployRequest(request);

        AppService app = appRegistry.registerIfAbsent(
                request.getServiceName(),
                request.getDisplayName(),
                request.getModulePath()
        );
        log.info("应用服务准备完成: serviceName={}, id={}", app.getServiceName(), app.getId());

        CloudConfig cloud = configService.getById(request.getCloudConfigId());
        if (cloud == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "云平台配置不存在: " + request.getCloudConfigId());
        }
        log.info("云平台配置已加载: provider={}, region={}, clusterId={}",
                cloud.getPlatform(), cloud.getRegion(), cloud.getClusterId());

        String taskId = UUID.randomUUID().toString().replace("-", "");
        DeployTask task = DeployTask.builder()
                .taskId(taskId)
                .deployName(request.getDisplayName() != null ? request.getDisplayName() : request.getServiceName())
                .serviceId(app.getId())
                .cloudConfigId(request.getCloudConfigId())
                .version(StringUtils.hasText(request.getVersion()) ? request.getVersion() : "latest")
                .status(DeployStatus.PENDING)
                .progress(0)
                .spec(request.getSpec() != null ? request.getSpec() : defaultSpec())
                .domain(request.getDomain())
                .startedAt(new Date())
                .build();

        tracker.registerTask(task);
        log.info("部署任务已注册: taskId={}", taskId);

        final DeployTask taskRef = task;
        final AppService appRef = app;
        final CloudConfig cloudRef = cloud;
        taskExecutor.execute(() -> doDeploy(taskRef, appRef, cloudRef, request));

        return task;
    }

    public void doDeploy(DeployTask task, AppService app, CloudConfig cloud, DeployRequest request) {
        String taskId = task.getTaskId();
        try {
            log.info("[{}] 开始执行部署流程", taskId);

            tracker.updateProgress(taskId, 5, "初始化", DeployStatus.BUILDING, "INFO",
                    "部署任务启动，准备构建镜像");

            Consumer<String> logConsumer = msg -> tracker.appendLog(taskId, "INFO", msg);

            log.info("[{}] 步骤1: 构建并推送Docker镜像", taskId);
            String imageFullPath = dockerService.buildAndPushImage(
                    app,
                    task.getSpec(),
                    cloud,
                    taskId,
                    logConsumer
            );
            log.info("[{}] 镜像构建推送完成: {}", taskId, imageFullPath);

            tracker.updateProgress(taskId, 65, "部署中", DeployStatus.DEPLOYING, "INFO",
                    "镜像推送成功，开始部署到Kubernetes集群");

            log.info("[{}] 步骤2: 部署到Kubernetes集群", taskId);
            k8sService.deployApp(app, task, cloud, imageFullPath, logConsumer);

            tracker.updateProgress(taskId, 100, "部署成功", DeployStatus.SUCCESS, "INFO",
                    "部署流程全部完成，应用已成功上线");

            log.info("[{}] 部署成功完成", taskId);

        } catch (Exception e) {
            log.error("[{}] 部署失败: {}", taskId, e.getMessage(), e);
            DeployTask currentTask = tracker.getTask(taskId);
            Integer currentProgress = currentTask != null && currentTask.getProgress() != null
                    ? currentTask.getProgress() : 0;
            tracker.updateProgress(taskId, currentProgress, "部署失败", DeployStatus.FAILED, "ERROR",
                    e.getMessage());
        } finally {
            publishTaskFinishedEvent(taskId);
        }
    }

    public DeployTask rollback(String taskId) {
        log.info("收到回滚请求: taskId={}", taskId);

        DeployTask originalTask = tracker.getTask(taskId);
        if (originalTask == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "原始部署任务不存在: " + taskId);
        }

        String rollbackTaskId = UUID.randomUUID().toString().replace("-", "");
        DeployTask rollbackTask = DeployTask.builder()
                .taskId(rollbackTaskId)
                .deployName(originalTask.getDeployName() + "-rollback")
                .serviceId(originalTask.getServiceId())
                .cloudConfigId(originalTask.getCloudConfigId())
                .version(originalTask.getVersion())
                .status(DeployStatus.ROLLING_BACK)
                .progress(0)
                .spec(originalTask.getSpec())
                .domain(originalTask.getDomain())
                .rollbackFromTaskId(taskId)
                .startedAt(new Date())
                .build();

        tracker.registerTask(rollbackTask);
        log.info("回滚任务已注册: rollbackTaskId={}, 原始任务={}", rollbackTaskId, taskId);

        final Long cloudConfigId = originalTask.getCloudConfigId();
        taskExecutor.execute(() -> {
            try {
                tracker.updateProgress(rollbackTaskId, 20, "回滚中", DeployStatus.ROLLING_BACK, "INFO",
                        "开始执行回滚操作");

                Consumer<String> logConsumer = msg -> tracker.appendLog(rollbackTaskId, "INFO", msg);
                k8sService.rollback(taskId, cloudConfigId, logConsumer);

                tracker.updateProgress(rollbackTaskId, 100, "回滚完成", DeployStatus.ROLLED_BACK, "INFO",
                        "回滚成功完成");
                log.info("[{}] 回滚成功完成", rollbackTaskId);

            } catch (Exception e) {
                log.error("[{}] 回滚失败: {}", rollbackTaskId, e.getMessage(), e);
                tracker.updateProgress(rollbackTaskId, 50, "回滚失败", DeployStatus.FAILED, "ERROR",
                        e.getMessage());
            } finally {
                publishTaskFinishedEvent(rollbackTaskId);
            }
        });

        return rollbackTask;
    }

    public DeployTask getTaskStatus(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        return tracker.getTask(taskId);
    }

    public List<DeployProgressEvent> getTaskEvents(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        return tracker.getEventHistory(taskId);
    }

    private void validateDeployRequest(DeployRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部署请求不能为空");
        }
        if (!StringUtils.hasText(request.getServiceName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务名不能为空");
        }
        if (request.getCloudConfigId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "云平台配置ID不能为空");
        }
    }

    private DeploySpec defaultSpec() {
        return DeploySpec.builder()
                .replicas(2)
                .cpuRequest("500m")
                .memoryRequest("512Mi")
                .cpuLimit("1000m")
                .memoryLimit("1Gi")
                .enableHpa(false)
                .rolloutStrategy("RollingUpdate")
                .maxSurge("25%")
                .maxUnavailable("25%")
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(String taskId) {
        log.info("收到取消部署任务请求: taskId={}", taskId);
        if (!StringUtils.hasText(taskId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }

        DeployTask task = tracker.getTask(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部署任务不存在: " + taskId);
        }

        DeployStatus status = task.getStatus();
        if (status == DeployStatus.SUCCESS || status == DeployStatus.FAILED
                || status == DeployStatus.ROLLED_BACK) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "任务已结束，无法取消: " + (status != null ? status.getName() : "未知"));
        }

        Integer currentProgress = task.getProgress() != null ? task.getProgress() : 0;
        tracker.updateProgress(taskId, currentProgress, "任务已取消", DeployStatus.FAILED, "WARN",
                "任务被用户主动取消");

        publishTaskFinishedEvent(taskId);
        log.info("部署任务已取消: taskId={}", taskId);
    }

    private void publishTaskFinishedEvent(String taskId) {
        try {
            DeployTask finishedTask = tracker.getTask(taskId);
            DeployProgressEvent event = DeployProgressEvent.builder()
                    .taskId(taskId)
                    .step("finished")
                    .message("任务执行结束")
                    .timestamp(new Date())
                    .progress(finishedTask != null ? finishedTask.getProgress() : null)
                    .status(finishedTask != null && finishedTask.getStatus() != null
                            ? finishedTask.getStatus().getCode() : null)
                    .logLevel("INFO")
                    .build();
            eventPublisher.publishEvent(new DeployProgressApplicationEvent(this, event));
            log.info("已发布任务完成事件: taskId={}, status={}", taskId, event.getStatus());
        } catch (Exception e) {
            log.warn("发布任务完成事件失败: taskId={}, error={}", taskId, e.getMessage());
        }
    }
}
