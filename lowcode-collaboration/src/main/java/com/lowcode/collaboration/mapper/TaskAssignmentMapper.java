package com.lowcode.collaboration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.collaboration.entity.TaskAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskAssignmentMapper extends BaseMapper<TaskAssignment> {

    @Select("SELECT t.*, u1.avatar as assigneeAvatar, u2.nickname as createdByName, u2.avatar as createdByAvatar " +
            "FROM sys_task_assignment t " +
            "LEFT JOIN sys_user u1 ON t.assignee_id = u1.id " +
            "LEFT JOIN sys_user u2 ON t.created_by = u2.id " +
            "WHERE t.app_id = #{appId} AND t.target_type = #{targetType} AND t.target_id = #{targetId} AND t.deleted = 0 " +
            "ORDER BY t.created_time DESC")
    List<TaskAssignment> selectByTarget(@Param("appId") Long appId,
                                         @Param("targetType") String targetType,
                                         @Param("targetId") Long targetId);

    @Select("SELECT t.*, u1.avatar as assigneeAvatar, u2.nickname as createdByName, u2.avatar as createdByAvatar " +
            "FROM sys_task_assignment t " +
            "LEFT JOIN sys_user u1 ON t.assignee_id = u1.id " +
            "LEFT JOIN sys_user u2 ON t.created_by = u2.id " +
            "WHERE t.assignee_id = #{assigneeId} AND t.deleted = 0 " +
            "ORDER BY " +
            "CASE t.task_status WHEN 'TODO' THEN 1 WHEN 'IN_PROGRESS' THEN 2 WHEN 'DONE' THEN 3 ELSE 4 END, " +
            "CASE t.task_priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 ELSE 5 END, " +
            "t.created_time DESC")
    List<TaskAssignment> selectByAssignee(@Param("assigneeId") Long assigneeId);
}
