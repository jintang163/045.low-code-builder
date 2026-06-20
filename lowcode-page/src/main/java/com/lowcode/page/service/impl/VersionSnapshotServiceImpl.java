package com.lowcode.page.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.result.Result;
import com.lowcode.flow.entity.BusinessLogic;
import com.lowcode.model.entity.DataModel;
import com.lowcode.page.dto.VersionSnapshotDTO;
import com.lowcode.page.entity.Page;
import com.lowcode.page.entity.ReleaseRecord;
import com.lowcode.page.entity.VersionSnapshot;
import com.lowcode.page.feign.BusinessLogicFeignClient;
import com.lowcode.page.feign.DataModelFeignClient;
import com.lowcode.page.mapper.PageMapper;
import com.lowcode.page.mapper.ReleaseRecordMapper;
import com.lowcode.page.mapper.VersionSnapshotMapper;
import com.lowcode.page.service.PageService;
import com.lowcode.page.service.VersionSnapshotService;
import com.lowcode.page.vo.VersionDiffVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VersionSnapshotServiceImpl extends ServiceImpl<VersionSnapshotMapper, VersionSnapshot> implements VersionSnapshotService {

    @Autowired
    private PageService pageService;

    @Autowired
    private PageMapper pageMapper;

    @Autowired
    private ReleaseRecordMapper releaseRecordMapper;

    @Autowired
    private DataModelFeignClient dataModelFeignClient;

    @Autowired
    private BusinessLogicFeignClient businessLogicFeignClient;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String RESOURCE_TYPE_PAGE = "PAGE";
    private static final String RESOURCE_TYPE_DATA_MODEL = "DATA_MODEL";
    private static final String RESOURCE_TYPE_BUSINESS_LOGIC = "BUSINESS_LOGIC";

    private static final int SNAPSHOT_TYPE_AUTO = 1;
    private static final int SNAPSHOT_TYPE_MANUAL = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VersionSnapshot createSnapshot(VersionSnapshotDTO dto) {
        log.info("创建版本快照，resourceType: {}, resourceId: {}", dto.getResourceType(), dto.getResourceId());

        VersionSnapshot snapshot = new VersionSnapshot();
        BeanUtils.copyProperties(dto, snapshot);

        String resourceName = getResourceName(dto.getResourceType(), dto.getResourceId());
        snapshot.setResourceName(resourceName);

        String version = generateVersion(dto.getResourceType(), dto.getResourceId(), dto.getAppId());
        snapshot.setVersion(version);

        Integer snapshotType = Boolean.TRUE.equals(dto.getAutoCreate()) ? SNAPSHOT_TYPE_AUTO : SNAPSHOT_TYPE_MANUAL;
        snapshot.setSnapshotType(snapshotType);

        collectSnapshotData(snapshot, dto.getResourceType(), dto.getResourceId());

        collectGitInfo(snapshot);

        snapshot.setIsPublished(0);

        save(snapshot);

        log.info("版本快照创建成功，id: {}, version: {}", snapshot.getId(), snapshot.getVersion());
        return snapshot;
    }

    @Override
    public List<VersionSnapshot> getSnapshotList(Long resourceId, String resourceType, Long appId) {
        log.info("获取版本快照列表，resourceType: {}, resourceId: {}, appId: {}", resourceType, resourceId, appId);

        LambdaQueryWrapper<VersionSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VersionSnapshot::getAppId, appId);
        wrapper.eq(VersionSnapshot::getResourceType, resourceType);
        if (resourceId != null) {
            wrapper.eq(VersionSnapshot::getResourceId, resourceId);
        }
        wrapper.orderByDesc(VersionSnapshot::getCreatedTime);

        List<VersionSnapshot> list = list(wrapper);
        log.info("查询到版本快照数量: {}", list.size());
        return list;
    }

    @Override
    public VersionSnapshot getSnapshotDetail(Long id) {
        log.info("获取版本快照详情，id: {}", id);

        VersionSnapshot snapshot = getById(id);
        if (snapshot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本快照不存在");
        }

        return snapshot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VersionSnapshot rollbackToSnapshot(Long snapshotId, String reason, Boolean createNewSnapshot) {
        log.info("回滚到版本快照，snapshotId: {}, reason: {}", snapshotId, reason);

        VersionSnapshot targetSnapshot = getById(snapshotId);
        if (targetSnapshot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标版本快照不存在");
        }

        if (Boolean.TRUE.equals(createNewSnapshot)) {
            log.info("创建回滚前的新快照");
            VersionSnapshotDTO autoDto = new VersionSnapshotDTO();
            autoDto.setAppId(targetSnapshot.getAppId());
            autoDto.setResourceType(targetSnapshot.getResourceType());
            autoDto.setResourceId(targetSnapshot.getResourceId());
            autoDto.setDescription("回滚前自动快照");
            autoDto.setAutoCreate(true);
            createSnapshot(autoDto);
        }

        restoreFromSnapshot(targetSnapshot);

        createRollbackRecord(targetSnapshot, reason);

        log.info("回滚完成，snapshotId: {}", snapshotId);
        return targetSnapshot;
    }

    @Override
    public VersionDiffVO compareVersions(Long oldSnapshotId, Long newSnapshotId) {
        log.info("对比版本差异，oldSnapshotId: {}, newSnapshotId: {}", oldSnapshotId, newSnapshotId);

        VersionSnapshot oldSnapshot = getById(oldSnapshotId);
        VersionSnapshot newSnapshot = getById(newSnapshotId);

        if (oldSnapshot == null || newSnapshot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本快照不存在");
        }

        VersionDiffVO diffVO = new VersionDiffVO();
        diffVO.setOldSnapshotId(oldSnapshotId);
        diffVO.setNewSnapshotId(newSnapshotId);
        diffVO.setOldVersion(oldSnapshot.getVersion());
        diffVO.setNewVersion(newSnapshot.getVersion());

        diffVO.setPageDiffs(compareJson(oldSnapshot.getPageSnapshot(), newSnapshot.getPageSnapshot(), "page"));
        diffVO.setDataModelDiffs(compareJson(oldSnapshot.getDataModelSnapshot(), newSnapshot.getDataModelSnapshot(), "dataModel"));
        diffVO.setLogicDiffs(compareJson(oldSnapshot.getLogicSnapshot(), newSnapshot.getLogicSnapshot(), "logic"));

        try {
            if (oldSnapshot.getSnapshotData() != null) {
                diffVO.setOldData(objectMapper.readValue(oldSnapshot.getSnapshotData(), Map.class));
            }
            if (newSnapshot.getSnapshotData() != null) {
                diffVO.setNewData(objectMapper.readValue(newSnapshot.getSnapshotData(), Map.class));
            }
        } catch (Exception e) {
            log.warn("解析快照数据失败", e);
        }

        return diffVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VersionSnapshot createAutoSnapshot(String resourceType, Long resourceId, Long appId) {
        log.info("创建自动快照，resourceType: {}, resourceId: {}, appId: {}", resourceType, resourceId, appId);

        VersionSnapshotDTO dto = new VersionSnapshotDTO();
        dto.setAppId(appId);
        dto.setResourceType(resourceType);
        dto.setResourceId(resourceId);
        dto.setDescription("保存时自动快照");
        dto.setAutoCreate(true);

        return createSnapshot(dto);
    }

    private String getResourceName(String resourceType, Long resourceId) {
        try {
            switch (resourceType) {
                case RESOURCE_TYPE_PAGE:
                    Page page = pageService.getById(resourceId);
                    return page != null ? page.getPageName() : "未知页面";
                case RESOURCE_TYPE_DATA_MODEL:
                    Result<DataModel> modelResult = dataModelFeignClient.getById(resourceId);
                    if (modelResult != null && modelResult.getData() != null) {
                        return modelResult.getData().getModelName();
                    }
                    return "未知数据模型";
                case RESOURCE_TYPE_BUSINESS_LOGIC:
                    Result<BusinessLogic> logicResult = businessLogicFeignClient.getById(resourceId);
                    if (logicResult != null && logicResult.getData() != null) {
                        return logicResult.getData().getLogicName();
                    }
                    return "未知业务逻辑";
                default:
                    return "未知资源";
            }
        } catch (Exception e) {
            log.warn("获取资源名称失败，resourceType: {}, resourceId: {}", resourceType, resourceId, e);
            return "未知资源";
        }
    }

    private String generateVersion(String resourceType, Long resourceId, Long appId) {
        LambdaQueryWrapper<VersionSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VersionSnapshot::getAppId, appId);
        wrapper.eq(VersionSnapshot::getResourceType, resourceType);
        wrapper.eq(VersionSnapshot::getResourceId, resourceId);
        wrapper.orderByDesc(VersionSnapshot::getCreatedTime);
        wrapper.last("LIMIT 1");

        VersionSnapshot lastSnapshot = getOne(wrapper);

        int major = 1;
        int minor = 0;
        int patch = 0;

        if (lastSnapshot != null && lastSnapshot.getVersion() != null) {
            String version = lastSnapshot.getVersion();
            if (version.startsWith("v")) {
                version = version.substring(1);
            }
            String[] parts = version.split("\\.");
            if (parts.length == 3) {
                try {
                    major = Integer.parseInt(parts[0]);
                    minor = Integer.parseInt(parts[1]);
                    patch = Integer.parseInt(parts[2]) + 1;

                    if (patch > 99) {
                        patch = 0;
                        minor++;
                    }
                    if (minor > 99) {
                        minor = 0;
                        major++;
                    }
                } catch (NumberFormatException e) {
                    log.warn("解析版本号失败，使用默认版本号", e);
                }
            }
        }

        return String.format("v%d.%d.%d", major, minor, patch);
    }

    private void collectSnapshotData(VersionSnapshot snapshot, String resourceType, Long resourceId) {
        try {
            switch (resourceType) {
                case RESOURCE_TYPE_PAGE:
                    Page page = pageService.getPageDetail(resourceId);
                    if (page != null) {
                        snapshot.setPageSnapshot(JSON.toJSONString(page));
                        Map<String, Object> snapshotData = new java.util.HashMap<>();
                        snapshotData.put("page", page);
                        snapshot.setSnapshotData(JSON.toJSONString(snapshotData));
                    }
                    break;
                case RESOURCE_TYPE_DATA_MODEL:
                    Result<DataModel> modelResult = dataModelFeignClient.getById(resourceId);
                    if (modelResult != null && modelResult.getData() != null) {
                        snapshot.setDataModelSnapshot(JSON.toJSONString(modelResult.getData()));
                        Map<String, Object> snapshotData = new java.util.HashMap<>();
                        snapshotData.put("dataModel", modelResult.getData());
                        snapshot.setSnapshotData(JSON.toJSONString(snapshotData));
                    }
                    break;
                case RESOURCE_TYPE_BUSINESS_LOGIC:
                    Result<BusinessLogic> logicResult = businessLogicFeignClient.getById(resourceId);
                    if (logicResult != null && logicResult.getData() != null) {
                        snapshot.setLogicSnapshot(JSON.toJSONString(logicResult.getData()));
                        Map<String, Object> snapshotData = new java.util.HashMap<>();
                        snapshotData.put("businessLogic", logicResult.getData());
                        snapshot.setSnapshotData(JSON.toJSONString(snapshotData));
                    }
                    break;
                default:
                    log.warn("不支持的资源类型: {}", resourceType);
            }
        } catch (Exception e) {
            log.error("收集快照数据失败，resourceType: {}, resourceId: {}", resourceType, resourceId, e);
            throw new BusinessException("收集快照数据失败: " + e.getMessage());
        }
    }

    private void collectGitInfo(VersionSnapshot snapshot) {
        try {
            String commitId = executeGitCommand("git rev-parse HEAD");
            String commitMessage = executeGitCommand("git log -1 --pretty=%B");
            String branch = executeGitCommand("git rev-parse --abbrev-ref HEAD");

            if (commitId != null && !commitId.isEmpty()) {
                snapshot.setGitCommitId(commitId.trim());
            }
            if (commitMessage != null && !commitMessage.isEmpty()) {
                snapshot.setGitCommitMessage(commitMessage.trim());
            }
            if (branch != null && !branch.isEmpty()) {
                snapshot.setGitBranch(branch.trim());
            }
        } catch (Exception e) {
            log.warn("获取Git信息失败，跳过", e);
        }
    }

    private String executeGitCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            process.waitFor();
            return result.toString();
        } catch (Exception e) {
            log.debug("执行Git命令失败: {}", command, e);
            return null;
        }
    }

    private void restoreFromSnapshot(VersionSnapshot snapshot) {
        log.info("从快照恢复数据，resourceType: {}, resourceId: {}", snapshot.getResourceType(), snapshot.getResourceId());

        try {
            switch (snapshot.getResourceType()) {
                case RESOURCE_TYPE_PAGE:
                    if (snapshot.getPageSnapshot() != null) {
                        Page page = JSON.parseObject(snapshot.getPageSnapshot(), Page.class);
                        page.setId(snapshot.getResourceId());
                        pageService.updatePage(page);
                    }
                    break;
                case RESOURCE_TYPE_DATA_MODEL:
                    if (snapshot.getDataModelSnapshot() != null) {
                        DataModel dataModel = JSON.parseObject(snapshot.getDataModelSnapshot(), DataModel.class);
                        dataModel.setId(snapshot.getResourceId());
                        Result<DataModel> result = dataModelFeignClient.update(dataModel);
                        if (result == null || result.getCode() != 0) {
                            throw new BusinessException("恢复数据模型失败: " + (result != null ? result.getMessage() : "未知错误"));
                        }
                    }
                    break;
                case RESOURCE_TYPE_BUSINESS_LOGIC:
                    if (snapshot.getLogicSnapshot() != null) {
                        BusinessLogic logic = JSON.parseObject(snapshot.getLogicSnapshot(), BusinessLogic.class);
                        logic.setId(snapshot.getResourceId());
                        Result<BusinessLogic> result = businessLogicFeignClient.update(logic);
                        if (result == null || result.getCode() != 0) {
                            throw new BusinessException("恢复业务逻辑失败: " + (result != null ? result.getMessage() : "未知错误"));
                        }
                    }
                    break;
                default:
                    throw new BusinessException("不支持的资源类型: " + snapshot.getResourceType());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("从快照恢复数据失败", e);
            throw new BusinessException("恢复数据失败: " + e.getMessage());
        }
    }

    private void createRollbackRecord(VersionSnapshot snapshot, String reason) {
        ReleaseRecord record = new ReleaseRecord();
        record.setAppId(snapshot.getAppId());
        record.setResourceType(snapshot.getResourceType());
        record.setResourceId(snapshot.getResourceId());
        record.setResourceName(snapshot.getResourceName());
        record.setSnapshotId(snapshot.getId());
        record.setVersion(snapshot.getVersion());
        record.setReleaseTitle("回滚到版本 " + snapshot.getVersion());
        record.setReleaseNote(reason);
        record.setReleaseType(2);
        record.setReleaseStatus(1);
        record.setRollbackTime(LocalDateTime.now());
        record.setRollbackFromSnapshotId(snapshot.getId());
        record.setRollbackReason(reason);
        record.setIsRollback(1);
        record.setGitCommitId(snapshot.getGitCommitId());
        record.setGitCommitMessage(snapshot.getGitCommitMessage());
        record.setGitBranch(snapshot.getGitBranch());

        releaseRecordMapper.insert(record);
        log.info("创建回滚记录成功，recordId: {}", record.getId());
    }

    private List<VersionDiffVO.DiffItem> compareJson(String oldJson, String newJson, String prefix) {
        List<VersionDiffVO.DiffItem> diffs = new ArrayList<>();

        if (oldJson == null && newJson == null) {
            return diffs;
        }

        try {
            JsonNode oldNode = oldJson != null ? objectMapper.readTree(oldJson) : objectMapper.createObjectNode();
            JsonNode newNode = newJson != null ? objectMapper.readTree(newJson) : objectMapper.createObjectNode();

            compareJsonNode(oldNode, newNode, prefix, diffs);
        } catch (Exception e) {
            log.warn("对比JSON差异失败", e);
        }

        return diffs;
    }

    private void compareJsonNode(JsonNode oldNode, JsonNode newNode, String path, List<VersionDiffVO.DiffItem> diffs) {
        if (oldNode == null && newNode == null) {
            return;
        }

        if (oldNode == null) {
            VersionDiffVO.DiffItem item = new VersionDiffVO.DiffItem();
            item.setField(path);
            item.setPath(path);
            item.setOldValue(null);
            item.setNewValue(newNode.toString());
            item.setDiffType("ADD");
            diffs.add(item);
            return;
        }

        if (newNode == null) {
            VersionDiffVO.DiffItem item = new VersionDiffVO.DiffItem();
            item.setField(path);
            item.setPath(path);
            item.setOldValue(oldNode.toString());
            item.setNewValue(null);
            item.setDiffType("REMOVE");
            diffs.add(item);
            return;
        }

        if (oldNode.isObject() && newNode.isObject()) {
            ObjectNode oldObj = (ObjectNode) oldNode;
            ObjectNode newObj = (ObjectNode) newNode;

            Iterator<String> fieldNames = oldObj.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                String newPath = path + "." + fieldName;
                JsonNode oldField = oldObj.get(fieldName);
                JsonNode newField = newObj.get(fieldName);
                compareJsonNode(oldField, newField, newPath, diffs);
            }

            fieldNames = newObj.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (!oldObj.has(fieldName)) {
                    String newPath = path + "." + fieldName;
                    compareJsonNode(null, newObj.get(fieldName), newPath, diffs);
                }
            }
        } else if (oldNode.isArray() && newNode.isArray()) {
            ArrayNode oldArray = (ArrayNode) oldNode;
            ArrayNode newArray = (ArrayNode) newNode;

            int maxLen = Math.max(oldArray.size(), newArray.size());
            for (int i = 0; i < maxLen; i++) {
                String newPath = path + "[" + i + "]";
                JsonNode oldItem = i < oldArray.size() ? oldArray.get(i) : null;
                JsonNode newItem = i < newArray.size() ? newArray.get(i) : null;
                compareJsonNode(oldItem, newItem, newPath, diffs);
            }
        } else if (oldNode.isValueNode() && newNode.isValueNode()) {
            String oldValue = oldNode.asText();
            String newValue = newNode.asText();
            if (!oldValue.equals(newValue)) {
                VersionDiffVO.DiffItem item = new VersionDiffVO.DiffItem();
                item.setField(path);
                item.setPath(path);
                item.setOldValue(oldValue);
                item.setNewValue(newValue);
                item.setDiffType("MODIFY");
                diffs.add(item);
            }
        } else {
            if (!oldNode.toString().equals(newNode.toString())) {
                VersionDiffVO.DiffItem item = new VersionDiffVO.DiffItem();
                item.setField(path);
                item.setPath(path);
                item.setOldValue(oldNode.toString());
                item.setNewValue(newNode.toString());
                item.setDiffType("MODIFY");
                diffs.add(item);
            }
        }
    }
}
