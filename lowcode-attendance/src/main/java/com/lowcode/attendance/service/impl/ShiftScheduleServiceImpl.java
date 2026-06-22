package com.lowcode.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.attendance.dto.BatchScheduleDTO;
import com.lowcode.attendance.dto.ShiftScheduleDTO;
import com.lowcode.attendance.entity.ShiftConfig;
import com.lowcode.attendance.entity.ShiftSchedule;
import com.lowcode.attendance.mapper.ShiftConfigMapper;
import com.lowcode.attendance.mapper.ShiftScheduleMapper;
import com.lowcode.attendance.service.ShiftScheduleService;
import com.lowcode.common.util.UserContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShiftScheduleServiceImpl extends ServiceImpl<ShiftScheduleMapper, ShiftSchedule> implements ShiftScheduleService {

    @Autowired
    private ShiftConfigMapper shiftConfigMapper;

    @Override
    public List<ShiftSchedule> getByUserAndDateRange(Long appId, Long userId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectByUserAndDateRange(appId, userId, startDate, endDate);
    }

    @Override
    public List<ShiftSchedule> getByAppAndDateRange(Long appId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectByAppAndDateRange(appId, startDate, endDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShiftSchedule saveOrUpdate(ShiftScheduleDTO dto) {
        Long currentUserId = UserContext.getCurrentUserId();

        LambdaQueryWrapper<ShiftSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShiftSchedule::getAppId, dto.getAppId())
                .eq(ShiftSchedule::getUserId, dto.getUserId())
                .eq(ShiftSchedule::getShiftDate, dto.getShiftDate())
                .last("LIMIT 1");
        ShiftSchedule existing = baseMapper.selectOne(wrapper);

        ShiftSchedule schedule;
        if (existing != null) {
            schedule = existing;
            BeanUtils.copyProperties(dto, schedule, "id", "appId", "userId", "shiftDate");
            schedule.setUpdatedBy(currentUserId);
            baseMapper.updateById(schedule);
        } else {
            schedule = new ShiftSchedule();
            BeanUtils.copyProperties(dto, schedule);
            schedule.setCreatedBy(currentUserId);
            schedule.setUpdatedBy(currentUserId);

            if (dto.getStartTime() == null || dto.getEndTime() == null || dto.getWorkHours() == null) {
                fillShiftConfigInfo(dto.getAppId(), dto.getShiftType(), schedule);
            }

            baseMapper.insert(schedule);
        }
        return schedule;
    }

    private void fillShiftConfigInfo(Long appId, String shiftType, ShiftSchedule schedule) {
        List<ShiftConfig> configs = shiftConfigMapper.selectByAppId(appId);
        Map<String, ShiftConfig> configMap = configs.stream()
                .collect(Collectors.toMap(ShiftConfig::getShiftCode, c -> c));
        ShiftConfig config = configMap.get(shiftType);
        if (config != null) {
            if (schedule.getStartTime() == null) {
                schedule.setStartTime(config.getStartTime());
            }
            if (schedule.getEndTime() == null) {
                schedule.setEndTime(config.getEndTime());
            }
            if (schedule.getWorkHours() == null || schedule.getWorkHours().compareTo(BigDecimal.ZERO) == 0) {
                schedule.setWorkHours(config.getWorkHours());
            }
            if (schedule.getHourlyWage() == null || schedule.getHourlyWage().compareTo(BigDecimal.ZERO) == 0) {
                schedule.setHourlyWage(config.getHourlyWage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSchedule(BatchScheduleDTO dto) {
        Long currentUserId = UserContext.getCurrentUserId();
        List<ShiftConfig> configs = shiftConfigMapper.selectByAppId(dto.getAppId());
        Map<String, ShiftConfig> configMap = configs.stream()
                .collect(Collectors.toMap(ShiftConfig::getShiftCode, c -> c));
        ShiftConfig config = configMap.get(dto.getShiftType());

        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = dto.getStartDate();
        while (!date.isAfter(dto.getEndDate())) {
            dates.add(date);
            date = date.plusDays(1);
        }

        for (Long userId : dto.getUserIds()) {
            for (LocalDate shiftDate : dates) {
                LambdaQueryWrapper<ShiftSchedule> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ShiftSchedule::getAppId, dto.getAppId())
                        .eq(ShiftSchedule::getUserId, userId)
                        .eq(ShiftSchedule::getShiftDate, shiftDate)
                        .last("LIMIT 1");
                ShiftSchedule existing = baseMapper.selectOne(wrapper);

                if (existing != null) {
                    existing.setShiftType(dto.getShiftType());
                    if (config != null) {
                        existing.setStartTime(config.getStartTime());
                        existing.setEndTime(config.getEndTime());
                        existing.setWorkHours(config.getWorkHours());
                        existing.setHourlyWage(config.getHourlyWage());
                    }
                    existing.setUpdatedBy(currentUserId);
                    baseMapper.updateById(existing);
                } else {
                    ShiftSchedule schedule = new ShiftSchedule();
                    schedule.setAppId(dto.getAppId());
                    schedule.setUserId(userId);
                    schedule.setShiftDate(shiftDate);
                    schedule.setShiftType(dto.getShiftType());
                    if (config != null) {
                        schedule.setStartTime(config.getStartTime());
                        schedule.setEndTime(config.getEndTime());
                        schedule.setWorkHours(config.getWorkHours());
                        schedule.setHourlyWage(config.getHourlyWage());
                    }
                    schedule.setCreatedBy(currentUserId);
                    schedule.setUpdatedBy(currentUserId);
                    baseMapper.insert(schedule);
                }
            }
        }
    }

    @Override
    public void deleteByUserAndDate(Long appId, Long userId, LocalDate shiftDate) {
        LambdaQueryWrapper<ShiftSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShiftSchedule::getAppId, appId)
                .eq(ShiftSchedule::getUserId, userId)
                .eq(ShiftSchedule::getShiftDate, shiftDate);
        baseMapper.delete(wrapper);
    }
}
