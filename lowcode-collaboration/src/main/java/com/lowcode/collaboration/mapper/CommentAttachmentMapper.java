package com.lowcode.collaboration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.collaboration.entity.CommentAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentAttachmentMapper extends BaseMapper<CommentAttachment> {

    @Select("SELECT * FROM sys_comment_attachment WHERE comment_id = #{commentId} AND deleted = 0")
    List<CommentAttachment> selectByCommentId(@Param("commentId") Long commentId);
}
