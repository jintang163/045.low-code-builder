package com.lowcode.model.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.model.dto.RollbackCheckResult;
import com.lowcode.model.dto.VersionCompareResult;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.DataModelVersion;
import com.lowcode.model.service.DataModelVersionService;
import com.lowcode.model.service.VersionCompareService;
import com.lowcode.model.service.VersionRollbackService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "数据模型版本管理")
@RestController
@RequestMapping("/api/model/version")
public class DataModelVersionController {

    @Autowired
    private DataModelVersionService versionService;

    @Autowired
    private VersionCompareService compareService;

    @Autowired
    private VersionRollbackService rollbackService;

    @ApiOperation("获取模型版本列表")
    @GetMapping("/list/{modelId}")
    public Result<List<DataModelVersion>> listVersions(@PathVariable Long modelId) {
        return Result.success(versionService.listVersions(modelId));
    }

    @ApiOperation("分页查询版本列表")
    @GetMapping("/page")
    public Result<Page<DataModelVersion>> pageVersions(@RequestParam Long modelId,
                                                        @RequestParam(defaultValue = "1") Integer current,
                                                        @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(versionService.pageVersions(modelId, current, size));
    }

    @ApiOperation("获取版本详情")
    @GetMapping("/{versionId}")
    public Result<DataModelVersion> getVersion(@PathVariable Long versionId) {
        return Result.success(versionService.getVersion(versionId));
    }

    @ApiOperation("获取最新版本")
    @GetMapping("/latest/{modelId}")
    public Result<DataModelVersion> getLatestVersion(@PathVariable Long modelId) {
        return Result.success(versionService.getLatestVersion(modelId));
    }

    @ApiOperation("手动创建快照")
    @PostMapping("/snapshot/{modelId}")
    public Result<DataModelVersion> createSnapshot(@PathVariable Long modelId,
                                                     @RequestParam(required = false) String description) {
        return Result.success(versionService.createSnapshot(modelId, description, 2));
    }

    @ApiOperation("对比两个版本")
    @GetMapping("/compare")
    public Result<VersionCompareResult> compareVersions(@RequestParam Long modelId,
                                                          @RequestParam String sourceVersion,
                                                          @RequestParam String targetVersion) {
        return Result.success(compareService.compareVersions(modelId, sourceVersion, targetVersion));
    }

    @ApiOperation("对比两个版本(通过版本ID)")
    @GetMapping("/compareById")
    public Result<VersionCompareResult> compareVersionsById(@RequestParam Long sourceVersionId,
                                                              @RequestParam Long targetVersionId) {
        DataModelVersion source = versionService.getVersion(sourceVersionId);
        DataModelVersion target = versionService.getVersion(targetVersionId);
        if (source == null || source.getSnapshot() == null
                || target == null || target.getSnapshot() == null) {
            return Result.fail("版本数据不存在");
        }
        return Result.success(compareService.compareModels(source.getSnapshot(), target.getSnapshot()));
    }

    @ApiOperation("回滚前校验")
    @GetMapping("/checkRollback")
    public Result<RollbackCheckResult> checkRollback(@RequestParam Long modelId,
                                                      @RequestParam Long targetVersionId) {
        return Result.success(rollbackService.checkRollback(modelId, targetVersionId));
    }

    @ApiOperation("执行版本回滚")
    @PostMapping("/rollback")
    public Result<DataModel> rollback(@RequestParam Long modelId,
                                       @RequestParam Long targetVersionId,
                                       @RequestParam(required = false) String reason) {
        return Result.success(rollbackService.rollback(modelId, targetVersionId, reason));
    }
}
