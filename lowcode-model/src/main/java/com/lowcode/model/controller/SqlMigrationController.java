package com.lowcode.model.controller;

import com.lowcode.common.result.Result;
import com.lowcode.model.entity.SqlMigration;
import com.lowcode.model.service.SqlMigrationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "SQL迁移管理")
@RestController
@RequestMapping("/api/migration")
public class SqlMigrationController {

    @Autowired
    private SqlMigrationService sqlMigrationService;

    @ApiOperation("获取迁移列表")
    @GetMapping("/list")
    public Result<List<SqlMigration>> list(@RequestParam Long appId,
                                           @RequestParam(required = false) Long dataSourceId) {
        return Result.success(sqlMigrationService.getMigrations(appId, dataSourceId));
    }

    @ApiOperation("获取迁移详情")
    @GetMapping("/{id}")
    public Result<SqlMigration> getById(@PathVariable Long id) {
        return Result.success(sqlMigrationService.getMigrationDetail(id));
    }

    @ApiOperation("执行迁移")
    @PostMapping("/{id}/execute")
    public Result<SqlMigration> execute(@PathVariable Long id) {
        return Result.success(sqlMigrationService.executeMigration(id));
    }
}
