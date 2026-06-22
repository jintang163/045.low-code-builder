package com.lowcode.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_salary_record")
public class SalaryRecord extends BaseEntity {
    private Long appId;
    private Long userId;
    private String userName;
    private String salaryMonth;
    private Integer totalWorkDays;
    private BigDecimal totalWorkHours;
    private BigDecimal baseSalary;
    private BigDecimal hourlyWage;
    private BigDecimal overtimeHours;
    private BigDecimal overtimePay;
    private BigDecimal leaveDays;
    private BigDecimal leaveDeduction;
    private BigDecimal lateDeduction;
    private BigDecimal earlyDeduction;
    private BigDecimal absentDeduction;
    private BigDecimal bonus;
    private BigDecimal subsidy;
    private BigDecimal deductionTotal;
    private BigDecimal netSalary;
    private String remark;
    private String status;
    private LocalDateTime paidTime;
}
