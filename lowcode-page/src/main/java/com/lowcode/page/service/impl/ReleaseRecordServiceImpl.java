package com.lowcode.page.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.dto.ReleaseRecordDTO;
import com.lowcode.page.entity.GrayReleaseConfig;
import com.lowcode.page.entity.ReleaseRecord;
import com.lowcode.page.entity.VersionSnapshot;
import com.lowcode.page.mapper.GrayReleaseConfigMapper;
import com.lowcode.page.mapper.ReleaseRecordMapper;
import com.lowcode.page.mapper.VersionSnapshotMapper;
import com.lowcode.page.service.GrayReleaseService;
import com.lowcode.page.service.ReleaseRecordService;
import com.lowcode.page.service.VersionSnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ReleaseRecordServiceImpl extends ServiceImpl<ReleaseRecordMapper, ReleaseRecord> implements ReleaseRecordService {

    @Autowired
    private VersionSnapshotService versionSnapshotService;

    @Autowired
    private VersionSnapshotMapper versionSnapshotMapper;

    @Autowired
    private GrayReleaseConfigMapper grayReleaseConfigMapper;

    @Autowired
    private GrayReleaseService grayReleaseService;

    private static final int RELEASE_STATUS_PENDING = 0;
    private static final int RELEASE_STATUS_PUBLISHED = 1;
    private static final int RELEASE_STATUS_PUBLISHING = 2;
    private static final int RELEASE_STATUS_FAILED = 3;
    private static final int RELEASE_STATUS_ROLLED_BACK = 4;
    private static final int RELEASE_STATUS_GRAYING = 5;

    private static final int RELEASE_TYPE_FORMAL = 1;
    private static final int RELEASE_TYPE_ROLLBACK = 2;
    private static final int RELEASE_TYPE_GRAY = 3;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReleaseRecord createRelease(ReleaseRecordDTO dto) {
        log.info("创建发布记录，resourceType: {}, resourceId: {}, releaseType: {}", 
                dto.getResourceType(), dto.getResourceId(), dto.getReleaseType());

        VersionSnapshot snapshot = versionSnapshotMapper.selectById(dto.getSnapshotId());
        if (snapshot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本快照不存在");
        }

        ReleaseRecord record = new ReleaseRecord();
        BeanUtils.copyProperties(dto, record);

        record.setResourceName(snapshot.getResourceName());
        record.setReleaseStatus(RELEASE_STATUS_PENDING);
        record.setReleaseTime(null);
        record.setIsRollback(0);
        record.setGitCommitId(snapshot.getGitCommitId());
        record.setGitCommitMessage(snapshot.getGitCommitMessage());
        record.setGitBranch(snapshot.getGitBranch());

        if (dto.getReleaseType() == null) {
            record.setReleaseType(RELEASE_TYPE_FORMAL);
        }

        save(record);

        if (dto.getReleaseType() != null && dto.getReleaseType() == RELEASE_TYPE_GRAY) {
            log.info("发布类型为灰度发布，创建灰度配置");
            GrayReleaseConfig grayConfig = createGrayConfig(record, snapshot);
            grayConfig.setStatus(1);
            grayReleaseConfigMapper.updateById(grayConfig);
            record.setReleaseStatus(RELEASE_STATUS_GRAYING);
            record.setReleaseTime(LocalDateTime.now());
            updateById(record);
            log.info("灰度发布已激活，releaseId: {}, grayConfigId: {}", record.getId(), grayConfig.getId());
        }

        log.info("发布记录创建成功，id: {}, version: {}", record.getId(), record.getVersion());
        return record;
    }

    @Override
    public List<ReleaseRecord> getReleaseList(Long resourceId, String resourceType, Long appId) {
        log.info("获取发布记录列表，resourceType: {}, resourceId: {}, appId: {}", 
                resourceType, resourceId, appId);

        LambdaQueryWrapper<ReleaseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseRecord::getAppId, appId);
        wrapper.eq(ReleaseRecord::getResourceType, resourceType);
        if (resourceId != null) {
            wrapper.eq(ReleaseRecord::getResourceId, resourceId);
        }
        wrapper.orderByDesc(ReleaseRecord::getCreatedTime);

        List<ReleaseRecord> list = list(wrapper);
        log.info("查询到发布记录数量: {}", list.size());
        return list;
    }

    @Override
    public ReleaseRecord getReleaseDetail(Long id) {
        log.info("获取发布详情，id: {}", id);

        ReleaseRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发布记录不存在");
        }

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReleaseRecord publishRelease(Long id) {
        log.info("执行发布，id: {}", id);

        ReleaseRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发布记录不存在");
        }

        if (record.getReleaseStatus() != RELEASE_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许发布");
        }

        record.setReleaseStatus(RELEASE_STATUS_PUBLISHING);
        updateById(record);
        log.info("更新状态为发布中，id: {}", id);

        try {
            VersionSnapshot snapshot = versionSnapshotMapper.selectById(record.getSnapshotId());
            if (snapshot != null) {
                snapshot.setIsPublished(1);
                snapshot.setPublishedVersion(record.getVersion());
                versionSnapshotMapper.updateById(snapshot);
                log.info("更新快照发布状态，snapshotId: {}", snapshot.getId());
            }

            record.setReleaseStatus(RELEASE_STATUS_PUBLISHED);
            record.setReleaseTime(LocalDateTime.now());
            updateById(record);

            log.info("发布成功，id: {}, version: {}", id, record.getVersion());
        } catch (Exception e) {
            log.error("发布失败，id: {}", id, e);
            record.setReleaseStatus(RELEASE_STATUS_FAILED);
            updateById(record);
            throw new BusinessException("发布失败: " + e.getMessage());
        }

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReleaseRecord rollbackRelease(Long id, String reason) {
        log.info("回滚发布，id: {}, reason: {}", id, reason);

        ReleaseRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发布记录不存在");
        }

        if (record.getReleaseStatus() != RELEASE_STATUS_PUBLISHED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许回滚");
        }

        try {
            versionSnapshotService.rollbackToSnapshot(record.getSnapshotId(), reason, true);

            record.setReleaseStatus(RELEASE_STATUS_ROLLED_BACK);
            record.setRollbackTime(LocalDateTime.now());
            record.setRollbackReason(reason);
            record.setRollbackFromSnapshotId(record.getSnapshotId());
            record.setIsRollback(1);
            updateById(record);

            LambdaQueryWrapper<ReleaseRecord> rollbackWrapper = new LambdaQueryWrapper<>();
            rollbackWrapper.eq(ReleaseRecord::getAppId, record.getAppId());
            rollbackWrapper.eq(ReleaseRecord::getResourceType, record.getResourceType());
            rollbackWrapper.eq(ReleaseRecord::getResourceId, record.getResourceId());
            rollbackWrapper.eq(ReleaseRecord::getReleaseStatus, RELEASE_STATUS_PUBLISHED);
            rollbackWrapper.ne(ReleaseRecord::getId, id);
            List<ReleaseRecord> otherPublished = list(rollbackWrapper);
            for (ReleaseRecord r : otherPublished) {
                r.setReleaseStatus(RELEASE_STATUS_ROLLED_BACK);
                updateById(r);
            }

            log.info("回滚成功，id: {}", id);
        } catch (Exception e) {
            log.error("回滚失败，id: {}", id, e);
            throw new BusinessException("回滚失败: " + e.getMessage());
        }

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReleaseRecord stopGrayRelease(Long id) {
        log.info("停止灰度发布，全量发布新版本，id: {}", id);

        ReleaseRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发布记录不存在");
        }

        if (record.getReleaseStatus() != RELEASE_STATUS_GRAYING) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许停止灰度");
        }

        try {
            LambdaQueryWrapper<GrayReleaseConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GrayReleaseConfig::getReleaseRecordId, id);
            wrapper.eq(GrayReleaseConfig::getStatus, 1);
            GrayReleaseConfig grayConfig = grayReleaseConfigMapper.selectOne(wrapper);

            if (grayConfig != null) {
                grayReleaseService.stopGrayRelease(grayConfig.getId());
            }

            VersionSnapshot snapshot = versionSnapshotMapper.selectById(record.getSnapshotId());
            if (snapshot != null) {
                snapshot.setIsPublished(1);
                snapshot.setPublishedVersion(record.getVersion());
                versionSnapshotMapper.updateById(snapshot);
            }

            record.setReleaseStatus(RELEASE_STATUS_PUBLISHED);
            record.setReleaseTime(LocalDateTime.now());
            updateById(record);

            log.info("灰度发布停止，已全量发布新版本，id: {}", id);
        } catch (Exception e) {
            log.error("停止灰度发布失败，id: {}", id, e);
            throw new BusinessException("停止灰度发布失败: " + e.getMessage());
        }

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReleaseRecord cancelGrayRelease(Long id) {
        log.info("取消灰度发布，回滚到旧版本，id: {}", id);

        ReleaseRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发布记录不存在");
        }

        if (record.getReleaseStatus() != RELEASE_STATUS_GRAYING) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许取消灰度");
        }

        try {
            LambdaQueryWrapper<GrayReleaseConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GrayReleaseConfig::getReleaseRecordId, id);
            wrapper.eq(GrayReleaseConfig::getStatus, 1);
            GrayReleaseConfig grayConfig = grayReleaseConfigMapper.selectOne(wrapper);

            if (grayConfig != null) {
                grayReleaseService.cancelGrayRelease(grayConfig.getId());

                versionSnapshotService.rollbackToSnapshot(grayConfig.getOldSnapshotId(), "取消灰度发布，回滚到旧版本", true);
            }

            record.setReleaseStatus(RELEASE_STATUS_ROLLED_BACK);
            record.setRollbackTime(LocalDateTime.now());
            record.setRollbackReason("取消灰度发布");
            record.setIsRollback(1);
            updateById(record);

            log.info("灰度发布已取消，已回滚到旧版本，id: {}", id);
        } catch (Exception e) {
            log.error("取消灰度发布失败，id: {}", id, e);
            throw new BusinessException("取消灰度发布失败: " + e.getMessage());
        }

        return record;
    }

    private GrayReleaseConfig createGrayConfig(ReleaseRecord record, VersionSnapshot newSnapshot) {
        LambdaQueryWrapper<ReleaseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseRecord::getAppId, record.getAppId());
        wrapper.eq(ReleaseRecord::getResourceType, record.getResourceType());
        wrapper.eq(ReleaseRecord::getResourceId, record.getResourceId());
        wrapper.eq(ReleaseRecord::getReleaseStatus, RELEASE_STATUS_PUBLISHED);
        wrapper.orderByDesc(ReleaseRecord::getReleaseTime);
        wrapper.last("LIMIT 1");

        ReleaseRecord lastPublished = getOne(wrapper);
        Long oldSnapshotId = null;
        String oldVersion = null;

        if (lastPublished != null) {
            oldSnapshotId = lastPublished.getSnapshotId();
            oldVersion = lastPublished.getVersion();
        }

        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setAppId(record.getAppId());
        grayConfig.setResourceType(record.getResourceType());
        grayConfig.setResourceId(record.getResourceId());
        grayConfig.setReleaseRecordId(record.getId());
        grayConfig.setNewSnapshotId(record.getSnapshotId());
        grayConfig.setOldSnapshotId(oldSnapshotId);
        grayConfig.setNewVersion(record.getVersion());
        grayConfig.setOldVersion(oldVersion);
        grayConfig.setGrayType(record.getGrayType());
        grayConfig.setGrayPercent(record.getGrayPercent());
        grayConfig.setGrayUserGroup(record.getGrayUserGroup());
        grayConfig.setGrayUserIds(record.getGrayUserIds());
        grayConfig.setStatus(0);
        grayConfig.setStartTime(LocalDateTime.now());
        grayConfig.setHashField("userId");

        grayReleaseConfigMapper.insert(grayConfig);
        log.info("灰度配置创建成功，grayConfigId: {}", grayConfig.getId());
        return grayConfig;
    }
}
