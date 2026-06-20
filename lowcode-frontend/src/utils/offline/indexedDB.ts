export type StoreName =
  | 'pages'
  | 'dataModels'
  | 'businessLogics'
  | 'components'
  | 'dataSources'
  | 'pendingChanges'
  | 'syncHistory'
  | 'appData'

export type ResourceType = 'page' | 'dataModel' | 'businessLogic' | 'dataSource'
export type ChangeAction = 'create' | 'update' | 'delete'
export type ChangeStatus = 'pending' | 'syncing' | 'failed' | 'synced'

export interface PendingChange {
  id?: number
  resourceType: ResourceType
  resourceId: number | string
  action: ChangeAction
  data: any
  status: ChangeStatus
  retryCount: number
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export interface SyncHistoryItem {
  id?: number
  startTime: string
  endTime?: string
  status: 'success' | 'failed' | 'partial'
  totalCount: number
  successCount: number
  failedCount: number
  errorMessage?: string
}

export interface BaseEntity {
  id?: number
  createdAt?: string
  updatedAt?: string
}

const DB_NAME = 'lowcode-designer-db'
const DB_VERSION = 2

const STORE_CONFIGS: {
  name: StoreName
  keyPath: string
  autoIncrement?: boolean
  indexes?: { name: string; keyPath: string | string[]; options?: IDBIndexParameters }[]
}[] = [
  {
    name: 'pages',
    keyPath: 'id',
  },
  {
    name: 'dataModels',
    keyPath: 'id',
  },
  {
    name: 'businessLogics',
    keyPath: 'id',
  },
  {
    name: 'components',
    keyPath: 'id',
  },
  {
    name: 'dataSources',
    keyPath: 'id',
  },
  {
    name: 'pendingChanges',
    keyPath: 'id',
    autoIncrement: true,
    indexes: [
      { name: 'resourceType', keyPath: 'resourceType' },
      { name: 'status', keyPath: 'status' },
      { name: 'resourceType_status', keyPath: ['resourceType', 'status'] },
    ],
  },
  {
    name: 'syncHistory',
    keyPath: 'id',
    autoIncrement: true,
  },
  {
    name: 'appData',
    keyPath: 'key',
  },
]

let dbInstance: IDBDatabase | null = null
let initPromise: Promise<IDBDatabase> | null = null

export function initDB(): Promise<IDBDatabase> {
  if (dbInstance) {
    return Promise.resolve(dbInstance)
  }

  if (initPromise) {
    return initPromise
  }

  initPromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onerror = () => {
      console.error('IndexedDB 打开失败:', request.error)
      initPromise = null
      reject(request.error)
    }

    request.onsuccess = () => {
      dbInstance = request.result
      dbInstance.onversionchange = () => {
        dbInstance?.close()
        dbInstance = null
        initPromise = null
      }
      resolve(dbInstance)
    }

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result

      STORE_CONFIGS.forEach((config) => {
        if (!db.objectStoreNames.contains(config.name)) {
          const store = db.createObjectStore(config.name, {
            keyPath: config.keyPath,
            autoIncrement: config.autoIncrement,
          })

          config.indexes?.forEach((index) => {
            store.createIndex(index.name, index.keyPath, index.options)
          })
        }
      })
    }
  })

  return initPromise
}

export async function getDB(): Promise<IDBDatabase> {
  if (!dbInstance) {
    return initDB()
  }
  return dbInstance
}

function promisifyRequest<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

export async function get<T = any>(storeName: StoreName, key: IDBValidKey): Promise<T | undefined> {
  const db = await getDB()
  const transaction = db.transaction(storeName, 'readonly')
  const store = transaction.objectStore(storeName)
  return promisifyRequest<T>(store.get(key) as IDBRequest<T>)
}

export async function set<T = any>(storeName: StoreName, key: IDBValidKey, value: T): Promise<void> {
  const db = await getDB()
  const transaction = db.transaction(storeName, 'readwrite')
  const store = transaction.objectStore(storeName)

  const data = value as any
  const now = new Date().toISOString()

  if (typeof data === 'object' && data !== null) {
    if (!data.createdAt) {
      data.createdAt = now
    }
    data.updatedAt = now
  }

  if (storeName === 'appData') {
    store.put({ key, value, updatedAt: now })
  } else {
    store.put(data)
  }

  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error)
  })
}

export async function remove(storeName: StoreName, key: IDBValidKey): Promise<void> {
  const db = await getDB()
  const transaction = db.transaction(storeName, 'readwrite')
  const store = transaction.objectStore(storeName)
  store.delete(key)

  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error)
  })
}

export async function getAll<T = any>(storeName: StoreName): Promise<T[]> {
  const db = await getDB()
  const transaction = db.transaction(storeName, 'readonly')
  const store = transaction.objectStore(storeName)
  return promisifyRequest<T[]>(store.getAll() as IDBRequest<T[]>)
}

export async function clear(storeName: StoreName): Promise<void> {
  const db = await getDB()
  const transaction = db.transaction(storeName, 'readwrite')
  const store = transaction.objectStore(storeName)
  store.clear()

  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error)
  })
}

export async function bulkPut<T = any>(storeName: StoreName, items: T[]): Promise<void> {
  const db = await getDB()
  const transaction = db.transaction(storeName, 'readwrite')
  const store = transaction.objectStore(storeName)
  const now = new Date().toISOString()

  items.forEach((item) => {
    const data = item as any
    if (typeof data === 'object' && data !== null) {
      if (!data.createdAt) {
        data.createdAt = now
      }
      data.updatedAt = now
    }
    store.put(data)
  })

  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error)
  })
}

