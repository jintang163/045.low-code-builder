package com.lowcode.page.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_page_component")
public class PageComponent extends BaseEntity {

    private Long pageId;
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
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    private Integer sortOrder;
}
