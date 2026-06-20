package com.lowcode.deploy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.deploy.entity.DeployTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeployTaskMapper extends BaseMapper<DeployTask> {

    @Select("SELECT * FROM sys_deploy_task WHERE service_id = #{serviceId} AND deleted = 0 ORDER BY created_time DESC LIMIT #{limit}")
    List<DeployTask> selectRecentTasks(@Param("serviceId") Long serviceId, @Param("limit") Integer limit);

    @Select("SELECT * FROM sys_deploy_task WHERE task_id = #{taskId} AND deleted = 0 LIMIT 1")
    DeployTask selectByTaskId(@Param("taskId") String taskId);
}
