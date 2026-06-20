package com.lowcode.deploy.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeInfo {

    private String nodeId;

    private String instanceId;

    private String name;

    private String status;

    private String role;

    private Double cpuTotal;

    private Double cpuUsed;

    private Double memoryTotal;

    private Double memoryUsed;

    private Integer podCapacity;

    private Integer podAllocatable;

    private String internalIp;

    private String externalIp;

    private Map<String, String> labels;
}
