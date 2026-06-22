package com.lowcode.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_shift_schedule")
public class ShiftSchedule extends BaseEntity {
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
