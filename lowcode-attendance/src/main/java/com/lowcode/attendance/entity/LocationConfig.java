package com.lowcode.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_location_config")
public class LocationConfig extends BaseEntity {
    private Long appId;
    private String locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer allowRadius;
    private Integer isDefault;
    private Integer sortOrder;
    private Integer status;
}
