package com.lowcode.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.model.entity.ModelRelation;
import com.lowcode.model.mapper.ModelRelationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ModelRelationService extends ServiceImpl<ModelRelationMapper, ModelRelation> {

    public List<ModelRelation> getRelationsByAppId(Long appId) {
        LambdaQueryWrapper<ModelRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelRelation::getAppId, appId);
        return list(wrapper);
    }

    public List<ModelRelation> getRelationsByModelId(Long modelId) {
        LambdaQueryWrapper<ModelRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ModelRelation::getSourceModelId, modelId)
                .or().eq(ModelRelation::getTargetModelId, modelId));
        return list(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelRelation saveRelation(ModelRelation relation) {
        save(relation);
        return relation;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRelation(Long id) {
        ModelRelation relation = getById(id);
        if (relation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "关联关系不存在");
        }
        removeById(id);
    }
}
