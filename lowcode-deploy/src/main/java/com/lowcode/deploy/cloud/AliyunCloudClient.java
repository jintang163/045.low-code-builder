package com.lowcode.deploy.cloud;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.cs.model.v20151215.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.deploy.cloud.dto.ClusterInfo;
import com.lowcode.deploy.cloud.dto.NodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@ConditionalOnClass(name = "com.aliyun.cs.Client")
public class AliyunCloudClient extends AbstractCloudClient {

    private static final DateTimeFormatter ALIYUN_DATE_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private IAcsClient createAcsClient(CloudConfig config) {
        DefaultProfile profile = DefaultProfile.getProfile(
                config.getRegionId(),
                config.getAccessKeyId(),
                config.getAccessKeySecret()
        );
        return new DefaultAcsClient(profile);
    }

    @Override
    public String getClusterKubeconfig(CloudConfig config) {
        log.info("阿里云ACK获取集群kubeconfig, clusterId: {}", config.getClusterId());
        try {
            IAcsClient client = createAcsClient(config);
            DescribeClusterUserKubeconfigRequest request = new DescribeClusterUserKubeconfigRequest();
            request.setClusterId(config.getClusterId());
            DescribeClusterUserKubeconfigResponse response = client.getAcsResponse(request);
            return response.getConfig();
        } catch (ServerException e) {
            log.error("阿里云ACK服务端异常获取kubeconfig: {}", e.getMessage(), e);
            return getKubeconfigByHttp(config);
        } catch (ClientException e) {
            log.error("阿里云ACK客户端异常获取kubeconfig: {}", e.getMessage(), e);
            return getKubeconfigByHttp(config);
        } catch (Exception e) {
            log.error("阿里云ACK获取kubeconfig失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取kubeconfig失败: " + e.getMessage());
        }
    }

    private String getKubeconfigByHttp(CloudConfig config) {
        log.info("通过HTTP方式获取阿里云ACK kubeconfig");
        String url = String.format("https://cs.aliyuncs.com/clusters/%s/user_kubeconfig", config.getClusterId());
        Map<String, String> headers = buildAliyunHeaders(config, "GET", "/clusters/" + config.getClusterId() + "/user_kubeconfig", null);
        String response = httpGet(url, headers, null);
        JSONObject json = JSON.parseObject(response);
        return json.getString("config");
    }

    @Override
    public List<NodeInfo> listNodes(CloudConfig config) {
        log.info("阿里云ACK获取节点列表, clusterId: {}", config.getClusterId());
        try {
            IAcsClient client = createAcsClient(config);
            DescribeClusterNodesRequest request = new DescribeClusterNodesRequest();
            request.setClusterId(config.getClusterId());
            DescribeClusterNodesResponse response = client.getAcsResponse(request);
            return parseNodeInfos(response.getNodes());
        } catch (Exception e) {
            log.error("阿里云ACK获取节点列表失败,尝试HTTP方式: {}", e.getMessage());
            return listNodesByHttp(config);
        }
    }

    private List<NodeInfo> listNodesByHttp(CloudConfig config) {
        String url = String.format("https://cs.aliyuncs.com/clusters/%s/nodes", config.getClusterId());
        Map<String, String> headers = buildAliyunHeaders(config, "GET", "/clusters/" + config.getClusterId() + "/nodes", null);
        String response = httpGet(url, headers, null);
        JSONObject json = JSON.parseObject(response);
        return parseNodeInfosFromJson(json.getJSONArray("nodes"));
    }

    @Override
    public NodeInfo getNodeInfo(CloudConfig config, String nodeId) {
        log.info("阿里云ACK获取节点详情, clusterId: {}, nodeId: {}", config.getClusterId(), nodeId);
        List<NodeInfo> nodes = listNodes(config);
        return nodes.stream()
                .filter(n -> nodeId.equals(n.getNodeId()) || nodeId.equals(n.getInstanceId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "节点不存在: " + nodeId));
    }

