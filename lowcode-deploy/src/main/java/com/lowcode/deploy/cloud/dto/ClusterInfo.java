package com.lowcode.deploy.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterInfo {

    private String clusterId;

    private String name;

    private String status;

    private String version;

    private String region;

    private String vpcId;

    private Integer nodeCount;

    private Integer masterCount;

    private Integer workerCount;

    private String apiServerEndpoint;

    private String domain;

    private LocalDateTime createTime;
}
