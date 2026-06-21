package com.lowcode.page.controller.dashboard;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.page.entity.dashboard.Dashboard;
import com.lowcode.page.service.dashboard.DashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "大屏管理")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @ApiOperation("保存大屏")
    @PostMapping
    public Result<Dashboard> save(@RequestBody Dashboard dashboard) {
        return Result.success(dashboardService.saveDashboard(dashboard));
    }

    @ApiOperation("更新大屏")
    @PutMapping
    public Result<Dashboard> update(@RequestBody Dashboard dashboard) {
        return Result.success(dashboardService.updateDashboard(dashboard));
    }

    @ApiOperation("删除大屏")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dashboardService.deleteDashboard(id);
        return Result.success();
    }

    @ApiOperation("获取大屏详情")
    @GetMapping("/{id}")
    public Result<Dashboard> getById(@PathVariable Long id) {
        return Result.success(dashboardService.getDashboardDetail(id));
    }

    @ApiOperation("获取大屏列表")
    @GetMapping("/list/{appId}")
    public Result<List<Dashboard>> list(@PathVariable Long appId) {
        return Result.success(dashboardService.getDashboardList(appId));
    }

    @ApiOperation("分页查询大屏")
    @GetMapping("/page")
    public Result<Page<Dashboard>> page(@RequestParam(defaultValue = "1") Integer current,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam Long appId,
                                        @RequestParam(required = false) String keyword) {
        return Result.success(dashboardService.getDashboardPage(current, size, appId, keyword));
    }

    @ApiOperation("发布大屏")
    @PostMapping("/{id}/publish")
    public Result<Dashboard> publish(@PathVariable Long id) {
        return Result.success(dashboardService.publishDashboard(id));
    }

    @ApiOperation("复制大屏")
    @PostMapping("/copy/{id}")
    public Result<Dashboard> copy(@PathVariable Long id,
                                  @RequestParam(required = false) String newName,
                                  @RequestParam(required = false) String newCode) {
        return Result.success(dashboardService.copyDashboard(id, newName, newCode));
    }

    @ApiOperation("生成分享链接")
    @GetMapping("/{id}/share")
    public Result<String> generateShareLink(@PathVariable Long id) {
        return Result.success(dashboardService.generateShareLink(id));
    }
}
