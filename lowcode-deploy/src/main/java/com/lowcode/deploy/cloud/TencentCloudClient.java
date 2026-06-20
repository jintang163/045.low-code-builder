package com.lowcode.deploy.cloud;

import cn.hutool.core.util.StrUtil;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.deploy.cloud.dto.ClusterInfo;
import com.lowcode.deploy.cloud.dto.NodeInfo;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.tke.v20180525.TkeClient;
import com.tencentcloudapi.tke.v20180525.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnClass(name = "com.tencentcloudapi.tke.v20180525.TkeClient")
public class TencentCloudClient extends AbstractCloudClient {

    private static final DateTimeFormatter TENCENT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private TkeClient createTkeClient(CloudConfig config) {
        Credential credential = new Credential(
                config.getAccessKeyId(),
                config.getAccessKeySecret()
        );

        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(StrUtil.isNotBlank(config.getEndpoint()) ? config.getEndpoint() : "tke.tencentcloudapi.com");

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        return new TkeClient(credential, config.getRegionId(), clientProfile);
    }

    @Override
    public String getClusterKubeconfig(CloudConfig config) {
        log.info("腾讯云TKE获取集群kubeconfig, clusterId: {}", config.getClusterId());
        try {
            TkeClient client = createTkeClient(config);
            DescribeClusterKubeconfigRequest request = new DescribeClusterKubeconfigRequest();
            request.setClusterId(config.getClusterId());
            DescribeClusterKubeconfigResponse response = client.DescribeClusterKubeconfig(request);
            return response.getKubeconfig();
        } catch (TencentCloudSDKException e) {
            log.error("腾讯云TKE获取kubeconfig失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取kubeconfig失败: " + e.getMessage());
        }
    }

    @Override
    public List<NodeInfo> listNodes(CloudConfig config) {
        log.info("腾讯云TKE获取节点列表, clusterId: {}", config.getClusterId());
        try {
            TkeClient client = createTkeClient(config);
            DescribeClusterInstancesRequest request = new DescribeClusterInstancesRequest();
            request.setClusterId(config.getClusterId());
            request.setLimit(100L);
            DescribeClusterInstancesResponse response = client.DescribeClusterInstances(request);
            return parseInstanceInfos(response.getInstanceSet());
        } catch (TencentCloudSDKException e) {
            log.error("腾讯云TKE获取节点列表失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取节点列表失败: " + e.getMessage());
        }
    }

    @Override
    public NodeInfo getNodeInfo(CloudConfig config, String nodeId) {
        log.info("腾讯云TKE获取节点详情, clusterId: {}, nodeId: {}", config.getClusterId(), nodeId);
        List<NodeInfo> nodes = listNodes(config);
        return nodes.stream()
                .filter(n -> nodeId.equals(n.getNodeId()) || nodeId.equals(n.getInstanceId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "节点不存在: " + nodeId));
    }

