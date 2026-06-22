package com.lowcode.attendance.controller;

import com.lowcode.attendance.dto.AttendanceStatsDTO;
import com.lowcode.attendance.dto.ClockInDTO;
import com.lowcode.attendance.entity.AttendanceRecord;
import com.lowcode.attendance.service.AttendanceRecordService;
import com.lowcode.common.result.Result;
import com.lowcode.common.util.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Api(tags = "考勤打卡")
@RestController
@RequestMapping("/attendance/record")
public class AttendanceRecordController {

    @Autowired
    private AttendanceRecordService attendanceRecordService;

    @ApiOperation("上班打卡")
    @PostMapping("/clock-in")
    public Result<AttendanceRecord> clockIn(@RequestBody ClockInDTO dto) {
        AttendanceRecord record = attendanceRecordService.clockIn(dto);
        return Result.ok(record);
    }

    @ApiOperation("下班打卡")
    @PostMapping("/clock-out")
    public Result<AttendanceRecord> clockOut(@RequestBody ClockInDTO dto) {
        AttendanceRecord record = attendanceRecordService.clockOut(dto);
        return Result.ok(record);
    }

    @ApiOperation("获取今日打卡记录")
    @GetMapping("/today")
    public Result<AttendanceRecord> getTodayRecord(@RequestParam Long appId) {
        Long userId = UserContext.getCurrentUserId();
        AttendanceRecord record = attendanceRecordService.getTodayRecord(appId, userId);
        return Result.ok(record);
    }

    @ApiOperation("获取我的考勤记录（日期范围）")
    @GetMapping("/my")
    public Result<List<AttendanceRecord>> getMyRecords(
            @RequestParam Long appId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long userId = UserContext.getCurrentUserId();
        List<AttendanceRecord> list = attendanceRecordService.getByUserAndDateRange(appId, userId, startDate, endDate);
        return Result.ok(list);
    }

    @ApiOperation("获取员工考勤记录（日期范围）")
    @GetMapping("/user/list")
    public Result<List<AttendanceRecord>> getByUser(
            @RequestParam Long appId,
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<AttendanceRecord> list = attendanceRecordService.getByUserAndDateRange(appId, userId, startDate, endDate);
        return Result.ok(list);
    }

    @ApiOperation("获取全部考勤记录（日期范围）")
    @GetMapping("/app/list")
    public Result<List<AttendanceRecord>> getByApp(
            @RequestParam Long appId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<AttendanceRecord> list = attendanceRecordService.getByAppAndDateRange(appId, startDate, endDate);
        return Result.ok(list);
    }

    @ApiOperation("获取考勤统计（日期范围）")
    @GetMapping("/stats")
    public Result<List<AttendanceStatsDTO>> getStats(
            @RequestParam Long appId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<AttendanceStatsDTO> stats = attendanceRecordService.getStatsByDateRange(appId, startDate, endDate);
        return Result.ok(stats);
    }
}