    @Override
    public void scaleCluster(CloudConfig config, Integer targetReplicas) {
        log.info("阿里云ACK扩缩容集群, clusterId: {}, targetReplicas: {}", config.getClusterId(), targetReplicas);
        try {
            IAcsClient client = createAcsClient(config);
            ScaleClusterRequest request = new ScaleClusterRequest();
            request.setClusterId(config.getClusterId());
            JSONObject body = new JSONObject();
            body.put("key_pairs", Collections.emptyList());
            body.put("count", targetReplicas);
            body.put("login_password", "");
            body.put("runtime", "docker");
            body.put("runtime_version", "19.03.15");
            request.setHttpContent(JSON.toJSONString(body).getBytes(), "UTF-8", com.aliyuncs.http.FormatType.JSON);
            client.getAcsResponse(request);
            log.info("阿里云ACK集群扩缩容请求已提交");
        } catch (Exception e) {
            log.error("阿里云ACK扩缩容集群失败: {}", e.getMessage(), e);
            scaleClusterByHttp(config, targetReplicas);
        }
    }

    private void scaleClusterByHttp(CloudConfig config, Integer targetReplicas) {
        String url = String.format("https://cs.aliyuncs.com/clusters/%s", config.getClusterId());
        JSONObject body = new JSONObject();
        body.put("key_pairs", Collections.emptyList());
        body.put("count", targetReplicas);
        body.put("login_password", "");
        body.put("runtime", "docker");
        body.put("runtime_version", "19.03.15");
        Map<String, String> headers = buildAliyunHeaders(config, "PUT", "/clusters/" + config.getClusterId(), body);
        httpPut(url, headers, body);
    }

