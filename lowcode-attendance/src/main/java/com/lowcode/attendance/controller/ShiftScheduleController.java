package com.lowcode.attendance.controller;

import com.lowcode.attendance.dto.BatchScheduleDTO;
import com.lowcode.attendance.dto.ShiftScheduleDTO;
import com.lowcode.attendance.entity.ShiftConfig;
import com.lowcode.attendance.entity.ShiftSchedule;
import com.lowcode.attendance.mapper.ShiftConfigMapper;
import com.lowcode.attendance.service.ShiftScheduleService;
import com.lowcode.common.result.Result;
import com.lowcode.common.util.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Api(tags = "排班管理")
@RestController
@RequestMapping("/attendance/shift")
public class ShiftScheduleController {

    @Autowired
    private ShiftScheduleService shiftScheduleService;

    @Autowired
    private ShiftConfigMapper shiftConfigMapper;

    @ApiOperation("获取班次配置列表")
    @GetMapping("/configs")
    public Result<List<ShiftConfig>> getShiftConfigs(@RequestParam Long appId) {
        List<ShiftConfig> configs = shiftConfigMapper.selectByAppId(appId);
        return Result.ok(configs);
    }

    @ApiOperation("获取员工排班（日期范围）")
    @GetMapping("/user/list")
    public Result<List<ShiftSchedule>> getByUserAndDateRange(
            @RequestParam Long appId,
            @RequestParam(required = false) Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long uid = userId != null ? userId : UserContext.getCurrentUserId();
        List<ShiftSchedule> list = shiftScheduleService.getByUserAndDateRange(appId, uid, startDate, endDate);
        return Result.ok(list);
    }

    @ApiOperation("获取应用全部排班（日期范围）")
    @GetMapping("/app/list")
    public Result<List<ShiftSchedule>> getByAppAndDateRange(
            @RequestParam Long appId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<ShiftSchedule> list = shiftScheduleService.getByAppAndDateRange(appId, startDate, endDate);
        return Result.ok(list);
    }

    @ApiOperation("保存或更新单日排班")
    @PostMapping
    public Result<ShiftSchedule> saveOrUpdate(@RequestBody ShiftScheduleDTO dto) {
        ShiftSchedule schedule = shiftScheduleService.saveOrUpdate(dto);
        return Result.ok(schedule);
    }

    @ApiOperation("批量排班")
    @PostMapping("/batch")
    public Result<Void> batchSchedule(@RequestBody BatchScheduleDTO dto) {
        shiftScheduleService.batchSchedule(dto);
        return Result.ok();
    }

    @ApiOperation("删除单日排班")
    @DeleteMapping
    public Result<Void> delete(
            @RequestParam Long appId,
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate shiftDate) {
        shiftScheduleService.deleteByUserAndDate(appId, userId, shiftDate);
        return Result.ok();
    }
}
