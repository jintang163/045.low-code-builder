package com.lowcode.attendance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class LeaveRequestDTO {
    private Long appId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal leaveDays;
    private BigDecimal leaveHours;
    private String reason;
    private String attachmentUrl;
}
