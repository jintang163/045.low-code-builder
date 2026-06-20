package com.lowcode.deploy.service;

import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.deploy.cloud.CloudClient;
import com.lowcode.deploy.entity.AppService;
import com.lowcode.deploy.entity.CloudConfig;
import com.lowcode.deploy.entity.CloudPlatform;
import com.lowcode.deploy.entity.DeployResourceInfo;
import com.lowcode.deploy.entity.DeploySpec;
import com.lowcode.deploy.entity.DeployStatus;
import com.lowcode.deploy.entity.DeployTask;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.TCPSocketActionBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeployment;
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeploymentBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2.CrossVersionObjectReference;
import io.fabric8.kubernetes.api.model.autoscaling.v2.CrossVersionObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscalerBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscalerSpec;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscalerSpecBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2.MetricSpec;
import io.fabric8.kubernetes.api.model.autoscaling.v2.MetricSpecBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2.ResourceMetricSource;
import io.fabric8.kubernetes.api.model.autoscaling.v2.ResourceMetricSourceBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValueBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackendBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
public class KubernetesDeployService {

    private static final String DEFAULT_NAMESPACE = "lowcode";
    private static final int DEFAULT_PORT = 8080;
    private static final long DEFAULT_WAIT_TIMEOUT_MINUTES = 5;
    private static final long WAIT_POLL_INTERVAL_SECONDS = 10;

    @Autowired(required = false)
    private Object cloudConfigService;

    @Autowired(required = false)
    private Object deployProgressTracker;

    @Autowired(required = false)
    private Map<String, CloudClient> cloudClientMap;

    public DeployResourceInfo deployApp(AppService app, DeployTask task, CloudConfig cloud,
                                        String imageFullPath, Consumer<String> logConsumer) {
        String namespace = DEFAULT_NAMESPACE;
        String deploymentName = buildDeploymentName(app, task);
        String serviceName = deploymentName;
        String ingressName = deploymentName + "-ingress";
        String version = task.getVersion() != null ? task.getVersion() : "latest";

        logInfo(logConsumer, "开始部署应用: service={}, version={}, image={}",
                app.getServiceName(), version, imageFullPath);
        updateProgress(task.getTaskId(), DeployStatus.DEPLOYING, 65, "初始化Kubernetes客户端", logConsumer);

        try (KubernetesClient client = createClient(cloud)) {
            ensureNamespace(client, namespace, logConsumer);
            updateProgress(task.getTaskId(), DeployStatus.DEPLOYING, 70, "命名空间检查完成", logConsumer);

            DeploySpec spec = task.getSpec() != null ? task.getSpec() : new DeploySpec();

            Deployment deployment = createOrUpdateDeployment(client, namespace, deploymentName,
                    app, version, imageFullPath, spec, logConsumer);
            updateProgress(task.getTaskId(), DeployStatus.DEPLOYING, 78, "Deployment创建/更新完成", logConsumer);

            Service service = createOrUpdateService(client, namespace, serviceName, deploymentName,
                    DEFAULT_PORT, logConsumer);
            updateProgress(task.getTaskId(), DeployStatus.DEPLOYING, 82, "Service创建/更新完成", logConsumer);

            if (task.getDomain() != null && !task.getDomain().trim().isEmpty()) {
                createOrUpdateIngress(client, namespace, ingressName, serviceName,
                        task.getDomain(), DEFAULT_PORT, logConsumer);
                updateProgress(task.getTaskId(), DeployStatus.DEPLOYING, 86, "Ingress创建/更新完成", logConsumer);
            }

            if (Boolean.TRUE.equals(spec.getEnableHpa())) {
                createOrUpdateHpa(client, namespace, deploymentName + "-hpa", deploymentName, spec, logConsumer);
                updateProgress(task.getTaskId(), DeployStatus.DEPLOYING, 88, "HPA创建/更新完成", logConsumer);
            }

            updateProgress(task.getTaskId(), DeployStatus.DEPLOYING, 90, "等待部署就绪...", logConsumer);
            waitForDeploymentReady(client, namespace, deploymentName, logConsumer);

            updateProgress(task.getTaskId(), DeployStatus.RUNNING, 93, "部署已就绪，检查服务状态...", logConsumer);

            DeployResourceInfo resourceInfo = buildResourceInfo(client, namespace, deploymentName,
                    serviceName, ingressName, task.getDomain(), spec);

            updateProgress(task.getTaskId(), DeployStatus.SUCCESS, 100, "部署成功完成", logConsumer);
            logInfo(logConsumer, "应用部署成功: deployment={}, namespace={}", deploymentName, namespace);

            return resourceInfo;

        } catch (BusinessException e) {
            logError(logConsumer, "部署业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("部署应用失败: service={}, taskId={}, error={}",
                    app.getServiceName(), task.getTaskId(), e.getMessage(), e);
            logError(logConsumer, "部署失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署Kubernetes应用失败: " + e.getMessage());
        }
    }

