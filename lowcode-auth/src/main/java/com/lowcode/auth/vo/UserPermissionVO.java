package com.lowcode.auth.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserPermissionVO {
    private Long userId;
    private String username;
    private List<String> roles;
    private List<String> permissions;
    private List<Long> accessiblePageIds;
    private Map<String, ComponentPermissionVO> componentPermissions;
    private Map<Long, FieldPermissionVO> fieldPermissions;
    private String rowLevelSqlFilter;

    @Data
    public static class ComponentPermissionVO {
        private String componentId;
        private Boolean visible;
        private Boolean disabled;
    }

    @Data
    public static class FieldPermissionVO {
        private Long fieldId;
        private Boolean canView;
        private Boolean canEdit;
    }
}
