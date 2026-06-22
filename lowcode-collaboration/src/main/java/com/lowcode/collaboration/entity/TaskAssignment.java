package com.lowcode.collaboration.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_task_assignment")
public class TaskAssignment extends BaseEntity {

    private Long appId;

    private String taskTitle;

    private String taskDesc;

    private String taskPriority;

    private String taskStatus;

    private String targetType;

    private Long targetId;

    private String targetName;

    private Long assigneeId;

    private String assigneeName;

    private Long commentId;

    private LocalDateTime dueDate;

    private LocalDateTime completedTime;

    private String completedNote;

    @TableField(exist = false)
    private String assigneeAvatar;

    @TableField(exist = false)
    private String createdByName;

    @TableField(exist = false)
    private String createdByAvatar;
}
