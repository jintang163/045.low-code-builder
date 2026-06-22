package com.lowcode.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_attendance_record")
public class AttendanceRecord extends BaseEntity {
    private Long appId;
    private Long userId;
    private String userName;
    private LocalDate attendanceDate;
    private LocalDateTime clockInTime;
    private LocalDateTime clockOutTime;
    private BigDecimal clockInLatitude;
    private BigDecimal clockInLongitude;
    private BigDecimal clockOutLatitude;
    private BigDecimal clockOutLongitude;
    private String clockInLocation;
    private String clockOutLocation;
    private BigDecimal workHours;
    private String status;
    private Integer lateMinutes;
    private Integer earlyMinutes;
    private String shiftType;
    private String remark;
}
