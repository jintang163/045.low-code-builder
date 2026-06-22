package com.lowcode.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.attendance.dto.LeaveApprovalDTO;
import com.lowcode.attendance.dto.LeaveRequestDTO;
import com.lowcode.attendance.entity.LeaveRequest;
import com.lowcode.attendance.mapper.LeaveRequestMapper;
import com.lowcode.attendance.service.LeaveRequestService;
import com.lowcode.common.util.UserContext;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveRequestServiceImpl extends ServiceImpl<LeaveRequestMapper, LeaveRequest> implements LeaveRequestService {

    @Override
    public LeaveRequest create(LeaveRequestDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        String userName = UserContext.getCurrentUsername();

        LeaveRequest leave = new LeaveRequest();
        BeanUtils.copyProperties(dto, leave);
        leave.setUserId(userId);
        leave.setUserName(userName);
        leave.setStatus("PENDING");

        if (dto.getLeaveDays() == null || dto.getLeaveDays().compareTo(BigDecimal.ZERO) == 0) {
            long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
            leave.setLeaveDays(BigDecimal.valueOf(days));
        }

        baseMapper.insert(leave);
        return leave;
    }

    @Override
    public LeaveRequest approve(LeaveApprovalDTO dto) {
        Long approverId = UserContext.getCurrentUserId();
        String approverName = UserContext.getCurrentUsername();

        LeaveRequest leave = baseMapper.selectById(dto.getId());
        if (leave == null) {
            throw new IllegalArgumentException("请假申请不存在");
        }
        if (!"PENDING".equals(leave.getStatus())) {
            throw new IllegalStateException("该申请已处理");
        }

        leave.setStatus(dto.getStatus());
        leave.setApproverId(approverId);
        leave.setApproverName(approverName);
        leave.setApprovalRemark(dto.getApprovalRemark());
        leave.setApprovalTime(LocalDateTime.now());

        baseMapper.updateById(leave);
        return leave;
    }

    @Override
    public List<LeaveRequest> getByUser(Long appId, Long userId) {
        return baseMapper.selectByUser(appId, userId);
    }

    @Override
    public List<LeaveRequest> getByAppAndStatus(Long appId, String status) {
        return baseMapper.selectByAppAndStatus(appId, status);
    }

    @Override
    public List<LeaveRequest> getMyLeaves(Long appId) {
        Long userId = UserContext.getCurrentUserId();
        return baseMapper.selectByUser(appId, userId);
    }
}
