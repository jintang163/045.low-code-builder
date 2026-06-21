package com.lowcode.flow.dto;

import lombok.Data;

@Data
public class RpaScheduleDTO {
    private String cronExpression;
    private String scheduleParams;
}
