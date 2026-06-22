package com.lowcode.collaboration.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class CommentCreateDTO {

    @NotNull(message = "应用ID不能为空")
    private Long appId;

    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    private String targetName;

    private Long parentId;

    private Long replyToUserId;

    private String replyToUserName;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private String commentTag;

    private List<CommentAttachmentDTO> attachments;

    private List<Long> mentionUserIds;

    private TaskAssignmentDTO taskAssignment;
}
