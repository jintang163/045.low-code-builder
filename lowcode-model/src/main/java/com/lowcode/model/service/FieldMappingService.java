package com.lowcode.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.model.entity.FieldMapping;
import com.lowcode.model.mapper.FieldMappingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FieldMappingService extends ServiceImpl<FieldMappingMapper, FieldMapping> {

    public FieldMapping saveFieldMapping(FieldMapping fieldMapping) {
        if (fieldMapping.getSortOrder() == null) {
            long maxSort = count(new LambdaQueryWrapper<FieldMapping>()
                    .eq(FieldMapping::getDataSourceId, fieldMapping.getDataSourceId())
                    .eq(FieldMapping::getPageId, fieldMapping.getPageId())
                    .eq(FieldMapping::getTargetComponentId, fieldMapping.getTargetComponentId())) + 1;
            fieldMapping.setSortOrder((int) maxSort);
        }
        save(fieldMapping);
        return fieldMapping;
    }

    public FieldMapping updateFieldMapping(FieldMapping fieldMapping) {
        FieldMapping existing = getById(fieldMapping.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字段映射不存在");
        }
        updateById(fieldMapping);
        return getById(fieldMapping.getId());
    }

    public void deleteFieldMapping(Long id) {
        FieldMapping existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字段映射不存在");
        }
        removeById(id);
    }

    public List<FieldMapping> listByDataSourceId(Long dataSourceId) {
        LambdaQueryWrapper<FieldMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldMapping::getDataSourceId, dataSourceId);
        wrapper.orderByAsc(FieldMapping::getSortOrder);
        return list(wrapper);
    }

    public List<FieldMapping> listByPageId(Long pageId) {
        LambdaQueryWrapper<FieldMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldMapping::getPageId, pageId);
        wrapper.orderByAsc(FieldMapping::getSortOrder);
        return list(wrapper);
    }

    public List<FieldMapping> listByComponentId(Long pageId, String targetComponentId) {
        LambdaQueryWrapper<FieldMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldMapping::getPageId, pageId);
        wrapper.eq(FieldMapping::getTargetComponentId, targetComponentId);
        wrapper.orderByAsc(FieldMapping::getSortOrder);
        return list(wrapper);
    }

    public List<FieldMapping> listByAppId(Long appId) {
        LambdaQueryWrapper<FieldMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldMapping::getAppId, appId);
        wrapper.orderByAsc(FieldMapping::getSortOrder);
        return list(wrapper);
    }

    public boolean saveBatchByPageId(Long pageId, List<FieldMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return true;
        }
        remove(new LambdaQueryWrapper<FieldMapping>().eq(FieldMapping::getPageId, pageId));
        for (FieldMapping mapping : mappings) {
            mapping.setPageId(pageId);
        }
        return saveBatch(mappings);
    }

    public boolean saveBatchByDataSourceId(Long dataSourceId, Long pageId, List<FieldMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return true;
        }
        remove(new LambdaQueryWrapper<FieldMapping>()
                .eq(FieldMapping::getDataSourceId, dataSourceId)
                .eq(FieldMapping::getPageId, pageId));
        for (FieldMapping mapping : mappings) {
            mapping.setDataSourceId(dataSourceId);
            mapping.setPageId(pageId);
        }
        return saveBatch(mappings);
    }
}
