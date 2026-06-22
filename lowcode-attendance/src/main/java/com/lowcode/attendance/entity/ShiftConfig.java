package com.lowcode.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_shift_config")
public class ShiftConfig extends BaseEntity {
    private Long appId;
    private String shiftCode;
    private String shiftName;
    private String shiftColor;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal workHours;
    private BigDecimal hourlyWage;
    private Integer sortOrder;
    private Integer status;
}
