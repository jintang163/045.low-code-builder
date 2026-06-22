package com.lowcode.collaboration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_comment_mention")
public class CommentMention extends BaseEntity {

    private Long commentId;

    private Long userId;

    private String username;

    private String nickname;

    private Integer isRead;

    private java.time.LocalDateTime readTime;
}
