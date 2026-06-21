package com.lowcode.page.entity.report;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_report_schedule")
public class ReportSchedule extends BaseEntity {

    private Long reportId;

    private String scheduleName;

    private String cronExpression;

    private String emailSubject;

    private String emailRecipients;

    private String emailCc;

    private String emailContent;

    private String attachType;

    private Integer status;

    private LocalDateTime lastExecuteTime;

    private LocalDateTime nextExecuteTime;
}