    public void rollback(String taskId, Long cloudConfigId, Consumer<String> logConsumer) {
        logInfo(logConsumer, "开始回滚任务: taskId={}", taskId);
        updateProgress(taskId, DeployStatus.ROLLING_BACK, 50, "准备回滚操作", logConsumer);

        CloudConfig cloud = getCloudConfig(cloudConfigId);
        String namespace = DEFAULT_NAMESPACE;
        String deploymentName = extractDeploymentNameFromTaskId(taskId);

        try (KubernetesClient client = createClient(cloud)) {
            Deployment deployment = client.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .get();

            if (deployment == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Deployment不存在: " + deploymentName);
            }

            client.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .rolling()
                    .undo();

            logInfo(logConsumer, "已触发回滚: deployment={}", deploymentName);
            updateProgress(taskId, DeployStatus.ROLLING_BACK, 75, "等待回滚完成...", logConsumer);

            waitForDeploymentReady(client, namespace, deploymentName, logConsumer);

            updateProgress(taskId, DeployStatus.ROLLED_BACK, 100, "回滚完成", logConsumer);
            logInfo(logConsumer, "回滚成功完成: deployment={}", deploymentName);

        } catch (BusinessException e) {
            logError(logConsumer, "回滚业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("回滚失败: taskId={}, deployment={}, error={}",
                    taskId, deploymentName, e.getMessage(), e);
            logError(logConsumer, "回滚失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "回滚失败: " + e.getMessage());
        }
    }

    public void scale(String namespace, String deploymentName, Integer replicas, Long cloudConfigId) {
        log.info("调整Deployment副本数: namespace={}, deployment={}, replicas={}",
                namespace, deploymentName, replicas);

        CloudConfig cloud = getCloudConfig(cloudConfigId);
        try (KubernetesClient client = createClient(cloud)) {
            Deployment deployment = client.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .get();

            if (deployment == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Deployment不存在: " + deploymentName);
            }

            client.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .scale(replicas);

            log.info("Deployment副本数调整成功: namespace={}, deployment={}, replicas={}",
                    namespace, deploymentName, replicas);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调整副本数失败: namespace={}, deployment={}, error={}",
                    namespace, deploymentName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调整副本数失败: " + e.getMessage());
        }
    }

    public DeployResourceInfo getResourceInfo(String namespace, String deploymentName, Long cloudConfigId) {
        log.info("获取部署资源信息: namespace={}, deployment={}", namespace, deploymentName);

        CloudConfig cloud = getCloudConfig(cloudConfigId);
        try (KubernetesClient client = createClient(cloud)) {
            Deployment deployment = client.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .get();

            if (deployment == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Deployment不存在: " + deploymentName);
            }

            String serviceName = deploymentName;
            String ingressName = deploymentName + "-ingress";
            String host = null;

            Ingress ingress = client.network().v1().ingresses()
                    .inNamespace(namespace)
                    .withName(ingressName)
                    .get();

            if (ingress != null && ingress.getSpec() != null
                    && ingress.getSpec().getRules() != null
                    && !ingress.getSpec().getRules().isEmpty()) {
                host = ingress.getSpec().getRules().get(0).getHost();
            }

            return buildResourceInfo(client, namespace, deploymentName, serviceName, ingressName, host, null);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取资源信息失败: namespace={}, deployment={}, error={}",
                    namespace, deploymentName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取资源信息失败: " + e.getMessage());
        }
    }

    public List<String> getDeploymentLogs(String namespace, String podName, Integer tailLines, Long cloudConfigId) {
        log.info("获取Pod日志: namespace={}, pod={}, tailLines={}", namespace, podName, tailLines);

        CloudConfig cloud = getCloudConfig(cloudConfigId);
        try (KubernetesClient client = createClient(cloud)) {
            String logOutput = client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .tailingLines(tailLines != null ? tailLines : 100)
                    .getLog();

            if (logOutput == null || logOutput.trim().isEmpty()) {
                return Collections.emptyList();
            }

            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(logOutput.getBytes(StandardCharsets.UTF_8))))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            return lines;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取Pod日志失败: namespace={}, pod={}, error={}",
                    namespace, podName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取Pod日志失败: " + e.getMessage());
        }
    }

    private KubernetesClient createClient(CloudConfig cloud) throws Exception {
        log.info("创建Kubernetes客户端: platform={}, clusterId={}", cloud.getPlatform(), cloud.getClusterId());

        CloudClient cloudClient = getCloudClient(cloud.getPlatform());
        com.lowcode.deploy.cloud.CloudConfig clientConfig = convertToCloudClientConfig(cloud);

        String kubeconfigYaml = cloudClient.getClusterKubeconfig(clientConfig);
        if (kubeconfigYaml == null || kubeconfigYaml.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取kubeconfig为空");
        }

        Config config = Config.fromKubeconfig(kubeconfigYaml);
        return new KubernetesClientBuilder()
                .withConfig(config)
                .build();
    }

    private CloudClient getCloudClient(CloudPlatform platform) {
        if (cloudClientMap == null || cloudClientMap.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未配置任何云平台客户端");
        }

        CloudClient client = null;
        if (platform == CloudPlatform.ALIYUN) {
            client = cloudClientMap.get("aliyunCloudClient");
        } else if (platform == CloudPlatform.TENCENT) {
            client = cloudClientMap.get("tencentCloudClient");
        }

        if (client == null) {
            for (CloudClient cc : cloudClientMap.values()) {
                if (cc != null) {
                    client = cc;
                    break;
                }
            }
        }

        if (client == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "找不到对应的云平台客户端: " + (platform != null ? platform.getName() : "null"));
        }

        return client;
    }

    private com.lowcode.deploy.cloud.CloudConfig convertToCloudClientConfig(CloudConfig cloud) {
        return com.lowcode.deploy.cloud.CloudConfig.builder()
                .provider(cloud.getPlatform() != null ? cloud.getPlatform().getCode() : null)
                .accessKeyId(cloud.getAccessKey())
                .accessKeySecret(cloud.getAccessSecret())
                .regionId(cloud.getRegion())
                .clusterId(cloud.getClusterId())
                .build();
    }

    private CloudConfig getCloudConfig(Long cloudConfigId) {
        if (cloudConfigService != null) {
            try {
                java.lang.reflect.Method method = cloudConfigService.getClass().getMethod("getById", Long.class);
                Object result = method.invoke(cloudConfigService, cloudConfigId);
                if (result instanceof CloudConfig) {
                    return (CloudConfig) result;
                }
            } catch (Exception e) {
                log.warn("通过CloudConfigService获取配置失败: {}", e.getMessage());
            }
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "云配置不存在: " + cloudConfigId);
    }

    private void ensureNamespace(KubernetesClient client, String namespace, Consumer<String> logConsumer) {
        logInfo(logConsumer, "检查命名空间: {}", namespace);

        Namespace existing = client.namespaces().withName(namespace).get();
        if (existing != null) {
            logInfo(logConsumer, "命名空间已存在: {}", namespace);
            return;
        }

        Namespace ns = new NamespaceBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(namespace)
                        .build())
                .build();

        client.namespaces().createOrReplace(ns);
        logInfo(logConsumer, "命名空间创建成功: {}", namespace);
    }

    private Deployment createOrUpdateDeployment(KubernetesClient client, String namespace,
                                                 String deploymentName, AppService app, String version,
                                                 String imageFullPath, DeploySpec spec,
                                                 Consumer<String> logConsumer) {
        logInfo(logConsumer, "创建/更新Deployment: {}", deploymentName);

        int replicas = spec.getReplicas() != null ? spec.getReplicas() : 2;
        String cpuRequest = spec.getCpuRequest() != null ? spec.getCpuRequest() : "500m";
        String memoryRequest = spec.getMemoryRequest() != null ? spec.getMemoryRequest() : "512Mi";
        String cpuLimit = spec.getCpuLimit() != null ? spec.getCpuLimit() : "1000m";
        String memoryLimit = spec.getMemoryLimit() != null ? spec.getMemoryLimit() : "1Gi";

        Map<String, String> labels = new HashMap<>();
        labels.put("app", app.getServiceName());
        labels.put("version", version);

        Map<String, String> selectorLabels = new HashMap<>();
        selectorLabels.put("app", app.getServiceName());

        List<EnvVar> envVars = new ArrayList<>();
        if (spec.getEnvVars() != null && !spec.getEnvVars().isEmpty()) {
            spec.getEnvVars().forEach((k, v) -> envVars.add(new EnvVarBuilder()
                    .withName(k)
                    .withValue(v)
                    .build()));
        }

        List<ContainerPort> containerPorts = Collections.singletonList(
                new ContainerPortBuilder()
                        .withContainerPort(DEFAULT_PORT)
                        .withName("http")
                        .withProtocol("TCP")
                        .build()
        );

        ResourceRequirements resources = new ResourceRequirementsBuilder()
                .addToRequests("cpu", new Quantity(cpuRequest))
                .addToRequests("memory", new Quantity(memoryRequest))
                .addToLimits("cpu", new Quantity(cpuLimit))
                .addToLimits("memory", new Quantity(memoryLimit))
                .build();

        Probe readinessProbe = new ProbeBuilder()
                .withTcpSocket(new TCPSocketActionBuilder()
                        .withPort(new IntOrString(DEFAULT_PORT))
                        .build())
                .withInitialDelaySeconds(30)
                .withPeriodSeconds(10)
                .withTimeoutSeconds(5)
                .withFailureThreshold(3)
                .build();

        Probe livenessProbe = new ProbeBuilder()
                .withTcpSocket(new TCPSocketActionBuilder()
                        .withPort(new IntOrString(DEFAULT_PORT))
                        .build())
                .withInitialDelaySeconds(60)
                .withPeriodSeconds(30)
                .withTimeoutSeconds(5)
                .withFailureThreshold(3)
                .build();

        Container container = new ContainerBuilder()
                .withName(deploymentName)
                .withImage(imageFullPath)
                .withImagePullPolicy("Always")
                .withPorts(containerPorts)
                .withResources(resources)
                .withEnv(envVars)
                .withReadinessProbe(readinessProbe)
                .withLivenessProbe(livenessProbe)
                .build();

        RollingUpdateDeployment rollingUpdate = null;
        String strategy = spec.getRolloutStrategy() != null ? spec.getRolloutStrategy() : "RollingUpdate";
        if ("RollingUpdate".equalsIgnoreCase(strategy)) {
            String maxSurge = spec.getMaxSurge() != null ? spec.getMaxSurge() : "25%";
            String maxUnavailable = spec.getMaxUnavailable() != null ? spec.getMaxUnavailable() : "25%";
            rollingUpdate = new RollingUpdateDeploymentBuilder()
                    .withMaxSurge(new IntOrString(maxSurge))
                    .withMaxUnavailable(new IntOrString(maxUnavailable))
                    .build();
        }

        PodSpecBuilder podSpecBuilder = new PodSpecBuilder()
                .withContainers(container);

        if (spec.getNodeSelector() != null && !spec.getNodeSelector().isEmpty()) {
            podSpecBuilder.withNodeSelector(spec.getNodeSelector());
        }

        PodSpec podSpec = podSpecBuilder.build();

        ObjectMeta podMeta = new ObjectMetaBuilder()
                .withLabels(labels)
                .build();

        PodTemplateSpec template = new PodTemplateSpecBuilder()
                .withMetadata(podMeta)
                .withSpec(podSpec)
                .build();

        DeploymentSpecBuilder deploymentSpecBuilder = new DeploymentSpecBuilder()
                .withReplicas(replicas)
                .withNewSelector()
                .withMatchLabels(selectorLabels)
                .endSelector()
                .withTemplate(template);

        if ("RollingUpdate".equalsIgnoreCase(strategy) && rollingUpdate != null) {
            deploymentSpecBuilder.withNewStrategy()
                    .withType(strategy)
                    .withRollingUpdate(rollingUpdate)
                    .endStrategy();
        } else {
            deploymentSpecBuilder.withNewStrategy()
                    .withType("Recreate")
                    .endStrategy();
        }

        Deployment deployment = new DeploymentBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(deploymentName)
                        .withNamespace(namespace)
                        .withLabels(labels)
                        .build())
                .withSpec(deploymentSpecBuilder.build())
                .build();

        Deployment result = client.apps().deployments()
                .inNamespace(namespace)
                .createOrReplace(deployment);

        logInfo(logConsumer, "Deployment创建/更新成功: {}", deploymentName);
        return result;
    }

    private Service createOrUpdateService(KubernetesClient client, String namespace,
                                           String serviceName, String deploymentName,
                                           int port, Consumer<String> logConsumer) {
        logInfo(logConsumer, "创建/更新Service: {}", serviceName);

        String appName = extractAppName(deploymentName);

        ServicePort servicePort = new ServicePortBuilder()
                .withName("http")
                .withPort(port)
                .withTargetPort(new IntOrString(port))
                .withProtocol("TCP")
                .build();

        ServiceSpec serviceSpec = new ServiceSpecBuilder()
                .withType("ClusterIP")
                .withSelector(Collections.singletonMap("app", appName))
                .withPorts(servicePort)
                .build();

        Service service = new ServiceBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(serviceName)
                        .withNamespace(namespace)
                        .build())
                .withSpec(serviceSpec)
                .build();

        Service result = client.services()
                .inNamespace(namespace)
                .createOrReplace(service);

        logInfo(logConsumer, "Service创建/更新成功: {}", serviceName);
        return result;
    }

    private void createOrUpdateIngress(KubernetesClient client, String namespace,
                                        String ingressName, String serviceName,
                                        String host, int port, Consumer<String> logConsumer) {
        logInfo(logConsumer, "创建/更新Ingress: {}, host={}", ingressName, host);

        IngressBackend backend = new IngressBackendBuilder()
                .withService(new IngressServiceBackendBuilder()
                        .withName(serviceName)
                        .withPort(new io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPortBuilder()
                                .withNumber(port)
                                .build())
                        .build())
                .build();

        HTTPIngressPath path = new HTTPIngressPathBuilder()
                .withPath("/")
                .withPathType("Prefix")
                .withBackend(backend)
                .build();

        HTTPIngressRuleValue httpRule = new HTTPIngressRuleValueBuilder()
                .withPaths(path)
                .build();

        IngressRule rule = new IngressRuleBuilder()
                .withHost(host)
                .withHttp(httpRule)
                .build();

        ObjectMeta meta = new ObjectMetaBuilder()
                .withName(ingressName)
                .withNamespace(namespace)
                .withAnnotations(Collections.singletonMap(
                        "nginx.ingress.kubernetes.io/proxy-body-size", "100m"))
                .build();

        Ingress ingress = new IngressBuilder()
                .withMetadata(meta)
                .withNewSpec()
                .withRules(rule)
                .endSpec()
                .build();

        client.network().v1().ingresses()
                .inNamespace(namespace)
                .createOrReplace(ingress);

        logInfo(logConsumer, "Ingress创建/更新成功: {}, host={}", ingressName, host);
    }

    private void createOrUpdateHpa(KubernetesClient client, String namespace,
                                    String hpaName, String deploymentName,
                                    DeploySpec spec, Consumer<String> logConsumer) {
        logInfo(logConsumer, "创建/更新HPA: {}", hpaName);

        int minReplicas = spec.getMinReplicas() != null ? spec.getMinReplicas() : 2;
        int maxReplicas = spec.getMaxReplicas() != null ? spec.getMaxReplicas() : 10;
        int cpuThreshold = spec.getCpuThreshold() != null ? spec.getCpuThreshold() : 80;

        CrossVersionObjectReference scaleTarget = new CrossVersionObjectReferenceBuilder()
                .withApiVersion("apps/v1")
                .withKind("Deployment")
                .withName(deploymentName)
                .build();

        ResourceMetricSource resourceMetric = new ResourceMetricSourceBuilder()
                .withName("cpu")
                .withNewTarget()
                .withType("Utilization")
                .withAverageUtilization(cpuThreshold)
                .endTarget()
                .build();

        MetricSpec metricSpec = new MetricSpecBuilder()
                .withType("Resource")
                .withResource(resourceMetric)
                .build();

        HorizontalPodAutoscalerSpec hpaSpec = new HorizontalPodAutoscalerSpecBuilder()
                .withScaleTargetRef(scaleTarget)
                .withMinReplicas(minReplicas)
                .withMaxReplicas(maxReplicas)
                .withMetrics(metricSpec)
                .build();

        HorizontalPodAutoscaler hpa = new HorizontalPodAutoscalerBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(hpaName)
                        .withNamespace(namespace)
                        .build())
                .withSpec(hpaSpec)
                .build();

        client.autoscaling().v2().horizontalPodAutoscalers()
                .inNamespace(namespace)
                .createOrReplace(hpa);

        logInfo(logConsumer, "HPA创建/更新成功: {}, min={}, max={}, cpuThreshold={}%",
                hpaName, minReplicas, maxReplicas, cpuThreshold);
    }

    private void waitForDeploymentReady(KubernetesClient client, String namespace,
                                         String deploymentName, Consumer<String> logConsumer) {
        logInfo(logConsumer, "等待Deployment就绪: {}", deploymentName);

        long timeoutMs = TimeUnit.MINUTES.toMillis(DEFAULT_WAIT_TIMEOUT_MINUTES);
        long startTime = System.currentTimeMillis();
        int checkCount = 0;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                Deployment deployment = client.apps().deployments()
                        .inNamespace(namespace)
                        .withName(deploymentName)
                        .get();

                if (deployment == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "Deployment不存在: " + deploymentName);
                }

                Integer expectedReplicas = deployment.getSpec() != null ? deployment.getSpec().getReplicas() : 0;
                Integer readyReplicas = deployment.getStatus() != null ? deployment.getStatus().getReadyReplicas() : 0;
                Integer availableReplicas = deployment.getStatus() != null ? deployment.getStatus().getAvailableReplicas() : 0;
                Integer updatedReplicas = deployment.getStatus() != null ? deployment.getStatus().getUpdatedReplicas() : 0;

                checkCount++;
                logInfo(logConsumer, "检查[{}]: 期望={}, 已更新={}, 就绪={}, 可用={}",
                        checkCount, expectedReplicas, updatedReplicas, readyReplicas, availableReplicas);

                boolean allReady = expectedReplicas != null
                        && expectedReplicas.equals(readyReplicas)
                        && expectedReplicas.equals(availableReplicas)
                        && expectedReplicas.equals(updatedReplicas);

                if (allReady) {
                    logInfo(logConsumer, "Deployment已就绪: {}, 所有副本已可用", deploymentName);
                    return;
                }

                TimeUnit.SECONDS.sleep(WAIT_POLL_INTERVAL_SECONDS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "等待部署被中断");
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("检查Deployment状态异常: {}", e.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(WAIT_POLL_INTERVAL_SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                "等待Deployment就绪超时(" + DEFAULT_WAIT_TIMEOUT_MINUTES + "分钟): " + deploymentName);
    }

    private DeployResourceInfo buildResourceInfo(KubernetesClient client, String namespace,
                                                  String deploymentName, String serviceName,
                                                  String ingressName, String host, DeploySpec spec) {
        DeployResourceInfo.DeployResourceInfoBuilder builder = DeployResourceInfo.builder()
                .deploymentName(deploymentName)
                .namespace(namespace)
                .serviceName(serviceName)
                .serviceType("ClusterIP")
                .servicePort(DEFAULT_PORT)
                .ingressName(ingressName)
                .host(host);

        try {
            Deployment deployment = client.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .get();

            if (deployment != null && deployment.getSpec() != null) {
                builder.replicas(deployment.getSpec().getReplicas());
                if (deployment.getStatus() != null) {
                    builder.readyReplicas(deployment.getStatus().getReadyReplicas());
                    builder.availableReplicas(deployment.getStatus().getAvailableReplicas());
                }
            }
        } catch (Exception e) {
            log.warn("获取Deployment状态失败: {}", e.getMessage());
        }

        String hpaName = deploymentName + "-hpa";
        try {
            HorizontalPodAutoscaler hpa = client.autoscaling().v2().horizontalPodAutoscalers()
                    .inNamespace(namespace)
                    .withName(hpaName)
                    .get();

            if (hpa != null) {
                builder.hpaName(hpaName);
                if (hpa.getStatus() != null) {
                    builder.currentReplicas(hpa.getStatus().getCurrentReplicas());
                    if (hpa.getSpec() != null && hpa.getSpec().getMetrics() != null
                            && !hpa.getSpec().getMetrics().isEmpty()
                            && hpa.getSpec().getMetrics().get(0).getResource() != null
                            && hpa.getSpec().getMetrics().get(0).getResource().getTarget() != null) {
                        builder.targetCpuUtilization(
                                hpa.getSpec().getMetrics().get(0).getResource().getTarget().getAverageUtilization());
                    }
                    if (hpa.getStatus().getCurrentMetrics() != null
                            && !hpa.getStatus().getCurrentMetrics().isEmpty()
                            && hpa.getStatus().getCurrentMetrics().get(0).getResource() != null
                            && hpa.getStatus().getCurrentMetrics().get(0).getResource().getCurrent() != null) {
                        builder.currentCpuUtilization(
                                hpa.getStatus().getCurrentMetrics().get(0).getResource().getCurrent().getAverageUtilization());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取HPA状态失败: {}", e.getMessage());
        }

        return builder.build();
    }

    private String buildDeploymentName(AppService app, DeployTask task) {
        String base = app.getServiceName() != null ? app.getServiceName() : "app";
        String version = task.getVersion() != null ? task.getVersion().replaceAll("[^a-zA-Z0-9-]", "-") : "latest";
        return (base + "-" + version).toLowerCase();
    }

    private String extractAppName(String deploymentName) {
        int idx = deploymentName.lastIndexOf('-');
        if (idx > 0) {
            return deploymentName.substring(0, idx);
        }
        return deploymentName;
    }

    private String extractDeploymentNameFromTaskId(String taskId) {
        return taskId;
    }

    private void updateProgress(String taskId, DeployStatus status, int progress,
                                 String message, Consumer<String> logConsumer) {
        logInfo(logConsumer, "[{}%] {} - {}", progress, status.getName(), message);

        if (deployProgressTracker != null) {
            try {
                java.lang.reflect.Method[] methods = deployProgressTracker.getClass().getMethods();
                for (java.lang.reflect.Method method : methods) {
                    if ("updateProgress".equals(method.getName())
                            || "trackProgress".equals(method.getName())) {
                        try {
                            method.invoke(deployProgressTracker, taskId, status, progress, message);
                            return;
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("更新进度失败: {}", e.getMessage());
            }
        }
    }

    private void logInfo(Consumer<String> logConsumer, String format, Object... args) {
        String message = String.format(format, args);
        log.info(message);
        if (logConsumer != null) {
            try {
                logConsumer.accept("[INFO] " + message);
            } catch (Exception e) {
                log.debug("日志回调异常: {}", e.getMessage());
            }
        }
    }

    private void logError(Consumer<String> logConsumer, String format, Object... args) {
        String message = String.format(format, args);
        log.error(message);
        if (logConsumer != null) {
            try {
                logConsumer.accept("[ERROR] " + message);
            } catch (Exception e) {
                log.debug("日志回调异常: {}", e.getMessage());
            }
        }
    }
}
