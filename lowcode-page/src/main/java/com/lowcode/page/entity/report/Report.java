package com.lowcode.page.entity.report;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_report")
public class Report extends BaseEntity {

    private Long appId;

    private String reportName;

    private String reportCode;

    private String reportType;

    private String description;

    private String layoutType;

    private String pageConfig;

    private String reportConfig;

    private Integer status;

    private String version;

    private String scheduleConfig;

    private String emailConfig;

    private Integer autoRefresh;

    private Integer refreshInterval;

    @TableField(exist = false)
    private List<ReportComponent> components;
}
