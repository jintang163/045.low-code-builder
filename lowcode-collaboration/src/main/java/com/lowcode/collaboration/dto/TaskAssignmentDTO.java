package com.lowcode.collaboration.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class TaskAssignmentDTO {

    @NotBlank(message = "任务标题不能为空")
    private String taskTitle;

    private String taskDesc;

    private String taskPriority;

    @NotNull(message = "指派用户ID不能为空")
    private Long assigneeId;

    private String assigneeName;

    private LocalDateTime dueDate;
}
