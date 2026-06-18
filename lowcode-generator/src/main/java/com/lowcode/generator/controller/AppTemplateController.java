package com.lowcode.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.generator.entity.AppInfo;
import com.lowcode.generator.entity.AppTemplate;
import com.lowcode.generator.entity.TemplateData;
import com.lowcode.generator.service.AppTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Api(tags = "应用模板市场")
@RestController
@RequestMapping("/api/template")
public class AppTemplateController {

    @Autowired
    private AppTemplateService appTemplateService;

    @ApiOperation("分页查询模板")
    @GetMapping("/page")
    public Result<Page<AppTemplate>> getTemplatePage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer templateType) {
        return Result.success(appTemplateService.getTemplatePage(current, size, keyword, category, templateType));
    }

    @ApiOperation("获取模板列表")
    @GetMapping("/list")
    public Result<List<AppTemplate>> getTemplateList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer limit) {
        return Result.success(appTemplateService.getTemplateList(category, limit));
    }

    @ApiOperation("获取模板详情")
    @GetMapping("/{id}")
    public Result<AppTemplate> getTemplateDetail(@PathVariable Long id) {
        return Result.success(appTemplateService.getTemplateDetail(id));
    }

    @ApiOperation("获取模板数据（模型/页面/逻辑等）")
    @GetMapping("/{id}/data")
    public Result<TemplateData> getTemplateData(@PathVariable Long id) {
        return Result.success(appTemplateService.getTemplateData(id));
    }

    @ApiOperation("发布应用为模板")
    @PostMapping("/publish")
    public Result<AppTemplate> publishAsTemplate(
            @RequestParam Long appId,
            @RequestParam String templateName,
            @RequestParam String templateCode,
            @RequestParam(required = false) String templateDesc,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "1.0.0") String version,
            @RequestParam(defaultValue = "1") Integer templateType) {
        return Result.success(appTemplateService.publishAsTemplate(
                appId, templateName, templateCode, templateDesc, icon, category, tags, version, templateType));
    }

    @ApiOperation("一键安装模板")
    @PostMapping("/{id}/install")
    public Result<AppInfo> installTemplate(
            @PathVariable Long id,
            @RequestParam String appName,
            @RequestParam String appCode,
            @RequestParam(required = false) Long userId) {
        return Result.success(appTemplateService.installTemplate(id, appName, appCode, userId));
    }

    @ApiOperation("导出模板")
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportTemplate(@PathVariable Long id) {
        AppTemplate template = appTemplateService.getTemplateDetail(id);
        byte[] data = appTemplateService.exportTemplate(id);

        String fileName = URLEncoder.encode(template.getTemplateCode() + "-template.json", StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    @ApiOperation("导入模板")
    @PostMapping("/import")
    public Result<AppTemplate> importTemplate(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.success(appTemplateService.importTemplate(file.getBytes()));
    }

    @ApiOperation("更新模板")
    @PutMapping
    public Result<AppTemplate> updateTemplate(@RequestBody AppTemplate template) {
        return Result.success(appTemplateService.updateTemplate(template));
    }

    @ApiOperation("删除模板")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        appTemplateService.deleteTemplate(id);
        return Result.success();
    }

    @ApiOperation("获取模板统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getTemplateStats() {
        return Result.success(appTemplateService.getTemplateStats());
    }

    @ApiOperation("初始化内置模板")
    @PostMapping("/init-builtin")
    public Result<String> initBuiltinTemplates() {
        appTemplateService.initBuiltinTemplates();
        return Result.success("内置模板初始化成功");
    }

    @ApiOperation("获取模板分类列表")
    @GetMapping("/categories")
    public Result<List<Map<String, String>>> getCategories() {
        List<Map<String, String>> categories = List.of(
                Map.of("key", "", "name", "全部"),
                Map.of("key", "oa", "name", "OA办公"),
                Map.of("key", "crm", "name", "CRM客户"),
                Map.of("key", "inventory", "name", "进销存"),
                Map.of("key", "business", "name", "业务系统"),
                Map.of("key", "system", "name", "系统工具"),
                Map.of("key", "other", "name", "其他")
        );
        return Result.success(categories);
    }
}
