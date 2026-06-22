package com.lowcode.collaboration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_comment_attachment")
public class CommentAttachment extends BaseEntity {

    private Long commentId;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private Integer width;

    private Integer height;
}
