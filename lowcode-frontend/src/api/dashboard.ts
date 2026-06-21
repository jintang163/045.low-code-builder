import request from '@/utils/request'

export interface DashboardInfo {
  id?: number
  appId: number
  dashboardName: string
  dashboardCode: string
  description?: string
  status?: number
  version?: string
  width?: number
  height?: number
  backgroundColor?: string
  backgroundImage?: string
  gridSize?: number
  components?: DashboardComponent[]
  autoRefresh?: boolean
  refreshInterval?: number
  carouselConfig?: CarouselConfig
  displayConfig?: DisplayConfig
  createdTime?: string
  updatedTime?: string
}

export interface DashboardComponent {
  id: string
  componentType: 'chart' | 'table' | 'text' | 'image' | 'indicator' | 'map' | 'gauge'
  componentName: string
  positionX: number
  positionY: number
  width: number
  height: number
  propsConfig: string
  dataSourceConfig: string
  styleConfig: string
  animationConfig?: string
  sortOrder?: number
  zIndex?: number
}

export interface CarouselConfig {
  enabled: boolean
  pages: CarouselPage[]
  interval: number
  autoPlay: boolean
  showIndicator: boolean
  transitionEffect: 'slide' | 'fade' | 'zoom'
}

export interface CarouselPage {
  id: string
  pageName: string
  componentIds: string[]
}

export interface DisplayConfig {
  fullscreen: boolean
  fitScreen: 'width' | 'height' | 'contain' | 'cover'
  scaleMode: 'fixed' | 'responsive'
  showToolbar: boolean
  showPageTitle: boolean
}

export interface DashboardDataSource {
  sourceType: 'sql' | 'model' | 'api' | 'websocket'
  dataSourceId?: number
  modelId?: number
  sqlQuery?: string
  apiUrl?: string
  websocketUrl?: string
  realTime?: boolean
  refreshInterval?: number
  fields: DashboardField[]
}

export interface DashboardField {
  fieldName: string
  fieldLabel: string
  fieldType: string
  isDimension?: boolean
  isMeasure?: boolean
  aggregation?: string
  format?: string
  unit?: string
}

export interface DashboardDataResult {
  data: Record<string, any>[]
  timestamp: string
  total?: number
}

export interface DashboardShareInfo {
  shareUrl: string
  shareCode: string
  expireTime?: string
  password?: string
}

export interface ScreenCastDevice {
  deviceId: string
  deviceName: string
  deviceType: string
  status: 'online' | 'offline'
}

export interface ScreenCastStatus {
  isCasting: boolean
  dashboardId?: number
  deviceId?: string
  startTime?: string
}

export interface IndicatorConfig {
  title: string
  value: string | number
  unit?: string
  trend?: 'up' | 'down' | 'none'
  trendValue?: string
  icon?: string
  color?: string
  fontSize?: number
}

export interface ChartTheme {
  name: string
  backgroundColor: string
  textColor: string
  axisColor: string
  splitLineColor: string
  colors: string[]
}

export const dashboardApi = {
  list: (appId: number) => request.get<DashboardInfo[]>(`/dashboard/list/${appId}`),
  page: (appId: number, current = 1, size = 10, keyword?: string) =>
    request.get(`/dashboard/page`, { params: { appId, current, size, keyword } }),
  get: (id: number) => request.get<DashboardInfo>(`/dashboard/${id}`),
  save: (data: DashboardInfo) => request.post<DashboardInfo>('/dashboard', data),
  update: (data: DashboardInfo) => request.put<DashboardInfo>('/dashboard', data),
  delete: (id: number) => request.delete(`/dashboard/${id}`),
  copy: (id: number, newName: string, newCode: string) =>
    request.post<DashboardInfo>(`/dashboard/copy/${id}`, { newName, newCode }),
  publish: (id: number) => request.post(`/dashboard/${id}/publish`),

  getDashboardData: (dashboardId: number, params?: Record<string, any>) =>
    request.get<Record<string, DashboardDataResult>>(`/dashboard/data/${dashboardId}`, { params }),

  getComponentData: (dashboardId: number, componentId: string, params?: Record<string, any>) =>
    request.get<DashboardDataResult>(`/dashboard/data/${dashboardId}/component/${componentId}`, { params }),

  getShareLink: (id: number, expireTime?: string, password?: string) =>
    request.post<DashboardShareInfo>(`/dashboard/${id}/share`, { expireTime, password }),

  revokeShare: (id: number) =>
    request.delete(`/dashboard/${id}/share`),

  getThemes: () => request.get<ChartTheme[]>(`/dashboard/themes`),
  getTemplates: () => request.get<any[]>(`/dashboard/templates`),
}

export const screenCastApi = {
  cast: (dashboardId: number, deviceId?: string) =>
    request.post(`/dashboard/${dashboardId}/cast`, { deviceId }),

  stopCast: (dashboardId: number) =>
    request.post(`/dashboard/${dashboardId}/stopCast`),

  getDevices: () => request.get<ScreenCastDevice[]>(`/dashboard/cast/devices`),

  getCastStatus: (dashboardId: number) =>
    request.get<ScreenCastStatus>(`/dashboard/${dashboardId}/castStatus`),
}
