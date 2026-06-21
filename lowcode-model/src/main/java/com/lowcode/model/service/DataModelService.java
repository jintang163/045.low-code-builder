package com.lowcode.model.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.enums.FieldTypeEnum;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.util.JdbcUtil;
import com.lowcode.model.entity.*;
import com.lowcode.model.mapper.DataModelMapper;
import com.lowcode.model.mapper.ModelFieldMapper;
import com.lowcode.model.mapper.ModelIndexMapper;
import com.lowcode.model.util.SqlGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataModelService extends ServiceImpl<DataModelMapper, DataModel> {

    @Autowired
    private ModelFieldMapper fieldMapper;

    @Autowired
    private ModelIndexMapper indexMapper;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private SqlMigrationService migrationService;

    @Autowired
    private ModelRelationService relationService;

    @Autowired(required = false)
    private Object versionService;

    public DataModel getModelDetail(Long id) {
        DataModel model = getById(id);
        if (model == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        LambdaQueryWrapper<ModelField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.eq(ModelField::getModelId, id);
        fieldWrapper.orderByAsc(ModelField::getSortOrder);
        List<ModelField> fields = fieldMapper.selectList(fieldWrapper);
        model.setFields(fields);

        LambdaQueryWrapper<ModelIndex> indexWrapper = new LambdaQueryWrapper<>();
        indexWrapper.eq(ModelIndex::getModelId, id);
        List<ModelIndex> indexes = indexMapper.selectList(indexWrapper);
        model.setIndexes(indexes);

        return model;
    }

    public List<DataModel> getModelList(Long appId) {
        LambdaQueryWrapper<DataModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModel::getAppId, appId);
        wrapper.orderByDesc(DataModel::getCreatedTime);
        List<DataModel> models = list(wrapper);

        for (DataModel model : models) {
            LambdaQueryWrapper<ModelField> fieldWrapper = new LambdaQueryWrapper<>();
            fieldWrapper.eq(ModelField::getModelId, model.getId());
            fieldWrapper.orderByAsc(ModelField::getSortOrder);
            List<ModelField> fields = fieldMapper.selectList(fieldWrapper);
            model.setFields(fields);
        }

        return models;
    }

    @Transactional(rollbackFor = Exception.class)
    public DataModel saveModel(DataModel model) {
        LambdaQueryWrapper<DataModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModel::getTableName, model.getTableName());
        wrapper.eq(DataModel::getDataSourceId, model.getDataSourceId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.MODEL_EXISTS);
        }

        model.setEntityName(generateEntityName(model.getTableName()));
        save(model);

        if (model.getFields() != null) {
            for (ModelField field : model.getFields()) {
                field.setModelId(model.getId());
                setFieldTypeInfo(field);
                fieldMapper.insert(field);
            }
        }

        if (model.getIndexes() != null) {
            for (ModelIndex index : model.getIndexes()) {
                index.setModelId(model.getId());
                indexMapper.insert(index);
            }
        }

        return getModelDetail(model.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public DataModel updateModel(DataModel newModel) {
        DataModel oldModel = getModelDetail(newModel.getId());
        if (oldModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        newModel.setEntityName(generateEntityName(newModel.getTableName()));
        updateById(newModel);

        Set<Long> existingFieldIds = oldModel.getFields().stream()
                .map(ModelField::getId)
                .collect(Collectors.toSet());

        Set<Long> newFieldIds = newModel.getFields().stream()
                .map(ModelField::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long id : existingFieldIds) {
            if (!newFieldIds.contains(id)) {
                fieldMapper.deleteById(id);
            }
        }

        if (newModel.getFields() != null) {
            int sortOrder = 0;
            for (ModelField field : newModel.getFields()) {
                field.setModelId(newModel.getId());
                field.setSortOrder(sortOrder++);
                setFieldTypeInfo(field);
                if (field.getId() != null) {
                    fieldMapper.updateById(field);
                } else {
                    fieldMapper.insert(field);
                }
            }
        }

        if (oldModel.getIndexes() != null) {
            for (ModelIndex index : oldModel.getIndexes()) {
                indexMapper.deleteById(index.getId());
            }
        }

        if (newModel.getIndexes() != null) {
            for (ModelIndex index : newModel.getIndexes()) {
                index.setModelId(newModel.getId());
                indexMapper.insert(index);
            }
        }

        if (versionService != null) {
            try {
                java.lang.reflect.Method method = versionService.getClass().getMethod("createSnapshot", Long.class, String.class, Integer.class);
                method.invoke(versionService, newModel.getId(), "模型更新", 2);
            } catch (Exception e) {
                log.warn("创建版本快照失败", e);
            }
        }

        return getModelDetail(newModel.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        DataModel model = getById(id);
        if (model == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        LambdaQueryWrapper<ModelField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.eq(ModelField::getModelId, id);
        fieldMapper.delete(fieldWrapper);

        LambdaQueryWrapper<ModelIndex> indexWrapper = new LambdaQueryWrapper<>();
        indexWrapper.eq(ModelIndex::getModelId, id);
        indexMapper.delete(indexWrapper);

        removeById(id);
    }

    public String generateCreateSql(Long modelId) {
        DataModel model = getModelDetail(modelId);
        DataSource dataSource = dataSourceService.getById(model.getDataSourceId());
        return SqlGenerator.generateCreateTableSql(model, dataSource.getDbType());
    }

    public String generateDropSql(Long modelId) {
        DataModel model = getById(modelId);
        DataSource dataSource = dataSourceService.getById(model.getDataSourceId());
        return SqlGenerator.generateDropTableSql(model.getTableName(), dataSource.getDbType());
    }

    @Transactional(rollbackFor = Exception.class)
    public SqlMigration publishModel(Long modelId) {
        DataModel newModel = getModelDetail(modelId);
        DataSource dataSource = dataSourceService.getById(newModel.getDataSourceId());

        DataModel oldModel = null;
        if (newModel.getStatus() != null && newModel.getStatus() == 1) {
            oldModel = getModelDetail(modelId);
        }

        List<String> sqlList = SqlGenerator.generateAlterTableSql(oldModel, newModel, dataSource.getDbType());
        String sqlContent = String.join("\n\n", sqlList);

        Map<String, Object> changes = new HashMap<>();
        changes.put("oldModel", oldModel);
        changes.put("newModel", newModel);
        changes.put("sqlList", sqlList);

        String version = "v" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss"));

        SqlMigration migration = new SqlMigration();
        migration.setAppId(newModel.getAppId());
        migration.setDataSourceId(newModel.getDataSourceId());
        migration.setVersion(version);
        migration.setMigrationName(newModel.getTableName() + "_" + version);
        migration.setSqlContent(sqlContent);
        migration.setModelChanges(JSON.toJSONString(changes));
        migration.setStatus(0);

        migrationService.save(migration);

        newModel.setStatus(1);
        updateById(newModel);

        return migration;
    }

    public Map<String, Object> getErDiagram(Long appId) {
        Map<String, Object> result = new HashMap<>();

        List<DataModel> models = getModelList(appId);
        List<ModelRelation> relations = relationService.getRelationsByAppId(appId);

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (DataModel model : models) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", model.getId());
            node.put("name", model.getModelName());
            node.put("tableName", model.getTableName());
            node.put("x", model.getPositionX());
            node.put("y", model.getPositionY());

            List<Map<String, Object>> fields = new ArrayList<>();
            for (ModelField field : model.getFields()) {
                Map<String, Object> fieldMap = new HashMap<>();
                fieldMap.put("id", field.getId());
                fieldMap.put("name", field.getFieldName());
                fieldMap.put("columnName", field.getColumnName());
                fieldMap.put("type", field.getFieldType());
                fieldMap.put("isPrimary", field.getIsPrimary());
                fieldMap.put("isRequired", field.getIsRequired());
                fields.add(fieldMap);
            }
            node.put("fields", fields);
            nodes.add(node);
        }

        List<Map<String, Object>> edges = new ArrayList<>();
        for (ModelRelation relation : relations) {
            Map<String, Object> edge = new HashMap<>();
            edge.put("id", relation.getId());
            edge.put("source", relation.getSourceModelId());
            edge.put("target", relation.getTargetModelId());
            edge.put("sourceField", relation.getSourceFieldId());
            edge.put("targetField", relation.getTargetFieldId());
            edge.put("relationType", relation.getRelationType());
            edges.add(edge);
        }

        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public DataModel importFromTable(Long dataSourceId, String tableName) {
        DataSource dataSource = dataSourceService.getById(dataSourceId);
        Connection conn = null;
        try {
            conn = dataSourceService.getConnection(dataSourceId);

            LambdaQueryWrapper<DataModel> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DataModel::getTableName, tableName);
            wrapper.eq(DataModel::getDataSourceId, dataSourceId);
            if (count(wrapper) > 0) {
                throw new BusinessException(ErrorCode.MODEL_EXISTS);
            }

            DataModel model = new DataModel();
            model.setDataSourceId(dataSourceId);
            model.setTableName(tableName);
            model.setModelName(tableName);
            model.setEntityName(generateEntityName(tableName));
            model.setTableCharset("utf8mb4");
            model.setTableEngine("InnoDB");
            model.setStatus(1);
            save(model);

            String columnsSql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, " +
                    "NUMERIC_SCALE, IS_NULLABLE, COLUMN_KEY, COLUMN_DEFAULT, COLUMN_COMMENT, EXTRA " +
                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";

            List<Map<String, Object>> columns = JdbcUtil.executeQuery(conn,
                    "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, " +
                            "NUMERIC_SCALE, IS_NULLABLE, COLUMN_KEY, COLUMN_DEFAULT, COLUMN_COMMENT, EXTRA " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_NAME = '" + tableName + "' ORDER BY ORDINAL_POSITION");

            int sortOrder = 0;
            for (Map<String, Object> col : columns) {
                ModelField field = new ModelField();
                field.setModelId(model.getId());
                field.setColumnName((String) col.get("COLUMN_NAME"));
                field.setFieldName((String) col.get("COLUMN_NAME"));
                field.setFieldComment((String) col.get("COLUMN_NAME"));

                String dataType = ((String) col.get("DATA_TYPE")).toUpperCase();
                FieldTypeEnum fieldType = mapDataTypeToFieldType(dataType);
                field.setFieldType(fieldType.getCode());
                field.setJavaType(fieldType.getJavaType());
                field.setJdbcType(fieldType.getJdbcType());

                if (col.get("CHARACTER_MAXIMUM_LENGTH") != null) {
                    field.setLength(((Number) col.get("CHARACTER_MAXIMUM_LENGTH")).intValue());
                }
                if (col.get("NUMERIC_PRECISION") != null) {
                    field.setPrecision(((Number) col.get("NUMERIC_PRECISION")).intValue());
                }
                if (col.get("NUMERIC_SCALE") != null) {
                    field.setScale(((Number) col.get("NUMERIC_SCALE")).intValue());
                }

                field.setIsRequired("NO".equals(col.get("IS_NULLABLE")) ? 1 : 0);
                field.setIsPrimary("PRI".equals(col.get("COLUMN_KEY")) ? 1 : 0);
                field.setIsUnique("UNI".equals(col.get("COLUMN_KEY")) ? 1 : 0);
                field.setIsAutoIncrement(col.get("EXTRA") != null &&
                        ((String) col.get("EXTRA")).contains("auto_increment") ? 1 : 0);

                if (col.get("COLUMN_DEFAULT") != null) {
                    field.setDefaultValue(col.get("COLUMN_DEFAULT").toString());
                }

                if (col.get("COLUMN_COMMENT") != null && !((String) col.get("COLUMN_COMMENT")).isEmpty()) {
                    field.setFieldComment((String) col.get("COLUMN_COMMENT"));
                }

                field.setSortOrder(sortOrder++);
                fieldMapper.insert(field);
            }

            return getModelDetail(model.getId());
        } finally {
            JdbcUtil.close(conn, null, null);
        }
    }

    private String generateEntityName(String tableName) {
        String[] parts = tableName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }

    private void setFieldTypeInfo(ModelField field) {
        FieldTypeEnum typeEnum = FieldTypeEnum.getByCode(field.getFieldType());
        if (field.getJavaType() == null || field.getJavaType().isEmpty()) {
            field.setJavaType(typeEnum.getJavaType());
        }
        if (field.getJdbcType() == null || field.getJdbcType().isEmpty()) {
            field.setJdbcType(typeEnum.getJdbcType());
        }
        if (field.getLength() == null && typeEnum.getDefaultLengths() != null && !typeEnum.getDefaultLengths().isEmpty()) {
            field.setLength(typeEnum.getDefaultLengths().get(0));
        }
    }

    private FieldTypeEnum mapDataTypeToFieldType(String dataType) {
        switch (dataType) {
            case "VARCHAR":
            case "CHAR":
            case "NVARCHAR":
            case "TEXT":
            case "LONGTEXT":
            case "CLOB":
                return FieldTypeEnum.STRING;
            case "INT":
            case "INTEGER":
            case "TINYINT":
            case "SMALLINT":
            case "MEDIUMINT":
                return FieldTypeEnum.INTEGER;
            case "BIGINT":
                return FieldTypeEnum.NUMBER;
            case "DECIMAL":
            case "NUMERIC":
            case "FLOAT":
            case "DOUBLE":
            case "REAL":
                return FieldTypeEnum.DECIMAL;
            case "DATE":
                return FieldTypeEnum.DATE;
            case "DATETIME":
            case "TIMESTAMP":
                return FieldTypeEnum.DATETIME;
            case "TIME":
                return FieldTypeEnum.TIME;
            case "BOOLEAN":
            case "BIT":
                return FieldTypeEnum.BOOLEAN;
            case "JSON":
                return FieldTypeEnum.JSON;
            case "BLOB":
            case "LONGBLOB":
            case "BINARY":
            case "VARBINARY":
                return FieldTypeEnum.BLOB;
            case "ENUM":
                return FieldTypeEnum.ENUM;
            default:
                return FieldTypeEnum.STRING;
        }
    }
}
