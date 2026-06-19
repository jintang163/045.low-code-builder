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
}
