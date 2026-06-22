package com.lowcode.attendance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ShiftScheduleDTO {
    private Long appId;
    private Long userId;
    private String userName;
    private String shiftType;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal workHours;
    private BigDecimal hourlyWage;
    private String remark;
}
