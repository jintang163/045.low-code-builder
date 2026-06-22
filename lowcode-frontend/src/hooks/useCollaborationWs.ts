import { useEffect, useRef, useState, useCallback } from 'react'

export interface WsMessage {
  type: string
  data: any
  timestamp: number
}

export interface UseCollaborationWsOptions {
  appId: number | string
  userId: number | string
  username?: string
  avatar?: string
  targetType?: string
  targetId?: number | string
  onMessage?: (message: WsMessage) => void
  onConnected?: () => void
  onDisconnected?: () => void
}

export const useCollaborationWs = (options: UseCollaborationWsOptions) => {
  const { appId, userId, username, avatar, targetType, targetId, onMessage, onConnected, onDisconnected } = options
  const [isConnected, setIsConnected] = useState(false)
  const [lastMessage, setLastMessage] = useState<WsMessage | null>(null)
  const wsRef = useRef<WebSocket | null>(null)
  const heartbeatRef = useRef<number | null>(null)
  const reconnectRef = useRef<number>(0)

  const connect = useCallback(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.hostname
    const port = '8087'
    const params = new URLSearchParams({
      userId: String(userId),
      username: username || '',
      avatar: avatar || '',
      targetType: targetType || '',
      targetId: targetId ? String(targetId) : '',
    })
    const url = `${protocol}//${host}:${port}/ws/collaboration/${appId}?${params.toString()}`

    try {
      const ws = new WebSocket(url)
      wsRef.current = ws

      ws.onopen = () => {
        setIsConnected(true)
        reconnectRef.current = 0
        onConnected?.()
        startHeartbeat()
      }

      ws.onmessage = (event) => {
        try {
          const msg: WsMessage = JSON.parse(event.data)
          setLastMessage(msg)
          onMessage?.(msg)
        } catch (e) {
          console.error('解析WebSocket消息失败:', e)
        }
      }

      ws.onerror = (error) => {
        console.error('WebSocket错误:', error)
      }

      ws.onclose = () => {
        setIsConnected(false)
        stopHeartbeat()
        onDisconnected?.()
        tryReconnect()
      }
    } catch (e) {
      console.error('创建WebSocket连接失败:', e)
      tryReconnect()
    }
  }, [appId, userId, username, avatar, targetType, targetId, onConnected, onDisconnected, onMessage])

  const startHeartbeat = () => {
    stopHeartbeat()
    heartbeatRef.current = window.setInterval(() => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ type: 'PING', timestamp: Date.now() }))
      }
    }, 30000)
  }

  const stopHeartbeat = () => {
    if (heartbeatRef.current) {
      clearInterval(heartbeatRef.current)
      heartbeatRef.current = null
    }
  }

  const tryReconnect = () => {
    if (reconnectRef.current >= 5) return
    const delay = Math.min(1000 * Math.pow(2, reconnectRef.current), 30000)
    reconnectRef.current++
    setTimeout(() => {
      if (!isConnected) {
        connect()
      }
    }, delay)
  }

  const disconnect = useCallback(() => {
    stopHeartbeat()
    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
    }
    setIsConnected(false)
  }, [])

  const send = useCallback((data: any) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(data))
      return true
    }
    return false
  }, [])

  useEffect(() => {
    if (appId && userId) {
      connect()
    }
    return () => {
      disconnect()
    }
  }, [appId, userId, connect, disconnect])

  return {
    isConnected,
    lastMessage,
    send,
    reconnect: connect,
    disconnect,
  }
}

export default useCollaborationWs
