package com.lowcode.generator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_template_version")
public class TemplateVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String version;

    private String changeLog;

    private String templateData;

    private Long publishedBy;

    private LocalDateTime publishTime;

    private Integer status;

    private String md5;

    private Long createdBy;

    private LocalDateTime createdTime;

    @TableLogic
    private Integer deleted;
}
