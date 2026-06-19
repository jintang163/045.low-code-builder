package com.lowcode.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.auth.entity.SysUserAppRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserAppRoleMapper extends BaseMapper<SysUserAppRole> {

    @Delete("DELETE FROM sys_user_app_role WHERE user_id = #{userId} AND app_id = #{appId}")
    void deleteByUserAndApp(@Param("userId") Long userId, @Param("appId") Long appId);

    @Select("SELECT role_id FROM sys_user_app_role WHERE user_id = #{userId} AND app_id = #{appId}")
    List<Long> selectRoleIdsByUserAndApp(@Param("userId") Long userId, @Param("appId") Long appId);
}
