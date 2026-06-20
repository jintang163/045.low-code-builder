package com.lowcode.deploy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.deploy.controller.dto.CloudConfigCreateRequest;
import com.lowcode.deploy.controller.dto.DeployRequest;
import com.lowcode.deploy.entity.AppService;
import com.lowcode.deploy.entity.CloudConfig;
import com.lowcode.deploy.entity.CloudPlatform;
import com.lowcode.deploy.entity.DeployProgressEvent;
import com.lowcode.deploy.entity.DeployResourceInfo;
import com.lowcode.deploy.entity.DeploySpec;
import com.lowcode.deploy.entity.DeployStatus;
import com.lowcode.deploy.entity.DeployTask;
import com.lowcode.deploy.service.AppServiceRegistry;
import com.lowcode.deploy.service.CloudConfigService;
import com.lowcode.deploy.service.DeployOrchestrationService;
import com.lowcode.deploy.service.DeployProgressTracker;
import com.lowcode.deploy.service.KubernetesDeployService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "云部署管理")
@RestController
@RequestMapping("/api/deploy")
public class DeployController {

    @Autowired
    private DeployOrchestrationService orchestration;

    @Autowired
    private DeployProgressTracker tracker;

    @Autowired
    private AppServiceRegistry appRegistry;

    @Autowired
    private CloudConfigService configService;

    @Autowired
    private KubernetesDeployService k8sService;

    // ==================== 1. 部署任务相关 ====================

    @ApiOperation("提交部署任务")
    @PostMapping("/submit")
    public Result<DeployTask> submitDeploy(@RequestBody DeployRequest req) {
        DeployTask task = orchestration.submitDeploy(req);
        return Result.success(task);
    }

    @ApiOperation("查询部署任务状态")
    @GetMapping("/task/{taskId}")
    public Result<DeployTask> getTask(@PathVariable String taskId) {
        return Result.success(tracker.getTask(taskId));
    }

    @ApiOperation("查询部署进度事件历史")
    @GetMapping("/task/{taskId}/events")
    public Result<List<DeployProgressEvent>> getTaskEvents(@PathVariable String taskId) {
        return Result.success(tracker.getEventHistory(taskId));
    }

