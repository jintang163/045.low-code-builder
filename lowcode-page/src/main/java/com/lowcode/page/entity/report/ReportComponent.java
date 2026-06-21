package com.lowcode.page.entity.report;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_report_component")
public class ReportComponent extends BaseEntity {

    private Long reportId;

    private String componentId;

    private String componentType;

    private String componentName;

    private String parentId;

    private String slotName;

    private String propsConfig;

    private String styleConfig;

    private String eventConfig;

    private String dataSourceConfig;

    private String validationConfig;

    private String linkageConfig;

    private Integer positionX;

    private Integer positionY;

    private Integer width;

    private Integer height;

    private Integer sortOrder;

    private Integer zIndex;
}
