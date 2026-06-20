import {
  PendingChange,
  ResourceType,
  ChangeAction,
  getPendingChanges,
  markChangeSynced,
  markChangeFailed,
  markChangeSyncing,
  clearSyncedChanges,
  addSyncHistory,
  getPendingCount,
  SyncHistoryItem,
  getLastSync,
} from './indexedDB'
import { isOnline, subscribeNetworkStatus, NetworkStatus } from './networkDetector'
import request from '@/utils/request'

export type SyncStatus = 'idle' | 'syncing' | 'success' | 'error'

export interface SyncState {
  status: SyncStatus
  pendingCount: number
  lastSyncTime: string | null
  errorMessage?: string
}

export interface ConflictInfo {
  resourceType: ResourceType
  resourceId: number
  localData: any
  serverData: any
  resolved: boolean
}

export type ConflictResolver = (conflict: ConflictInfo) => Promise<'local' | 'server'>

const MAX_RETRY_COUNT = 3

class SyncManager {
  private state: SyncState = {
    status: 'idle',
    pendingCount: 0,
    lastSyncTime: null,
  }

  private listeners: Set<(state: SyncState) => void> = new Set()
  private conflictResolver: ConflictResolver | null = null
  private syncing = false
  private autoSyncEnabled = true
  private networkUnsubscribe: (() => void) | null = null

  constructor() {
    this.init()
  }

  private async init() {
    try {
      this.state.pendingCount = await getPendingCount()

      const lastSync = await getLastSync()
      if (lastSync?.endTime) {
        this.state.lastSyncTime = lastSync.endTime
      }

      this.notifyListeners()

      this.networkUnsubscribe = subscribeNetworkStatus(this.handleNetworkChange)
    } catch (e) {
      console.error('同步管理器初始化失败:', e)
    }
  }

  private handleNetworkChange = async (status: NetworkStatus) => {
    if (status === 'online' && this.autoSyncEnabled) {
      try {
        await this.startSync()
      } catch (e) {
        console.error('网络恢复后自动同步失败:', e)
      }
    }
  }

  private setState(partial: Partial<SyncState>) {
    this.state = { ...this.state, ...partial }
    this.notifyListeners()
  }

  private notifyListeners() {
    this.listeners.forEach((listener) => {
      try {
        listener({ ...this.state })
      } catch (e) {
        console.error('同步状态监听回调执行失败:', e)
      }
    })
  }

  public getState(): SyncState {
    return { ...this.state }
  }

