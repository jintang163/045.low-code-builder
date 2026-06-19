package com.lowcode.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.auth.entity.SysComponentPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysComponentPermissionMapper extends BaseMapper<SysComponentPermission> {

    @Select("SELECT cp.* FROM sys_component_permission cp " +
            "WHERE cp.role_id IN (SELECT role_id FROM sys_user_app_role WHERE user_id = #{userId} AND app_id = #{appId}) " +
            "AND cp.page_id = #{pageId} AND cp.deleted = 0")
    List<SysComponentPermission> selectComponentPermissions(@Param("userId") Long userId, @Param("appId") Long appId, @Param("pageId") Long pageId);

    @Select("SELECT cp.* FROM sys_component_permission cp " +
            "WHERE cp.role_id = #{roleId} AND cp.page_id = #{pageId} AND cp.deleted = 0")
    List<SysComponentPermission> selectByRoleAndPage(@Param("roleId") Long roleId, @Param("pageId") Long pageId);
}
