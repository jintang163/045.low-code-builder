package com.lowcode.model.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.util.JdbcUtil;
import com.lowcode.model.datasource.DynamicDataSourceManager;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.entity.ModelField;
import com.lowcode.model.entity.ModelRelation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubFormService {

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private ModelRelationService modelRelationService;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    public List<Map<String, Object>> querySubFormList(Long mainModelId, Object mainId, Long subModelId, String foreignKeyField) {
        DataModel subModel = dataModelService.getModelDetail(subModelId);
        if (subModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "子表数据模型不存在");
        }

        String fkColumn = getForeignKeyColumn(subModel, foreignKeyField);

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(subModel.getTableName())
                .append(" WHERE ").append(fkColumn).append(" = ?");

        List<Map<String, Object>> result = executeQuery(subModel.getDataSourceId(), sql.toString(),
                Collections.singletonList(mainId));
        return result;
    }

    public IPage<Map<String, Object>> querySubFormPage(Long mainModelId, Object mainId, Long subModelId,
                                                        String foreignKeyField, int current, int size) {
        DataModel subModel = dataModelService.getModelDetail(subModelId);
        if (subModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "子表数据模型不存在");
        }

        String fkColumn = getForeignKeyColumn(subModel, foreignKeyField);

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM ").append(subModel.getTableName())
                .append(" WHERE ").append(fkColumn).append(" = ?");

        StringBuilder dataSql = new StringBuilder("SELECT * FROM ").append(subModel.getTableName())
                .append(" WHERE ").append(fkColumn).append(" = ?")
                .append(" LIMIT ").append((current - 1) * size).append(", ").append(size);

        List<Object> params = Collections.singletonList(mainId);

        long total = executeCount(subModel.getDataSourceId(), countSql.toString(), params);
        List<Map<String, Object>> records = executeQuery(subModel.getDataSourceId(), dataSql.toString(), params);

        Page<Map<String, Object>> page = new Page<>(current, size, total);
        page.setRecords(records);
        return page;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveSubFormData(Long mainModelId, Object mainId, Long subModelId,
                                                String foreignKeyField, Map<String, Object> data) {
        DataModel subModel = dataModelService.getModelDetail(subModelId);
        if (subModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "子表数据模型不存在");
        }

        String fkColumn = getForeignKeyColumn(subModel, foreignKeyField);
        data.put(fkColumn, mainId);

        ModelField pkField = getPrimaryKeyField(subModel);
        Object idValue = data.get(pkField.getFieldName());
        if (idValue == null) {
            idValue = data.get(pkField.getColumnName());
        }

        if (idValue != null && !idValue.toString().isEmpty()) {
            updateById(subModel, data, pkField);
        } else {
            idValue = insert(subModel, data, pkField);
            data.put(pkField.getFieldName(), idValue);
        }

        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> batchSaveSubFormData(Long mainModelId, Object mainId, Long subModelId,
                                                           String foreignKeyField, List<Map<String, Object>> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> data : dataList) {
            result.add(saveSubFormData(mainModelId, mainId, subModelId, foreignKeyField, data));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSubFormData(Long subModelId, Object id) {
        DataModel subModel = dataModelService.getModelDetail(subModelId);
        if (subModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "子表数据模型不存在");
        }

        ModelField pkField = getPrimaryKeyField(subModel);
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(subModel.getTableName())
                .append(" WHERE ").append(pkField.getColumnName()).append(" = ?");

        executeUpdate(subModel.getDataSourceId(), sql.toString(), Collections.singletonList(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSubFormByForeignKey(Long subModelId, String foreignKeyField, Object foreignKeyValue) {
        DataModel subModel = dataModelService.getModelDetail(subModelId);
        if (subModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "子表数据模型不存在");
        }

        String fkColumn = getForeignKeyColumn(subModel, foreignKeyField);
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(subModel.getTableName())
                .append(" WHERE ").append(fkColumn).append(" = ?");

        executeUpdate(subModel.getDataSourceId(), sql.toString(), Collections.singletonList(foreignKeyValue));
    }

    public Map<String, Object> queryMasterWithSubForms(Long mainModelId, Object mainId,
                                                        List<SubFormConfig> subFormConfigs) {
        DataModel mainModel = dataModelService.getModelDetail(mainModelId);
        if (mainModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "主表数据模型不存在");
        }

        ModelField pkField = getPrimaryKeyField(mainModel);

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(mainModel.getTableName())
                .append(" WHERE ").append(pkField.getColumnName()).append(" = ?");

        List<Map<String, Object>> mainResult = executeQuery(mainModel.getDataSourceId(), sql.toString(),
                Collections.singletonList(mainId));

        if (mainResult.isEmpty()) {
            return null;
        }

        Map<String, Object> result = mainResult.get(0);

        if (subFormConfigs != null && !subFormConfigs.isEmpty()) {
            for (SubFormConfig config : subFormConfigs) {
                List<Map<String, Object>> subData = querySubFormList(mainModelId, mainId,
                        config.getSubModelId(), config.getForeignKeyField());
                result.put(config.getSubFormKey(), subData);
            }
        }

        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveMasterWithSubForms(Long mainModelId, Map<String, Object> masterData,
                                                       List<SubFormData> subFormDataList) {
        DataModel mainModel = dataModelService.getModelDetail(mainModelId);
        if (mainModel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "主表数据模型不存在");
        }

        ModelField pkField = getPrimaryKeyField(mainModel);
        Object mainId = masterData.get(pkField.getFieldName());
        if (mainId == null) {
            mainId = masterData.get(pkField.getColumnName());
        }

        if (mainId != null && !mainId.toString().isEmpty()) {
            updateById(mainModel, masterData, pkField);
        } else {
            mainId = insert(mainModel, masterData, pkField);
            masterData.put(pkField.getFieldName(), mainId);
        }

        if (subFormDataList != null && !subFormDataList.isEmpty()) {
            for (SubFormData subFormData : subFormDataList) {
                if (subFormData.getDataList() != null) {
                    batchSaveSubFormData(mainModelId, mainId, subFormData.getSubModelId(),
                            subFormData.getForeignKeyField(), subFormData.getDataList());
                }
            }
        }

        return masterData;
    }

    private String getForeignKeyColumn(DataModel subModel, String foreignKeyField) {
        if (foreignKeyField == null || foreignKeyField.isEmpty()) {
            throw new BusinessException("外键字段不能为空");
        }

        for (ModelField field : subModel.getFields()) {
            if (foreignKeyField.equals(field.getFieldName()) || foreignKeyField.equals(field.getColumnName())) {
                return field.getColumnName();
            }
        }
        return foreignKeyField;
    }

    private ModelField getPrimaryKeyField(DataModel model) {
        return model.getFields().stream()
                .filter(f -> f.getIsPrimary() != null && f.getIsPrimary() == 1)
                .findFirst()
                .orElse(model.getFields().get(0));
    }

    private Object insert(DataModel model, Map<String, Object> data, ModelField pkField) {
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (ModelField field : model.getFields()) {
            String fieldName = field.getFieldName();
            String columnName = field.getColumnName();

            if (data.containsKey(fieldName)) {
                columns.add(columnName);
                values.add(data.get(fieldName));
                placeholders.add("?");
            } else if (data.containsKey(columnName)) {
                columns.add(columnName);
                values.add(data.get(columnName));
                placeholders.add("?");
            }
        }

        if (columns.isEmpty()) {
            throw new BusinessException("没有可插入的数据");
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(model.getTableName())
                .append(" (").append(String.join(", ", columns)).append(")")
                .append(" VALUES (").append(String.join(", ", placeholders)).append(")");

        return executeInsert(model.getDataSourceId(), sql.toString(), values, pkField);
    }

    private void updateById(DataModel model, Map<String, Object> data, ModelField pkField) {
        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        Object idValue = data.get(pkField.getFieldName());
        if (idValue == null) {
            idValue = data.get(pkField.getColumnName());
        }

        for (ModelField field : model.getFields()) {
            String fieldName = field.getFieldName();
            String columnName = field.getColumnName();

            if (field.getIsPrimary() != null && field.getIsPrimary() == 1) {
                continue;
            }

            if (data.containsKey(fieldName)) {
                setClauses.add(columnName + " = ?");
                values.add(data.get(fieldName));
            } else if (data.containsKey(columnName)) {
                setClauses.add(columnName + " = ?");
                values.add(data.get(columnName));
            }
        }

        if (setClauses.isEmpty()) {
            return;
        }

        values.add(idValue);

        StringBuilder sql = new StringBuilder("UPDATE ").append(model.getTableName())
                .append(" SET ").append(String.join(", ", setClauses))
                .append(" WHERE ").append(pkField.getColumnName()).append(" = ?");

        executeUpdate(model.getDataSourceId(), sql.toString(), values);
    }

    private Object executeInsert(Long dataSourceId, String sql, List<Object> params, ModelField pkField) {
        DataSource ds = dataSourceService.getById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dynamicDataSourceManager.getConnection(dataSourceId, ds);
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            log.debug("执行插入SQL: dataSourceId={}, sql={}, params={}", dataSourceId, sql, params);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new BusinessException("插入失败，没有影响任何行");
            }

            Object generatedId = null;
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getObject(1);
            }

            if (generatedId == null) {
                for (Object param : params) {
                    if (param != null && param.toString().equals(pkField.getFieldName())) {
                        generatedId = param;
                        break;
                    }
                }
            }

            return generatedId;
        } catch (SQLException e) {
            log.error("插入数据执行失败: dataSourceId={}, sql={}, error={}", dataSourceId, sql, e.getMessage());
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            JdbcUtil.close(conn, stmt, rs);
        }
    }

    private int executeUpdate(Long dataSourceId, String sql, List<Object> params) {
        DataSource ds = dataSourceService.getById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dynamicDataSourceManager.getConnection(dataSourceId, ds);
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            log.debug("执行更新SQL: dataSourceId={}, sql={}, params={}", dataSourceId, sql, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("更新数据执行失败: dataSourceId={}, sql={}, error={}", dataSourceId, sql, e.getMessage());
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            JdbcUtil.close(conn, stmt, null);
        }
    }

    private List<Map<String, Object>> executeQuery(Long dataSourceId, String sql, List<Object> params) {
        DataSource ds = dataSourceService.getById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dynamicDataSourceManager.getConnection(dataSourceId, ds);
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            log.debug("执行数据查询SQL: dataSourceId={}, sql={}, params={}", dataSourceId, sql, params);
            rs = stmt.executeQuery();
            return resultSetToList(rs);
        } catch (SQLException e) {
            log.error("数据查询执行失败: dataSourceId={}, sql={}, error={}", dataSourceId, sql, e.getMessage());
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            JdbcUtil.close(conn, stmt, rs);
        }
    }

    private long executeCount(Long dataSourceId, String sql, List<Object> params) {
        DataSource ds = dataSourceService.getById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dynamicDataSourceManager.getConnection(dataSourceId, ds);
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            log.debug("执行计数查询SQL: dataSourceId={}, sql={}, params={}", dataSourceId, sql, params);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            log.error("计数查询执行失败: dataSourceId={}, sql={}, error={}", dataSourceId, sql, e.getMessage());
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            JdbcUtil.close(conn, stmt, rs);
        }
    }

    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnLabel(i), rs.getObject(i));
            }
            result.add(row);
        }
        return result;
    }

    public static class SubFormConfig {
        private String subFormKey;
        private Long subModelId;
        private String foreignKeyField;

        public String getSubFormKey() {
            return subFormKey;
        }

        public void setSubFormKey(String subFormKey) {
            this.subFormKey = subFormKey;
        }

        public Long getSubModelId() {
            return subModelId;
        }

        public void setSubModelId(Long subModelId) {
            this.subModelId = subModelId;
        }

        public String getForeignKeyField() {
            return foreignKeyField;
        }

        public void setForeignKeyField(String foreignKeyField) {
            this.foreignKeyField = foreignKeyField;
        }
    }

    public static class SubFormData {
        private Long subModelId;
        private String foreignKeyField;
        private List<Map<String, Object>> dataList;

        public Long getSubModelId() {
            return subModelId;
        }

        public void setSubModelId(Long subModelId) {
            this.subModelId = subModelId;
        }

        public String getForeignKeyField() {
            return foreignKeyField;
        }

        public void setForeignKeyField(String foreignKeyField) {
            this.foreignKeyField = foreignKeyField;
        }

        public List<Map<String, Object>> getDataList() {
            return dataList;
        }

        public void setDataList(List<Map<String, Object>> dataList) {
            this.dataList = dataList;
        }
    }
}
