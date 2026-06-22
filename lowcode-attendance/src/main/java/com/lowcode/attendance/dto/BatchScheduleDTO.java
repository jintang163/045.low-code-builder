package com.lowcode.attendance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BatchScheduleDTO {
    private Long appId;
    private List<Long> userIds;
    private LocalDate startDate;
    private LocalDate endDate;
    private String shiftType;
    private BigDecimal workHours;
    private BigDecimal hourlyWage;
}
