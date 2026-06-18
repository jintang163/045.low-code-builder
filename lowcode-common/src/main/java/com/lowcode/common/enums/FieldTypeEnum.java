package com.lowcode.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum FieldTypeEnum {

    STRING("STRING", "文本", "String", "VARCHAR", Arrays.asList(255, 500, 1000, 2000, 4000)),
    TEXT("TEXT", "长文本", "String", "TEXT", null),
    NUMBER("NUMBER", "数字", "Long", "BIGINT", Arrays.asList(3, 5, 10, 15, 20)),
    DECIMAL("DECIMAL", "小数", "BigDecimal", "DECIMAL", Arrays.asList(10, 15, 20, 30)),
    INTEGER("INTEGER", "整数", "Integer", "INT", Arrays.asList(3, 5, 10, 15)),
    DATE("DATE", "日期", "LocalDate", "DATE", null),
    DATETIME("DATETIME", "日期时间", "LocalDateTime", "DATETIME", null),
    TIME("TIME", "时间", "LocalTime", "TIME", null),
    BOOLEAN("BOOLEAN", "布尔", "Boolean", "TINYINT(1)", null),
    ENUM("ENUM", "枚举", "Enum", "VARCHAR(50)", null),
    REFERENCE("REFERENCE", "关联", "Long", "BIGINT", null),
    JSON("JSON", "JSON", "String", "JSON", null),
    BLOB("BLOB", "二进制", "byte[]", "BLOB", null);

    private final String code;
    private final String name;
    private final String javaType;
    private final String jdbcType;
    private final List<Integer> defaultLengths;

    FieldTypeEnum(String code, String name, String javaType, String jdbcType, List<Integer> defaultLengths) {
        this.code = code;
        this.name = name;
        this.javaType = javaType;
        this.jdbcType = jdbcType;
        this.defaultLengths = defaultLengths;
    }

    public static FieldTypeEnum getByCode(String code) {
        for (FieldTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return STRING;
    }
}
