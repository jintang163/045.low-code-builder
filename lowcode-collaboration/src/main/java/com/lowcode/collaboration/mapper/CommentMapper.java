package com.lowcode.collaboration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.collaboration.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("SELECT c.*, u.nickname as createdByName, u.avatar as createdByAvatar " +
            "FROM sys_comment c " +
            "LEFT JOIN sys_user u ON c.created_by = u.id " +
            "WHERE c.app_id = #{appId} AND c.target_type = #{targetType} AND c.target_id = #{targetId} " +
            "AND c.parent_id IS NULL AND c.deleted = 0 " +
            "ORDER BY c.created_time DESC")
    List<Comment> selectRootComments(@Param("appId") Long appId,
                                      @Param("targetType") String targetType,
                                      @Param("targetId") Long targetId);

    @Select("SELECT c.*, u.nickname as createdByName, u.avatar as createdByAvatar " +
            "FROM sys_comment c " +
            "LEFT JOIN sys_user u ON c.created_by = u.id " +
            "WHERE c.parent_id = #{parentId} AND c.deleted = 0 " +
            "ORDER BY c.created_time ASC")
    List<Comment> selectReplies(@Param("parentId") Long parentId);
}
