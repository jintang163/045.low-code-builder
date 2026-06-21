package com.lowcode.page.entity.dashboard;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dashboard_component")
public class DashboardComponent extends BaseEntity {

    private Long dashboardId;

    private String componentId;

    private String componentType;

    private String componentName;

    private String propsConfig;

    private String styleConfig;

    private String dataSourceConfig;

    private String refreshConfig;

    private String linkageConfig;

    private Integer positionX;

    private Integer positionY;

    private Integer width;

    private Integer height;

    private Integer sortOrder;

    private Integer zIndex;
}
