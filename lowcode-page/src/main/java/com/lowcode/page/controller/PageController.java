package com.lowcode.page.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.page.service.PageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "页面管理")
@RestController
@RequestMapping("/page")
public class PageController {

    @Autowired
    private PageService pageService;

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
}
