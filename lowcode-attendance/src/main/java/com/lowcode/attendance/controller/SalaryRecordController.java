package com.lowcode.attendance.controller;

import com.lowcode.attendance.dto.SalaryGenerateDTO;
import com.lowcode.attendance.entity.SalaryRecord;
import com.lowcode.attendance.service.SalaryRecordService;
import com.lowcode.common.result.Result;
import com.lowcode.common.util.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Api(tags = "工资管理")
@RestController
@RequestMapping("/attendance/salary")
public class SalaryRecordController {

    @Autowired
    private SalaryRecordService salaryRecordService;

    @ApiOperation("生成工资")
    @PostMapping("/generate")
    public Result<List<SalaryRecord>> generate(@RequestBody SalaryGenerateDTO dto) {
        List<SalaryRecord> list = salaryRecordService.generateSalary(dto);
        return Result.ok(list);
    }

    @ApiOperation("获取某月工资表")
    @GetMapping("/month")
    public Result<List<SalaryRecord>> getByMonth(
            @RequestParam Long appId,
            @RequestParam String salaryMonth) {
        List<SalaryRecord> list = salaryRecordService.getByMonth(appId, salaryMonth);
        return Result.ok(list);
    }

    @ApiOperation("获取我的工资记录")
    @GetMapping("/my")
    public Result<List<SalaryRecord>> getMySalary(@RequestParam Long appId) {
        Long userId = UserContext.getCurrentUserId();
        List<SalaryRecord> list = salaryRecordService.getByUser(appId, userId);
        return Result.ok(list);
    }

    @ApiOperation("标记为已发薪")
    @PostMapping("/mark-paid")
    public Result<Void> markPaid(
            @RequestParam Long appId,
            @RequestParam String salaryMonth) {
        salaryRecordService.markPaid(appId, salaryMonth);
        return Result.ok();
    }

    @ApiOperation("导出工资表Excel")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam Long appId,
            @RequestParam String salaryMonth) {
        byte[] data = salaryRecordService.exportSalaryExcel(appId, salaryMonth);
        String fileName = "工资表_" + salaryMonth + ".xlsx";
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", encodedName);
        headers.setContentLength(data.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}
