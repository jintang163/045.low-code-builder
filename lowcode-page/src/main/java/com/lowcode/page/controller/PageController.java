package com.lowcode.page.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.page.service.GrayReleaseService;
import com.lowcode.page.service.PageService;
import com.lowcode.page.vo.GrayReleaseResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "页面管理")
@RestController
@RequestMapping("/api/page")
public class PageController {

    @Autowired
    private PageService pageService;

    @Autowired
    private GrayReleaseService grayReleaseService;

    @ApiOperation("保存页面")
    @PostMapping
    public Result<com.lowcode.page.entity.Page> save(@RequestBody com.lowcode.page.entity.Page page) {
        return Result.success(pageService.savePage(page));
    }

    @ApiOperation("更新页面")
    @PutMapping
    public Result<com.lowcode.page.entity.Page> update(@RequestBody com.lowcode.page.entity.Page page) {
        return Result.success(pageService.updatePage(page));
    }

    @ApiOperation("删除页面")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pageService.deletePage(id);
        return Result.success();
    }

    @ApiOperation("获取页面详情")
    @GetMapping("/{id}")
    public Result<com.lowcode.page.entity.Page> getById(@PathVariable Long id) {
        return Result.success(pageService.getPageDetail(id));
    }

    @ApiOperation("获取页面列表")
    @GetMapping("/list/{appId}")
    public Result<List<com.lowcode.page.entity.Page>> list(@PathVariable Long appId) {
        return Result.success(pageService.getPageList(appId));
    }

    @ApiOperation("分页查询页面")
    @GetMapping("/page")
    public Result<Page<com.lowcode.page.entity.Page>> page(@RequestParam(defaultValue = "1") Integer current,
                                                           @RequestParam(defaultValue = "10") Integer size,
                                                           @RequestParam Long appId) {
        LambdaQueryWrapper<com.lowcode.page.entity.Page> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.lowcode.page.entity.Page::getAppId, appId);
        wrapper.orderByDesc(com.lowcode.page.entity.Page::getCreatedTime);
        Page<com.lowcode.page.entity.Page> page = pageService.page(new Page<>(current, size), wrapper);
        return Result.success(page);
    }

    @ApiOperation("发布页面")
    @PostMapping("/{id}/publish")
    public Result<com.lowcode.page.entity.Page> publish(@PathVariable Long id) {
        return Result.success(pageService.publishPage(id));
    }

    @ApiOperation("生成页面代码")
    @GetMapping("/{id}/generateCode")
    public Result<String> generateCode(@PathVariable Long id) {
        return Result.success(pageService.generatePageCode(id));
    }

    @ApiOperation("获取页面预览数据")
    @GetMapping("/{id}/preview")
    public Result<Map<String, Object>> getPreviewData(@PathVariable Long id) {
        return Result.success(pageService.getPagePreviewData(id));
    }

    @ApiOperation("获取灰度版本页面预览数据")
    @GetMapping("/{id}/preview/gray")
    public Result<Map<String, Object>> getGrayPreviewData(@PathVariable Long id,
                                                          @RequestParam(required = false) Long userId,
                                                          @RequestParam(required = false) String userGroup) {
        log.info("获取灰度版本页面预览数据，pageId: {}, userId: {}, userGroup: {}", id, userId, userGroup);

        GrayReleaseResultVO grayResult = grayReleaseService.checkGrayRelease(id, "PAGE", userId, userGroup);

        Map<String, Object> result;
        if (Boolean.TRUE.equals(grayResult.getShouldUseNewVersion())) {
            log.info("命中灰度发布，使用新版本快照，snapshotId: {}, version: {}", 
                    grayResult.getActiveSnapshotId(), grayResult.getActiveVersion());
            result = pageService.getPagePreviewData(id, grayResult.getActiveSnapshotId());
        } else {
            log.info("未命中灰度发布，使用当前版本");
            result = pageService.getPagePreviewData(id);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("grayInfo", grayResult);

        return Result.success(response);
    }

    @ApiOperation("复制页面")
    @PostMapping("/copy/{id}")
    public Result<com.lowcode.page.entity.Page> copyPage(
            @PathVariable Long id,
            @RequestParam(required = false) String newPageName,
            @RequestParam(required = false) String newPageCode,
            @RequestParam(defaultValue = "full") String copyMode) {
        return Result.success(pageService.copyPage(id, newPageName, newPageCode, copyMode));
    }
}
