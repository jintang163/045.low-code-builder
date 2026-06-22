package com.lowcode.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_leave_request")
public class LeaveRequest extends BaseEntity {
    private Long appId;
    private Long userId;
    private String userName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal leaveDays;
    private BigDecimal leaveHours;
    private String reason;
    private String attachmentUrl;
    private Long approverId;
    private String approverName;
    private LocalDateTime approvalTime;
    private String approvalRemark;
    private String status;
}
