package com.lowcode.page.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.entity.ComponentLibrary;
import com.lowcode.page.mapper.ComponentLibraryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ComponentLibraryService extends ServiceImpl<ComponentLibraryMapper, ComponentLibrary> {

    public Map<String, List<ComponentLibrary>> getComponentTree() {
        LambdaQueryWrapper<ComponentLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ComponentLibrary::getStatus, 1);
        wrapper.orderByAsc(ComponentLibrary::getComponentCategory, ComponentLibrary::getId);
        List<ComponentLibrary> components = list(wrapper);

        return components.stream()
                .collect(Collectors.groupingBy(
                        ComponentLibrary::getComponentCategory,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public List<ComponentLibrary> getComponentList(String category) {
        LambdaQueryWrapper<ComponentLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ComponentLibrary::getStatus, 1);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(ComponentLibrary::getComponentCategory, category);
        }
        wrapper.orderByAsc(ComponentLibrary::getId);
        return list(wrapper);
    }

    public ComponentLibrary getComponentDetail(Long id) {
        ComponentLibrary component = getById(id);
        if (component == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件不存在");
        }
        return component;
    }

    public ComponentLibrary saveComponent(ComponentLibrary component) {
        LambdaQueryWrapper<ComponentLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ComponentLibrary::getComponentType, component.getComponentType());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("组件类型已存在");
        }
        save(component);
        return component;
    }

    public ComponentLibrary updateComponent(ComponentLibrary component) {
        ComponentLibrary existing = getById(component.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件不存在");
        }
        updateById(component);
        return getById(component.getId());
    }

    public void deleteComponent(Long id) {
        ComponentLibrary component = getById(id);
        if (component == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件不存在");
        }
        removeById(id);
    }

    public List<ComponentLibrary> getMobileComponents() {
        LambdaQueryWrapper<ComponentLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ComponentLibrary::getStatus, 1);
        wrapper.and(w -> w.eq(ComponentLibrary::getSupportPlatform, "MOBILE")
                .or().eq(ComponentLibrary::getSupportPlatform, "ALL"));
        wrapper.orderByAsc(ComponentLibrary::getComponentCategory, ComponentLibrary::getId);
        return list(wrapper);
    }

    public List<ComponentLibrary> getComponentsByPlatform(String platform) {
        LambdaQueryWrapper<ComponentLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ComponentLibrary::getStatus, 1);
        if (platform != null && !platform.isEmpty()) {
            wrapper.and(w -> w.eq(ComponentLibrary::getSupportPlatform, platform)
                    .or().eq(ComponentLibrary::getSupportPlatform, "ALL"));
        }
        wrapper.orderByAsc(ComponentLibrary::getComponentCategory, ComponentLibrary::getId);
        return list(wrapper);
    }
}
