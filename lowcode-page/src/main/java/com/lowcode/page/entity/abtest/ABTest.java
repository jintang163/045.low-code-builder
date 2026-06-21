package com.lowcode.page.entity.abtest;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ab_test")
public class ABTest extends BaseEntity {

    private Long appId;

    private Long pageId;

    private String testName;

    private String testCode;

    private String description;

    private String testType;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String targetUserGroup;

    private Integer trafficPercent;

    private String hashField;

    private Long winnerVariantId;

    private String testConfig;

    private String remark;

    @TableField(exist = false)
    private List<ABTestVariant> variants;
}
