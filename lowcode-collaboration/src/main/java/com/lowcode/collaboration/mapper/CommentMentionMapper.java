package com.lowcode.collaboration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.collaboration.entity.CommentMention;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMentionMapper extends BaseMapper<CommentMention> {

    @Select("SELECT * FROM sys_comment_mention WHERE comment_id = #{commentId} AND deleted = 0")
    List<CommentMention> selectByCommentId(@Param("commentId") Long commentId);

    @Select("SELECT * FROM sys_comment_mention WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0 ORDER BY created_time DESC")
    List<CommentMention> selectUnreadByUserId(@Param("userId") Long userId);
}
