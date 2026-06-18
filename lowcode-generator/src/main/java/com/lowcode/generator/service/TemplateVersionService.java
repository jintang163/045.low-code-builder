package com.lowcode.generator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.generator.entity.TemplateVersion;
import com.lowcode.generator.mapper.TemplateVersionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TemplateVersionService extends ServiceImpl<TemplateVersionMapper, TemplateVersion> {

    public List<TemplateVersion> getVersionList(Long templateId) {
        LambdaQueryWrapper<TemplateVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateVersion::getTemplateId, templateId);
        wrapper.orderByDesc(TemplateVersion::getPublishTime);
        return this.list(wrapper);
    }

    public Page<TemplateVersion> getVersionPage(Long templateId, int pageNum, int pageSize) {
        LambdaQueryWrapper<TemplateVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateVersion::getTemplateId, templateId);
        wrapper.orderByDesc(TemplateVersion::getPublishTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    public TemplateVersion getLatestVersion(Long templateId) {
        LambdaQueryWrapper<TemplateVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateVersion::getTemplateId, templateId);
        wrapper.eq(TemplateVersion::getStatus, 1);
        wrapper.orderByDesc(TemplateVersion::getPublishTime);
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }

    public TemplateVersion getByVersion(Long templateId, String version) {
        LambdaQueryWrapper<TemplateVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateVersion::getTemplateId, templateId);
        wrapper.eq(TemplateVersion::getVersion, version);
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }
}
