package com.lowcode.common.enums;

import lombok.Getter;

@Getter
public enum RelationTypeEnum {

    ONE_TO_ONE("ONE_TO_ONE", "一对一"),
    ONE_TO_MANY("ONE_TO_MANY", "一对多"),
    MANY_TO_ONE("MANY_TO_ONE", "多对一"),
    MANY_TO_MANY("MANY_TO_MANY", "多对多");

    private final String code;
    private final String name;

    RelationTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static RelationTypeEnum getByCode(String code) {
        for (RelationTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return ONE_TO_MANY;
    }
}
