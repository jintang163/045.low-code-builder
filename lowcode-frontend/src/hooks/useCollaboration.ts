import { useState, useCallback, useEffect, useRef } from 'react'
import {
  Collaborator,
  CRDTOperation,
  ConflictInfo,
  generateColor,
  generateOperationId,
  throttle,
  WsMessage,
  WsMessageType,
  JoinPayload,
  CursorPayload,
  PresencePayload,
  SyncPayload,
  AckPayload,
  ErrorPayload,
} from '@/utils/collaboration'

export interface CursorPosition {
  x: number
  y: number
  componentId?: string
  selection?: {
    start: number
    end: number
  }
}

export interface UserInfo {
  userId: string
  username: string
  avatar?: string
}

export interface CollaborationState {
  isConnected: boolean
  collaborators: Collaborator[]
  conflicts: ConflictInfo[]
  documentVersion: number
  connectionError: string | null
}

const HEARTBEAT_INTERVAL = 30000
const MAX_RECONNECT_ATTEMPTS = 10
const BASE_RECONNECT_DELAY = 1000
const MESSAGE_QUEUE_KEY = 'collab_message_queue'

function normalizeCollaborator(c: Partial<Collaborator> & { userId: string; username?: string }): Collaborator {
  return {
    userId: c.userId,
    username: c.username || c.userId,
    avatar: c.avatar || '',
    color: c.color || generateColor(c.userId),
    cursorPosition: c.cursorPosition,
    isOnline: c.isOnline !== undefined ? c.isOnline : true,
    joinTime: c.joinTime || Date.now(),
  }
}

function extractMessageData(message: any): any {
  if (message.data !== undefined) {
    return message.data
  }
  return message
}

function extractCursorPosition(data: any): { x: number; y: number; componentId?: string } | undefined {
  if (data.position) return data.position
  if (data.cursorPosition) return data.cursorPosition
  return undefined
}