    @Override
    public List<String> listNamespaces(CloudConfig config) {
        log.info("阿里云ACK获取命名空间列表, clusterId: {}", config.getClusterId());
        try {
            String kubeconfig = getClusterKubeconfig(config);
            return listNamespacesFromKubeconfig(kubeconfig);
        } catch (Exception e) {
            log.error("阿里云ACK获取命名空间列表失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取命名空间列表失败: " + e.getMessage());
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
                        .toList();
            }
        } catch (Exception e) {
            log.error("通过kubeconfig获取命名空间失败: {}", e.getMessage());
            return Collections.singletonList("default");
        }
    }

    @Override
    public List<ClusterInfo> listClusters(CloudConfig config) {
        log.info("阿里云ACK获取集群列表, regionId: {}", config.getRegionId());
        try {
            IAcsClient client = createAcsClient(config);
            DescribeClustersRequest request = new DescribeClustersRequest();
            DescribeClustersResponse response = client.getAcsResponse(request);
            return parseClusterInfos(response.getClusters());
        } catch (Exception e) {
            log.error("阿里云ACK获取集群列表失败,尝试HTTP方式: {}", e.getMessage());
            return listClustersByHttp(config);
        }
    }

    private List<ClusterInfo> listClustersByHttp(CloudConfig config) {
        String url = "https://cs.aliyuncs.com/clusters";
        Map<String, String> headers = buildAliyunHeaders(config, "GET", "/clusters", null);
        String response = httpGet(url, headers, null);
        JSONArray jsonArray = JSON.parseArray(response);
        return parseClusterInfosFromJson(jsonArray);
    }

    private List<NodeInfo> parseNodeInfos(List<DescribeClusterNodesResponse.Nodes> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<NodeInfo> result = new ArrayList<>();
        for (DescribeClusterNodesResponse.Nodes node : nodes) {
            NodeInfo info = NodeInfo.builder()
                    .nodeId(node.getId())
                    .instanceId(node.getInstanceId())
                    .name(node.getNodeName())
                    .status(node.getStatus())
                    .role(node.getNodeRole())
                    .internalIp(node.getPrivateIp())
                    .externalIp(node.getEip())
                    .build();
            if (StrUtil.isNotBlank(node.getNodeStatus())) {
                info.setStatus(node.getNodeStatus());
            }
            result.add(info);
        }
        return result;
    }

    private List<NodeInfo> parseNodeInfosFromJson(JSONArray nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<NodeInfo> result = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            NodeInfo info = NodeInfo.builder()
                    .nodeId(node.getString("id"))
                    .instanceId(node.getString("instance_id"))
                    .name(node.getString("instance_name"))
                    .status(node.getString("status"))
                    .role(node.getString("node_role"))
                    .internalIp(node.getString("private_ip"))
                    .externalIp(node.getString("eip"))
                    .build();
            if (node.containsKey("node_status")) {
                info.setStatus(node.getString("node_status"));
            }
            if (node.containsKey("labels")) {
                JSONObject labels = node.getJSONObject("labels");
                if (labels != null) {
                    Map<String, String> labelMap = new HashMap<>();
                    labels.forEach((k, v) -> labelMap.put(k, String.valueOf(v)));
                    info.setLabels(labelMap);
                }
            }
            result.add(info);
        }
        return result;
    }

    private List<ClusterInfo> parseClusterInfos(List<DescribeClustersResponse.Clusters> clusters) {
        if (clusters == null || clusters.isEmpty()) {
            return Collections.emptyList();
        }
        List<ClusterInfo> result = new ArrayList<>();
        for (DescribeClustersResponse.Clusters cluster : clusters) {
            ClusterInfo info = ClusterInfo.builder()
                    .clusterId(cluster.getClusterId())
                    .name(cluster.getName())
                    .status(cluster.getState())
                    .version(cluster.getClusterType())
                    .region(cluster.getRegionId())
                    .vpcId(cluster.getVpcId())
                    .nodeCount(cluster.getSize())
                    .apiServerEndpoint(cluster.getApiServer())
                    .domain(cluster.getClusterType())
                    .build();
            if (cluster.getKubernetesVersion() != null) {
                info.setVersion(cluster.getKubernetesVersion());
            }
            if (cluster.getCreated() != null) {
                try {
                    info.setCreateTime(LocalDateTime.ofInstant(
                            ALIYUN_DATE_FORMAT.parse(cluster.getCreated(), java.time.temporal.TemporalAccessor::from),
                            ZoneId.systemDefault()
                    ));
                } catch (Exception e) {
                    log.warn("解析创建时间失败: {}", cluster.getCreated());
                }
            }
            result.add(info);
        }
        return result;
    }

    private List<ClusterInfo> parseClusterInfosFromJson(JSONArray clusters) {
        if (clusters == null || clusters.isEmpty()) {
            return Collections.emptyList();
        }
        List<ClusterInfo> result = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            JSONObject cluster = clusters.getJSONObject(i);
            ClusterInfo info = ClusterInfo.builder()
                    .clusterId(cluster.getString("cluster_id"))
                    .name(cluster.getString("name"))
                    .status(cluster.getString("state"))
                    .version(cluster.getString("cluster_type"))
                    .region(cluster.getString("region_id"))
                    .vpcId(cluster.getString("vpc_id"))
                    .nodeCount(cluster.getInteger("size"))
                    .apiServerEndpoint(cluster.getString("api_server"))
                    .domain(cluster.getString("cluster_type"))
                    .build();
            if (cluster.containsKey("kubernetes_version")) {
                info.setVersion(cluster.getString("kubernetes_version"));
            }
            if (cluster.containsKey("created")) {
                try {
                    info.setCreateTime(LocalDateTime.ofInstant(
                            ALIYUN_DATE_FORMAT.parse(cluster.getString("created"), java.time.temporal.TemporalAccessor::from),
                            ZoneId.systemDefault()
                    ));
                } catch (Exception e) {
                    log.warn("解析创建时间失败: {}", cluster.getString("created"));
                }
            }
            result.add(info);
        }
        return result;
    }

    private Map<String, String> buildAliyunHeaders(CloudConfig config, String method, String path, JSONObject body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-acs-version", "2015-12-15");
        headers.put("x-acs-signature-nonce", UUID.randomUUID().toString());
        headers.put("x-acs-signature-method", "HMAC-SHA1");
        headers.put("x-acs-signature-version", "1.0");
        headers.put("Date", DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)));
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
