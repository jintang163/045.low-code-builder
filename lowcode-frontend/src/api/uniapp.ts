import request from '@/utils/request'
import type { Platform, DeviceConfig } from '@/hooks/useMobileSimulator'

export interface UniAppConfig {
  appName: string
  appId: string
  platforms: Platform[]
  dataModelIds?: number[]
  pageIds?: number[]
  touchEventsEnabled?: boolean
  gesturesEnabled?: boolean
  description?: string
  version?: string
}

export interface UniAppGenerateResult {
  appCode: string
  previewToken: string
  projectPath: string
  packageSize: number
  generatedTime: string
  platforms: Platform[]
}

export interface PreviewCreateParams {
  appCode: string
  deviceType?: string
  platform?: Platform
  url?: string
}

export interface PreviewInfo {
  previewToken: string
  appCode: string
  appName: string
  platform: Platform
  deviceType: string
  previewUrl: string
  qrCodeUrl: string
  expireTime: string
  createTime: string
  status: 'active' | 'expired' | 'error'
}

export interface SimulatorConfig {
  device: DeviceConfig
  previewUrl: string
  touchEventsEnabled: boolean
  gesturesEnabled: boolean
  platform: Platform
}

export interface PlatformInfo {
  code: Platform
  name: string
  icon: string
  enabled: boolean
}

export interface QRCodeResult {
  url: string
  base64: string
  size: number
}

export const uniappApi = {
  generateUniApp: (config: UniAppConfig) =>
    request.post<UniAppGenerateResult>('/uniapp/generate', config),

  createPreview: (params: PreviewCreateParams) =>
    request.post<PreviewInfo>('/uniapp/preview', params),

  getPreview: (previewToken: string) =>
    request.get<PreviewInfo>(`/uniapp/preview/${previewToken}`),

  getSimulatorConfig: (previewToken: string, deviceType?: string) =>
    request.get<SimulatorConfig>(`/uniapp/preview/${previewToken}/simulator`, {
      params: { deviceType },
    }),

  getDevices: () =>
    request.get<DeviceConfig[]>('/uniapp/devices'),

  getPlatforms: () =>
    request.get<PlatformInfo[]>('/uniapp/platforms'),

  getQRCode: (previewToken: string, size = 256) =>
    request.get<QRCodeResult>(`/uniapp/preview/${previewToken}/qrcode`, {
      params: { size },
    }),

  expirePreview: (previewToken: string) =>
    request.post(`/uniapp/preview/${previewToken}/expire`),

  downloadUniApp: (appCode: string) =>
    request.get(`/uniapp/download/${appCode}`, {
      responseType: 'blob',
    }),
}

export default uniappApi
