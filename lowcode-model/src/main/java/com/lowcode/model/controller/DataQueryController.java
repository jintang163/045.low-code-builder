package com.lowcode.model.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lowcode.common.result.Result;
import com.lowcode.model.service.DataQueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "数据查询服务")
@RestController
@RequestMapping("/api/data")
public class DataQueryController {

    @Autowired
    private DataQueryService dataQueryService;

    @ApiOperation("查询数据列表")
    @PostMapping("/list/{modelId}")
    public Result<List<Map<String, Object>>> queryList(
            @PathVariable Long modelId,
            @RequestBody(required = false) Map<String, Object> conditions,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDir) {
        return Result.success(dataQueryService.queryList(modelId, conditions, orderBy, orderDir));
    }

    @ApiOperation("分页查询数据")
    @PostMapping("/page/{modelId}")
    public Result<IPage<Map<String, Object>>> queryPage(
            @PathVariable Long modelId,
            @RequestBody(required = false) Map<String, Object> conditions,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDir) {
        return Result.success(dataQueryService.queryPage(modelId, conditions, current, size, orderBy, orderDir));
    }

    @ApiOperation("根据ID查询单条数据")
    @GetMapping("/{modelId}/{id}")
    public Result<Map<String, Object>> queryById(
            @PathVariable Long modelId,
            @PathVariable Object id) {
        return Result.success(dataQueryService.queryById(modelId, id));
    }

    @ApiOperation("执行SQL查询")
    @PostMapping("/sql/execute/{dataSourceId}")
    public Result<List<Map<String, Object>>> executeSql(
            @PathVariable Long dataSourceId,
            @RequestBody Map<String, Object> params) {
        String sql = (String) params.get("sql");
        @SuppressWarnings("unchecked")
        List<Object> sqlParams = (List<Object>) params.get("params");
        return Result.success(dataQueryService.executeSql(dataSourceId, sql, sqlParams));
    }

    @ApiOperation("分页执行SQL查询")
    @PostMapping("/sql/page/{dataSourceId}")
    public Result<IPage<Map<String, Object>>> executeSqlPage(
            @PathVariable Long dataSourceId,
            @RequestBody Map<String, Object> params,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        String sql = (String) params.get("sql");
        @SuppressWarnings("unchecked")
        List<Object> sqlParams = (List<Object>) params.get("params");
        return Result.success(dataQueryService.executeSqlPage(dataSourceId, sql, sqlParams, current, size));
    }

    @ApiOperation("测试SQL查询")
    @PostMapping("/sql/test/{dataSourceId}")
    public Result<List<Map<String, Object>>> testSql(
            @PathVariable Long dataSourceId,
            @RequestBody Map<String, Object> params) {
        String sql = (String) params.get("sql");
        @SuppressWarnings("unchecked")
        List<Object> sqlParams = (List<Object>) params.get("params");
        return Result.success(dataQueryService.testSql(dataSourceId, sql, sqlParams));
    }
}
