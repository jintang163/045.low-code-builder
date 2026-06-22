package com.lowcode.collaboration.service.impl;

import com.lowcode.collaboration.dto.TaskAssignmentDTO;
import com.lowcode.collaboration.dto.TaskUpdateDTO;
import com.lowcode.collaboration.entity.TaskAssignment;
import com.lowcode.collaboration.mapper.TaskAssignmentMapper;
import com.lowcode.collaboration.service.TaskAssignmentService;
import com.lowcode.collaboration.websocket.CollaborationWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TaskAssignmentServiceImpl implements TaskAssignmentService {

    @Autowired
    private TaskAssignmentMapper taskMapper;

    @Autowired(required = false)
    private CollaborationWebSocketHandler webSocketHandler;

    @Override
    @Transactional
    public TaskAssignment createTask(TaskAssignmentDTO dto, Long appId, String targetType,
                                      Long targetId, String targetName, Long commentId) {
        TaskAssignment task = new TaskAssignment();
        BeanUtils.copyProperties(dto, task);
        task.setAppId(appId);
        task.setTargetType(targetType);
        task.setTargetId(targetId);
        task.setTargetName(targetName);
        task.setCommentId(commentId);
        task.setTaskStatus("TODO");
        if (task.getTaskPriority() == null) {
            task.setTaskPriority("MEDIUM");
        }
        taskMapper.insert(task);

        TaskAssignment result = getTaskWithDetails(task.getId());

        if (webSocketHandler != null) {
            try {
                webSocketHandler.broadcastTask(appId, targetType, targetId, result);
            } catch (Exception e) {
                log.warn("WebSocket推送任务消息失败", e);
            }
        }

        return result;
    }

    @Override
    public TaskAssignment getTaskById(Long id) {
        return getTaskWithDetails(id);
    }

    @Override
    public List<TaskAssignment> getTasksByTarget(Long appId, String targetType, Long targetId) {
        return taskMapper.selectByTarget(appId, targetType, targetId);
    }

    @Override
    public List<TaskAssignment> getTasksByAssignee(Long assigneeId) {
        return taskMapper.selectByAssignee(assigneeId);
    }

    @Override
    @Transactional
    public TaskAssignment updateTask(Long id, TaskUpdateDTO dto) {
        TaskAssignment task = taskMapper.selectById(id);
        if (task == null) {
            return null;
        }
        if (dto.getTaskStatus() != null) {
            task.setTaskStatus(dto.getTaskStatus());
            if ("DONE".equals(dto.getTaskStatus())) {
                task.setCompletedTime(LocalDateTime.now());
                if (dto.getCompletedNote() != null) {
                    task.setCompletedNote(dto.getCompletedNote());
                }
            }
        }
        if (dto.getCompletedNote() != null) {
            task.setCompletedNote(dto.getCompletedNote());
        }
        if (dto.getTaskPriority() != null) {
            task.setTaskPriority(dto.getTaskPriority());
        }
        if (dto.getAssigneeId() != null) {
            task.setAssigneeId(dto.getAssigneeId());
        }
        if (dto.getAssigneeName() != null) {
            task.setAssigneeName(dto.getAssigneeName());
        }
        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate());
        }
        taskMapper.updateById(task);

        TaskAssignment result = getTaskWithDetails(id);

        if (webSocketHandler != null) {
            try {
                webSocketHandler.broadcastTask(task.getAppId(), task.getTargetType(), task.getTargetId(), result);
            } catch (Exception e) {
                log.warn("WebSocket推送任务更新消息失败", e);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public boolean deleteTask(Long id) {
        TaskAssignment task = taskMapper.selectById(id);
        if (task == null) {
            return false;
        }
        taskMapper.deleteById(id);
        return true;
    }

    private TaskAssignment getTaskWithDetails(Long id) {
        return taskMapper.selectById(id);
    }
}
