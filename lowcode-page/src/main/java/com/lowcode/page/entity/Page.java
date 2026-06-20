package com.lowcode.page.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_page")
public class Page extends BaseEntity {

    private Long appId;
    private String pageName;
    private String pageCode;
    private String pageType;
    private String pagePath;
    private String layoutType;
    private String pageConfig;
    private String mobileConfig;
    private String pageSchema;
    private Integer isHome;
    private Integer status;
    private String version;

    @TableField(exist = false)
    private List<PageComponent> components;
}
