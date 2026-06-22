package com.lowcode.attendance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AttendanceStatsDTO {
    private Long userId;
    private String userName;
    private Integer workDays;
    private BigDecimal totalHours;
    private Integer lateCount;
    private Integer earlyCount;
    private Integer absentCount;
    private BigDecimal leaveDays;
}
