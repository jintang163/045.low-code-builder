package com.lowcode.generator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.generator.entity.AppInfo;
import com.lowcode.generator.mapper.AppInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppInfoService extends ServiceImpl<AppInfoMapper, AppInfo> {

    public AppInfo saveApp(AppInfo app) {
        if (app.getId() == null) {
            app.setCreatedTime(LocalDateTime.now());
            app.setUpdatedTime(LocalDateTime.now());
            if (app.getStatus() == null) {
                app.setStatus(1);
            }
            if (app.getVersion() == null) {
                app.setVersion("1.0.0");
            }
            save(app);
        } else {
            app.setUpdatedTime(LocalDateTime.now());
            updateById(app);
        }
        return getById(app.getId());
    }

    public AppInfo updateApp(AppInfo app) {
        app.setUpdatedTime(LocalDateTime.now());
        updateById(app);
        return getById(app.getId());
    }

    public void deleteApp(Long id) {
        removeById(id);
    }

    public AppInfo getAppDetail(Long id) {
        return getById(id);
    }

    public List<AppInfo> getAppList() {
        LambdaQueryWrapper<AppInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AppInfo::getCreatedTime);
        return list(wrapper);
    }

    public Page<AppInfo> getAppPage(Integer current, Integer size, String keyword) {
        LambdaQueryWrapper<AppInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AppInfo::getAppName, keyword)
                    .or().like(AppInfo::getAppCode, keyword);
        }
        wrapper.orderByDesc(AppInfo::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    public AppInfo publishApp(Long id) {
        AppInfo app = getById(id);
        if (app != null) {
            app.setStatus(1);
            app.setUpdatedTime(LocalDateTime.now());
            updateById(app);
        }
        return app;
    }

    public AppInfo stopApp(Long id) {
        AppInfo app = getById(id);
        if (app != null) {
            app.setStatus(0);
            app.setUpdatedTime(LocalDateTime.now());
            updateById(app);
        }
        return app;
    }
}
