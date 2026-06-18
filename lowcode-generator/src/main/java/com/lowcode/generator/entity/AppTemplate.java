package com.lowcode.generator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_app_template")
public class AppTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateName;

    private String templateCode;

    private String templateDesc;

    private String icon;

    private String category;

    private String tags;

    private String version;

    private Integer installCount;

    private Integer starCount;

    private String screenshot;

    private String templateData;

    private Integer templateType;

    private String publisher;

    private Long publisherId;

    private Integer status;

    private LocalDateTime publishTime;

    private Long createdBy;

    private LocalDateTime createdTime;

    private Long updatedBy;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