  public subscribe(listener: (state: SyncState) => void): () => void {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  public setConflictResolver(resolver: ConflictResolver) {
    this.conflictResolver = resolver
  }

  public setAutoSyncEnabled(enabled: boolean) {
    this.autoSyncEnabled = enabled
  }

  public async startSync(): Promise<void> {
    if (this.syncing) return

    if (!isOnline()) {
      this.setState({ status: 'error', errorMessage: '网络不可用' })
      throw new Error('网络不可用，无法同步')
    }

    this.syncing = true
    this.setState({ status: 'syncing' })

    const startTime = new Date().toISOString()
    let successCount = 0
    let failedCount = 0
    let totalCount = 0
    let error: Error | null = null

    try {
      const pendingChanges = await getPendingChanges()
      const changesToSync = pendingChanges.filter(
        (c) => c.status === 'pending' || (c.status === 'failed' && (c.retryCount || 0) < MAX_RETRY_COUNT)
      )

      totalCount = changesToSync.length

      if (totalCount === 0) {
        this.setState({ status: 'success', pendingCount: 0 })
        return
      }

      for (const change of changesToSync) {
        if (!isOnline()) {
          break
        }

        try {
          await markChangeSyncing(change.id!)
          await this.syncSingleChange(change)
          await markChangeSynced(change.id!)
          successCount++
        } catch (e: any) {
          failedCount++
          const errorMsg = e?.message || '同步失败'
          await markChangeFailed(change.id!, errorMsg)
          console.error(`同步失败 [${change.resourceType}:${change.resourceId}]:`, e)
        }

        const newPendingCount = await getPendingCount()
        this.setState({ pendingCount: newPendingCount })
      }

      if (successCount > 0) {
        await clearSyncedChanges()
      }

      const finalPendingCount = await getPendingCount()
      this.setState({
        pendingCount: finalPendingCount,
        status: failedCount === 0 ? 'success' : 'error',
        lastSyncTime: new Date().toISOString(),
        errorMessage: failedCount > 0 ? `有 ${failedCount} 项同步失败` : undefined,
      })
    } catch (e: any) {
      error = e
      this.setState({
        status: 'error',
        errorMessage: e?.message || '同步过程中发生错误',
      })
    } finally {
      const endTime = new Date().toISOString()
      const history: Omit<SyncHistoryItem, 'id'> = {
        startTime,
        endTime,
        status: error ? 'failed' : failedCount > 0 ? 'partial' : 'success',
        totalCount,
        successCount,
        failedCount,
        errorMessage: error?.message || undefined,
      }

      try {
        await addSyncHistory(history)
      } catch (e) {
        console.error('保存同步历史失败:', e)
      }

      this.syncing = false
    }
  }

  private async syncSingleChange(change: PendingChange): Promise<void> {
    const { resourceType, action, resourceId, data } = change

    switch (resourceType) {
      case 'page':
        await this.syncPage(action, resourceId, data)
        break
      case 'dataModel':
        await this.syncDataModel(action, resourceId, data)
        break
      case 'businessLogic':
        await this.syncBusinessLogic(action, resourceId, data)
        break
      default:
        throw new Error(`未知的资源类型: ${resourceType}`)
    }
  }

  private async syncPage(action: ChangeAction, resourceId: number, data: any): Promise<void> {
    switch (action) {
      case 'create':
        await request.post('/page', data)
        break
      case 'update':
        await request.put('/page', data)
        break
      case 'delete':
        await request.delete(`/page/${resourceId}`)
        break
    }
  }

  private async syncDataModel(action: ChangeAction, resourceId: number, data: any): Promise<void> {
    switch (action) {
      case 'create':
        await request.post('/model', data)
        break
      case 'update':
        await request.put('/model', data)
        break
      case 'delete':
        await request.delete(`/model/${resourceId}`)
        break
    }
  }

  private async syncBusinessLogic(action: ChangeAction, resourceId: number, data: any): Promise<void> {
    switch (action) {
      case 'create':
        await request.post('/logic', data)
        break
      case 'update':
        await request.put('/logic', data)
        break
      case 'delete':
        await request.delete(`/logic/${resourceId}`)
        break
    }
  }

  public async syncPage(pageId: number): Promise<void> {
    const pageChanges = await getPendingChanges('page')
    const pageChange = pageChanges.find((c) => c.resourceId === pageId && c.status !== 'synced')

    if (pageChange) {
      await markChangeSyncing(pageChange.id!)
      try {
        await this.syncSingleChange(pageChange)
        await markChangeSynced(pageChange.id!)
        await clearSyncedChanges()
      } catch (e: any) {
        await markChangeFailed(pageChange.id!, e?.message || '同步失败')
        throw e
      }
    }

    const newPendingCount = await getPendingCount()
    this.setState({ pendingCount: newPendingCount })
  }

  public async syncAll(): Promise<void> {
    if (!isOnline()) {
      throw new Error('网络不可用')
    }

    await this.startSync()
  }

  public async pullFromServer(appId: number): Promise<void> {
    if (!isOnline()) {
      throw new Error('网络不可用')
    }

    this.setState({ status: 'syncing' })

    try {
      const [pagesRes, dataModelsRes, logicsRes] = await Promise.all([
        request.get(`/page/list/${appId}`),
        request.get(`/model/list/${appId}`),
        request.get(`/logic/list/${appId}`),
      ])

      const { bulkPut } = await import('./indexedDB')

      if (pagesRes.data) {
        await bulkPut('pages', pagesRes.data)
      }

      if (dataModelsRes.data) {
        await bulkPut('dataModels', dataModelsRes.data)
      }

      if (logicsRes.data) {
        await bulkPut('businessLogics', logicsRes.data)
      }

      this.setState({ status: 'success', lastSyncTime: new Date().toISOString() })
    } catch (e: any) {
      this.setState({ status: 'error', errorMessage: e?.message || '拉取数据失败' })
      throw e
    }
  }

  public async pushToServer(): Promise<void> {
    await this.startSync()
  }

  public async refreshPendingCount(): Promise<void> {
    const count = await getPendingCount()
    this.setState({ pendingCount: count })
  }

  public destroy() {
    if (this.networkUnsubscribe) {
      this.networkUnsubscribe()
      this.networkUnsubscribe = null
    }
    this.listeners.clear()
  }
}

let syncManagerInstance: SyncManager | null = null

export function getSyncManager(): SyncManager {
  if (!syncManagerInstance) {
    syncManagerInstance = new SyncManager()
  }
  return syncManagerInstance
}

export function startSync(): Promise<void> {
  return getSyncManager().startSync()
}

export function getSyncState(): SyncState {
  return getSyncManager().getState()
}

export function subscribeSyncState(listener: (state: SyncState) => void): () => void {
  return getSyncManager().subscribe(listener)
}
