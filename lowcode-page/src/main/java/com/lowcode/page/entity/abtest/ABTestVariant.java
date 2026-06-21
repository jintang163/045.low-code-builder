package com.lowcode.page.entity.abtest;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ab_test_variant")
public class ABTestVariant extends BaseEntity {

    private Long testId;

    private String variantName;

    private String variantType;

    private Long snapshotId;

    private String version;

    private Integer trafficWeight;

    private String description;

    private Long pageViews;

    private Long uniqueVisitors;

    private Long conversions;

    private BigDecimal conversionRate;
}
