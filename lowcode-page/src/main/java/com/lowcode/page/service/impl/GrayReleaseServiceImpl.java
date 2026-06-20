package com.lowcode.page.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.entity.GrayReleaseConfig;
import com.lowcode.page.entity.Page;
import com.lowcode.page.entity.ReleaseRecord;
import com.lowcode.page.entity.VersionSnapshot;
import com.lowcode.page.mapper.GrayReleaseConfigMapper;
import com.lowcode.page.mapper.ReleaseRecordMapper;
import com.lowcode.page.service.GrayReleaseService;
import com.lowcode.page.service.PageService;
import com.lowcode.page.service.VersionSnapshotService;
import com.lowcode.page.vo.GrayReleaseResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class GrayReleaseServiceImpl extends ServiceImpl<GrayReleaseConfigMapper, GrayReleaseConfig> implements GrayReleaseService {

    @Autowired
    private GrayReleaseConfigMapper grayReleaseConfigMapper;

    @Autowired
    private VersionSnapshotService versionSnapshotService;

    @Autowired
    private PageService pageService;

    @Autowired
    private ReleaseRecordMapper releaseRecordMapper;

    private static final int GRAY_STATUS_INACTIVE = 0;
    private static final int GRAY_STATUS_ACTIVE = 1;
    private static final int GRAY_STATUS_STOPPED = 2;
    private static final int GRAY_STATUS_CANCELLED = 3;

    private static final int RELEASE_STATUS_PUBLISHED = 1;
    private static final int RELEASE_STATUS_ROLLED_BACK = 4;

    private static final int RELEASE_TYPE_FORMAL = 1;
    private static final int RELEASE_TYPE_ROLLBACK = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrayReleaseConfig createGrayConfig(GrayReleaseConfig config) {
        log.info("创建灰度发布配置，resourceType: {}, resourceId: {}", 
                config.getResourceType(), config.getResourceId());

        stopExistingGrayConfig(config.getResourceId(), config.getResourceType(), config.getAppId());

        config.setStatus(GRAY_STATUS_ACTIVE);
        if (config.getStartTime() == null) {
            config.setStartTime(LocalDateTime.now());
        }
        if (config.getHashField() == null || config.getHashField().isEmpty()) {
            config.setHashField("userId");
        }

        save(config);

        log.info("灰度发布配置创建成功，id: {}", config.getId());
        return config;
    }

    @Override
    public GrayReleaseResultVO checkGrayRelease(Long resourceId, String resourceType, Long userId, String userGroup) {
        log.info("检查灰度发布命中，resourceType: {}, resourceId: {}, userId: {}, userGroup: {}", 
                resourceType, resourceId, userId, userGroup);

        GrayReleaseResultVO result = new GrayReleaseResultVO();
        result.setShouldUseNewVersion(false);
        result.setMatchReason("未命中灰度发布，使用旧版本");

        GrayReleaseConfig config = getActiveGrayConfig(resourceId, resourceType);
        if (config == null) {
            log.info("没有生效的灰度配置，返回旧版本");
            return result;
        }

        result.setGrayPercent(config.getGrayPercent());
        result.setActiveVersion(config.getOldVersion());
        result.setActiveSnapshotId(config.getOldSnapshotId());

        if (checkBlackList(config, userId)) {
            log.info("用户在黑名单中，返回旧版本，userId: {}", userId);
            result.setMatchReason("用户在黑名单中，使用旧版本");
            result.setMatchedRule("BLACK_LIST");
            return result;
        }

        if (checkWhiteList(config, userId)) {
            log.info("用户在白名单中，返回新版本，userId: {}", userId);
            result.setShouldUseNewVersion(true);
            result.setActiveVersion(config.getNewVersion());
            result.setActiveSnapshotId(config.getNewSnapshotId());
            result.setMatchReason("用户在白名单中，使用新版本");
            result.setMatchedRule("WHITE_LIST");
            return result;
        }

        if (checkUserGroup(config, userGroup)) {
            log.info("用户组匹配，返回新版本，userGroup: {}", userGroup);
            result.setShouldUseNewVersion(true);
            result.setActiveVersion(config.getNewVersion());
            result.setActiveSnapshotId(config.getNewSnapshotId());
            result.setMatchReason("用户组匹配，使用新版本");
            result.setMatchedRule("USER_GROUP");
            return result;
        }

        if (checkUserIds(config, userId)) {
            log.info("用户ID匹配，返回新版本，userId: {}", userId);
            result.setShouldUseNewVersion(true);
            result.setActiveVersion(config.getNewVersion());
            result.setActiveSnapshotId(config.getNewSnapshotId());
            result.setMatchReason("用户ID匹配，使用新版本");
            result.setMatchedRule("USER_IDS");
            return result;
        }

        int hashValue = calculateConsistentHash(userId, config.getHashField());
        result.setHashValue(String.valueOf(hashValue));
        log.info("一致性哈希计算结果，userId: {}, hashValue: {}, grayPercent: {}", 
                userId, hashValue, config.getGrayPercent());

        if (hashValue < config.getGrayPercent()) {
            log.info("百分比命中，返回新版本，userId: {}", userId);
            result.setShouldUseNewVersion(true);
            result.setActiveVersion(config.getNewVersion());
            result.setActiveSnapshotId(config.getNewSnapshotId());
            result.setMatchReason("灰度百分比命中，使用新版本");
            result.setMatchedRule("PERCENTAGE");
        } else {
            log.info("未命中灰度百分比，返回旧版本，userId: {}", userId);
            result.setMatchReason("未命中灰度百分比，使用旧版本");
            result.setMatchedRule("PERCENTAGE");
        }

        return result;
    }

    @Override
    public GrayReleaseConfig getActiveGrayConfig(Long resourceId, String resourceType) {
        log.info("获取当前生效的灰度配置，resourceType: {}, resourceId: {}", resourceType, resourceId);

        LambdaQueryWrapper<GrayReleaseConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrayReleaseConfig::getResourceType, resourceType);
        wrapper.eq(GrayReleaseConfig::getResourceId, resourceId);
        wrapper.eq(GrayReleaseConfig::getStatus, GRAY_STATUS_ACTIVE);
        wrapper.orderByDesc(GrayReleaseConfig::getCreatedTime);
        wrapper.last("LIMIT 1");

        GrayReleaseConfig config = getOne(wrapper);
        if (config != null) {
            log.info("找到生效的灰度配置，id: {}", config.getId());
        } else {
            log.info("没有找到生效的灰度配置");
        }

        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrayReleaseConfig stopGrayRelease(Long configId) {
        log.info("停止灰度发布，全量发布新版本，configId: {}", configId);

        GrayReleaseConfig config = getById(configId);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "灰度配置不存在");
        }

        if (config.getStatus() != GRAY_STATUS_ACTIVE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前灰度配置状态不允许停止");
        }

        VersionSnapshot newSnapshot = null;
        if (config.getNewSnapshotId() != null) {
            newSnapshot = versionSnapshotService.getSnapshotDetail(config.getNewSnapshotId());
            if (newSnapshot == null) {
                log.warn("新版本快照不存在，snapshotId: {}", config.getNewSnapshotId());
            }
        }

        if (newSnapshot != null && newSnapshot.getPageSnapshot() != null 
                && "PAGE".equals(config.getResourceType())) {
            log.info("将新版本快照内容恢复到实际页面，snapshotId: {}", config.getNewSnapshotId());
            Page page = JSON.parseObject(newSnapshot.getPageSnapshot(), Page.class);
            page.setId(config.getResourceId());
            pageService.updatePage(page);
            log.info("页面内容已更新为新版本，pageId: {}", config.getResourceId());
        }

        if (newSnapshot != null) {
            newSnapshot.setIsPublished(1);
            newSnapshot.setPublishedVersion(config.getNewVersion());
            versionSnapshotService.updateById(newSnapshot);
            log.info("更新快照发布状态，snapshotId: {}, version: {}", newSnapshot.getId(), config.getNewVersion());
        }

        config.setStatus(GRAY_STATUS_STOPPED);
        config.setEndTime(LocalDateTime.now());
        updateById(config);

        ReleaseRecord releaseRecord = new ReleaseRecord();
        releaseRecord.setAppId(config.getAppId());
        releaseRecord.setResourceType(config.getResourceType());
        releaseRecord.setResourceId(config.getResourceId());
        if (newSnapshot != null) {
            releaseRecord.setResourceName(newSnapshot.getResourceName());
        }
        releaseRecord.setSnapshotId(config.getNewSnapshotId());
        releaseRecord.setVersion(config.getNewVersion());
        releaseRecord.setReleaseTitle("灰度发布全量-版本 " + config.getNewVersion());
        releaseRecord.setReleaseNote("灰度发布停止，全量发布新版本");
        releaseRecord.setReleaseType(RELEASE_TYPE_FORMAL);
        releaseRecord.setReleaseStatus(RELEASE_STATUS_PUBLISHED);
        releaseRecord.setReleaseTime(LocalDateTime.now());
        releaseRecord.setIsRollback(0);
        if (newSnapshot != null) {
            releaseRecord.setGitCommitId(newSnapshot.getGitCommitId());
            releaseRecord.setGitCommitMessage(newSnapshot.getGitCommitMessage());
            releaseRecord.setGitBranch(newSnapshot.getGitBranch());
        }
        releaseRecordMapper.insert(releaseRecord);
        log.info("创建发布记录成功，recordId: {}", releaseRecord.getId());

        log.info("灰度发布已停止，已全量发布新版本，configId: {}", configId);
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrayReleaseConfig cancelGrayRelease(Long configId) {
        log.info("取消灰度发布，回滚到旧版本，configId: {}", configId);

        GrayReleaseConfig config = getById(configId);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "灰度配置不存在");
        }

        if (config.getStatus() != GRAY_STATUS_ACTIVE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前灰度配置状态不允许取消");
        }

        VersionSnapshot oldSnapshot = null;
        if (config.getOldSnapshotId() != null) {
            oldSnapshot = versionSnapshotService.getSnapshotDetail(config.getOldSnapshotId());
            if (oldSnapshot == null) {
                log.warn("旧版本快照不存在，snapshotId: {}", config.getOldSnapshotId());
            }
        }

        if (oldSnapshot != null && "PAGE".equals(config.getResourceType())) {
            log.info("将旧版本快照内容恢复到实际页面，snapshotId: {}", config.getOldSnapshotId());
            if (oldSnapshot.getPageSnapshot() != null) {
                Page page = JSON.parseObject(oldSnapshot.getPageSnapshot(), Page.class);
                page.setId(config.getResourceId());
                pageService.updatePage(page);
                log.info("页面内容已回滚到旧版本，pageId: {}", config.getResourceId());
            } else {
                versionSnapshotService.rollbackToSnapshot(config.getOldSnapshotId(), "取消灰度发布，回滚到旧版本", false);
                log.info("通过 rollbackToSnapshot 回滚页面，pageId: {}", config.getResourceId());
            }
        }

        config.setStatus(GRAY_STATUS_CANCELLED);
        config.setEndTime(LocalDateTime.now());
        updateById(config);

        ReleaseRecord releaseRecord = new ReleaseRecord();
        releaseRecord.setAppId(config.getAppId());
        releaseRecord.setResourceType(config.getResourceType());
        releaseRecord.setResourceId(config.getResourceId());
        if (oldSnapshot != null) {
            releaseRecord.setResourceName(oldSnapshot.getResourceName());
        }
        releaseRecord.setSnapshotId(config.getOldSnapshotId());
        releaseRecord.setVersion(config.getOldVersion());
        releaseRecord.setReleaseTitle("取消灰度发布-回滚到版本 " + config.getOldVersion());
        releaseRecord.setReleaseNote("灰度发布取消，回滚到旧版本");
        releaseRecord.setReleaseType(RELEASE_TYPE_ROLLBACK);
        releaseRecord.setReleaseStatus(RELEASE_STATUS_ROLLED_BACK);
        releaseRecord.setRollbackTime(LocalDateTime.now());
        releaseRecord.setRollbackFromSnapshotId(config.getOldSnapshotId());
        releaseRecord.setRollbackReason("取消灰度发布");
        releaseRecord.setIsRollback(1);
        if (oldSnapshot != null) {
            releaseRecord.setGitCommitId(oldSnapshot.getGitCommitId());
            releaseRecord.setGitCommitMessage(oldSnapshot.getGitCommitMessage());
            releaseRecord.setGitBranch(oldSnapshot.getGitBranch());
        }
        releaseRecordMapper.insert(releaseRecord);
        log.info("创建回滚发布记录成功，recordId: {}", releaseRecord.getId());

        log.info("灰度发布已取消，已回滚到旧版本，configId: {}", configId);
        return config;
    }

    private void stopExistingGrayConfig(Long resourceId, String resourceType, Long appId) {
        LambdaQueryWrapper<GrayReleaseConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrayReleaseConfig::getResourceType, resourceType);
        wrapper.eq(GrayReleaseConfig::getResourceId, resourceId);
        wrapper.eq(GrayReleaseConfig::getAppId, appId);
        wrapper.eq(GrayReleaseConfig::getStatus, GRAY_STATUS_ACTIVE);

        List<GrayReleaseConfig> activeConfigs = list(wrapper);
        for (GrayReleaseConfig activeConfig : activeConfigs) {
            activeConfig.setStatus(GRAY_STATUS_STOPPED);
            activeConfig.setEndTime(LocalDateTime.now());
            updateById(activeConfig);
            log.info("已停止现有灰度配置，id: {}", activeConfig.getId());
        }
    }

    private boolean checkBlackList(GrayReleaseConfig config, Long userId) {
        if (config.getBlackListUserIds() == null || config.getBlackListUserIds().isEmpty()) {
            return false;
        }

        Set<String> blackList = new HashSet<>(Arrays.asList(config.getBlackListUserIds().split(",")));
        return blackList.contains(String.valueOf(userId));
    }

    private boolean checkWhiteList(GrayReleaseConfig config, Long userId) {
        if (config.getWhiteListUserIds() == null || config.getWhiteListUserIds().isEmpty()) {
            return false;
        }

        Set<String> whiteList = new HashSet<>(Arrays.asList(config.getWhiteListUserIds().split(",")));
        return whiteList.contains(String.valueOf(userId));
    }

    private boolean checkUserGroup(GrayReleaseConfig config, String userGroup) {
        if (config.getGrayUserGroup() == null || config.getGrayUserGroup().isEmpty() || userGroup == null) {
            return false;
        }

        Set<String> userGroups = new HashSet<>(Arrays.asList(config.getGrayUserGroup().split(",")));
        return userGroups.contains(userGroup);
    }

    private boolean checkUserIds(GrayReleaseConfig config, Long userId) {
        if (config.getGrayUserIds() == null || config.getGrayUserIds().isEmpty()) {
            return false;
        }

        Set<String> userIds = new HashSet<>(Arrays.asList(config.getGrayUserIds().split(",")));
        return userIds.contains(String.valueOf(userId));
    }

    private int calculateConsistentHash(Long userId, String hashField) {
        if (userId == null) {
            return 100;
        }

        String hashSource = userId + ":" + (hashField != null ? hashField : "userId");
        int hash = hashSource.hashCode();
        
        if (hash < 0) {
            hash = -hash;
        }

        int result = hash % 100;
        log.debug("一致性哈希计算，source: {}, hash: {}, result: {}", hashSource, hash, result);
        return result;
    }
}
