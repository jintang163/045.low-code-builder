package com.lowcode.attendance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryGenerateDTO {
    private Long appId;
    private String salaryMonth;
    private BigDecimal defaultHourlyWage;
}
