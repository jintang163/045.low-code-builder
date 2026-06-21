package com.lowcode.flow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RpaExecuteDTO {
    private Long scriptId;
    private String triggerType;
    private Long triggerLogicId;
    private String triggerNodeId;
    private Map<String, Object> inputParams;
}
