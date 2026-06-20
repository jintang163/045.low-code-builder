package com.lowcode.deploy.cloud;

import com.lowcode.deploy.cloud.dto.ClusterInfo;
import com.lowcode.deploy.cloud.dto.NodeInfo;

import java.util.List;

public interface CloudClient {

    String getClusterKubeconfig(CloudConfig config);

    List<NodeInfo> listNodes(CloudConfig config);

    NodeInfo getNodeInfo(CloudConfig config, String nodeId);

    void scaleCluster(CloudConfig config, Integer targetReplicas);

    List<String> listNamespaces(CloudConfig config);

    List<ClusterInfo> listClusters(CloudConfig config);

    boolean testConnection(CloudConfig config);
}
