package com.lowcode.model.service;

import com.alibaba.fastjson2.JSON;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.ModelField;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class ExcelService {

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private DataQueryService dataQueryService;

    public void exportTemplate(Long modelId, HttpServletResponse response) throws IOException {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (model == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        List<ModelField> fields = model.getFields();
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("数据模型没有字段");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("数据导入模板");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        for (ModelField field : fields) {
            if (field.getIsPrimary() != null && field.getIsPrimary() == 1) {
                continue;
            }
            Cell cell = headerRow.createCell(colIndex);
            String headerText = field.getFieldComment() != null && !field.getFieldComment().isEmpty()
                    ? field.getFieldComment()
                    : field.getFieldName();
            cell.setCellValue(headerText);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(colIndex, 20 * 256);
            colIndex++;
        }

        Row exampleRow = sheet.createRow(1);
        colIndex = 0;
        for (ModelField field : fields) {
            if (field.getIsPrimary() != null && field.getIsPrimary() == 1) {
                continue;
            }
            Cell cell = exampleRow.createCell(colIndex);
            String exampleValue = getExampleValue(field);
            cell.setCellValue(exampleValue);
            cell.setCellStyle(dataStyle);
            colIndex++;
        }

        String fileName = model.getModelName() + "_导入模板.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()));

        try (OutputStream out = response.getOutputStream()) {
            workbook.write(out);
            out.flush();
        } finally {
            workbook.close();
        }
    }

    public void exportData(Long modelId, Map<String, Object> conditions,
                           String orderBy, String orderDir,
                           HttpServletResponse response) throws IOException {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (model == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        List<Map<String, Object>> dataList = dataQueryService.queryList(modelId, conditions, orderBy, orderDir);
        List<ModelField> fields = model.getFields();

        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("数据模型没有字段");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("数据");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        Map<String, Integer> fieldIndexMap = new LinkedHashMap<>();

        for (ModelField field : fields) {
            Cell cell = headerRow.createCell(colIndex);
            String headerText = field.getFieldComment() != null && !field.getFieldComment().isEmpty()
                    ? field.getFieldComment()
                    : field.getFieldName();
            cell.setCellValue(headerText);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(colIndex, 20 * 256);
            fieldIndexMap.put(field.getFieldName(), colIndex);
            fieldIndexMap.put(field.getColumnName(), colIndex);
            colIndex++;
        }

        if (dataList != null && !dataList.isEmpty()) {
            int rowIndex = 1;
            for (Map<String, Object> data : dataList) {
                Row row = sheet.createRow(rowIndex);
                for (ModelField field : fields) {
                    Integer idx = fieldIndexMap.get(field.getFieldName());
                    if (idx == null) {
                        idx = fieldIndexMap.get(field.getColumnName());
                    }
                    if (idx != null) {
                        Cell cell = row.createCell(idx);
                        Object value = data.get(field.getFieldName());
                        if (value == null) {
                            value = data.get(field.getColumnName());
                        }
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                        cell.setCellStyle(dataStyle);
                    }
                }
                rowIndex++;
            }
        }

        String fileName = model.getModelName() + "_数据导出.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()));

        try (OutputStream out = response.getOutputStream()) {
            workbook.write(out);
            out.flush();
        } finally {
            workbook.close();
        }
    }

    public Map<String, Object> importData(Long modelId, MultipartFile file,
                                           Integer sheetIndex, Integer startRow) throws IOException {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (model == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的文件");
        }

        if (sheetIndex == null) {
            sheetIndex = 0;
        }
        if (startRow == null) {
            startRow = 1;
        }

        List<ModelField> fields = model.getFields();
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("数据模型没有字段");
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        List<String> errorMessages = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (sheet == null) {
                throw new BusinessException("工作表不存在");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException("Excel文件没有表头");
            }

            Map<Integer, ModelField> columnFieldMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String headerValue = getCellStringValue(cell).trim();
                    for (ModelField field : fields) {
                        if (headerValue.equals(field.getFieldComment())
                                || headerValue.equals(field.getFieldName())
                                || headerValue.equals(field.getColumnName())) {
                            columnFieldMap.put(i, field);
                            break;
                        }
                    }
                }
            }

            for (int rowNum = startRow; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }

                try {
                    Map<String, Object> data = new HashMap<>();
                    boolean hasData = false;

                    for (Map.Entry<Integer, ModelField> entry : columnFieldMap.entrySet()) {
                        Cell cell = row.getCell(entry.getKey());
                        ModelField field = entry.getValue();
                        String value = cell != null ? getCellStringValue(cell) : null;

                        if (value != null && !value.trim().isEmpty()) {
                            hasData = true;
                            Object typedValue = convertValue(value, field);
                            data.put(field.getFieldName(), typedValue);
                        }
                    }

                    if (hasData) {
                        dataList.add(data);
                        successCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    errorMessages.add("第" + (rowNum + 1) + "行: " + e.getMessage());
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("totalCount", successCount + failCount);
        result.put("dataList", dataList);
        if (!errorMessages.isEmpty()) {
            result.put("errors", errorMessages);
        }

        return result;
    }

    private String getExampleValue(ModelField field) {
        String fieldType = field.getFieldType();
        if ("STRING".equals(fieldType)) {
            return "示例文本";
        } else if ("NUMBER".equals(fieldType)) {
            return "100";
        } else if ("DATE".equals(fieldType) || "DATETIME".equals(fieldType)) {
            return "2024-01-01";
        } else if ("ENUM".equals(fieldType)) {
            if (field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
                try {
                    List<Map> enumValues = JSON.parseArray(field.getEnumValues(), Map.class);
                    if (!enumValues.isEmpty()) {
                        return enumValues.get(0).get("label") != null
                                ? enumValues.get(0).get("label").toString()
                                : "枚举值";
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            return "枚举值";
        }
        return "示例值";
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                        return String.valueOf((long) numericValue);
                    }
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    private Object convertValue(String value, ModelField field) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String fieldType = field.getFieldType();
        if ("NUMBER".equals(fieldType)) {
            try {
                if (field.getScale() != null && field.getScale() > 0) {
                    return Double.parseDouble(value.trim());
                } else {
                    return Long.parseLong(value.trim());
                }
            } catch (NumberFormatException e) {
                throw new BusinessException("字段[" + field.getFieldName() + "]值格式错误: " + value);
            }
        } else if ("DATE".equals(fieldType) || "DATETIME".equals(fieldType)) {
            return value.trim();
        }

        return value.trim();
    }

    public byte[] exportDataToBytes(Long modelId, List<Map<String, Object>> dataList) throws IOException {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (model == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        List<ModelField> fields = model.getFields();
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("数据模型没有字段");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("数据");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        Map<String, Integer> fieldIndexMap = new LinkedHashMap<>();

        for (ModelField field : fields) {
            Cell cell = headerRow.createCell(colIndex);
            String headerText = field.getFieldComment() != null && !field.getFieldComment().isEmpty()
                    ? field.getFieldComment()
                    : field.getFieldName();
            cell.setCellValue(headerText);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(colIndex, 20 * 256);
            fieldIndexMap.put(field.getFieldName(), colIndex);
            fieldIndexMap.put(field.getColumnName(), colIndex);
            colIndex++;
        }

        if (dataList != null && !dataList.isEmpty()) {
            int rowIndex = 1;
            for (Map<String, Object> data : dataList) {
                Row row = sheet.createRow(rowIndex);
                for (ModelField field : fields) {
                    Integer idx = fieldIndexMap.get(field.getFieldName());
                    if (idx == null) {
                        idx = fieldIndexMap.get(field.getColumnName());
                    }
                    if (idx != null) {
                        Cell cell = row.createCell(idx);
                        Object value = data.get(field.getFieldName());
                        if (value == null) {
                            value = data.get(field.getColumnName());
                        }
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
                rowIndex++;
            }
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            workbook.close();
            return out.toByteArray();
        }
    }
}
