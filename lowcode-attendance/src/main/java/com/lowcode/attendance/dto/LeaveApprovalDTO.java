package com.lowcode.attendance.dto;

import lombok.Data;

@Data
public class LeaveApprovalDTO {
    private Long id;
    private String status;
    private String approvalRemark;
}
