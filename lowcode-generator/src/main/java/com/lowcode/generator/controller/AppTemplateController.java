package com.lowcode.generator.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.common.util.UserContext;
import com.lowcode.generator.entity.AppTemplate;
import com.lowcode.generator.entity.TemplateData;
import com.lowcode.generator.entity.TemplateVersion;
import com.lowcode.generator.service.AppTemplateService;
import com.lowcode.generator.service.TemplateUpgradeService;
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

    @Autowired
    private TemplateUpgradeService upgradeService;

    @ApiOperation("分页查询模板")
    @GetMapping("/page")
    public Result<Page<AppTemplate>> getTemplatePage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer templateType) {
        return Result.success(appTemplateService.getTemplatePage(current, size, category, keyword, templateType));
    }

    @ApiOperation("获取模板列表")
    @GetMapping("/list")
    public Result<List<AppTemplate>> getTemplateList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer templateType) {
        return Result.success(appTemplateService.getTemplateList(category, keyword, templateType));
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
    public Result<Map<String, Object>> publishAsTemplate(@RequestBody Map<String, Object> params) {
        return Result.success(appTemplateService.publishAsTemplate(params));
    }

    @ApiOperation("一键安装模板")
    @PostMapping("/{id}/install")
    public Result<Map<String, Object>> installTemplate(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(appTemplateService.installTemplate(id, userId));
    }

    @ApiOperation("导出模板")
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportTemplate(@PathVariable Long id) {
        AppTemplate template = appTemplateService.getTemplateDetail(id);
        String jsonStr = appTemplateService.exportTemplate(id);
        byte[] data = jsonStr.getBytes(StandardCharsets.UTF_8);

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
    public Result<Map<String, Object>> importTemplate(@RequestParam("file") MultipartFile file) throws Exception {
        String templateJson = new String(file.getBytes(), StandardCharsets.UTF_8);
        Long userId = UserContext.getCurrentUserId();
        return Result.success(appTemplateService.importTemplate(templateJson, userId));
    }

    @ApiOperation("更新模板")
    @PutMapping
    public Result<Boolean> updateTemplate(@RequestBody AppTemplate template) {
        return Result.success(appTemplateService.updateTemplate(template));
    }

    @ApiOperation("删除模板")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteTemplate(@PathVariable Long id) {
        return Result.success(appTemplateService.deleteTemplate(id));
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
    public Result<List<String>> getCategories() {
        return Result.success(appTemplateService.getCategoryList());
    }

    @ApiOperation("模板点赞/收藏")
    @PostMapping("/{id}/star")
    public Result<Boolean> starTemplate(@PathVariable Long id) {
        return Result.success(appTemplateService.starTemplate(id));
    }

    @ApiOperation("获取模板版本历史")
    @GetMapping("/{id}/versions")
    public Result<List<TemplateVersion>> getTemplateVersions(@PathVariable Long id) {
        return Result.success(upgradeService.getTemplateVersions(id));
    }

    @ApiOperation("检查应用是否有模板更新")
    @GetMapping("/app/{appId}/check-update")
    public Result<Map<String, Object>> checkUpdate(@PathVariable Long appId) {
        return Result.success(upgradeService.checkUpdate(appId));
    }

    @ApiOperation("从模板更新应用")
    @PostMapping("/app/{appId}/update")
    public Result<Map<String, Object>> updateAppFromTemplate(
            @PathVariable Long appId,
            @RequestParam(defaultValue = "incremental") String updateMode) {
        return Result.success(upgradeService.updateAppFromTemplate(appId, updateMode));
    }
}
