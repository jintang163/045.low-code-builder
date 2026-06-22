package com.lowcode.collaboration.service;

import com.lowcode.collaboration.dto.TaskAssignmentDTO;
import com.lowcode.collaboration.dto.TaskUpdateDTO;
import com.lowcode.collaboration.entity.TaskAssignment;

import java.util.List;

public interface TaskAssignmentService {

    TaskAssignment createTask(TaskAssignmentDTO dto, Long appId, String targetType, Long targetId, String targetName, Long commentId);

    TaskAssignment getTaskById(Long id);

    List<TaskAssignment> getTasksByTarget(Long appId, String targetType, Long targetId);

    List<TaskAssignment> getTasksByAssignee(Long assigneeId);

    TaskAssignment updateTask(Long id, TaskUpdateDTO dto);

    boolean deleteTask(Long id);
}
