package com.lowcode.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sql_migration")
public class SqlMigration extends BaseEntity {

    private Long appId;
    private Long dataSourceId;
    private String version;
    private String migrationName;
    private String sqlContent;
    private String modelChanges;
    private Integer status;
    private LocalDateTime executeTime;
    private String executeResult;
}
