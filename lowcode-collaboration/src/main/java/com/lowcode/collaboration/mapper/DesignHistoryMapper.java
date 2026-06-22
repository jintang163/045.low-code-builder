package com.lowcode.collaboration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.collaboration.entity.DesignHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DesignHistoryMapper extends BaseMapper<DesignHistory> {

    @Select("SELECT h.*, u.nickname as createdByName, u.avatar as createdByAvatar " +
            "FROM sys_design_history h " +
            "LEFT JOIN sys_user u ON h.created_by = u.id " +
            "WHERE h.app_id = #{appId} AND h.target_type = #{targetType} AND h.target_id = #{targetId} AND h.deleted = 0 " +
            "ORDER BY h.created_time DESC " +
            "LIMIT #{limit}")
    List<DesignHistory> selectByTarget(@Param("appId") Long appId,
                                        @Param("targetType") String targetType,
                                        @Param("targetId") Long targetId,
                                        @Param("limit") Integer limit);
}
