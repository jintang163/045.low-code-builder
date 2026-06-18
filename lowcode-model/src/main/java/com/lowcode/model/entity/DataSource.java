package com.lowcode.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_source")
public class DataSource extends BaseEntity {

    private Long appId;
    private String sourceName;
    private String sourceCode;
    private String dbType;
    private String host;
    private Integer port;
    private String dbName;
    private String username;
    private String password;
    private String driverClass;
    private String connectionParams;
    private Integer status;
}
