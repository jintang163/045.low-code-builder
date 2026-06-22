package com.lowcode.attendance.controller;

import com.lowcode.attendance.dto.AttendanceStatsDTO;
import com.lowcode.attendance.dto.ClockInDTO;
import com.lowcode.attendance.entity.AttendanceRecord;
import com.lowcode.attendance.entity.LocationConfig;
import com.lowcode.attendance.mapper.LocationConfigMapper;
import com.lowcode.attendance.service.AttendanceRecordService;
import com.lowcode.common.result.Result;
import com.lowcode.common.util.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "考勤打卡")
@RestController
@RequestMapping("/attendance/record")
public class AttendanceRecordController {

    @Autowired
    private AttendanceRecordService attendanceRecordService;

    @Autowired
    private LocationConfigMapper locationConfigMapper;

    @Value("${attendance.gps.default-latitude:39.9042}")
    private BigDecimal defaultLatitude;

    @Value("${attendance.gps.default-longitude:116.4074}")
    private BigDecimal defaultLongitude;

    @Value("${attendance.gps.allow-radius-meters:500}")
    private Integer defaultAllowRadius;

    @ApiOperation("校验GPS位置")
    @GetMapping("/check-location")
    public Result<Map<String, Object>> checkLocation(
            @RequestParam Long appId,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        Map<String, Object> result = new HashMap<>();
        boolean inRange = false;
        double minDistance = Double.MAX_VALUE;

        List<LocationConfig> locations = locationConfigMapper.selectByAppId(appId);
        if (locations.isEmpty()) {
            double distance = calculateDistance(latitude.doubleValue(), longitude.doubleValue(),
                    defaultLatitude.doubleValue(), defaultLongitude.doubleValue());
            minDistance = distance;
            inRange = distance <= defaultAllowRadius;
        } else {
            for (LocationConfig loc : locations) {
                double distance = calculateDistance(latitude.doubleValue(), longitude.doubleValue(),
                        loc.getLatitude().doubleValue(), loc.getLongitude().doubleValue());
                minDistance = Math.min(minDistance, distance);
                if (distance <= loc.getAllowRadius()) {
                    inRange = true;
                    break;
                }
            }
        }

        result.put("inRange", inRange);
        result.put("distance", minDistance);
        return Result.ok(result);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

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
