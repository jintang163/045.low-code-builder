package com.lowcode.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.auth.entity.SysRowPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRowPermissionMapper extends BaseMapper<SysRowPermission> {

    @Select("SELECT rp.* FROM sys_row_permission rp " +
            "WHERE rp.role_id IN (SELECT role_id FROM sys_user_app_role WHERE user_id = #{userId} AND app_id = #{appId}) " +
            "AND rp.model_id = #{modelId} AND rp.status = 1 AND rp.deleted = 0 " +
            "ORDER BY rp.priority ASC")
    List<SysRowPermission> selectRowPermissions(@Param("userId") Long userId, @Param("appId") Long appId, @Param("modelId") Long modelId);

    @Select("SELECT rp.* FROM sys_row_permission rp " +
            "WHERE rp.role_id = #{roleId} AND rp.model_id = #{modelId} AND rp.deleted = 0 " +
            "ORDER BY rp.priority ASC")
    List<SysRowPermission> selectByRoleAndModel(@Param("roleId") Long roleId, @Param("modelId") Long modelId);
}
