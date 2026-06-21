import request from '@/utils/request'

export interface ReportInfo {
  id?: number
  appId: number
  reportName: string
  reportCode: string
  reportType: 'table' | 'chart' | 'comprehensive'
  description?: string
  status?: number
  version?: string
  layoutConfig?: string
  components?: ReportComponent[]
  dataSourceConfig?: DataSourceConfig
  scheduleConfig?: ScheduleConfig
  createdTime?: string
  updatedTime?: string
}

export interface ReportComponent {
  id: string
  componentType: 'crosstab' | 'groupSummary' | 'chart' | 'text' | 'table'
  componentName: string
  positionX: number
  positionY: number
  width: number
  height: number
  propsConfig: string
  dataSourceConfig: string
  styleConfig: string
  linkageConfig?: string
  sortOrder?: number
}

export interface DataSourceConfig {
  sourceType: 'sql' | 'model' | 'api'
  dataSourceId?: number
  modelId?: number
  sqlQuery?: string
  apiUrl?: string
  fields: DataField[]
  filters?: FilterCondition[]
}

export interface DataField {
  fieldName: string
  fieldLabel: string
  fieldType: string
  isDimension?: boolean
  isMeasure?: boolean
  aggregation?: 'sum' | 'avg' | 'count' | 'max' | 'min' | 'none'
}

export interface FilterCondition {
  field: string
  operator: string
  value: any
}

export interface ScheduleConfig {
  enabled: boolean
  cronExpression: string
  emailConfig: EmailConfig
  exportFormat: 'pdf' | 'excel' | 'image'
  sendType: 'daily' | 'weekly' | 'monthly' | 'custom'
}

export interface EmailConfig {
  recipients: string[]
  subject: string
  content: string
  attachReport: boolean
}

export interface ReportSchedule {
  id?: number
  reportId: number
  scheduleName: string
  cronExpression: string
  status?: number
  exportFormat: 'pdf' | 'excel' | 'image'
  emailConfig: EmailConfig
  params?: Record<string, any>
  lastExecuteTime?: string
  nextExecuteTime?: string
  createdTime?: string
  updatedTime?: string
}

export interface ReportDataQuery {
  reportId: number
  params?: Record<string, any>
}

export interface ReportDataResult {
  columns: DataField[]
  rows: Record<string, any>[]
  summary?: Record<string, any>
}

export interface ChartLinkageConfig {
  sourceComponentId: string
  targetComponentId: string
  triggerType: 'click' | 'hover' | 'select'
  mappingFields: { sourceField: string; targetField: string }[]
}

export interface CrosstabConfig {
  rowFields: string[]
  columnFields: string[]
  valueFields: { field: string; aggregation: string }[]
  showRowTotal?: boolean
  showColumnTotal?: boolean
  showRowSubtotal?: boolean
  showColumnSubtotal?: boolean
}

export interface GroupSummaryConfig {
  groupFields: string[]
  summaryFields: { field: string; aggregation: string; label?: string }[]
  showGrandTotal?: boolean
  showGroupTotal?: boolean
}

export const reportApi = {
  list: (appId: number) => request.get<ReportInfo[]>(`/report/list/${appId}`),
  page: (appId: number, current = 1, size = 10, keyword?: string) =>
    request.get(`/report/page`, { params: { appId, current, size, keyword } }),
  get: (id: number) => request.get<ReportInfo>(`/report/${id}`),
  save: (data: ReportInfo) => request.post<ReportInfo>('/report', data),
  update: (data: ReportInfo) => request.put<ReportInfo>('/report', data),
  delete: (id: number) => request.delete(`/report/${id}`),
  copy: (id: number, newName: string, newCode: string) =>
    request.post<ReportInfo>(`/report/copy/${id}`, { newName, newCode }),
  publish: (id: number) => request.post(`/report/${id}/publish`),

  getData: (reportId: number, params?: Record<string, any>) =>
    request.get<ReportDataResult>(`/report/data/${reportId}`, { params }),
  queryData: (reportId: number, params?: Record<string, any>) =>
    request.post<ReportDataResult>(`/report/${reportId}/query`, params),
  getReportData: (reportId: number, params?: Record<string, any>) =>
    request.get<ReportDataResult>(`/report/data/${reportId}`, { params }),

  getComponentData: (reportId: number, componentId: string, params?: Record<string, any>) =>
    request.get<ReportDataResult>(`/report/data/${reportId}/component/${componentId}`, { params }),

  testSql: (dataSourceId: number, sql: string) =>
    request.post<ReportDataResult>(`/report/testSql`, { dataSourceId, sql }),

  getSchedules: (reportId: number) =>
    request.get<ScheduleConfig[]>(`/report/${reportId}/schedules`),
  saveSchedule: (reportId: number, config: ScheduleConfig) =>
    request.post(`/report/${reportId}/schedule`, config),
  deleteSchedule: (reportId: number, scheduleId: number) =>
    request.delete(`/report/${reportId}/schedule/${scheduleId}`),
  runSchedule: (reportId: number, scheduleId: number) =>
    request.post(`/report/${reportId}/schedule/${scheduleId}/run`),

  sendEmail: (id: number, emailConfig: EmailConfig, params?: Record<string, any>) =>
    request.post(`/report/${id}/sendEmail`, { emailConfig, params }),

  exportReport: (id: number, format: 'pdf' | 'excel' | 'image', params?: Record<string, any>) =>
    request.post(`/report/${id}/export`, { format, params }, { responseType: 'blob' }),
}

export const reportScheduleApi = {
  list: (reportId: number) => request.get<ReportSchedule[]>(`/report/schedule`, { params: { reportId } }),
  page: (reportId: number, current = 1, size = 10) =>
    request.get(`/report/schedule/page`, { params: { reportId, current, size } }),
  get: (id: number) => request.get<ReportSchedule>(`/report/schedule/${id}`),
  save: (data: ReportSchedule) => request.post<ReportSchedule>('/report/schedule', data),
  update: (data: ReportSchedule) => request.put<ReportSchedule>('/report/schedule', data),
  delete: (id: number) => request.delete(`/report/schedule/${id}`),
  enable: (id: number) => request.post(`/report/schedule/${id}/enable`),
  disable: (id: number) => request.post(`/report/schedule/${id}/disable`),
  execute: (id: number) => request.post(`/report/schedule/${id}/execute`),
}

export const reportTemplateApi = {
  list: () => request.get<any[]>(`/report/templates`),
  get: (id: number) => request.get<any>(`/report/template/${id}`),
  apply: (templateId: number, reportId: number) =>
    request.post(`/report/template/${templateId}/apply/${reportId}`),
}
