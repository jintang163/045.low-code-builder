import request from '@/utils/request'

export interface VersionSnapshot {
  id?: number
  appId: number
  resourceType: string
  resourceId: number
  resourceName: string
  version: string
  snapshotType: number
  snapshotData?: string
  pageSnapshot?: string
  dataModelSnapshot?: string
  logicSnapshot?: string
  description?: string
  gitCommitId?: string
  gitCommitMessage?: string
  gitBranch?: string
  isPublished?: number
  publishedVersion?: string
  tag?: string
  createdBy?: number
  createdTime?: string
  updatedBy?: number
  updatedTime?: string
}

export interface ReleaseRecord {
  id?: number
  appId: number
  resourceType: string
  resourceId: number
  resourceName: string
  snapshotId: number
  version: string
  releaseTitle: string
  releaseNote?: string
  releaseType: number
  releaseStatus: number
  grayType?: number
  grayPercent?: number
  grayUserGroup?: string
  grayUserIds?: string
  targetEnvironment?: string
  gitCommitId?: string
  gitCommitMessage?: string
  gitBranch?: string
  scheduledTime?: string
  releaseTime?: string
  rollbackTime?: string
  rollbackFromSnapshotId?: number
  rollbackReason?: string
  isRollback?: number
  releaseConfig?: string
  createdBy?: number
  createdTime?: string
}

export interface GrayReleaseConfig {
  id?: number
  appId: number
  resourceType: string
  resourceId: number
  releaseRecordId?: number
  newSnapshotId: number
  oldSnapshotId?: number
  newVersion: string
  oldVersion?: string
  grayType: number
  grayPercent?: number
  grayUserGroup?: string
  grayUserIds?: string
  whiteListUserIds?: string
  blackListUserIds?: string
  hashField?: string
  status?: number
  startTime?: string
  endTime?: string
  ruleConfig?: string
  createdBy?: number
  createdTime?: string
}

export interface VersionDiffVO {
  oldSnapshotId: number
  newSnapshotId: number
  oldVersion: string
  newVersion: string
  pageDiffs: DiffItem[]
  dataModelDiffs: DiffItem[]
  logicDiffs: DiffItem[]
  oldData: Record<string, any>
  newData: Record<string, any>
}

export interface DiffItem {
  field: string
  oldValue: string
  newValue: string
  diffType: string
  path: string
}

export interface GrayReleaseResultVO {
  shouldUseNewVersion: boolean
  activeVersion: string
  activeSnapshotId: number
  matchReason: string
  matchedRule: string
  grayPercent: number
  hashValue?: string
}

export interface VersionSnapshotDTO {
  appId: number
  resourceType: string
  resourceId: number
  description?: string
  tag?: string
  autoCreate?: boolean
}

export interface ReleaseRecordDTO {
  appId: number
  resourceType: string
  resourceId: number
  snapshotId: number
  version: string
  releaseTitle: string
  releaseNote?: string
  releaseType: number
  grayType?: number
  grayPercent?: number
  grayUserGroup?: string
  grayUserIds?: string
  targetEnvironment?: string
  scheduledTime?: string
  releaseConfig?: string
}

export interface RollbackDTO {
  snapshotId: number
  rollbackReason?: string
  createNewSnapshot?: boolean
}

export const versionApi = {
  createSnapshot: (data: VersionSnapshotDTO) =>
    request.post<VersionSnapshot>('/api/version/snapshot', data),

  getSnapshotList: (params: {
    resourceId?: number
    resourceType: string
    appId: number
  }) =>
    request.get<VersionSnapshot[]>('/api/version/snapshot/list', { params }),

  getSnapshotDetail: (id: number) =>
    request.get<VersionSnapshot>(`/api/version/snapshot/${id}`),

  rollbackToSnapshot: (id: number, data: RollbackDTO) =>
    request.post<VersionSnapshot>(`/api/version/snapshot/${id}/rollback`, data),

  compareVersions: (params: {
    oldSnapshotId: number
    newSnapshotId: number
    resourceType?: string
  }) =>
    request.get<VersionDiffVO>('/api/version/snapshot/diff', { params }),

  createRelease: (data: ReleaseRecordDTO) =>
    request.post<ReleaseRecord>('/api/version/release', data),

  getReleaseList: (params: {
    resourceId?: number
    resourceType: string
    appId: number
  }) =>
    request.get<ReleaseRecord[]>('/api/version/release/list', { params }),

  publishRelease: (id: number) =>
    request.post<ReleaseRecord>(`/api/version/release/${id}/publish`),

  rollbackRelease: (id: number, params: { reason: string }) =>
    request.post<ReleaseRecord>(`/api/version/release/${id}/rollback`, null, { params }),

  createGrayConfig: (data: GrayReleaseConfig) =>
    request.post<GrayReleaseConfig>('/api/version/gray/config', data),

  checkGrayRelease: (params: {
    resourceId: number
    resourceType: string
    userId?: number
    userGroup?: string
  }) =>
    request.get<GrayReleaseResultVO>('/api/version/gray/check', { params }),

  stopGrayRelease: (id: number) =>
    request.post(`/api/version/gray/${id}/stop`),

  cancelGrayRelease: (id: number) =>
    request.post(`/api/version/gray/${id}/cancel`),
}
