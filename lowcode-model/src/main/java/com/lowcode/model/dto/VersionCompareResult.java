package com.lowcode.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class VersionCompareResult {

    private boolean compatible;
    private List<String> warnings;
    private List<String> errors;

    private List<String> modelChangedProperties;

    private List<FieldChange> addedFields;
    private List<FieldChange> deletedFields;
    private List<FieldChange> modifiedFields;

    private List<IndexChange> addedIndexes;
    private List<IndexChange> deletedIndexes;
    private List<IndexChange> modifiedIndexes;

    public VersionCompareResult() {
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.modelChangedProperties = new ArrayList<>();
        this.addedFields = new ArrayList<>();
        this.deletedFields = new ArrayList<>();
        this.modifiedFields = new ArrayList<>();
        this.addedIndexes = new ArrayList<>();
        this.deletedIndexes = new ArrayList<>();
        this.modifiedIndexes = new ArrayList<>();
    }

    @Data
    public static class FieldChange {
        private String fieldName;
        private List<String> changedProperties;
        private Map<String, Object> oldValue;
        private Map<String, Object> newValue;

        public FieldChange() {
            this.changedProperties = new ArrayList<>();
        }
    }

    @Data
    public static class IndexChange {
        private String indexName;
        private List<String> changedProperties;
        private Map<String, Object> oldValue;
        private Map<String, Object> newValue;

        public IndexChange() {
            this.changedProperties = new ArrayList<>();
        }
    }
}
