package com.lowcode.attendance.controller;

import com.lowcode.attendance.dto.LeaveApprovalDTO;
import com.lowcode.attendance.dto.LeaveRequestDTO;
import com.lowcode.attendance.entity.LeaveRequest;
import com.lowcode.attendance.service.LeaveRequestService;
import com.lowcode.common.result.Result;
import com.lowcode.common.util.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "请假管理")
@RestController
@RequestMapping("/attendance/leave")
public class LeaveRequestController {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @ApiOperation("提交请假申请")
    @PostMapping
    public Result<LeaveRequest> create(@RequestBody LeaveRequestDTO dto) {
        LeaveRequest leave = leaveRequestService.create(dto);
        return Result.ok(leave);
    }

    @ApiOperation("审批请假")
    @PostMapping("/approve")
    public Result<LeaveRequest> approve(@RequestBody LeaveApprovalDTO dto) {
        LeaveRequest leave = leaveRequestService.approve(dto);
        return Result.ok(leave);
    }

    @ApiOperation("获取我的请假记录")
    @GetMapping("/my")
    public Result<List<LeaveRequest>> getMyLeaves(@RequestParam Long appId) {
        List<LeaveRequest> list = leaveRequestService.getMyLeaves(appId);
        return Result.ok(list);
    }

    @ApiOperation("获取员工请假记录")
    @GetMapping("/user")
    public Result<List<LeaveRequest>> getByUser(@RequestParam Long appId, @RequestParam Long userId) {
        List<LeaveRequest> list = leaveRequestService.getByUser(appId, userId);
        return Result.ok(list);
    }

    @ApiOperation("获取待审批列表")
    @GetMapping("/pending")
    public Result<List<LeaveRequest>> getPending(@RequestParam Long appId) {
        List<LeaveRequest> list = leaveRequestService.getByAppAndStatus(appId, "PENDING");
        return Result.ok(list);
    }

    @ApiOperation("获取全部请假申请（按状态）")
    @GetMapping("/app/list")
    public Result<List<LeaveRequest>> getByAppAndStatus(
            @RequestParam Long appId,
            @RequestParam(required = false, defaultValue = "") String status) {
        if (status != null && !status.isEmpty()) {
            return Result.ok(leaveRequestService.getByAppAndStatus(appId, status));
        }
        List<LeaveRequest> all = leaveRequestService.getByAppAndStatus(appId, "APPROVED");
        all.addAll(leaveRequestService.getByAppAndStatus(appId, "PENDING"));
        all.addAll(leaveRequestService.getByAppAndStatus(appId, "REJECTED"));
        return Result.ok(all);
    }
}
