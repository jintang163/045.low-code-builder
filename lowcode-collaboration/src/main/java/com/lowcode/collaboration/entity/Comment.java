package com.lowcode.collaboration.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_comment")
public class Comment extends BaseEntity {

    private Long appId;

    private String targetType;

    private Long targetId;

    private String targetName;

    private Long parentId;

    private Long replyToUserId;

    private String replyToUserName;

    private String content;

    private Integer status;

    private Integer likeCount;

    private Long resolvedBy;

    private java.time.LocalDateTime resolvedTime;

    private String commentTag;

    @TableField(exist = false)
    private String createdByName;

    @TableField(exist = false)
    private String createdByAvatar;

    @TableField(exist = false)
    private List<CommentAttachment> attachments;

    @TableField(exist = false)
    private List<CommentMention> mentions;

    @TableField(exist = false)
    private List<Comment> replies;
}
