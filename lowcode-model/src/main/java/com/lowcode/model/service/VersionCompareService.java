package com.lowcode.model.service;

import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.model.dto.VersionCompareResult;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.DataModelVersion;
import com.lowcode.model.entity.ModelField;
import com.lowcode.model.entity.ModelIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VersionCompareService {

    @Autowired
    private DataModelVersionService versionService;

    public VersionCompareResult compareVersions(Long modelId, String sourceVersion, String targetVersion) {
        DataModelVersion sourceVersionEntity = versionService.getVersionByVersion(modelId, sourceVersion);
        DataModelVersion targetVersionEntity = versionService.getVersionByVersion(modelId, targetVersion);

        if (sourceVersionEntity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "源版本不存在");
        }
        if (targetVersionEntity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标版本不存在");
        }

        DataModel oldModel = versionService.deserializeVersion(sourceVersionEntity);
        DataModel newModel = versionService.deserializeVersion(targetVersionEntity);

        return compareModels(oldModel, newModel);
    }

    public VersionCompareResult compareModels(DataModel oldModel, DataModel newModel) {
        VersionCompareResult result = new VersionCompareResult();

        compareModelProperties(oldModel, newModel, result);
        compareFields(oldModel, newModel, result);
        compareIndexes(oldModel, newModel, result);

        boolean compatible = isCompatibleChange(result);
        result.setCompatible(compatible);

        return result;
    }

    private void compareModelProperties(DataModel oldModel, DataModel newModel, VersionCompareResult result) {
        List<String> changedProperties = result.getModelChangedProperties();

        if (!Objects.equals(oldModel.getModelName(), newModel.getModelName())) {
            changedProperties.add("modelName");
        }
        if (!Objects.equals(oldModel.getTableName(), newModel.getTableName())) {
            changedProperties.add("tableName");
        }
        if (!Objects.equals(oldModel.getEntityName(), newModel.getEntityName())) {
            changedProperties.add("entityName");
        }
        if (!Objects.equals(oldModel.getModelDesc(), newModel.getModelDesc())) {
            changedProperties.add("modelDesc");
        }
        if (!Objects.equals(oldModel.getTableCharset(), newModel.getTableCharset())) {
            changedProperties.add("tableCharset");
        }
        if (!Objects.equals(oldModel.getTableEngine(), newModel.getTableEngine())) {
            changedProperties.add("tableEngine");
        }
        if (!Objects.equals(oldModel.getPrimaryKeyStrategy(), newModel.getPrimaryKeyStrategy())) {
            changedProperties.add("primaryKeyStrategy");
        }
        if (!Objects.equals(oldModel.getLogicDeleteField(), newModel.getLogicDeleteField())) {
            changedProperties.add("logicDeleteField");
        }
        if (!Objects.equals(oldModel.getVersionField(), newModel.getVersionField())) {
            changedProperties.add("versionField");
        }
        if (!Objects.equals(oldModel.getTenantField(), newModel.getTenantField())) {
            changedProperties.add("tenantField");
        }
    }

    private void compareFields(DataModel oldModel, DataModel newModel, VersionCompareResult result) {
        List<ModelField> oldFields = oldModel.getFields() != null ? oldModel.getFields() : Collections.emptyList();
        List<ModelField> newFields = newModel.getFields() != null ? newModel.getFields() : Collections.emptyList();

        Map<String, ModelField> oldFieldMap = oldFields.stream()
                .collect(Collectors.toMap(ModelField::getFieldName, f -> f, (a, b) -> a));
        Map<String, ModelField> newFieldMap = newFields.stream()
                .collect(Collectors.toMap(ModelField::getFieldName, f -> f, (a, b) -> a));

        Set<String> allFieldNames = new HashSet<>();
        allFieldNames.addAll(oldFieldMap.keySet());
        allFieldNames.addAll(newFieldMap.keySet());

        for (String fieldName : allFieldNames) {
            ModelField oldField = oldFieldMap.get(fieldName);
            ModelField newField = newFieldMap.get(fieldName);

            if (oldField == null && newField != null) {
                VersionCompareResult.FieldChange change = new VersionCompareResult.FieldChange();
                change.setFieldName(fieldName);
                result.getAddedFields().add(change);
            } else if (oldField != null && newField == null) {
                VersionCompareResult.FieldChange change = new VersionCompareResult.FieldChange();
                change.setFieldName(fieldName);
                result.getDeletedFields().add(change);
            } else {
                List<String> changedProperties = compareFieldProperties(oldField, newField);
                if (!changedProperties.isEmpty()) {
                    VersionCompareResult.FieldChange change = new VersionCompareResult.FieldChange();
                    change.setFieldName(fieldName);
                    change.setChangedProperties(changedProperties);
                    result.getModifiedFields().add(change);
                }
            }
        }
    }

    private List<String> compareFieldProperties(ModelField oldField, ModelField newField) {
        List<String> changedProperties = new ArrayList<>();

        if (!Objects.equals(oldField.getFieldType(), newField.getFieldType())) {
            changedProperties.add("fieldType");
        }
        if (!Objects.equals(oldField.getColumnName(), newField.getColumnName())) {
            changedProperties.add("columnName");
        }
        if (!Objects.equals(oldField.getJavaType(), newField.getJavaType())) {
            changedProperties.add("javaType");
        }
        if (!Objects.equals(oldField.getJdbcType(), newField.getJdbcType())) {
            changedProperties.add("jdbcType");
        }
        if (!Objects.equals(oldField.getLength(), newField.getLength())) {
            changedProperties.add("length");
        }
        if (!Objects.equals(oldField.getPrecision(), newField.getPrecision())) {
            changedProperties.add("precision");
        }
        if (!Objects.equals(oldField.getScale(), newField.getScale())) {
            changedProperties.add("scale");
        }
        if (!Objects.equals(oldField.getDefaultValue(), newField.getDefaultValue())) {
            changedProperties.add("defaultValue");
        }
        if (!Objects.equals(oldField.getFieldComment(), newField.getFieldComment())) {
            changedProperties.add("fieldComment");
        }
        if (!Objects.equals(oldField.getIsRequired(), newField.getIsRequired())) {
            changedProperties.add("isRequired");
        }
        if (!Objects.equals(oldField.getIsPrimary(), newField.getIsPrimary())) {
            changedProperties.add("isPrimary");
        }
        if (!Objects.equals(oldField.getIsUnique(), newField.getIsUnique())) {
            changedProperties.add("isUnique");
        }
        if (!Objects.equals(oldField.getIsAutoIncrement(), newField.getIsAutoIncrement())) {
            changedProperties.add("isAutoIncrement");
        }
        if (!Objects.equals(oldField.getIsLogicDelete(), newField.getIsLogicDelete())) {
            changedProperties.add("isLogicDelete");
        }
        if (!Objects.equals(oldField.getIsVersion(), newField.getIsVersion())) {
            changedProperties.add("isVersion");
        }
        if (!Objects.equals(oldField.getIsTenant(), newField.getIsTenant())) {
            changedProperties.add("isTenant");
        }

        return changedProperties;
    }

    private void compareIndexes(DataModel oldModel, DataModel newModel, VersionCompareResult result) {
        List<ModelIndex> oldIndexes = oldModel.getIndexes() != null ? oldModel.getIndexes() : Collections.emptyList();
        List<ModelIndex> newIndexes = newModel.getIndexes() != null ? newModel.getIndexes() : Collections.emptyList();

        Map<String, ModelIndex> oldIndexMap = oldIndexes.stream()
                .collect(Collectors.toMap(ModelIndex::getIndexName, i -> i, (a, b) -> a));
        Map<String, ModelIndex> newIndexMap = newIndexes.stream()
                .collect(Collectors.toMap(ModelIndex::getIndexName, i -> i, (a, b) -> a));

        Set<String> allIndexNames = new HashSet<>();
        allIndexNames.addAll(oldIndexMap.keySet());
        allIndexNames.addAll(newIndexMap.keySet());

        for (String indexName : allIndexNames) {
            ModelIndex oldIndex = oldIndexMap.get(indexName);
            ModelIndex newIndex = newIndexMap.get(indexName);

            if (oldIndex == null && newIndex != null) {
                VersionCompareResult.IndexChange change = new VersionCompareResult.IndexChange();
                change.setIndexName(indexName);
                result.getAddedIndexes().add(change);
            } else if (oldIndex != null && newIndex == null) {
                VersionCompareResult.IndexChange change = new VersionCompareResult.IndexChange();
                change.setIndexName(indexName);
                result.getDeletedIndexes().add(change);
            } else {
                if (compareIndexProperties(oldIndex, newIndex)) {
                    VersionCompareResult.IndexChange change = new VersionCompareResult.IndexChange();
                    change.setIndexName(indexName);
                    change.setChangedProperties(Arrays.asList("indexType", "indexFields", "indexComment"));
                    result.getModifiedIndexes().add(change);
                }
            }
        }
    }

    private boolean compareIndexProperties(ModelIndex oldIndex, ModelIndex newIndex) {
        if (!Objects.equals(oldIndex.getIndexType(), newIndex.getIndexType())) {
            return true;
        }
        if (!Objects.equals(oldIndex.getIndexFields(), newIndex.getIndexFields())) {
            return true;
        }
        if (!Objects.equals(oldIndex.getIndexComment(), newIndex.getIndexComment())) {
            return true;
        }
        return false;
    }

    private boolean isCompatibleChange(VersionCompareResult result) {
        boolean compatible = true;

        if (!result.getDeletedFields().isEmpty()) {
            compatible = false;
            result.getWarnings().add("存在字段删除，可能导致数据丢失");
        }

        for (VersionCompareResult.FieldChange modifiedField : result.getModifiedFields()) {
            if (modifiedField.getChangedProperties().contains("isPrimary")) {
                compatible = false;
                result.getWarnings().add("主键字段发生变更");
            }
            if (modifiedField.getChangedProperties().contains("fieldType")) {
                result.getWarnings().add("字段 [" + modifiedField.getFieldName() + "] 类型发生变更，需评估数据兼容性");
            }
            if (modifiedField.getChangedProperties().contains("length")) {
                result.getWarnings().add("字段 [" + modifiedField.getFieldName() + "] 长度发生变更");
            }
        }

        if (result.getModelChangedProperties().contains("primaryKeyStrategy")) {
            compatible = false;
            result.getWarnings().add("主键策略发生变更");
        }

        return compatible;
    }
}