    @ApiOperation("分页查询部署任务列表")
    @GetMapping("/task/list")
    public Result<Page<DeployTask>> listTasks(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) DeployStatus status) {
        LambdaQueryWrapper<DeployTask> wrapper = new LambdaQueryWrapper<>();
        if (serviceId != null) {
            wrapper.eq(DeployTask::getServiceId, serviceId);
        }
        if (status != null) {
            wrapper.eq(DeployTask::getStatus, status);
        }
        wrapper.orderByDesc(DeployTask::getCreatedTime);
        Page<DeployTask> page = orchestration.page(new Page<>(current, size), wrapper);
        return Result.success(page);
    }

    @ApiOperation("回滚指定部署任务")
    @PostMapping("/task/{taskId}/rollback")
    public Result<DeployTask> rollback(@PathVariable String taskId) {
        return Result.success(orchestration.rollback(taskId));
    }

    @ApiOperation("取消部署任务")
    @PostMapping("/task/{taskId}/cancel")
    public Result<Void> cancelTask(@PathVariable String taskId) {
        orchestration.cancelTask(taskId);
        return Result.success();
    }

    // ==================== 2. 云配置管理 ====================

    @ApiOperation("保存云平台配置")
    @PostMapping("/cloud-config")
    public Result<CloudConfig> saveCloudConfig(@RequestBody CloudConfigCreateRequest req) {
        CloudConfig config = CloudConfig.builder()
                .id(req.getId())
                .platform(req.getPlatform())
                .region(req.getRegion())
                .accessKey(req.getAccessKey())
                .accessSecret(req.getAccessSecret())
                .clusterId(req.getClusterId())
                .registryUrl(req.getRegistryUrl())
                .registryNamespace(req.getRegistryNamespace())
                .registryUsername(req.getRegistryUsername())
                .registryPassword(req.getRegistryPassword())
                .vpcId(req.getVpcId())
                .securityGroupId(req.getSecurityGroupId())
                .description(req.getDescription())
                .build();
        configService.saveOrUpdate(config);
        return Result.success(config);
    }

    @ApiOperation("云配置列表")
    @GetMapping("/cloud-config/list")
    public Result<List<CloudConfig>> listCloudConfigs(
            @RequestParam(required = false) CloudPlatform platform) {
        return Result.success(configService.listByPlatform(platform));
    }

    @GetMapping("/cloud-config/{id}")
    public Result<CloudConfig> getCloudConfig(@PathVariable Long id) {
        return Result.success(configService.getById(id));
    }

    @DeleteMapping("/cloud-config/{id}")
    public Result<Void> deleteCloudConfig(@PathVariable Long id) {
        configService.removeById(id);
        return Result.success();
    }

    @ApiOperation("测试云配置连接")
    @PostMapping("/cloud-config/{id}/test")
    public Result<Boolean> testCloudConfig(@PathVariable Long id) {
        return Result.success(configService.testConnection(id));
    }

    // ==================== 3. 应用服务管理 ====================

    @ApiOperation("注册应用服务")
    @PostMapping("/app/register")
    public Result<AppService> registerApp(@RequestBody Map<String, String> params) {
        String serviceName = params.get("serviceName");
        String displayName = params.getOrDefault("displayName", serviceName);
        String modulePath = params.getOrDefault("modulePath", serviceName);
        return Result.success(appRegistry.registerIfAbsent(serviceName, displayName, modulePath));
    }

    @ApiOperation("应用服务列表")
    @GetMapping("/app/list")
    public Result<List<AppService>> listApps() {
        return Result.success(appRegistry.listAll());
    }

    // ==================== 4. K8s资源操作 ====================

    @ApiOperation("获取部署的K8s资源状态")
    @GetMapping("/resource/{taskId}")
    public Result<DeployResourceInfo> getResource(@PathVariable String taskId) {
        DeployTask task = tracker.getTask(taskId);
        AppService app = appRegistry.getById(task.getServiceId());
        String base = app != null ? app.getServiceName() : "app";
        String version = task.getVersion() != null ? task.getVersion().replaceAll("[^a-zA-Z0-9-]", "-") : "latest";
        String deploymentName = (base + "-" + version).toLowerCase();
        return Result.success(k8sService.getResourceInfo("lowcode", deploymentName, task.getCloudConfigId()));
    }

    @ApiOperation("手动扩缩容")
    @PostMapping("/resource/scale")
    public Result<Void> scale(
            @RequestParam String namespace,
            @RequestParam String deploymentName,
            @RequestParam Integer replicas,
            @RequestParam Long cloudConfigId) {
        k8sService.scale(namespace, deploymentName, replicas, cloudConfigId);
        return Result.success();
    }

    @ApiOperation("获取Pod日志")
    @GetMapping("/resource/logs")
    public Result<List<String>> getLogs(
            @RequestParam String namespace,
            @RequestParam String podName,
            @RequestParam(defaultValue = "200") Integer tailLines,
            @RequestParam Long cloudConfigId) {
        return Result.success(k8sService.getDeploymentLogs(namespace, podName, tailLines, cloudConfigId));
    }

    // ==================== 5. 一键部署预设模板 ====================

    @ApiOperation("获取部署规格预设列表")
    @GetMapping("/presets")
    public Result<List<DeploySpec>> getPresets() {
        List<DeploySpec> presets = new ArrayList<>();

        presets.add(DeploySpec.builder()
                .replicas(1)
                .cpuRequest("200m")
                .memoryRequest("256Mi")
                .cpuLimit("500m")
                .memoryLimit("512Mi")
                .enableHpa(false)
                .rolloutStrategy("RollingUpdate")
                .maxSurge("25%")
                .maxUnavailable("25%")
                .build());

        presets.add(DeploySpec.builder()
                .replicas(2)
                .cpuRequest("500m")
                .memoryRequest("512Mi")
                .cpuLimit("1000m")
                .memoryLimit("1Gi")
                .enableHpa(true)
                .minReplicas(2)
                .maxReplicas(5)
                .cpuThreshold(80)
                .rolloutStrategy("RollingUpdate")
                .maxSurge("25%")
                .maxUnavailable("25%")
                .build());

        presets.add(DeploySpec.builder()
                .replicas(3)
                .cpuRequest("1000m")
                .memoryRequest("2Gi")
                .cpuLimit("2000m")
                .memoryLimit("4Gi")
                .enableHpa(true)
                .minReplicas(3)
                .maxReplicas(20)
                .cpuThreshold(70)
                .rolloutStrategy("RollingUpdate")
                .maxSurge("50%")
                .maxUnavailable("0%")
                .build());

        presets.add(DeploySpec.builder()
                .replicas(1)
                .cpuRequest("100m")
                .memoryRequest("128Mi")
                .cpuLimit("300m")
                .memoryLimit("256Mi")
                .enableHpa(false)
                .rolloutStrategy("RollingUpdate")
                .maxSurge("100%")
                .maxUnavailable("0%")
                .build());

        presets.add(DeploySpec.builder()
                .replicas(2)
                .cpuRequest("200m")
                .memoryRequest("256Mi")
                .cpuLimit("500m")
                .memoryLimit("512Mi")
                .enableHpa(true)
                .minReplicas(2)
                .maxReplicas(8)
                .cpuThreshold(75)
                .rolloutStrategy("RollingUpdate")
                .maxSurge("100%")
                .maxUnavailable("0%")
                .build());

        presets.add(DeploySpec.builder()
                .replicas(4)
                .cpuRequest("500m")
                .memoryRequest("512Mi")
                .cpuLimit("1000m")
                .memoryLimit("1Gi")
                .enableHpa(true)
                .minReplicas(4)
                .maxReplicas(30)
                .cpuThreshold(60)
                .rolloutStrategy("RollingUpdate")
                .maxSurge("100%")
                .maxUnavailable("25%")
                .build());

        return Result.success(presets);
    }
}
