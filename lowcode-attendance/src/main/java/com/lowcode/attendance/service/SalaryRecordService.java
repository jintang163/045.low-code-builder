package com.lowcode.attendance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lowcode.attendance.dto.SalaryGenerateDTO;
import com.lowcode.attendance.entity.SalaryRecord;

import java.io.ByteArrayOutputStream;
import java.util.List;

public interface SalaryRecordService extends IService<SalaryRecord> {
    List<SalaryRecord> generateSalary(SalaryGenerateDTO dto);
    List<SalaryRecord> getByMonth(Long appId, String salaryMonth);
    List<SalaryRecord> getByUser(Long appId, Long userId);
    void markPaid(Long appId, String salaryMonth);
    byte[] exportSalaryExcel(Long appId, String salaryMonth);
}
