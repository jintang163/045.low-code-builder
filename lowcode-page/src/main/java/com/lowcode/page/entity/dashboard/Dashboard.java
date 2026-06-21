package com.lowcode.page.entity.dashboard;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dashboard")
public class Dashboard extends BaseEntity {

    private Long appId;

    private String dashboardName;

    private String dashboardCode;

    private String description;

    private String layoutType;

    private Integer width;

    private Integer height;

    private String backgroundColor;

    private String backgroundImage;

    private String theme;

    private Integer status;

    private String version;

    private Integer autoRefresh;

    private Integer refreshInterval;

    private String carouselConfig;

    private String displayConfig;

    private String shareConfig;

    @TableField(exist = false)
    private List<DashboardComponent> components;
}
