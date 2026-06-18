package com.lowcode.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.generator.entity.AppGenerateConfig;
import com.lowcode.generator.entity.AppInfo;
import com.lowcode.generator.entity.GeneratedApp;
import com.lowcode.generator.service.AppGeneratorService;
import com.lowcode.generator.service.AppInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.util.List;

@Api(tags = "应用管理与生成")
@RestController
@RequestMapping("/api/app")
public class AppGeneratorController {

    @Autowired
    private AppGeneratorService appGeneratorService;

    @Autowired
    private AppInfoService appInfoService;

    @ApiOperation("获取应用列表")
    @GetMapping("/list")
    public Result<List<AppInfo>> getAppList() {
        return Result.success(appInfoService.getAppList());
    }

    @ApiOperation("分页查询应用")
    @GetMapping("/page")
    public Result<Page<AppInfo>> getAppPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(appInfoService.getAppPage(current, size, keyword));
    }

    @ApiOperation("获取应用详情")
    @GetMapping("/{id}")
    public Result<AppInfo> getAppById(@PathVariable Long id) {
        return Result.success(appInfoService.getAppDetail(id));
    }

    @ApiOperation("创建应用")
    @PostMapping
    public Result<AppInfo> createApp(@RequestBody AppInfo app) {
        return Result.success(appInfoService.saveApp(app));
    }

    @ApiOperation("更新应用")
    @PutMapping
    public Result<AppInfo> updateApp(@RequestBody AppInfo app) {
        return Result.success(appInfoService.updateApp(app));
    }

    @ApiOperation("删除应用")
    @DeleteMapping("/{id}")
    public Result<Void> deleteApp(@PathVariable Long id) {
        appInfoService.deleteApp(id);
        return Result.success();
    }

    @ApiOperation("发布应用")
    @PostMapping("/{id}/publish")
    public Result<AppInfo> publishApp(@PathVariable Long id) {
        return Result.success(appInfoService.publishApp(id));
    }

    @ApiOperation("停止应用")
    @PostMapping("/{id}/stop")
    public Result<AppInfo> stopApp(@PathVariable Long id) {
        return Result.success(appInfoService.stopApp(id));
    }

    @ApiOperation("生成应用代码包")
    @PostMapping("/generate")
    public Result<GeneratedApp> generateApp(@RequestBody AppGenerateConfig config) throws Exception {
        return Result.success(appGeneratorService.generateApp(config));
    }

    @ApiOperation("下载应用代码包")
    @GetMapping("/download/{appCode}")
    public ResponseEntity<byte[]> downloadApp(@PathVariable String appCode) throws Exception {
        byte[] data = appGeneratorService.downloadApp(appCode);
        String fileName = URLEncoder.encode(appCode + ".zip", "UTF-8");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    @ApiOperation("预览应用生成配置")
    @PostMapping("/preview")
    public Result<AppGenerateConfig> previewConfig(@RequestBody AppGenerateConfig config) {
        return Result.success(config);
    }
}
