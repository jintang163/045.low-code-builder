package com.lowcode.collaboration.dto;

import lombok.Data;

@Data
public class CommentQueryDTO {

    private Long appId;

    private String targetType;

    private Long targetId;

    private Long createdBy;

    private String commentTag;

    private Integer status;
}
