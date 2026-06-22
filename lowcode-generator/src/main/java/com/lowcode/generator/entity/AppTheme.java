package com.lowcode.generator.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_app_theme")
public class AppTheme extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long appId;

    private String themeName;

    private String themeMode;

    private String primaryColor;

    private String successColor;

    private String warningColor;

    private String errorColor;

    private String infoColor;

    private String borderRadius;

    private String fontFamily;

    private String fontSize;

    private String layoutMode;

    private String sidebarTheme;

    private String headerTheme;

    private String customCss;

    private String themeConfig;

    private Integer isDefault;

    private Integer status;
}
