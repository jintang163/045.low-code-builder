package com.lowcode.page.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VersionDiffVO {

    private Long oldSnapshotId;

    private Long newSnapshotId;

    private String oldVersion;

    private String newVersion;

    private List<DiffItem> pageDiffs;

    private List<DiffItem> dataModelDiffs;

    private List<DiffItem> logicDiffs;

    private Map<String, Object> oldData;

    private Map<String, Object> newData;

    @Data
    public static class DiffItem {
        private String field;
        private String oldValue;
        private String newValue;
        private String diffType;
        private String path;
    }
}
