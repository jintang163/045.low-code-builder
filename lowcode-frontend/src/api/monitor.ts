import request from '@/utils/request'

export interface MonitorMetrics {
  pvTotal: number
  uvTotal: number
  requestTotal: number
  errorTotal: number
  errorRate: number
  avgResponseTime: number
  qps: number
  apiTop10: Array<{
    url: string
    count: number
    avgTime: number
    errorCount: number
  }>
  slowSqlList: Array<{
    sql: string
    executeTime: number
    dataSource: string
    happenTime: string
  }>
  pageVisitTrend: Array<{
    date: string
    count: number
  }>
  errorTrend: Array<{
    date: string
    total: number
    errors: number
    errorRate: number
  }>
  serviceStatus: Array<{
    name: string
    totalRequests: number
    errorCount: number
    status: string
  }>
  activeAlerts: Array<{
    id: string
    ruleId: number
    ruleName: string
    message: string
    level: string
    triggerTime: string
  }>
}

export interface RequestLog {
  traceId: string
  requestUrl: string
  requestMethod: string
  requestParams: string
  requestIp: string
  userId: number
  username: string
  serviceName: string
  responseStatus: number
  costTime: number
  errorStack: string
  userAgent: string
  requestTime: string
  responseTime: string
}

export interface SlowSqlLog {
  id: string
  traceId: string
  dataSource: string
  sql: string
  params: string
  executeTime: number
  threshold: number
  mapperName: string
  methodName: string
  happenTime: string
}

export interface AlertRule {
  id?: number
  ruleName: string
  ruleType: string
  metricName: string
  operator: string
  threshold: number
  duration: number
  notifyType: string
  notifyTargets: string[]
  webhookUrl: string
  enabled: boolean
  description: string
  createTime?: string
  updateTime?: string
}

export interface PageVisitLog {
  sessionId: string
  userId: string
  username: string
  pagePath: string
  pageTitle: string
  referrer: string
  userAgent: string
  clientIp: string
  stayTime: number
  visitTime: string
  leaveTime: string
}

export const monitorApi = {
  getMetrics: () => request.get<MonitorMetrics>('/monitor/metrics'),
  getRequestLogs: (limit = 100) => request.get<RequestLog[]>('/monitor/requests', { params: { limit } }),
  getSlowSqlLogs: (limit = 100) => request.get<SlowSqlLog[]>('/monitor/slowSql', { params: { limit } }),
  addPageVisitLog: (data: Partial<PageVisitLog>) => request.post('/monitor/pageVisit', data),
  getAlertRules: () => request.get<AlertRule[]>('/monitor/alert/rules'),
  addAlertRule: (data: AlertRule) => request.post<AlertRule>('/monitor/alert/rules', data),
  updateAlertRule: (data: AlertRule) => request.put<AlertRule>('/monitor/alert/rules', data),
  deleteAlertRule: (id: number) => request.delete(`/monitor/alert/rules/${id}`),
  clearAlert: (alertId: string) => request.delete(`/monitor/alert/clear/${alertId}`),
}
