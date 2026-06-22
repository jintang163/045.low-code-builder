package com.lowcode.attendance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lowcode.attendance.dto.LeaveApprovalDTO;
import com.lowcode.attendance.dto.LeaveRequestDTO;
import com.lowcode.attendance.entity.LeaveRequest;

import java.util.List;

public interface LeaveRequestService extends IService<LeaveRequest> {
    LeaveRequest create(LeaveRequestDTO dto);
    LeaveRequest approve(LeaveApprovalDTO dto);
    List<LeaveRequest> getByUser(Long appId, Long userId);
    List<LeaveRequest> getByAppAndStatus(Long appId, String status);
    List<LeaveRequest> getMyLeaves(Long appId);
}
