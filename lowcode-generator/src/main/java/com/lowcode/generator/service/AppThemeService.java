package com.lowcode.generator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.generator.entity.AppTheme;
import com.lowcode.generator.mapper.AppThemeMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppThemeService extends ServiceImpl<AppThemeMapper, AppTheme> {

    public List<AppTheme> getThemeList(Long appId) {
        LambdaQueryWrapper<AppTheme> wrapper = new LambdaQueryWrapper<>();
        if (appId != null) {
            wrapper.eq(AppTheme::getAppId, appId);
        }
        wrapper.eq(AppTheme::getStatus, 1)
                .orderByDesc(AppTheme::getIsDefault)
                .orderByAsc(AppTheme::getThemeName);
        return list(wrapper);
    }

    public AppTheme getDefaultTheme(Long appId) {
        LambdaQueryWrapper<AppTheme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppTheme::getAppId, appId)
                .eq(AppTheme::getIsDefault, 1)
                .eq(AppTheme::getStatus, 1)
                .last("LIMIT 1");
        return getOne(wrapper);
    }

    public AppTheme getThemeById(Long id) {
        return getById(id);
    }

    public AppTheme saveTheme(AppTheme theme) {
        if (theme.getId() == null) {
            theme.setCreatedTime(LocalDateTime.now());
            theme.setUpdatedTime(LocalDateTime.now());
            if (theme.getStatus() == null) {
                theme.setStatus(1);
            }
            if (theme.getIsDefault() == null) {
                theme.setIsDefault(0);
            }
            if (theme.getIsDefault() == 1 && theme.getAppId() != null) {
                clearDefaultTheme(theme.getAppId());
            }
            save(theme);
        } else {
            theme.setUpdatedTime(LocalDateTime.now());
            if (theme.getIsDefault() != null && theme.getIsDefault() == 1 && theme.getAppId() != null) {
                clearDefaultTheme(theme.getAppId());
            }
            updateById(theme);
        }
        return getById(theme.getId());
    }

    public void deleteTheme(Long id) {
        removeById(id);
    }

    public AppTheme setDefaultTheme(Long appId, Long themeId) {
        clearDefaultTheme(appId);
        AppTheme theme = getById(themeId);
        if (theme != null) {
            theme.setIsDefault(1);
            theme.setUpdatedTime(LocalDateTime.now());
            updateById(theme);
        }
        return theme;
    }

    private void clearDefaultTheme(Long appId) {
        List<AppTheme> themes = list(new LambdaQueryWrapper<AppTheme>()
                .eq(AppTheme::getAppId, appId)
                .eq(AppTheme::getIsDefault, 1));
        for (AppTheme theme : themes) {
            theme.setIsDefault(0);
            theme.setUpdatedTime(LocalDateTime.now());
            updateById(theme);
        }
    }

    public AppTheme duplicateTheme(Long themeId, String newName) {
        AppTheme source = getById(themeId);
        if (source == null) {
            return null;
        }
        AppTheme newTheme = new AppTheme();
        newTheme.setAppId(source.getAppId());
        newTheme.setThemeName(StringUtils.hasText(newName) ? newName : source.getThemeName() + " (副本)");
        newTheme.setThemeMode(source.getThemeMode());
        newTheme.setPrimaryColor(source.getPrimaryColor());
        newTheme.setSuccessColor(source.getSuccessColor());
        newTheme.setWarningColor(source.getWarningColor());
        newTheme.setErrorColor(source.getErrorColor());
        newTheme.setInfoColor(source.getInfoColor());
        newTheme.setBorderRadius(source.getBorderRadius());
        newTheme.setFontFamily(source.getFontFamily());
        newTheme.setFontSize(source.getFontSize());
        newTheme.setLayoutMode(source.getLayoutMode());
        newTheme.setSidebarTheme(source.getSidebarTheme());
        newTheme.setHeaderTheme(source.getHeaderTheme());
        newTheme.setCustomCss(source.getCustomCss());
        newTheme.setThemeConfig(source.getThemeConfig());
        newTheme.setIsDefault(0);
        newTheme.setStatus(1);
        return saveTheme(newTheme);
    }

    public String generateThemeCss(AppTheme theme) {
        StringBuilder css = new StringBuilder();
        css.append(":root {\n");

        if (StringUtils.hasText(theme.getPrimaryColor())) {
            css.append("  --primary-color: ").append(theme.getPrimaryColor()).append(";\n");
        }
        if (StringUtils.hasText(theme.getSuccessColor())) {
            css.append("  --success-color: ").append(theme.getSuccessColor()).append(";\n");
        }
        if (StringUtils.hasText(theme.getWarningColor())) {
            css.append("  --warning-color: ").append(theme.getWarningColor()).append(";\n");
        }
        if (StringUtils.hasText(theme.getErrorColor())) {
            css.append("  --error-color: ").append(theme.getErrorColor()).append(";\n");
        }
        if (StringUtils.hasText(theme.getInfoColor())) {
            css.append("  --info-color: ").append(theme.getInfoColor()).append(";\n");
        }
        if (StringUtils.hasText(theme.getBorderRadius())) {
            css.append("  --border-radius: ").append(theme.getBorderRadius()).append(";\n");
        }
        if (StringUtils.hasText(theme.getFontFamily())) {
            css.append("  --font-family: ").append(theme.getFontFamily()).append(";\n");
        }
        if (StringUtils.hasText(theme.getFontSize())) {
            css.append("  --font-size: ").append(theme.getFontSize()).append(";\n");
        }

        css.append("}\n");

        if (StringUtils.hasText(theme.getCustomCss())) {
            css.append("\n/* Custom CSS */\n");
            css.append(theme.getCustomCss()).append("\n");
        }

        return css.toString();
    }
}
