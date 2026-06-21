package com.lowcode.page.entity.abtest;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ab_test")
public class ABTest extends BaseEntity {

    private Long appId;

    private String testName;

    private String testCode;

    private String description;

    private String resourceType;

    private Long resourceId;

    private Long controlSnapshotId;

    private String controlVersion;

    private Integer status;

    private String trafficAllocationType;

    private Integer trafficPercent;

    private String targetUserGroup;

    private String hashField;

    private Integer sampleSize;

    private BigDecimal confidenceLevel;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long winnerVariantId;

    private String conclusion;

    @TableField(exist = false)
    private List<ABTestVariant> variants;
}