export function useCollaboration() {
  const [state, setState] = useState<CollaborationState>({
    isConnected: false,
    collaborators: [],
    conflicts: [],
    documentVersion: 0,
    connectionError: null,
  })

  const wsRef = useRef<WebSocket | null>(null)
  const pageIdRef = useRef<string | null>(null)
  const userInfoRef = useRef<UserInfo | null>(null)
  const reconnectAttemptsRef = useRef(0)
  const reconnectTimerRef = useRef<number | null>(null)
  const heartbeatTimerRef = useRef<number | null>(null)
  const messageQueueRef = useRef<WsMessage[]>([])
  const lamportClockRef = useRef(0)
  const onOperationRef = useRef<((op: CRDTOperation) => void) | null>(null)
  const onSyncRef = useRef<((data: any) => void) | null>(null)

  const incrementClock = useCallback(() => {
    lamportClockRef.current += 1
    return lamportClockRef.current
  }, [])

  const saveMessageQueue = useCallback(() => {
    try {
      localStorage.setItem(MESSAGE_QUEUE_KEY, JSON.stringify(messageQueueRef.current))
    } catch (e) {
      console.warn('Failed to save message queue:', e)
    }
  }, [])

  const loadMessageQueue = useCallback(() => {
    try {
      const saved = localStorage.getItem(MESSAGE_QUEUE_KEY)
      if (saved) {
        messageQueueRef.current = JSON.parse(saved)
      }
    } catch (e) {
      console.warn('Failed to load message queue:', e)
    }
  }, [])

  const sendMessage = useCallback((message: WsMessage) => {
    const msgWithTimestamp: WsMessage = {
      ...message,
      timestamp: message.timestamp ?? Date.now(),
    }
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(msgWithTimestamp))
    } else {
      messageQueueRef.current.push(msgWithTimestamp)
      saveMessageQueue()
    }
  }, [saveMessageQueue])

  const flushMessageQueue = useCallback(() => {
    while (messageQueueRef.current.length > 0) {
      const message = messageQueueRef.current.shift()
      if (message && wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify(message))
      } else if (message) {
        messageQueueRef.current.unshift(message)
        break
      }
    }
    saveMessageQueue()
  }, [saveMessageQueue])

  const startHeartbeat = useCallback(() => {
    if (heartbeatTimerRef.current) {
      clearInterval(heartbeatTimerRef.current)
    }
    heartbeatTimerRef.current = window.setInterval(() => {
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        const pingMsg: WsMessage = {
          type: 'PING',
          timestamp: Date.now(),
        }
        wsRef.current.send(JSON.stringify(pingMsg))
      }
    }, HEARTBEAT_INTERVAL)
  }, [])

  const stopHeartbeat = useCallback(() => {
    if (heartbeatTimerRef.current) {
      clearInterval(heartbeatTimerRef.current)
      heartbeatTimerRef.current = null
    }
  }, [])

  const handleMessage = useCallback((event: MessageEvent) => {
    try {
      const message: any = JSON.parse(event.data)
      const messageType: WsMessageType = message.type
      const data = extractMessageData(message)

      switch (messageType) {
        case 'SYNC': {
          const syncData = data as SyncPayload
          const { documentState, collaborators, conflicts, documentVersion } = syncData

          setState((prev) => ({
            ...prev,
            collaborators: Array.isArray(collaborators)
              ? collaborators.map((c) => normalizeCollaborator(c))
              : prev.collaborators,
            conflicts: Array.isArray(conflicts) ? conflicts : prev.conflicts,
            documentVersion: documentVersion ?? prev.documentVersion,
          }))

          if (onSyncRef.current && documentState !== undefined) {
            onSyncRef.current(documentState)
          }
          break
        }

        case 'PRESENCE': {
          const presenceData = data as PresencePayload
          if (presenceData.collaborators && Array.isArray(presenceData.collaborators)) {
            setState((prev) => ({
              ...prev,
              collaborators: presenceData.collaborators.map((c) => normalizeCollaborator(c)),
            }))
          }
          break
        }

        case 'JOIN': {
          const joinData = data as JoinPayload
          if (joinData && joinData.userId) {
            const newCollaborator: Collaborator = normalizeCollaborator({
              userId: joinData.userId,
              username: joinData.username,
              avatar: joinData.avatar,
              isOnline: true,
              joinTime: Date.now(),
            })
            setState((prev) => ({
              ...prev,
              collaborators: [
                ...prev.collaborators.filter((c) => c.userId !== newCollaborator.userId),
                newCollaborator,
              ],
            }))
          }
          break
        }

        case 'LEAVE': {
          const leaveData = data as { userId?: string }
          const userId = leaveData?.userId || message.userId
          if (userId) {
            setState((prev) => ({
              ...prev,
              collaborators: prev.collaborators.map((c) =>
                c.userId === userId ? { ...c, isOnline: false } : c
              ),
            }))
          }
          break
        }

        case 'OPERATION': {
          const operation: CRDTOperation = data as CRDTOperation
          if (operation && operation.lamportClock > lamportClockRef.current) {
            lamportClockRef.current = operation.lamportClock
          }
          if (onOperationRef.current && operation) {
            onOperationRef.current(operation)
          }
          break
        }

        case 'CURSOR': {
          const cursorData = data as CursorPayload
          const userId = cursorData?.userId || message.userId
          const position = extractCursorPosition(cursorData || message)
          if (userId && position) {
            setState((prev) => ({
              ...prev,
              collaborators: prev.collaborators.map((c) =>
                c.userId === userId ? { ...c, cursorPosition: position } : c
              ),
            }))
          }
          break
        }

        case 'CONFLICT': {
          const conflictData = data as { conflicts?: ConflictInfo[]; conflict?: ConflictInfo }
          const incomingConflicts: ConflictInfo[] = []
          if (conflictData?.conflicts && Array.isArray(conflictData.conflicts)) {
            incomingConflicts.push(...conflictData.conflicts)
          } else if (conflictData?.conflict) {
            incomingConflicts.push(conflictData.conflict)
          } else if (data && !Array.isArray(data) && (data as ConflictInfo).conflictId) {
            incomingConflicts.push(data as ConflictInfo)
          }
          if (incomingConflicts.length > 0) {
            setState((prev) => ({
              ...prev,
              conflicts: [...prev.conflicts, ...incomingConflicts],
            }))
          }
          break
        }

        case 'ACK': {
          const ackData = data as AckPayload
          if (ackData?.lamportClock !== undefined && ackData.lamportClock > lamportClockRef.current) {
            lamportClockRef.current = ackData.lamportClock
          }
          break
        }

        case 'PONG': {
          break
        }

        case 'PING': {
          if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
            const pongMsg: WsMessage = {
              type: 'PONG',
              timestamp: Date.now(),
            }
            wsRef.current.send(JSON.stringify(pongMsg))
          }
          break
        }

        case 'ERROR': {
          const errorData = data as ErrorPayload
          setState((prev) => ({
            ...prev,
            connectionError: errorData?.message || '连接错误',
          }))
          break
        }

        default:
          break
      }
    } catch (e) {
      console.error('Failed to parse WebSocket message:', e)
    }
  }, [])

  const connect = useCallback((pageId: string, userInfo: UserInfo) => {
    if (wsRef.current) {
      wsRef.current.close()
    }

    pageIdRef.current = pageId
    userInfoRef.current = userInfo
    loadMessageQueue()

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/collaboration?pageId=${pageId}&userId=${userInfo.userId}&username=${encodeURIComponent(userInfo.username)}&avatar=${encodeURIComponent(userInfo.avatar || '')}`

    try {
      const ws = new WebSocket(wsUrl)
      wsRef.current = ws

      ws.onopen = () => {
        setState((prev) => ({
          ...prev,
          isConnected: true,
          connectionError: null,
        }))
        reconnectAttemptsRef.current = 0
        startHeartbeat()
        flushMessageQueue()
      }

      ws.onmessage = handleMessage

      ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        setState((prev) => ({
          ...prev,
          connectionError: 'WebSocket连接错误',
        }))
      }

      ws.onclose = (event) => {
        setState((prev) => ({
          ...prev,
          isConnected: false,
        }))
        stopHeartbeat()

        if (event.code !== 1000) {
          attemptReconnect()
        }
      }
    } catch (e) {
      console.error('Failed to create WebSocket connection:', e)
      setState((prev) => ({
        ...prev,
        connectionError: '创建连接失败',
      }))
      attemptReconnect()
    }
  }, [handleMessage, startHeartbeat, stopHeartbeat, flushMessageQueue, loadMessageQueue])

  const attemptReconnect = useCallback(() => {
    if (reconnectAttemptsRef.current >= MAX_RECONNECT_ATTEMPTS) {
      setState((prev) => ({
        ...prev,
        connectionError: '重连次数已达上限，请刷新页面重试',
      }))
      return
    }

    const delay = BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttemptsRef.current)
    reconnectAttemptsRef.current += 1

    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current)
    }

    reconnectTimerRef.current = window.setTimeout(() => {
      if (pageIdRef.current && userInfoRef.current) {
        connect(pageIdRef.current, userInfoRef.current)
      }
    }, delay)
  }, [connect])

  const disconnect = useCallback(() => {
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current)
      reconnectTimerRef.current = null
    }
    stopHeartbeat()

    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
    }

    setState({
      isConnected: false,
      collaborators: [],
      conflicts: [],
      documentVersion: 0,
      connectionError: null,
    })
  }, [stopHeartbeat])

  const sendOperation = useCallback((operation: Omit<CRDTOperation, 'id' | 'timestamp' | 'lamportClock' | 'userId' | 'username'>) => {
    if (!userInfoRef.current) return

    const fullOperation: CRDTOperation = {
      id: generateOperationId(),
      userId: userInfoRef.current.userId,
      username: userInfoRef.current.username,
      type: operation.type,
      targetType: operation.targetType,
      targetId: operation.targetId,
      parentId: operation.parentId,
      position: operation.position,
      data: operation.data,
      oldData: operation.oldData,
      timestamp: Date.now(),
      lamportClock: incrementClock(),
    }

    const message: WsMessage = {
      type: 'OPERATION',
      data: fullOperation,
      timestamp: Date.now(),
    }

    sendMessage(message)
  }, [sendMessage, incrementClock])

  const sendCursorPosition = useCallback(
    throttle((position: CursorPosition) => {
      if (!userInfoRef.current) return

      const message: WsMessage = {
        type: 'CURSOR',
        data: {
          userId: userInfoRef.current.userId,
          position,
        },
        timestamp: Date.now(),
      }

      sendMessage(message)
    }, 50),
    [sendMessage]
  )

  const resolveConflict = useCallback((conflictId: string, resolution: 'A' | 'B' | 'MANUAL', chosenUserId?: string) => {
    const message: WsMessage = {
      type: 'RESOLVE',
      data: {
        conflictId,
        resolution,
        chosenUserId,
      },
      timestamp: Date.now(),
    }

    sendMessage(message)

    setState((prev) => ({
      ...prev,
      conflicts: prev.conflicts.map((c) =>
        c.conflictId === conflictId ? { ...c, status: 'RESOLVED' } : c
      ),
    }))
  }, [sendMessage])

  const setOnOperation = useCallback((callback: (op: CRDTOperation) => void) => {
    onOperationRef.current = callback
  }, [])

  const setOnSync = useCallback((callback: (data: any) => void) => {
    onSyncRef.current = callback
  }, [])

  useEffect(() => {
    return () => {
      disconnect()
    }
  }, [disconnect])

  return {
    state,
    collaborators: state.collaborators,
    conflicts: state.conflicts,
    isConnected: state.isConnected,
    connectionError: state.connectionError,
    documentVersion: state.documentVersion,
    connect,
    disconnect,
    sendOperation,
    sendCursorPosition,
    resolveConflict,
    setOnOperation,
    setOnSync,
  }
}

export default useCollaboration
