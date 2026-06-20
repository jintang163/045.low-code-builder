package com.lowcode.model.service;

import com.alibaba.fastjson2.JSON;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.ModelField;
import com.lowcode.model.entity.ModelIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataModelSnapshotUtil {

    public String serialize(DataModel model) {
        return JSON.toJSONString(model);
    }

    public DataModel deserialize(String json) {
        return JSON.parseObject(json, DataModel.class);
    }

    public DataModel deepCopy(DataModel model) {
        if (model == null) {
            return null;
        }
        String json = serialize(model);
        return deserialize(json);
    }

    public void resetAuditFields(DataModel model) {
        model.setId(null);
        model.setCreatedBy(null);
        model.setCreatedTime(null);
        model.setUpdatedBy(null);
        model.setUpdatedTime(null);
        model.setDeleted(null);

        if (model.getFields() != null) {
            for (ModelField field : model.getFields()) {
                field.setId(null);
                field.setModelId(null);
                field.setCreatedBy(null);
                field.setCreatedTime(null);
                field.setUpdatedBy(null);
                field.setUpdatedTime(null);
                field.setDeleted(null);
            }
        }

        if (model.getIndexes() != null) {
            for (ModelIndex index : model.getIndexes()) {
                index.setId(null);
                index.setModelId(null);
                index.setCreatedBy(null);
                index.setCreatedTime(null);
                index.setUpdatedBy(null);
                index.setUpdatedTime(null);
                index.setDeleted(null);
            }
        }
    }
}
