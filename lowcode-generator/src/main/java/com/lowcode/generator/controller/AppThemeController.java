package com.lowcode.generator.controller;

import com.lowcode.common.result.Result;
import com.lowcode.generator.entity.AppTheme;
import com.lowcode.generator.service.AppThemeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "应用主题管理")
@RestController
@RequestMapping("/api/theme")
public class AppThemeController {

    @Autowired
    private AppThemeService appThemeService;

    @ApiOperation("获取应用主题列表")
    @GetMapping("/list/{appId}")
    public Result<List<AppTheme>> getThemeList(@PathVariable Long appId) {
        return Result.success(appThemeService.getThemeList(appId));
    }

    @ApiOperation("获取默认主题")
    @GetMapping("/default/{appId}")
    public Result<AppTheme> getDefaultTheme(@PathVariable Long appId) {
        return Result.success(appThemeService.getDefaultTheme(appId));
    }

    @ApiOperation("获取主题详情")
    @GetMapping("/{id}")
    public Result<AppTheme> getThemeById(@PathVariable Long id) {
        return Result.success(appThemeService.getThemeById(id));
    }

    @ApiOperation("创建主题")
    @PostMapping
    public Result<AppTheme> createTheme(@RequestBody AppTheme theme) {
        return Result.success(appThemeService.saveTheme(theme));
    }

    @ApiOperation("更新主题")
    @PutMapping
    public Result<AppTheme> updateTheme(@RequestBody AppTheme theme) {
        return Result.success(appThemeService.saveTheme(theme));
    }

    @ApiOperation("删除主题")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTheme(@PathVariable Long id) {
        appThemeService.deleteTheme(id);
        return Result.success();
    }

    @ApiOperation("设置默认主题")
    @PostMapping("/default")
    public Result<AppTheme> setDefaultTheme(@RequestParam Long appId, @RequestParam Long themeId) {
        return Result.success(appThemeService.setDefaultTheme(appId, themeId));
    }

    @ApiOperation("复制主题")
    @PostMapping("/duplicate/{id}")
    public Result<AppTheme> duplicateTheme(@PathVariable Long id, @RequestParam(required = false) String newName) {
        return Result.success(appThemeService.duplicateTheme(id, newName));
    }

    @ApiOperation("生成主题CSS")
    @GetMapping("/{id}/css")
    public Result<String> generateThemeCss(@PathVariable Long id) {
        AppTheme theme = appThemeService.getThemeById(id);
        if (theme == null) {
            return Result.error("主题不存在");
        }
        return Result.success(appThemeService.generateThemeCss(theme));
    }

    @ApiOperation("获取应用默认主题CSS")
    @GetMapping("/app/{appId}/css")
    public Result<String> getDefaultThemeCss(@PathVariable Long appId) {
        AppTheme theme = appThemeService.getDefaultTheme(appId);
        if (theme == null) {
            return Result.success("");
        }
        return Result.success(appThemeService.generateThemeCss(theme));
    }
}
