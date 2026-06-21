package com.lowcode.model.controller;

import com.lowcode.common.result.Result;
import com.lowcode.model.service.ExcelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Api(tags = "Excel导入导出")
@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @ApiOperation("导出导入模板")
    @GetMapping("/template/{modelId}")
    public void exportTemplate(@PathVariable Long modelId, HttpServletResponse response) throws IOException {
        excelService.exportTemplate(modelId, response);
    }

    @ApiOperation("导出数据")
    @PostMapping("/export/{modelId}")
    public void exportData(
            @PathVariable Long modelId,
            @RequestBody(required = false) Map<String, Object> conditions,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDir,
            HttpServletResponse response) throws IOException {
        excelService.exportData(modelId, conditions, orderBy, orderDir, response);
    }

    @ApiOperation("导入数据预览")
    @PostMapping("/import/{modelId}")
    public Result<Map<String, Object>> importData(
            @PathVariable Long modelId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "0") Integer sheetIndex,
            @RequestParam(required = false, defaultValue = "1") Integer startRow) throws IOException {
        return Result.success(excelService.importData(modelId, file, sheetIndex, startRow));
    }
}
