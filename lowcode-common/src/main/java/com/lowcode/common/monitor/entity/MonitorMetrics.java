package com.lowcode.common.monitor.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class MonitorMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long pvTotal;

    private Long uvTotal;

    private Long requestTotal;

    private Long errorTotal;

    private Double errorRate;

    private Double avgResponseTime;

    private Long qps;

    private List<Map<String, Object>> apiTop10;

    private List<Map<String, Object>> slowSqlList;

    private List<Map<String, Object>> pageVisitTrend;

    private List<Map<String, Object>> errorTrend;

    private List<Map<String, Object>> serviceStatus;

    private List<Map<String, Object>> activeAlerts;
}
