package com.lowcode.model.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.service.DataSourceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "数据源管理")
@RestController
@RequestMapping("/dataSource")
public class DataSourceController {

    @Autowired
    private DataSourceService dataSourceService;

    @ApiOperation("测试连接")
    @PostMapping("/testConnection")
    public Result<Boolean> testConnection(@RequestBody DataSource dataSource) {
        return Result.success(dataSourceService.testConnection(dataSource));
    }

    @ApiOperation("保存数据源")
    @PostMapping
    public Result<DataSource> save(@RequestBody DataSource dataSource) {
        return Result.success(dataSourceService.saveDataSource(dataSource));
    }

    @ApiOperation("更新数据源")
    @PutMapping
    public Result<DataSource> update(@RequestBody DataSource dataSource) {
        return Result.success(dataSourceService.updateDataSource(dataSource));
    }

    @ApiOperation("删除数据源")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dataSourceService.deleteDataSource(id);
        return Result.success();
    }

    @ApiOperation("获取数据源详情")
    @GetMapping("/{id}")
    public Result<DataSource> getById(@PathVariable Long id) {
        DataSource dataSource = dataSourceService.getById(id);
        if (dataSource != null) {
            dataSource.setPassword(null);
        }
        return Result.success(dataSource);
    }

    @ApiOperation("获取数据源列表")
    @GetMapping("/list/{appId}")
    public Result<List<DataSource>> list(@PathVariable Long appId) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSource::getAppId, appId);
        wrapper.orderByDesc(DataSource::getCreatedTime);
        List<DataSource> list = dataSourceService.list(wrapper);
        for (DataSource ds : list) {
            ds.setPassword(null);
        }
        return Result.success(list);
    }

    @ApiOperation("分页查询数据源")
    @GetMapping("/page")
    public Result<Page<DataSource>> page(@RequestParam(defaultValue = "1") Integer current,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam Long appId) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSource::getAppId, appId);
        wrapper.orderByDesc(DataSource::getCreatedTime);
        Page<DataSource> page = dataSourceService.page(new Page<>(current, size), wrapper);
        for (DataSource ds : page.getRecords()) {
            ds.setPassword(null);
        }
        return Result.success(page);
    }

    @ApiOperation("获取数据源表列表")
    @GetMapping("/{id}/tables")
    public Result<List<String>> getTableNames(@PathVariable Long id) {
        return Result.success(dataSourceService.getTableNames(id));
    }
}
