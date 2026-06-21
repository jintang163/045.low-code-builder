package com.lowcode.model.service;

import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.model.dto.RollbackCheckResult;
import com.lowcode.model.dto.VersionCompareResult;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.DataModelVersion;
import com.lowcode.model.entity.ModelField;
import com.lowcode.model.entity.ModelIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class VersionRollbackService {

    @Autowired
    private DataModelVersionService versionService;

    @Autowired
    private DataModelService modelService;

    @Autowired
    private VersionCompareService compareService;

    @Autowired
    private DataModelSnapshotUtil snapshotUtil;

    public RollbackCheckResult checkRollback(Long modelId, Long targetVersionId) {
        RollbackCheckResult checkResult = new RollbackCheckResult();

        DataModel currentModel = modelService.getModelDetail(modelId);
        if (currentModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        DataModelVersion targetVersion = versionService.getVersion(targetVersionId);
        if (targetVersion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标版本不存在");
        }

        DataModel targetModel = versionService.deserializeVersion(targetVersion);
        if (targetModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标版本快照解析失败");
        }

        VersionCompareResult compareResult = compareService.compareModels(targetModel, currentModel);
        checkResult.setCompareResult(compareResult);

        checkResult.setDeletedFieldCount(compareResult.getDeletedFields().size());
        checkResult.setAddedFieldCount(compareResult.getAddedFields().size());
        checkResult.setModifiedFieldCount(compareResult.getModifiedFields().size());

        if (!compareResult.getDeletedFields().isEmpty()) {
            checkResult.setDataLossRisk(true);
            checkResult.addWarning("回滚将删除 " + compareResult.getDeletedFields().size() + " 个字段，可能导致数据丢失");
        }

        boolean primaryKeyDeleted = compareResult.getDeletedFields().stream()
                .anyMatch(f -> isPrimaryKeyField(targetModel, f.getFieldName()));
        if (primaryKeyDeleted) {
            checkResult.setPrimaryKeyChanged(true);
            checkResult.addError("回滚将删除主键字段，不允许回滚");
        }

        boolean primaryKeyModified = compareResult.getModifiedFields().stream()
                .anyMatch(f -> f.getChangedProperties().contains("isPrimary") || isPrimaryKeyField(currentModel, f.getFieldName()));
        if (primaryKeyModified) {
            checkResult.setPrimaryKeyChanged(true);
            checkResult.addWarning("主键字段属性发生变更");
        }

        for (VersionCompareResult.FieldChange modifiedField : compareResult.getModifiedFields()) {
            if (modifiedField.getChangedProperties().contains("fieldType")) {
                checkResult.addWarning("字段 [" + modifiedField.getFieldName() + "] 类型发生变更，可能存在数据兼容性问题");
            }
        }

        if (compareResult.getModelChangedProperties().contains("primaryKeyStrategy")) {
            checkResult.setPrimaryKeyChanged(true);
            checkResult.addWarning("主键策略发生变更");
        }

        log.info("回滚检查完成, modelId={}, targetVersionId={}, allowed={}",
                modelId, targetVersionId, checkResult.isAllowed());
        return checkResult;
    }

    @Transactional(rollbackFor = Exception.class)
    public DataModel rollback(Long modelId, Long targetVersionId, String rollbackReason) {
        RollbackCheckResult checkResult = checkRollback(modelId, targetVersionId);
        if (!checkResult.isAllowed()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "回滚检查不通过: " + String.join(", ", checkResult.getErrors()));
        }

        DataModelVersion targetVersion = versionService.getVersion(targetVersionId);
        DataModel snapshotModel = versionService.deserializeVersion(targetVersion);
        if (snapshotModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标版本快照解析失败");
        }

        DataModel currentModel = modelService.getModelDetail(modelId);

        DataModel updateModel = snapshotUtil.deepCopy(snapshotModel);
        updateModel.setId(currentModel.getId());
        updateModel.setCreatedBy(currentModel.getCreatedBy());
        updateModel.setCreatedTime(currentModel.getCreatedTime());

        if (updateModel.getFields() != null) {
            for (ModelField field : updateModel.getFields()) {
                field.setModelId(modelId);
                field.setId(null);
                field.setCreatedBy(null);
                field.setCreatedTime(null);
                field.setUpdatedBy(null);
                field.setUpdatedTime(null);
                field.setDeleted(null);
            }
        }

        if (updateModel.getIndexes() != null) {
            for (ModelIndex index : updateModel.getIndexes()) {
                index.setModelId(modelId);
                index.setId(null);
                index.setCreatedBy(null);
                index.setCreatedTime(null);
                index.setUpdatedBy(null);
                index.setUpdatedTime(null);
                index.setDeleted(null);
            }
        }

        DataModel updatedModel = modelService.updateModel(updateModel);

        String changeDescription = "回滚到版本" + targetVersion.getVersion() + ": " + rollbackReason;
        versionService.createSnapshot(modelId, changeDescription, 2);

        log.info("模型回滚成功, modelId={}, targetVersionId={}", modelId, targetVersionId);
        return updatedModel;
    }

    private boolean isPrimaryKeyField(DataModel model, String fieldName) {
        if (model == null || model.getFields() == null) {
            return false;
        }
        return model.getFields().stream()
                .filter(f -> Objects.equals(f.getFieldName(), fieldName))
                .anyMatch(f -> f.getIsPrimary() != null && f.getIsPrimary() == 1);
    }
}
