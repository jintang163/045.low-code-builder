package com.lowcode.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.DataModelVersion;
import com.lowcode.model.entity.ModelField;
import com.lowcode.model.entity.ModelIndex;
import com.lowcode.model.mapper.DataModelMapper;
import com.lowcode.model.mapper.DataModelVersionMapper;
import com.lowcode.model.mapper.ModelFieldMapper;
import com.lowcode.model.mapper.ModelIndexMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class DataModelVersionService extends ServiceImpl<DataModelVersionMapper, DataModelVersion> {

    @Autowired
    private DataModelMapper modelMapper;

    @Autowired
    private ModelFieldMapper fieldMapper;

    @Autowired
    private ModelIndexMapper indexMapper;

    @Autowired
    private DataModelSnapshotUtil snapshotUtil;

    public DataModelVersion createSnapshot(Long modelId, String changeDescription, Integer changeType) {
        DataModel model = loadFullModel(modelId);
        if (model == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        String version = "v" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss"));
        String snapshot = snapshotUtil.serialize(model);

        int fieldCount = model.getFields() != null ? model.getFields().size() : 0;
        int indexCount = model.getIndexes() != null ? model.getIndexes().size() : 0;

        DataModelVersion versionEntity = new DataModelVersion();
        versionEntity.setModelId(modelId);
        versionEntity.setVersion(version);
        versionEntity.setSnapshot(snapshot);
        versionEntity.setFieldCount(fieldCount);
        versionEntity.setIndexCount(indexCount);
        versionEntity.setChangeDescription(changeDescription);
        versionEntity.setChangeType(changeType);

        save(versionEntity);

        log.info("创建模型版本快照成功, modelId={}, version={}", modelId, version);
        return versionEntity;
    }

    public List<DataModelVersion> listVersions(Long modelId) {
        LambdaQueryWrapper<DataModelVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModelVersion::getModelId, modelId);
        wrapper.orderByDesc(DataModelVersion::getCreatedTime);
        return list(wrapper);
    }

    public Page<DataModelVersion> pageVersions(Long modelId, Integer current, Integer size) {
        LambdaQueryWrapper<DataModelVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModelVersion::getModelId, modelId);
        wrapper.orderByDesc(DataModelVersion::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    public DataModelVersion getVersion(Long versionId) {
        DataModelVersion version = getById(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        DataModel model = deserializeVersion(version);
        version.setModelData(model);
        return version;
    }

    public DataModelVersion getVersionByVersion(Long modelId, String version) {
        LambdaQueryWrapper<DataModelVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModelVersion::getModelId, modelId);
        wrapper.eq(DataModelVersion::getVersion, version);
        DataModelVersion versionEntity = getOne(wrapper);
        if (versionEntity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        return versionEntity;
    }

    public DataModelVersion getLatestVersion(Long modelId) {
        LambdaQueryWrapper<DataModelVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModelVersion::getModelId, modelId);
        wrapper.orderByDesc(DataModelVersion::getCreatedTime);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    public DataModel deserializeVersion(DataModelVersion version) {
        if (version == null || version.getSnapshot() == null) {
            return null;
        }
        return snapshotUtil.deserialize(version.getSnapshot());
    }

    private DataModel loadFullModel(Long modelId) {
        DataModel model = modelMapper.selectById(modelId);
        if (model == null) {
            return null;
        }

        LambdaQueryWrapper<ModelField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.eq(ModelField::getModelId, modelId);
        fieldWrapper.orderByAsc(ModelField::getSortOrder);
        List<ModelField> fields = fieldMapper.selectList(fieldWrapper);
        model.setFields(fields);

        LambdaQueryWrapper<ModelIndex> indexWrapper = new LambdaQueryWrapper<>();
        indexWrapper.eq(ModelIndex::getModelId, modelId);
        List<ModelIndex> indexes = indexMapper.selectList(indexWrapper);
        model.setIndexes(indexes);

        return model;
    }
}
