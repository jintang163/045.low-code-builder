package com.lowcode.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.attendance.dto.AttendanceStatsDTO;
import com.lowcode.attendance.dto.ClockInDTO;
import com.lowcode.attendance.entity.AttendanceRecord;
import com.lowcode.attendance.entity.LocationConfig;
import com.lowcode.attendance.entity.ShiftSchedule;
import com.lowcode.attendance.mapper.AttendanceRecordMapper;
import com.lowcode.attendance.mapper.LocationConfigMapper;
import com.lowcode.attendance.service.AttendanceRecordService;
import com.lowcode.attendance.service.ShiftScheduleService;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceRecordServiceImpl extends ServiceImpl<AttendanceRecordMapper, AttendanceRecord> implements AttendanceRecordService {

    @Autowired
    private LocationConfigMapper locationConfigMapper;

    @Autowired
    private ShiftScheduleService shiftScheduleService;

    @Value("${attendance.gps.default-latitude:39.9042}")
    private BigDecimal defaultLatitude;

    @Value("${attendance.gps.default-longitude:116.4074}")
    private BigDecimal defaultLongitude;

    @Value("${attendance.gps.allow-radius-meters:500}")
    private Integer defaultAllowRadius;

    @Override
    public AttendanceRecord clockIn(ClockInDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        String userName = UserContext.getCurrentUsername();
        LocalDate today = LocalDate.now();

        validateGpsLocation(dto.getAppId(), dto.getLatitude(), dto.getLongitude());

        AttendanceRecord record = baseMapper.selectByUserAndDate(dto.getAppId(), userId, today);
        if (record == null) {
            record = new AttendanceRecord();
            record.setAppId(dto.getAppId());
            record.setUserId(userId);
            record.setUserName(userName);
            record.setAttendanceDate(today);
            record.setStatus("NORMAL");
        }

        if (record.getClockInTime() != null) {
            throw new BusinessException("今天已经打过上班卡了");
        }

        record.setClockInTime(LocalDateTime.now());
        record.setClockInLatitude(dto.getLatitude());
        record.setClockInLongitude(dto.getLongitude());
        record.setClockInLocation(dto.getLocation());

        ShiftSchedule schedule = getTodaySchedule(dto.getAppId(), userId, today);
        if (schedule != null) {
            record.setShiftType(schedule.getShiftType());
            if (schedule.getStartTime() != null) {
                LocalTime scheduledStartTime = schedule.getStartTime();
                LocalTime actualStartTime = record.getClockInTime().toLocalTime();
                if (actualStartTime.isAfter(scheduledStartTime)) {
                    long minutes = Duration.between(scheduledStartTime, actualStartTime).toMinutes();
                    record.setLateMinutes((int) minutes);
                    if (minutes > 0) {
                        record.setStatus("LATE");
                    }
                }
            }
        }

        if (record.getId() == null) {
            baseMapper.insert(record);
        } else {
            baseMapper.updateById(record);
        }
        return record;
    }

    @Override
    public AttendanceRecord clockOut(ClockInDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        String userName = UserContext.getCurrentUsername();
        LocalDate today = LocalDate.now();

        validateGpsLocation(dto.getAppId(), dto.getLatitude(), dto.getLongitude());

        AttendanceRecord record = baseMapper.selectByUserAndDate(dto.getAppId(), userId, today);
        if (record == null || record.getClockInTime() == null) {
            throw new BusinessException("请先打上班卡");
        }
        if (record.getClockOutTime() != null) {
            throw new BusinessException("今天已经打过下班卡了");
        }

        record.setClockOutTime(LocalDateTime.now());
        record.setClockOutLatitude(dto.getLatitude());
        record.setClockOutLongitude(dto.getLongitude());
        record.setClockOutLocation(dto.getLocation());

        long minutes = Duration.between(record.getClockInTime(), record.getClockOutTime()).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        record.setWorkHours(hours);

        ShiftSchedule schedule = getTodaySchedule(dto.getAppId(), userId, today);
        if (schedule != null && schedule.getEndTime() != null && "REST".equals(schedule.getShiftType())) {
            schedule = null;
        }
        if (schedule != null && schedule.getEndTime() != null) {
            LocalTime scheduledEndTime = schedule.getEndTime();
            LocalTime actualEndTime = record.getClockOutTime().toLocalTime();
            if (actualEndTime.isBefore(scheduledEndTime)) {
                long earlyMinutes = Duration.between(actualEndTime, scheduledEndTime).toMinutes();
                record.setEarlyMinutes((int) earlyMinutes);
                if ("NORMAL".equals(record.getStatus())) {
                    record.setStatus("EARLY");
                } else if ("LATE".equals(record.getStatus())) {
                    record.setStatus("LATE");
                }
            }
        }

        baseMapper.updateById(record);
        return record;
    }

    private void validateGpsLocation(Long appId, BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new BusinessException("无法获取定位信息，请开启GPS定位后再打卡");
        }

        List<LocationConfig> locations = locationConfigMapper.selectByAppId(appId);
        if (locations.isEmpty()) {
            double distance = calculateDistance(latitude.doubleValue(), longitude.doubleValue(),
                    defaultLatitude.doubleValue(), defaultLongitude.doubleValue());
            if (distance > defaultAllowRadius) {
                throw new BusinessException("打卡位置超出允许范围，当前距离考勤点" + String.format("%.0f", distance) + "米，允许范围" + defaultAllowRadius + "米");
            }
        } else {
            boolean valid = false;
            double minDistance = Double.MAX_VALUE;
            for (LocationConfig loc : locations) {
                double distance = calculateDistance(latitude.doubleValue(), longitude.doubleValue(),
                        loc.getLatitude().doubleValue(), loc.getLongitude().doubleValue());
                minDistance = Math.min(minDistance, distance);
                if (distance <= loc.getAllowRadius()) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                throw new BusinessException("打卡位置超出允许范围，最近考勤点距离" + String.format("%.0f", minDistance) + "米");
            }
        }
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

    private ShiftSchedule getTodaySchedule(Long appId, Long userId, LocalDate date) {
        List<ShiftSchedule> schedules = shiftScheduleService.getByUserAndDateRange(appId, userId, date, date);
        return schedules.isEmpty() ? null : schedules.get(0);
    }

    @Override
    public AttendanceRecord getTodayRecord(Long appId, Long userId) {
        return baseMapper.selectByUserAndDate(appId, userId, LocalDate.now());
    }

    @Override
    public List<AttendanceRecord> getByUserAndDateRange(Long appId, Long userId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectByUserAndDateRange(appId, userId, startDate, endDate);
    }

    @Override
    public List<AttendanceRecord> getByAppAndDateRange(Long appId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectByAppAndDateRange(appId, startDate, endDate);
    }

    @Override
    public List<AttendanceStatsDTO> getStatsByDateRange(Long appId, LocalDate startDate, LocalDate endDate) {
        List<AttendanceRecord> records = baseMapper.selectByAppAndDateRange(appId, startDate, endDate);

        Map<Long, List<AttendanceRecord>> groupMap = records.stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getUserId));

        List<AttendanceStatsDTO> statsList = new ArrayList<>();
        for (Map.Entry<Long, List<AttendanceRecord>> entry : groupMap.entrySet()) {
            AttendanceStatsDTO stats = new AttendanceStatsDTO();
            stats.setUserId(entry.getKey());
            List<AttendanceRecord> userRecords = entry.getValue();

            if (!userRecords.isEmpty()) {
                stats.setUserName(userRecords.get(0).getUserName());
            }

            int workDays = 0;
            BigDecimal totalHours = BigDecimal.ZERO;
            int lateCount = 0;
            int earlyCount = 0;
            int absentCount = 0;

            for (AttendanceRecord rec : userRecords) {
                if (rec.getClockInTime() != null) {
                    workDays++;
                }
                if (rec.getWorkHours() != null) {
                    totalHours = totalHours.add(rec.getWorkHours());
                }
                if ("LATE".equals(rec.getStatus())) {
                    lateCount++;
                }
                if ("EARLY".equals(rec.getStatus())) {
                    earlyCount++;
                }
                if ("ABSENT".equals(rec.getStatus())) {
                    absentCount++;
                }
            }

            stats.setWorkDays(workDays);
            stats.setTotalHours(totalHours);
            stats.setLateCount(lateCount);
            stats.setEarlyCount(earlyCount);
            stats.setAbsentCount(absentCount);

            statsList.add(stats);
        }

        return statsList;
    }
}
