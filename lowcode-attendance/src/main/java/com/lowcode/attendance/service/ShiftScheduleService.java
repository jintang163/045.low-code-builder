package com.lowcode.attendance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lowcode.attendance.dto.BatchScheduleDTO;
import com.lowcode.attendance.dto.ShiftScheduleDTO;
import com.lowcode.attendance.entity.ShiftSchedule;

import java.time.LocalDate;
import java.util.List;

public interface ShiftScheduleService extends IService<ShiftSchedule> {
    List<ShiftSchedule> getByUserAndDateRange(Long appId, Long userId, LocalDate startDate, LocalDate endDate);
    List<ShiftSchedule> getByAppAndDateRange(Long appId, LocalDate startDate, LocalDate endDate);
    ShiftSchedule saveOrUpdate(ShiftScheduleDTO dto);
    void batchSchedule(BatchScheduleDTO dto);
    void deleteByUserAndDate(Long appId, Long userId, LocalDate shiftDate);
}
