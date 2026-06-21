package com.lowcode.page.entity.abtest;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ab_test_metric")
public class ABTestMetric extends BaseEntity {

    private Long testId;

    private Long variantId;

    private String metricName;

    private String metricType;

    private String metricKey;

    private BigDecimal totalValue;

    private Long count;

    private BigDecimal avgValue;

    private Long uniqueCount;

    private String confidenceInterval;

    private BigDecimal statisticalSignificance;
}
