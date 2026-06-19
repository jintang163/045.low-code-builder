package com.lowcode.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.auth.entity.SysPagePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPagePermissionMapper extends BaseMapper<SysPagePermission> {

    @Select("SELECT pp.* FROM sys_page_permission pp " +
            "WHERE pp.role_id IN (SELECT role_id FROM sys_user_app_role WHERE user_id = #{userId} AND app_id = #{appId}) " +
            "AND pp.page_id = #{pageId} AND pp.deleted = 0")
    List<SysPagePermission> selectPagePermissions(@Param("userId") Long userId, @Param("appId") Long appId, @Param("pageId") Long pageId);

    @Select("SELECT pp.* FROM sys_page_permission pp " +
            "WHERE pp.role_id = #{roleId} AND pp.deleted = 0")
    List<SysPagePermission> selectByRole(@Param("roleId") Long roleId);
}
