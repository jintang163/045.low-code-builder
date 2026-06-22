package com.lowcode.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.attendance.dto.SalaryGenerateDTO;
import com.lowcode.attendance.entity.*;
import com.lowcode.attendance.mapper.*;
import com.lowcode.attendance.service.SalaryRecordService;
import com.lowcode.common.util.UserContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalaryRecordServiceImpl extends ServiceImpl<SalaryRecordMapper, SalaryRecord> implements SalaryRecordService {

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private LeaveRequestMapper leaveRequestMapper;

    @Autowired
    private ShiftScheduleMapper shiftScheduleMapper;

    @Autowired
    private ShiftConfigMapper shiftConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SalaryRecord> generateSalary(SalaryGenerateDTO dto) {
        Long currentUserId = UserContext.getCurrentUserId();
        YearMonth yearMonth = YearMonth.parse(dto.getSalaryMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<ShiftSchedule> allSchedules = shiftScheduleMapper.selectByAppAndDateRange(dto.getAppId(), startDate, endDate);
        List<AttendanceRecord> allRecords = attendanceRecordMapper.selectByAppAndDateRange(dto.getAppId(), startDate, endDate);
        List<LeaveRequest> allLeaves = leaveRequestMapper.selectOverlappingLeaves(dto.getAppId(),
                0L, startDate, endDate);

        Map<Long, List<ShiftSchedule>> scheduleMap = allSchedules.stream()
                .filter(s -> !"REST".equals(s.getShiftType()))
                .collect(Collectors.groupingBy(ShiftSchedule::getUserId));

        Map<Long, List<AttendanceRecord>> recordMap = allRecords.stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getUserId));

        Map<Long, List<LeaveRequest>> leaveMap = new HashMap<>();
        for (LeaveRequest leave : allLeaves) {
            if (!"APPROVED".equals(leave.getStatus())) continue;
            leaveMap.computeIfAbsent(leave.getUserId(), k -> new ArrayList<>()).add(leave);
        }

        Set<Long> allUserIds = new HashSet<>();
        allUserIds.addAll(scheduleMap.keySet());
        allUserIds.addAll(recordMap.keySet());

        List<ShiftConfig> shiftConfigs = shiftConfigMapper.selectByAppId(dto.getAppId());
        BigDecimal defaultHourlyWage = dto.getDefaultHourlyWage() != null ? dto.getDefaultHourlyWage() :
                (shiftConfigs.isEmpty() ? new BigDecimal("20") : shiftConfigs.get(0).getHourlyWage());

        LambdaQueryWrapper<SalaryRecord> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SalaryRecord::getAppId, dto.getAppId())
                .eq(SalaryRecord::getSalaryMonth, dto.getSalaryMonth());
        baseMapper.delete(deleteWrapper);

        List<SalaryRecord> result = new ArrayList<>();

        for (Long userId : allUserIds) {
            SalaryRecord salary = new SalaryRecord();
            salary.setAppId(dto.getAppId());
            salary.setUserId(userId);
            salary.setSalaryMonth(dto.getSalaryMonth());
            salary.setStatus("UNPAID");
            salary.setCreatedBy(currentUserId);
            salary.setUpdatedBy(currentUserId);

            List<ShiftSchedule> userSchedules = scheduleMap.getOrDefault(userId, Collections.emptyList());
            List<AttendanceRecord> userRecords = recordMap.getOrDefault(userId, Collections.emptyList());
            List<LeaveRequest> userLeaves = leaveMap.getOrDefault(userId, Collections.emptyList());

            if (!userRecords.isEmpty()) {
                salary.setUserName(userRecords.get(0).getUserName());
            } else if (!userSchedules.isEmpty()) {
                salary.setUserName(userSchedules.get(0).getUserName());
            }

            BigDecimal totalHours = BigDecimal.ZERO;
            int workDays = 0;
            for (AttendanceRecord rec : userRecords) {
                if (rec.getClockInTime() != null) {
                    workDays++;
                }
                if (rec.getWorkHours() != null) {
                    totalHours = totalHours.add(rec.getWorkHours());
                }
            }

            salary.setTotalWorkDays(workDays);
            salary.setTotalWorkHours(totalHours);

            BigDecimal hourlyWage = defaultHourlyWage;
            if (!userSchedules.isEmpty() && userSchedules.get(0).getHourlyWage() != null
                    && userSchedules.get(0).getHourlyWage().compareTo(BigDecimal.ZERO) > 0) {
                hourlyWage = userSchedules.get(0).getHourlyWage();
            }
            salary.setHourlyWage(hourlyWage);

            BigDecimal baseSalary = hourlyWage.multiply(BigDecimal.valueOf(8 * 22));
            salary.setBaseSalary(baseSalary);

            BigDecimal leaveDays = BigDecimal.ZERO;
            for (LeaveRequest leave : userLeaves) {
                if (leave.getLeaveDays() != null) {
                    leaveDays = leaveDays.add(leave.getLeaveDays());
                }
            }
            salary.setLeaveDays(leaveDays);

            BigDecimal leaveDeduction = leaveDays.multiply(hourlyWage).multiply(BigDecimal.valueOf(8));
            salary.setLeaveDeduction(leaveDeduction);

            BigDecimal lateDeduction = BigDecimal.ZERO;
            BigDecimal earlyDeduction = BigDecimal.ZERO;
            int absentCount = 0;

            for (AttendanceRecord rec : userRecords) {
                if ("LATE".equals(rec.getStatus()) && rec.getLateMinutes() != null) {
                    lateDeduction = lateDeduction.add(BigDecimal.valueOf(rec.getLateMinutes())
                            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                            .multiply(hourlyWage));
                }
                if ("EARLY".equals(rec.getStatus()) && rec.getEarlyMinutes() != null) {
                    earlyDeduction = earlyDeduction.add(BigDecimal.valueOf(rec.getEarlyMinutes())
                            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                            .multiply(hourlyWage));
                }
                if ("ABSENT".equals(rec.getStatus())) {
                    absentCount++;
                }
            }
            salary.setLateDeduction(lateDeduction);
            salary.setEarlyDeduction(earlyDeduction);
            salary.setAbsentDeduction(BigDecimal.valueOf(absentCount).multiply(hourlyWage).multiply(BigDecimal.valueOf(8)));

            BigDecimal deductionTotal = leaveDeduction.add(lateDeduction).add(earlyDeduction)
                    .add(salary.getAbsentDeduction());
            salary.setDeductionTotal(deductionTotal);

            BigDecimal netSalary = baseSalary.add(salary.getOvertimePay() != null ? salary.getOvertimePay() : BigDecimal.ZERO)
                    .add(salary.getBonus() != null ? salary.getBonus() : BigDecimal.ZERO)
                    .add(salary.getSubsidy() != null ? salary.getSubsidy() : BigDecimal.ZERO)
                    .subtract(deductionTotal);
            if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
                netSalary = BigDecimal.ZERO;
            }
            salary.setNetSalary(netSalary);

            baseMapper.insert(salary);
            result.add(salary);
        }

        return result;
    }

    @Override
    public List<SalaryRecord> getByMonth(Long appId, String salaryMonth) {
        return baseMapper.selectByMonth(appId, salaryMonth);
    }

    @Override
    public List<SalaryRecord> getByUser(Long appId, Long userId) {
        return baseMapper.selectByUser(appId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long appId, String salaryMonth) {
        LambdaQueryWrapper<SalaryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalaryRecord::getAppId, appId)
                .eq(SalaryRecord::getSalaryMonth, salaryMonth);
        List<SalaryRecord> records = baseMapper.selectList(wrapper);

        for (SalaryRecord record : records) {
            record.setStatus("PAID");
            record.setPaidTime(java.time.LocalDateTime.now());
            baseMapper.updateById(record);
        }
    }

    @Override
    public byte[] exportSalaryExcel(Long appId, String salaryMonth) {
        List<SalaryRecord> records = getByMonth(appId, salaryMonth);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("工资表-" + salaryMonth);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"员工ID", "员工姓名", "出勤天数", "总工时(小时)", "基本工资", "时薪",
                    "加班费", "请假天数", "请假扣款", "迟到扣款", "早退扣款", "缺勤扣款",
                    "奖金", "补贴", "扣款合计", "实发工资", "状态", "备注"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 15 * 256);
            }

            int rowNum = 1;
            for (SalaryRecord rec : records) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rec.getUserId() != null ? rec.getUserId() : 0);
                row.createCell(1).setCellValue(rec.getUserName() != null ? rec.getUserName() : "");
                row.createCell(2).setCellValue(rec.getTotalWorkDays() != null ? rec.getTotalWorkDays() : 0);
                row.createCell(3).setCellValue(rec.getTotalWorkHours() != null ? rec.getTotalWorkHours().doubleValue() : 0);
                row.createCell(4).setCellValue(rec.getBaseSalary() != null ? rec.getBaseSalary().doubleValue() : 0);
                row.createCell(5).setCellValue(rec.getHourlyWage() != null ? rec.getHourlyWage().doubleValue() : 0);
                row.createCell(6).setCellValue(rec.getOvertimePay() != null ? rec.getOvertimePay().doubleValue() : 0);
                row.createCell(7).setCellValue(rec.getLeaveDays() != null ? rec.getLeaveDays().doubleValue() : 0);
                row.createCell(8).setCellValue(rec.getLeaveDeduction() != null ? rec.getLeaveDeduction().doubleValue() : 0);
                row.createCell(9).setCellValue(rec.getLateDeduction() != null ? rec.getLateDeduction().doubleValue() : 0);
                row.createCell(10).setCellValue(rec.getEarlyDeduction() != null ? rec.getEarlyDeduction().doubleValue() : 0);
                row.createCell(11).setCellValue(rec.getAbsentDeduction() != null ? rec.getAbsentDeduction().doubleValue() : 0);
                row.createCell(12).setCellValue(rec.getBonus() != null ? rec.getBonus().doubleValue() : 0);
                row.createCell(13).setCellValue(rec.getSubsidy() != null ? rec.getSubsidy().doubleValue() : 0);
                row.createCell(14).setCellValue(rec.getDeductionTotal() != null ? rec.getDeductionTotal().doubleValue() : 0);
                row.createCell(15).setCellValue(rec.getNetSalary() != null ? rec.getNetSalary().doubleValue() : 0);
                row.createCell(16).setCellValue("PAID".equals(rec.getStatus()) ? "已发薪" : "未发薪");
                row.createCell(17).setCellValue(rec.getRemark() != null ? rec.getRemark() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }
}
