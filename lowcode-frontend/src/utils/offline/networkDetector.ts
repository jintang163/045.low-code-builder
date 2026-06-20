export type NetworkStatus = 'online' | 'offline' | 'checking'

interface NetworkDetectorOptions {
  heartbeatUrl?: string
  heartbeatInterval?: number
  enableHeartbeat?: boolean
}

const DEFAULT_OPTIONS: Required<NetworkDetectorOptions> = {
  heartbeatUrl: '/api/health',
  heartbeatInterval: 30000,
  enableHeartbeat: false,
}

class NetworkDetector {
  private status: NetworkStatus = 'online'
  private listeners: Set<(status: NetworkStatus) => void> = new Set()
  private options: Required<NetworkDetectorOptions>
  private heartbeatTimer: number | null = null

  constructor(options?: NetworkDetectorOptions) {
    this.options = { ...DEFAULT_OPTIONS, ...options }
    this.init()
  }

  private init(): void {
    this.status = navigator.onLine ? 'online' : 'offline'

    window.addEventListener('online', this.handleOnline)
    window.addEventListener('offline', this.handleOffline)

    if (this.options.enableHeartbeat) {
      this.startHeartbeat()
    }
  }

  private handleOnline = (): void => {
    this.setStatus('online')
  }

  private handleOffline = (): void => {
    this.setStatus('offline')
  }

  private setStatus(status: NetworkStatus): void {
    if (this.status !== status) {
      this.status = status
      this.notifyListeners()
    }
  }

  private notifyListeners(): void {
    this.listeners.forEach((listener) => {
      try {
        listener(this.status)
      } catch (e) {
        console.error('网络状态监听回调执行失败:', e)
      }
    })
  }

  private async checkConnectivity(): Promise<boolean> {
    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 5000)

      const response = await fetch(this.options.heartbeatUrl, {
        method: 'HEAD',
        signal: controller.signal,
        cache: 'no-store',
      })

      clearTimeout(timeoutId)
      return response.ok
    } catch {
      return false
    }
  }

  private startHeartbeat(): void {
    if (this.heartbeatTimer) {
      return
    }

    const check = async () => {
      this.setStatus('checking')
      const isOnline = await this.checkConnectivity()
      this.setStatus(isOnline ? 'online' : 'offline')
    }

    this.heartbeatTimer = window.setInterval(check, this.options.heartbeatInterval)
    check()
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  public getStatus(): NetworkStatus {
    return this.status
  }

  public isOnline(): boolean {
    return this.status === 'online'
  }

  public async checkNow(): Promise<boolean> {
    this.setStatus('checking')
    const isOnline = await this.checkConnectivity()
    this.setStatus(isOnline ? 'online' : 'offline')
    return isOnline
  }

  public subscribe(listener: (status: NetworkStatus) => void): () => void {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  public enableHeartbeat(enable: boolean): void {
    this.options.enableHeartbeat = enable
    if (enable) {
      this.startHeartbeat()
    } else {
      this.stopHeartbeat()
    }
  }

  public destroy(): void {
    window.removeEventListener('online', this.handleOnline)
    window.removeEventListener('offline', this.handleOffline)
    this.stopHeartbeat()
    this.listeners.clear()
  }
}

let detectorInstance: NetworkDetector | null = null

export function getNetworkDetector(): NetworkDetector {
  if (!detectorInstance) {
    detectorInstance = new NetworkDetector()
  }
  return detectorInstance
}

export function isOnline(): boolean {
  return getNetworkDetector().isOnline()
}

export function getNetworkStatus(): NetworkStatus {
  return getNetworkDetector().getStatus()
}

export function subscribeNetworkStatus(listener: (status: NetworkStatus) => void): () => void {
  return getNetworkDetector().subscribe(listener)
}