export async function addPendingChange(change: Omit<PendingChange, 'id' | 'createdAt' | 'updatedAt' | 'retryCount' | 'status'> & { status?: ChangeStatus; retryCount?: number }): Promise<number> {
  const db = await getDB()
  const transaction = db.transaction('pendingChanges', 'readwrite')
  const store = transaction.objectStore('pendingChanges')
  const now = new Date().toISOString()

  const fullChange: PendingChange = {
    ...change,
    status: change.status || 'pending',
    retryCount: change.retryCount || 0,
    createdAt: now,
    updatedAt: now,
  }

  const request = store.add(fullChange)

  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result as number)
    transaction.onerror = () => reject(transaction.error)
  })
}

export async function getPendingChanges(resourceType?: ResourceType): Promise<PendingChange[]> {
  const db = await getDB()
  const transaction = db.transaction('pendingChanges', 'readonly')
  const store = transaction.objectStore('pendingChanges')

  let request: IDBRequest<PendingChange[]>

  if (resourceType) {
    const index = store.index('resourceType')
    request = index.getAll(resourceType) as IDBRequest<PendingChange[]>
  } else {
    request = store.getAll() as IDBRequest<PendingChange[]>
  }

  return promisifyRequest<PendingChange[]>(request)
}

export async function getPendingChangesByStatus(status: ChangeStatus): Promise<PendingChange[]> {
  const db = await getDB()
  const transaction = db.transaction('pendingChanges', 'readonly')
  const store = transaction.objectStore('pendingChanges')
  const index = store.index('status')
  return promisifyRequest<PendingChange[]>(index.getAll(status) as IDBRequest<PendingChange[]>)
}

export async function markChangeSynced(changeId: number): Promise<void> {
  const db = await getDB()
  const transaction = db.transaction('pendingChanges', 'readwrite')
  const store = transaction.objectStore('pendingChanges')

  const getRequest = store.get(changeId) as IDBRequest<PendingChange>

  getRequest.onsuccess = () => {
    const change = getRequest.result
    if (change) {
      change.status = 'synced'
      change.updatedAt = new Date().toISOString()
      store.put(change)
    }
  }

  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error)
  })
}

export async function markChangeFailed(changeId: number, errorMessage: string): Promise<void> {
  const db = await getDB()
  const transaction = db.transaction('pendingChanges', 'readwrite')
  const store = transaction.objectStore('pendingChanges')

  const getRequest = store.get(changeId) as IDBRequest<PendingChange>

  getRequest.onsuccess = () => {
    const change = getRequest.result
    if (change) {
      change.status = 'failed'
      change.retryCount = (change.retryCount || 0) + 1
      change.errorMessage = errorMessage
      change.updatedAt = new Date().toISOString()
      store.put(change)
    }
  }

  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error)
  })
}

export async function markChangeSyncing(changeId: number): Promise<void> {
  const db = await getDB()
  const transaction = db.transaction('pendingChanges', 'readwrite')
  const store = transaction.objectStore('pendingChanges')

  const getRequest = store.get(changeId) as IDBRequest<PendingChange>

  getRequest.onsuccess = () => {
    const change = getRequest.result
    if (change) {
      change.status = 'syncing'
      change.updatedAt = new Date().toISOString()
      store.put(change)
    }
  }

  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error)
  })
}

export async function clearSyncedChanges(): Promise<number> {
  const db = await getDB()
  const transaction = db.transaction('pendingChanges', 'readwrite')
  const store = transaction.objectStore('pendingChanges')
  const index = store.index('status')

  const cursorRequest = index.openCursor('synced')
  let count = 0

  cursorRequest.onsuccess = () => {
    const cursor = cursorRequest.result
    if (cursor) {
      cursor.delete()
      count++
      cursor.continue()
    }
  }

  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve(count)
    transaction.onerror = () => reject(transaction.error)
  })
}

export async function getPendingCount(): Promise<number> {
  const db = await getDB()
  const transaction = db.transaction('pendingChanges', 'readonly')
  const store = transaction.objectStore('pendingChanges')
  const index = store.index('status')

  return new Promise((resolve, reject) => {
    const countRequest = index.count('pending')
    countRequest.onsuccess = () => resolve(countRequest.result)
    countRequest.onerror = () => reject(countRequest.error)
  })
}

export async function addSyncHistory(history: Omit<SyncHistoryItem, 'id'>): Promise<number> {
  const db = await getDB()
  const transaction = db.transaction('syncHistory', 'readwrite')
  const store = transaction.objectStore('syncHistory')
  const request = store.add(history)

  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result as number)
    transaction.onerror = () => reject(transaction.error)
  })
}

export async function getLastSync(): Promise<SyncHistoryItem | undefined> {
  const items = await getAll<SyncHistoryItem>('syncHistory')
  if (items.length === 0) return undefined
  return items.sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime())[0]
}

export async function getAppData<T = any>(key: string): Promise<T | undefined> {
  const result = await get<any>('appData', key)
  return result?.value
}

export async function setAppData<T = any>(key: string, value: T): Promise<void> {
  const db = await getDB()
  const transaction = db.transaction('appData', 'readwrite')
  const store = transaction.objectStore('appData')
  const now = new Date().toISOString()
  store.put({ key, value, updatedAt: now, createdAt: now })

  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error)
  })
}

export function closeDB(): void {
  if (dbInstance) {
    dbInstance.close()
    dbInstance = null
    initPromise = null
  }
}

export function deleteDB(): Promise<void> {
  closeDB()
  return new Promise((resolve, reject) => {
    const request = indexedDB.deleteDatabase(DB_NAME)
    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}
