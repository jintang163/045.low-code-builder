package com.lowcode.collaboration.dto;

import lombok.Data;

@Data
public class TaskUpdateDTO {

    private String taskStatus;

    private String completedNote;

    private String taskPriority;

    private Long assigneeId;

    private String assigneeName;

    private java.time.LocalDateTime dueDate;
}
