import request from '@/utils/request'

export interface CloudConfig {
  id?: number
  platform: 'ALIYUN' | 'TENCENT' | 'CUSTOM'
  region: string
  accessKey: string
  accessSecret: string
  clusterId: string
  registryUrl: string
  registryNamespace?: string
  registryUsername?: string
  registryPassword?: string
  vpcId?: string
  securityGroupId?: string
  description?: string
}

export interface AppService {
  id?: number
  serviceName: string
  displayName: string
  modulePath: string
  imageName: string
  imageTag: string
}

export interface DeploySpec {
  replicas: number
  cpuRequest: string
  memoryRequest: string
  cpuLimit: string
  memoryLimit: string
  enableHpa: boolean
  minReplicas?: number
  maxReplicas?: number
  cpuThreshold?: number
  rolloutStrategy: 'RollingUpdate' | 'Recreate'
  maxSurge?: string
  maxUnavailable?: string
  nodeSelector?: Record<string, string>
  envVars?: Record<string, string>
}

export type DeployStatus =
  | 'PENDING'
  | 'BUILDING'
  | 'PUSHING'
  | 'DEPLOYING'
  | 'RUNNING'
  | 'SUCCESS'
  | 'FAILED'
  | 'ROLLING_BACK'
  | 'ROLLED_BACK'

export interface DeployTask {
  id?: number
  taskId: string
  deployName: string
  serviceId: number
  cloudConfigId: number
  version: string
  status: DeployStatus
  progress: number
  currentStep: string
  errorMessage?: string
  spec: DeploySpec
  domain?: string
  startedAt: string
  finishedAt?: string
  rollbackFromTaskId?: string
}

export interface DeployProgressEvent {
  taskId: string
  step: string
  message: string
  timestamp: string
  progress: number
  status: DeployStatus
  logLevel: 'INFO' | 'WARN' | 'ERROR'
}

export interface DeployRequest {
  serviceName: string
  displayName: string
  modulePath: string
  cloudConfigId: number
  version: string
  spec: DeploySpec
  domain?: string
}

export interface DeployResourceInfo {
  deploymentName: string
  namespace: string
  replicas: number
  readyReplicas: number
  availableReplicas: number
  serviceName: string
  serviceType: string
  servicePort: number
  ingressName: string
  host: string
  hpaName: string
  currentCpuUtilization: number
  targetCpuUtilization: number
  currentReplicas: number
}

export const deployApi = {
  submitDeploy: (data: DeployRequest) => request.post<DeployTask>('/deploy/submit', data),
  getTask: (taskId: string) => request.get<DeployTask>(`/deploy/task/${taskId}`),
  getTaskEvents: (taskId: string) => request.get<DeployProgressEvent[]>(`/deploy/task/${taskId}/events`),
  listTasks: (params?: any) => request.get<any>('/deploy/task/list', { params }),
  rollback: (taskId: string) => request.post<DeployTask>(`/deploy/task/${taskId}/rollback`),

  saveCloudConfig: (data: Partial<CloudConfig>) => request.post<CloudConfig>('/deploy/cloud-config', data),
  listCloudConfigs: (params?: any) => request.get<CloudConfig[]>('/deploy/cloud-config/list', { params }),
  deleteCloudConfig: (id: number) => request.delete(`/deploy/cloud-config/${id}`),
  testCloudConfig: (id: number) => request.post<boolean>(`/deploy/cloud-config/${id}/test`),

  registerApp: (data: any) => request.post<AppService>('/deploy/app/register', data),
  listApps: () => request.get<AppService[]>('/deploy/app/list'),

  getResource: (taskId: string) => request.get<DeployResourceInfo>(`/deploy/resource/${taskId}`),
  scale: (params: any) => request.post('/deploy/resource/scale', null, { params }),
  getLogs: (params: any) => request.get<string[]>('/deploy/resource/logs', { params }),

  getPresets: () => request.get<DeploySpec[]>('/deploy/presets'),
}