    @Override
    public void scaleCluster(CloudConfig config, Integer targetReplicas) {
        log.info("腾讯云TKE扩缩容集群, clusterId: {}, targetReplicas: {}", config.getClusterId(), targetReplicas);
        try {
            TkeClient client = createTkeClient(config);
            ScaleInClusterMachineRequest request = new ScaleInClusterMachineRequest();
            request.setClusterId(config.getClusterId());

            List<NodeInfo> nodes = listNodes(config);
            int currentCount = (int) nodes.stream()
                    .filter(n -> "worker".equalsIgnoreCase(n.getRole()) || "WORKER".equalsIgnoreCase(n.getRole()))
                    .count();

            if (targetReplicas < currentCount) {
                int scaleInCount = currentCount - targetReplicas;
                List<String> instanceIds = nodes.stream()
                        .filter(n -> "worker".equalsIgnoreCase(n.getRole()) || "WORKER".equalsIgnoreCase(n.getRole()))
                        .limit(scaleInCount)
                        .map(NodeInfo::getInstanceId)
                        .collect(Collectors.toList());
                request.setMachineIds(instanceIds.toArray(new String[0]));
                client.ScaleInClusterMachine(request);
                log.info("腾讯云TKE缩容请求已提交, 缩容节点数: {}", scaleInCount);
            } else if (targetReplicas > currentCount) {
                int scaleOutCount = targetReplicas - currentCount;
                ScaleOutClusterInstancesRequest scaleOutRequest = new ScaleOutClusterInstancesRequest();
                scaleOutRequest.setClusterId(config.getClusterId());
                scaleOutRequest.setRunInstancesPara(new String[]{
                        "{\"InstanceCount\": " + scaleOutCount + "}"
                });
                scaleOutRequest.setScaleType("MANUAL_ADJUST");
                client.ScaleOutClusterInstances(scaleOutRequest);
                log.info("腾讯云TKE扩容请求已提交, 扩容节点数: {}", scaleOutCount);
            } else {
                log.info("目标节点数与当前节点数一致，无需扩缩容, currentCount: {}", currentCount);
            }
        } catch (TencentCloudSDKException e) {
            log.error("腾讯云TKE扩缩容集群失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "扩缩容集群失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> listNamespaces(CloudConfig config) {
        log.info("腾讯云TKE获取命名空间列表, clusterId: {}", config.getClusterId());
        try {
            TkeClient client = createTkeClient(config);
            DescribeClusterNamespacesRequest request = new DescribeClusterNamespacesRequest();
            request.setClusterId(config.getClusterId());
            DescribeClusterNamespacesResponse response = client.DescribeClusterNamespaces(request);
            Namespace[] namespaces = response.getNamespaces();
            if (namespaces == null || namespaces.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.stream(namespaces)
                    .map(Namespace::getNamespace)
                    .collect(Collectors.toList());
        } catch (TencentCloudSDKException e) {
            log.warn("腾讯云TKE获取命名空间失败，尝试通过kubeconfig方式: {}", e.getMessage());
            try {
                String kubeconfig = getClusterKubeconfig(config);
                return listNamespacesFromKubeconfig(kubeconfig);
            } catch (Exception ex) {
                log.error("腾讯云TKE获取命名空间列表失败: {}", ex.getMessage(), ex);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取命名空间列表失败: " + ex.getMessage());
            }
        }
    }

    private List<String> listNamespacesFromKubeconfig(String kubeconfig) {
        try {
            io.fabric8.kubernetes.client.Config config = io.fabric8.kubernetes.client.Config.fromKubeconfig(kubeconfig);
            try (io.fabric8.kubernetes.client.KubernetesClient client = new io.fabric8.kubernetes.client.KubernetesClientBuilder()
                    .withConfig(config)
                    .build()) {
                return client.namespaces().list().getItems().stream()
                        .map(ns -> ns.getMetadata().getName())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("通过kubeconfig获取命名空间失败: {}", e.getMessage());
            return Collections.singletonList("default");
        }
    }

    @Override
    public List<ClusterInfo> listClusters(CloudConfig config) {
        log.info("腾讯云TKE获取集群列表, regionId: {}", config.getRegionId());
        try {
            TkeClient client = createTkeClient(config);
            DescribeClustersRequest request = new DescribeClustersRequest();
            request.setLimit(100L);
            DescribeClustersResponse response = client.DescribeClusters(request);
            return parseClusterInfos(response.getClusters());
        } catch (TencentCloudSDKException e) {
            log.error("腾讯云TKE获取集群列表失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取集群列表失败: " + e.getMessage());
        }
    }

    private List<NodeInfo> parseInstanceInfos(Instance[] instances) {
        if (instances == null || instances.length == 0) {
            return Collections.emptyList();
        }
        List<NodeInfo> result = new ArrayList<>();
        for (Instance instance : instances) {
            InstanceAdvancedSettings settings = instance.getAdvancedSettings();
            String role = (settings != null && settings.getIsMaster() != null && settings.getIsMaster() == 1L)
                    ? "master" : "worker";

            NodeInfo info = NodeInfo.builder()
                    .nodeId(instance.getNodeId())
                    .instanceId(instance.getInstanceId())
                    .name(instance.getInstanceName())
                    .status(instance.getInstanceState())
                    .role(role)
                    .internalIp(instance.getPrivateIp() != null && instance.getPrivateIp().length > 0
                            ? instance.getPrivateIp()[0] : null)
                    .externalIp(instance.getPublicIp() != null && instance.getPublicIp().length > 0
                            ? instance.getPublicIp()[0] : null)
                    .build();

            if (instance.getNodeRole() != null) {
                info.setRole(instance.getNodeRole());
            }

            if (instance.getLabels() != null && instance.getLabels().length > 0) {
                Map<String, String> labelMap = new HashMap<>();
                for (Label label : instance.getLabels()) {
                    labelMap.put(label.getName(), label.getValue());
                }
                info.setLabels(labelMap);
            }

            result.add(info);
        }
        return result;
    }

    private List<ClusterInfo> parseClusterInfos(Cluster[] clusters) {
        if (clusters == null || clusters.length == 0) {
            return Collections.emptyList();
        }
        List<ClusterInfo> result = new ArrayList<>();
        for (Cluster cluster : clusters) {
            ClusterInfo info = ClusterInfo.builder()
                    .clusterId(cluster.getClusterId())
                    .name(cluster.getClusterName())
                    .status(cluster.getClusterStatus())
                    .version(cluster.getClusterVersion())
                    .region(cluster.getClusterOs())
                    .vpcId(cluster.getClusterNetworkSettings() != null
                            ? cluster.getClusterNetworkSettings().getVpcId() : null)
                    .nodeCount(cluster.getClusterNodeNum() != null
                            ? cluster.getClusterNodeNum().intValue() : 0)
                    .build();

            if (cluster.getProperty() != null) {
                info.setRegion(cluster.getProperty().getRegion());
                if (cluster.getProperty().getMasterCount() != null) {
                    info.setMasterCount(cluster.getProperty().getMasterCount().intValue());
                }
            }

            if (info.getNodeCount() != null && info.getMasterCount() != null) {
                info.setWorkerCount(info.getNodeCount() - info.getMasterCount());
            } else {
                info.setWorkerCount(info.getNodeCount());
            }

            if (cluster.getClusterStatus() != null) {
                info.setStatus(cluster.getClusterStatus());
            }

            if (cluster.getServiceCIDR() != null) {
                info.setDomain(cluster.getServiceCIDR());
            }

            if (cluster.getCreatedAt() != null) {
                try {
                    info.setCreateTime(LocalDateTime.ofInstant(
                            TENCENT_DATE_FORMAT.parse(cluster.getCreatedAt(), java.time.temporal.TemporalAccessor::from),
                            ZoneId.systemDefault()
                    ));
                } catch (Exception e) {
                    log.warn("解析创建时间失败: {}", cluster.getCreatedAt());
                }
            }

            result.add(info);
        }
        return result;
    }
}
