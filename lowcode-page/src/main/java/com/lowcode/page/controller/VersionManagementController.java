package com.lowcode.page.controller;

import com.lowcode.common.result.Result;
import com.lowcode.page.dto.ReleaseRecordDTO;
import com.lowcode.page.dto.RollbackDTO;
import com.lowcode.page.dto.VersionDiffDTO;
import com.lowcode.page.dto.VersionSnapshotDTO;
import com.lowcode.page.entity.GrayReleaseConfig;
import com.lowcode.page.entity.ReleaseRecord;
import com.lowcode.page.entity.VersionSnapshot;
import com.lowcode.page.service.GrayReleaseService;
import com.lowcode.page.service.ReleaseRecordService;
import com.lowcode.page.service.VersionSnapshotService;
import com.lowcode.page.vo.GrayReleaseResultVO;
import com.lowcode.page.vo.VersionDiffVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "版本管理")
@RestController
@RequestMapping("/api/version")
public class VersionManagementController {

    @Autowired
    private VersionSnapshotService versionSnapshotService;

    @Autowired
    private ReleaseRecordService releaseRecordService;

    @Autowired
    private GrayReleaseService grayReleaseService;

    @ApiOperation("创建版本快照")
    @PostMapping("/snapshot")
    public Result<VersionSnapshot> createSnapshot(@RequestBody VersionSnapshotDTO dto) {
        log.info("创建版本快照，resourceType: {}, resourceId: {}", dto.getResourceType(), dto.getResourceId());
        VersionSnapshot snapshot = versionSnapshotService.createSnapshot(dto);
        return Result.success(snapshot);
    }

    @ApiOperation("获取版本快照列表")
    @GetMapping("/snapshot/list")
    public Result<List<VersionSnapshot>> getSnapshotList(
            @RequestParam(required = false) Long resourceId,
            @RequestParam String resourceType,
            @RequestParam Long appId) {
        log.info("获取版本快照列表，resourceType: {}, resourceId: {}, appId: {}", resourceType, resourceId, appId);
        List<VersionSnapshot> list = versionSnapshotService.getSnapshotList(resourceId, resourceType, appId);
        return Result.success(list);
    }

    @ApiOperation("获取快照详情")
    @GetMapping("/snapshot/{id}")
    public Result<VersionSnapshot> getSnapshotDetail(@PathVariable Long id) {
        log.info("获取快照详情，id: {}", id);
        VersionSnapshot snapshot = versionSnapshotService.getSnapshotDetail(id);
        return Result.success(snapshot);
    }

    @ApiOperation("回滚到指定版本")
    @PostMapping("/snapshot/{id}/rollback")
    public Result<VersionSnapshot> rollbackToSnapshot(@PathVariable Long id, @RequestBody RollbackDTO dto) {
        log.info("回滚到版本快照，snapshotId: {}, reason: {}", id, dto.getRollbackReason());
        VersionSnapshot snapshot = versionSnapshotService.rollbackToSnapshot(
                id, dto.getRollbackReason(), dto.getCreateNewSnapshot());
        return Result.success(snapshot);
    }

    @ApiOperation("版本对比")
    @GetMapping("/snapshot/diff")
    public Result<VersionDiffVO> compareVersions(@ModelAttribute VersionDiffDTO dto) {
        log.info("版本对比，oldSnapshotId: {}, newSnapshotId: {}", dto.getOldSnapshotId(), dto.getNewSnapshotId());
        VersionDiffVO diffVO = versionSnapshotService.compareVersions(dto.getOldSnapshotId(), dto.getNewSnapshotId());
        return Result.success(diffVO);
    }

    @ApiOperation("创建发布")
    @PostMapping("/release")
    public Result<ReleaseRecord> createRelease(@RequestBody ReleaseRecordDTO dto) {
        log.info("创建发布，resourceType: {}, resourceId: {}", dto.getResourceType(), dto.getResourceId());
        ReleaseRecord record = releaseRecordService.createRelease(dto);
        return Result.success(record);
    }

    @ApiOperation("获取发布记录")
    @GetMapping("/release/list")
    public Result<List<ReleaseRecord>> getReleaseList(
            @RequestParam(required = false) Long resourceId,
            @RequestParam String resourceType,
            @RequestParam Long appId) {
        log.info("获取发布记录列表，resourceType: {}, resourceId: {}, appId: {}", resourceType, resourceId, appId);
        List<ReleaseRecord> list = releaseRecordService.getReleaseList(resourceId, resourceType, appId);
        return Result.success(list);
    }

    @ApiOperation("执行发布")
    @PostMapping("/release/{id}/publish")
    public Result<ReleaseRecord> publishRelease(@PathVariable Long id) {
        log.info("执行发布，id: {}", id);
        ReleaseRecord record = releaseRecordService.publishRelease(id);
        return Result.success(record);
    }

    @ApiOperation("回滚发布")
    @PostMapping("/release/{id}/rollback")
    public Result<ReleaseRecord> rollbackRelease(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        log.info("回滚发布，id: {}, reason: {}", id, reason);
        ReleaseRecord record = releaseRecordService.rollbackRelease(id, reason);
        return Result.success(record);
    }

    @ApiOperation("创建灰度配置")
    @PostMapping("/gray/config")
    public Result<GrayReleaseConfig> createGrayConfig(@RequestBody GrayReleaseConfig config) {
        log.info("创建灰度配置，resourceType: {}, resourceId: {}", config.getResourceType(), config.getResourceId());
        GrayReleaseConfig result = grayReleaseService.createGrayConfig(config);
        return Result.success(result);
    }

    @ApiOperation("检查灰度命中")
    @GetMapping("/gray/check")
    public Result<GrayReleaseResultVO> checkGrayRelease(
            @RequestParam Long resourceId,
            @RequestParam String resourceType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userGroup) {
        log.info("检查灰度命中，resourceType: {}, resourceId: {}, userId: {}, userGroup: {}", 
                resourceType, resourceId, userId, userGroup);
        GrayReleaseResultVO result = grayReleaseService.checkGrayRelease(resourceId, resourceType, userId, userGroup);
        return Result.success(result);
    }

    @ApiOperation("停止灰度")
    @PostMapping("/gray/{id}/stop")
    public Result<GrayReleaseConfig> stopGrayRelease(@PathVariable Long id) {
        log.info("停止灰度，id: {}", id);
        GrayReleaseConfig config = grayReleaseService.stopGrayRelease(id);
        return Result.success(config);
    }

    @ApiOperation("取消灰度")
    @PostMapping("/gray/{id}/cancel")
    public Result<GrayReleaseConfig> cancelGrayRelease(@PathVariable Long id) {
        log.info("取消灰度，id: {}", id);
        GrayReleaseConfig config = grayReleaseService.cancelGrayRelease(id);
        return Result.success(config);
    }
}
