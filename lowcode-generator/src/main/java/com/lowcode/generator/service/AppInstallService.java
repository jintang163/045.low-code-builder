package com.lowcode.generator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.generator.entity.AppInstall;
import com.lowcode.generator.entity.AppTemplate;
import com.lowcode.generator.mapper.AppInstallMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppInstallService extends ServiceImpl<AppInstallMapper, AppInstall> {

    @Autowired
    private AppTemplateService appTemplateService;

    public List<AppInstall> getInstallList(Long userId) {
        LambdaQueryWrapper<AppInstall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppInstall::getUserId, userId);
        wrapper.orderByDesc(AppInstall::getInstallTime);
        return this.list(wrapper);
    }

    public Page<AppInstall> getInstallPage(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<AppInstall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppInstall::getUserId, userId);
        wrapper.orderByDesc(AppInstall::getInstallTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    public AppInstall getByAppId(Long appId) {
        LambdaQueryWrapper<AppInstall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppInstall::getAppId, appId);
        return this.getOne(wrapper);
    }

    public boolean hasUpdate(Long appId) {
        AppInstall install = getByAppId(appId);
        if (install == null) return false;
        AppTemplate template = appTemplateService.getById(install.getTemplateId());
        if (template == null) return false;
        return !install.getCurrentVersion().equals(template.getVersion());
    }

    public void markHasUpdate(Long templateId, String newVersion) {
        LambdaUpdateWrapper<AppInstall> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AppInstall::getTemplateId, templateId);
        wrapper.set(AppInstall::getLatestVersion, newVersion);
        wrapper.set(AppInstall::getHasUpdate, 1);
        wrapper.set(AppInstall::getUpdatedTime, LocalDateTime.now());
        this.update(wrapper);
    }

    public void updateVersion(Long appId, String newVersion) {
        LambdaUpdateWrapper<AppInstall> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AppInstall::getAppId, appId);
        wrapper.set(AppInstall::getCurrentVersion, newVersion);
        wrapper.set(AppInstall::getHasUpdate, 0);
        wrapper.set(AppInstall::getLastUpdateTime, LocalDateTime.now());
        wrapper.set(AppInstall::getUpdatedTime, LocalDateTime.now());
        this.update(wrapper);
    }
}
