package com.lowcode.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.auth.entity.SysFieldPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysFieldPermissionMapper extends BaseMapper<SysFieldPermission> {

    @Select("SELECT fp.* FROM sys_field_permission fp " +
            "WHERE fp.role_id IN (SELECT role_id FROM sys_user_app_role WHERE user_id = #{userId} AND app_id = #{appId}) " +
            "AND fp.model_id = #{modelId} AND fp.deleted = 0")
    List<SysFieldPermission> selectFieldPermissions(@Param("userId") Long userId, @Param("appId") Long appId, @Param("modelId") Long modelId);

    @Select("SELECT fp.* FROM sys_field_permission fp " +
            "WHERE fp.role_id = #{roleId} AND fp.model_id = #{modelId} AND fp.deleted = 0")
    List<SysFieldPermission> selectByRoleAndModel(@Param("roleId") Long roleId, @Param("modelId") Long modelId);
}
