package com.lowcode.common.monitor.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SlowSqlLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String traceId;

    private String dataSource;

    private String sql;

    private String params;

    private Long executeTime;

    private Long threshold;

    private String mapperName;

    private String methodName;

    private LocalDateTime happenTime;
}
