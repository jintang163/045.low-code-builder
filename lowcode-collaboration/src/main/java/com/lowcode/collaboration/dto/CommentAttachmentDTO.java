package com.lowcode.collaboration.dto;

import lombok.Data;

@Data
public class CommentAttachmentDTO {

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private Integer width;

    private Integer height;
}
