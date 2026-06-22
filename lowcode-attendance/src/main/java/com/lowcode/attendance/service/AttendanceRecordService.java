package com.lowcode.attendance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lowcode.attendance.dto.AttendanceStatsDTO;
import com.lowcode.attendance.dto.ClockInDTO;
import com.lowcode.attendance.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordService extends IService<AttendanceRecord> {
    AttendanceRecord clockIn(ClockInDTO dto);
    AttendanceRecord clockOut(ClockInDTO dto);
    AttendanceRecord getTodayRecord(Long appId, Long userId);
    List<AttendanceRecord> getByUserAndDateRange(Long appId, Long userId, LocalDate startDate, LocalDate endDate);
    List<AttendanceRecord> getByAppAndDateRange(Long appId, LocalDate startDate, LocalDate endDate);
    List<AttendanceStatsDTO> getStatsByDateRange(Long appId, LocalDate startDate, LocalDate endDate);
}
