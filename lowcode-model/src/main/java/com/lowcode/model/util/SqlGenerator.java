package com.lowcode.model.util;

import com.lowcode.common.enums.DbTypeEnum;
import com.lowcode.common.enums.FieldTypeEnum;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.ModelField;
import com.lowcode.model.entity.ModelIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SqlGenerator {

    public static String generateCreateTableSql(DataModel model, String dbType) {
        StringBuilder sql = new StringBuilder();
        DbTypeEnum dbTypeEnum = DbTypeEnum.getByCode(dbType);

        sql.append("CREATE TABLE ").append(quoteIdentifier(model.getTableName(), dbTypeEnum)).append(" (\n");

        List<String> columnDefs = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        List<String> uniqueKeys = new ArrayList<>();
        List<String> indexDefs = new ArrayList<>();

        for (ModelField field : model.getFields()) {
            columnDefs.add(generateColumnDefinition(field, dbTypeEnum));

            if (field.getIsPrimary() != null && field.getIsPrimary() == 1) {
                primaryKeys.add(quoteIdentifier(field.getColumnName(), dbTypeEnum));
            }

            if (field.getIsUnique() != null && field.getIsUnique() == 1) {
                String ukName = "uk_" + model.getTableName() + "_" + field.getColumnName();
                uniqueKeys.add("UNIQUE KEY " + quoteIdentifier(ukName, dbTypeEnum) +
                        " (" + quoteIdentifier(field.getColumnName(), dbTypeEnum) + ")");
            }

            if (field.getIsIndex() != null && field.getIsIndex() == 1 &&
                    (field.getIsPrimary() == null || field.getIsPrimary() == 0) &&
                    (field.getIsUnique() == null || field.getIsUnique() == 0)) {
                String idxName = "idx_" + model.getTableName() + "_" + field.getColumnName();
                indexDefs.add("INDEX " + quoteIdentifier(idxName, dbTypeEnum) +
                        " (" + quoteIdentifier(field.getColumnName(), dbTypeEnum) + ")");
            }
        }

        for (String colDef : columnDefs) {
            sql.append("  ").append(colDef).append(",\n");
        }

        if (!primaryKeys.isEmpty()) {
            sql.append("  PRIMARY KEY (").append(String.join(", ", primaryKeys)).append("),\n");
        }

        for (String uk : uniqueKeys) {
            sql.append("  ").append(uk).append(",\n");
        }

        if (model.getIndexes() != null) {
            for (ModelIndex index : model.getIndexes()) {
                String idxSql = generateIndexDefinition(model.getTableName(), index, dbTypeEnum);
                sql.append("  ").append(idxSql).append(",\n");
            }
        }

        for (String idx : indexDefs) {
            sql.append("  ").append(idx).append(",\n");
        }

        if (sql.charAt(sql.length() - 2) == ',') {
            sql.delete(sql.length() - 2, sql.length());
            sql.append("\n");
        }

        sql.append(")");

        if (dbTypeEnum == DbTypeEnum.MYSQL) {
            sql.append(" ENGINE=").append(model.getTableEngine() != null ? model.getTableEngine() : "InnoDB");
            sql.append(" DEFAULT CHARSET=").append(model.getTableCharset() != null ? model.getTableCharset() : "utf8mb4");
        }

        if (model.getModelDesc() != null && !model.getModelDesc().isEmpty()) {
            sql.append(" COMMENT='").append(escapeComment(model.getModelDesc())).append("'");
        }

        sql.append(";");
        return sql.toString();
    }

    public static String generateDropTableSql(String tableName, String dbType) {
        DbTypeEnum dbTypeEnum = DbTypeEnum.getByCode(dbType);
        return "DROP TABLE IF EXISTS " + quoteIdentifier(tableName, dbTypeEnum) + ";";
    }

    public static List<String> generateAlterTableSql(DataModel oldModel, DataModel newModel, String dbType) {
        List<String> sqlList = new ArrayList<>();
        DbTypeEnum dbTypeEnum = DbTypeEnum.getByCode(dbType);
        String tableName = quoteIdentifier(newModel.getTableName(), dbTypeEnum);

        if (oldModel == null) {
            sqlList.add(generateCreateTableSql(newModel, dbType));
            return sqlList;
        }

        if (!oldModel.getTableName().equals(newModel.getTableName())) {
            sqlList.add("ALTER TABLE " + quoteIdentifier(oldModel.getTableName(), dbTypeEnum) +
                    " RENAME TO " + tableName + ";");
        }

        List<ModelField> oldFields = oldModel.getFields();
        List<ModelField> newFields = newModel.getFields();

        for (ModelField newField : newFields) {
            ModelField oldField = findFieldByColumnName(oldFields, newField.getColumnName());
            if (oldField == null) {
                sqlList.add("ALTER TABLE " + tableName + " ADD COLUMN " +
                        generateColumnDefinition(newField, dbTypeEnum) + ";");
            } else if (!isFieldEqual(oldField, newField)) {
                sqlList.add("ALTER TABLE " + tableName + " MODIFY COLUMN " +
                        generateColumnDefinition(newField, dbTypeEnum) + ";");
            }
        }

        for (ModelField oldField : oldFields) {
            ModelField newField = findFieldByColumnName(newFields, oldField.getColumnName());
            if (newField == null) {
                sqlList.add("ALTER TABLE " + tableName + " DROP COLUMN " +
                        quoteIdentifier(oldField.getColumnName(), dbTypeEnum) + ";");
            }
        }

        return sqlList;
    }

    public static String generateColumnDefinition(ModelField field, DbTypeEnum dbType) {
        StringBuilder colDef = new StringBuilder();
        colDef.append(quoteIdentifier(field.getColumnName(), dbType)).append(" ");

        FieldTypeEnum fieldType = FieldTypeEnum.getByCode(field.getFieldType());
        String jdbcType = field.getJdbcType() != null ? field.getJdbcType() : fieldType.getJdbcType();

        if (field.getLength() != null && field.getLength() > 0) {
            if (field.getScale() != null && field.getScale() > 0) {
                jdbcType = jdbcType + "(" + field.getLength() + "," + field.getScale() + ")";
            } else {
                jdbcType = jdbcType + "(" + field.getLength() + ")";
            }
        }

        colDef.append(jdbcType);

        if (field.getIsRequired() != null && field.getIsRequired() == 1) {
            colDef.append(" NOT NULL");
        } else {
            colDef.append(" NULL");
        }

        if (field.getIsAutoIncrement() != null && field.getIsAutoIncrement() == 1) {
            if (dbType == DbTypeEnum.MYSQL || dbType == DbTypeEnum.DM) {
                colDef.append(" AUTO_INCREMENT");
            } else if (dbType == DbTypeEnum.POSTGRESQL) {
                colDef.append(" GENERATED ALWAYS AS IDENTITY");
            }
        }

        if (field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) {
            colDef.append(" DEFAULT '").append(escapeValue(field.getDefaultValue())).append("'");
        }

        if (field.getFieldComment() != null && !field.getFieldComment().isEmpty()) {
            colDef.append(" COMMENT '").append(escapeComment(field.getFieldComment())).append("'");
        }

        return colDef.toString();
    }

    public static String generateIndexDefinition(String tableName, ModelIndex index, DbTypeEnum dbType) {
        StringBuilder idxDef = new StringBuilder();

        if ("UNIQUE".equals(index.getIndexType())) {
            idxDef.append("UNIQUE KEY ");
        } else if ("FULLTEXT".equals(index.getIndexType())) {
            idxDef.append("FULLTEXT KEY ");
        } else {
            idxDef.append("KEY ");
        }

        idxDef.append(quoteIdentifier(index.getIndexName(), dbType)).append(" (");
        idxDef.append(index.getIndexFields()).append(")");

        if (index.getIndexComment() != null && !index.getIndexComment().isEmpty()) {
            idxDef.append(" COMMENT '").append(escapeComment(index.getIndexComment())).append("'");
        }

        return idxDef.toString();
    }

    public static String generateInsertSql(String tableName, List<String> columns, List<Object> values, String dbType) {
        DbTypeEnum dbTypeEnum = DbTypeEnum.getByCode(dbType);
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(quoteIdentifier(tableName, dbTypeEnum)).append(" (");

        sql.append(columns.stream()
                .map(col -> quoteIdentifier(col, dbTypeEnum))
                .collect(Collectors.joining(", ")));

        sql.append(") VALUES (");

        sql.append(values.stream()
                .map(SqlGenerator::formatValue)
                .collect(Collectors.joining(", ")));

        sql.append(");");
        return sql.toString();
    }

    public static String generateUpdateSql(String tableName, List<String> updateColumns, List<Object> updateValues,
                                           String whereColumn, Object whereValue, String dbType) {
        DbTypeEnum dbTypeEnum = DbTypeEnum.getByCode(dbType);
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(quoteIdentifier(tableName, dbTypeEnum)).append(" SET ");

        List<String> setClauses = new ArrayList<>();
        for (int i = 0; i < updateColumns.size(); i++) {
            setClauses.add(quoteIdentifier(updateColumns.get(i), dbTypeEnum) + " = " + formatValue(updateValues.get(i)));
        }
        sql.append(String.join(", ", setClauses));

        sql.append(" WHERE ").append(quoteIdentifier(whereColumn, dbTypeEnum)).append(" = ").append(formatValue(whereValue));
        sql.append(";");
        return sql.toString();
    }

    public static String generateDeleteSql(String tableName, String whereColumn, Object whereValue, String dbType) {
        DbTypeEnum dbTypeEnum = DbTypeEnum.getByCode(dbType);
        return "DELETE FROM " + quoteIdentifier(tableName, dbTypeEnum) +
                " WHERE " + quoteIdentifier(whereColumn, dbTypeEnum) + " = " + formatValue(whereValue) + ";";
    }

    public static String generateSelectSql(String tableName, List<String> columns, String whereClause, String orderBy,
                                           Integer limit, String dbType) {
        DbTypeEnum dbTypeEnum = DbTypeEnum.getByCode(dbType);
        StringBuilder sql = new StringBuilder("SELECT ");

        if (columns == null || columns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(columns.stream()
                    .map(col -> quoteIdentifier(col, dbTypeEnum))
                    .collect(Collectors.joining(", ")));
        }

        sql.append(" FROM ").append(quoteIdentifier(tableName, dbTypeEnum));

        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }

        if (orderBy != null && !orderBy.isEmpty()) {
            sql.append(" ORDER BY ").append(orderBy);
        }

        if (limit != null && limit > 0) {
            if (dbTypeEnum == DbTypeEnum.MYSQL || dbTypeEnum == DbTypeEnum.DM) {
                sql.append(" LIMIT ").append(limit);
            } else if (dbTypeEnum == DbTypeEnum.POSTGRESQL) {
                sql.append(" LIMIT ").append(limit);
            }
        }

        sql.append(";");
        return sql.toString();
    }

    private static String quoteIdentifier(String identifier, DbTypeEnum dbType) {
        if (identifier == null) return null;
        if (dbType == DbTypeEnum.MYSQL || dbType == DbTypeEnum.DM) {
            return "`" + identifier + "`";
        } else if (dbType == DbTypeEnum.POSTGRESQL) {
            return "\"" + identifier + "\"";
        }
        return identifier;
    }

    private static String escapeValue(Object value) {
        if (value == null) return "NULL";
        String str = value.toString();
        return str.replace("'", "''");
    }

    private static String escapeComment(String comment) {
        return comment.replace("'", "\\'").replace("\n", " ");
    }

    private static String formatValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "'" + escapeValue(value) + "'";
    }

    private static ModelField findFieldByColumnName(List<ModelField> fields, String columnName) {
        if (fields == null || columnName == null) return null;
        return fields.stream()
                .filter(f -> columnName.equals(f.getColumnName()))
                .findFirst()
                .orElse(null);
    }

    private static boolean isFieldEqual(ModelField oldField, ModelField newField) {
        if (!equals(oldField.getFieldType(), newField.getFieldType())) return false;
        if (!equals(oldField.getJavaType(), newField.getJavaType())) return false;
        if (!equals(oldField.getJdbcType(), newField.getJdbcType())) return false;
        if (!equals(oldField.getLength(), newField.getLength())) return false;
        if (!equals(oldField.getScale(), newField.getScale())) return false;
        if (!equals(oldField.getIsRequired(), newField.getIsRequired())) return false;
        if (!equals(oldField.getIsPrimary(), newField.getIsPrimary())) return false;
        if (!equals(oldField.getIsUnique(), newField.getIsUnique())) return false;
        if (!equals(oldField.getIsAutoIncrement(), newField.getIsAutoIncrement())) return false;
        if (!equals(oldField.getDefaultValue(), newField.getDefaultValue())) return false;
        if (!equals(oldField.getFieldComment(), newField.getFieldComment())) return false;
        return true;
    }

    private static boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
