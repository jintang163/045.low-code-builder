export interface Collaborator {
  userId: string
  username: string
  avatar: string
  color: string
  cursorPosition?: { x: number; y: number; componentId?: string }
  isOnline: boolean
  joinTime: number
}

export interface CRDTOperation {
  id: string
  userId: string
  username: string
  type: 'INSERT' | 'UPDATE' | 'DELETE' | 'MOVE' | 'PROP_CHANGE'
  targetType: 'COMPONENT' | 'PAGE' | 'PROPS' | 'STYLE' | 'EVENTS'
  targetId: string
  parentId?: string
  position?: number
  data?: any
  oldData?: any
  timestamp: number
  lamportClock: number
}

export interface ConflictInfo {
  conflictId: string
  operationA: CRDTOperation
  operationB: CRDTOperation
  conflictType: 'PROPERTY_CONFLICT' | 'STRUCTURE_CONFLICT' | 'DELETE_UPDATE_CONFLICT'
  description: string
  status: 'PENDING' | 'RESOLVED'
  createTime: number
}

const COLORS = [
  '#1890ff',
  '#52c41a',
  '#faad14',
  '#f5222d',
  '#722ed1',
  '#13c2c2',
  '#eb2f96',
  '#fa8c16',
  '#2f54eb',
  '#a0d911',
  '#fa541c',
  '#3fc6f7',
  '#9254de',
  '#48bb78',
  '#ed8936',
  '#e53e3e',
]

export function generateColor(userId: string): string {
  let hash = 0
  for (let i = 0; i < userId.length; i++) {
    hash = (hash << 5) - hash + userId.charCodeAt(i)
    hash = hash & hash
  }
  const index = Math.abs(hash) % COLORS.length
  return COLORS[index]
}

export function generateOperationId(): string {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

interface DiffResult {
  type: 'added' | 'removed' | 'updated'
  path: string
  oldValue?: any
  newValue?: any
}

export function deepDiff(oldObj: any, newObj: any, path = ''): DiffResult[] {
  const diffs: DiffResult[] = []

  if (oldObj === newObj) return diffs

  if (typeof oldObj !== 'object' || oldObj === null || typeof newObj !== 'object' || newObj === null) {
    diffs.push({
      type: 'updated',
      path: path || 'root',
      oldValue: oldObj,
      newValue: newObj,
    })
    return diffs
  }

  const oldKeys = Object.keys(oldObj)
  const newKeys = Object.keys(newObj)
  const allKeys = new Set([...oldKeys, ...newKeys])

  allKeys.forEach((key) => {
    const currentPath = path ? `${path}.${key}` : key

    if (!(key in oldObj)) {
      diffs.push({
        type: 'added',
        path: currentPath,
        newValue: newObj[key],
      })
    } else if (!(key in newObj)) {
      diffs.push({
        type: 'removed',
        path: currentPath,
        oldValue: oldObj[key],
      })
    } else if (typeof oldObj[key] === 'object' && typeof newObj[key] === 'object' && oldObj[key] !== null && newObj[key] !== null) {
      diffs.push(...deepDiff(oldObj[key], newObj[key], currentPath))
    } else if (oldObj[key] !== newObj[key]) {
      diffs.push({
        type: 'updated',
        path: currentPath,
        oldValue: oldObj[key],
        newValue: newObj[key],
      })
    }
  })

  return diffs
}

export function formatOperationDescription(op: CRDTOperation): string {
  const typeMap: Record<string, string> = {
    INSERT: '添加',
    UPDATE: '更新',
    DELETE: '删除',
    MOVE: '移动',
    PROP_CHANGE: '修改属性',
  }

  const targetTypeMap: Record<string, string> = {
    COMPONENT: '组件',
    PAGE: '页面',
    PROPS: '属性',
    STYLE: '样式',
    EVENTS: '事件',
  }

  const typeDesc = typeMap[op.type] || op.type
  const targetTypeDesc = targetTypeMap[op.targetType] || op.targetType

  let description = `${op.username} ${typeDesc}了${targetTypeDesc}`

  if (op.targetId) {
    description += ` (${op.targetId})`
  }

  return description
}

export function throttle<T extends (...args: any[]) => any>(fn: T, delay: number): (...args: Parameters<T>) => void {
  let lastTime = 0
  let timer: number | null = null

  return function (this: any, ...args: Parameters<T>) {
    const now = Date.now()
    const remaining = delay - (now - lastTime)

    if (remaining <= 0) {
      if (timer) {
        clearTimeout(timer)
        timer = null
      }
      lastTime = now
      fn.apply(this, args)
    } else if (!timer) {
      timer = window.setTimeout(() => {
        lastTime = Date.now()
        timer = null
        fn.apply(this, args)
      }, remaining)
    }
  }
}

export function debounce<T extends (...args: any[]) => any>(fn: T, delay: number): (...args: Parameters<T>) => void {
  let timer: number | null = null

  return function (this: any, ...args: Parameters<T>) {
    if (timer) {
      clearTimeout(timer)
    }
    timer = window.setTimeout(() => {
      fn.apply(this, args)
      timer = null
    }, delay)
  }
}

export function formatJoinTime(timestamp: number): string {
  const date = new Date(timestamp)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

export type WsMessageType =
  | 'JOIN' | 'LEAVE' | 'OPERATION' | 'CURSOR'
  | 'PRESENCE' | 'CONFLICT' | 'RESOLVE' | 'SYNC'
  | 'ACK' | 'PING' | 'PONG' | 'ERROR'

export interface WsMessage {
  type: WsMessageType
  data?: any
  timestamp?: number
}

export interface JoinPayload {
  userId: string
  username: string
  avatar?: string
}

export interface CursorPayload {
  userId: string
  position: {
    x: number
    y: number
    componentId?: string
  }
}

export interface PresencePayload {
  collaborators: Collaborator[]
}

export interface ConflictPayload {
  conflicts: ConflictInfo[]
}

export interface SyncPayload {
  documentState?: any
  collaborators: Collaborator[]
  conflicts?: ConflictInfo[]
  documentVersion?: number
}

export interface AckPayload {
  operationId?: string
  lamportClock?: number
  conflictId?: string
  success?: boolean
}

export interface ErrorPayload {
  code: string
  message: string
}
