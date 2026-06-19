package com.lowcode.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.auth.entity.SysAppRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysAppRoleMapper extends BaseMapper<SysAppRole> {

    @Select("SELECT * FROM sys_app_role WHERE app_id = #{appId} AND deleted = 0")
    List<SysAppRole> selectByAppId(@Param("appId") Long appId);

    @Select("SELECT ar.* FROM sys_app_role ar " +
            "INNER JOIN sys_user_app_role uar ON ar.role_id = uar.role_id " +
            "WHERE uar.user_id = #{userId} AND ar.app_id = #{appId} AND ar.deleted = 0")
    List<SysAppRole> selectUserAppRoles(@Param("userId") Long userId, @Param("appId") Long appId);
}
