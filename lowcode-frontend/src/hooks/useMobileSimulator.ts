import { useState, useCallback, useMemo } from 'react'

export type Platform = 'ios' | 'android' | 'harmony'

export interface DeviceConfig {
  id: string
  name: string
  width: number
  height: number
  pixelRatio: number
  platform: Platform
  hasNotch: boolean
  hasSafeArea: boolean
  notchHeight: number
  safeAreaBottom: number
  borderRadius: number
}

export interface SimulatorState {
  device: DeviceConfig
  rotation: 'portrait' | 'landscape'
  scale: number
  url: string
  touchEventsEnabled: boolean
  gesturesEnabled: boolean
}

export const DEVICE_LIST: DeviceConfig[] = [
  {
    id: 'iphone-14',
    name: 'iPhone 14',
    width: 390,
    height: 844,
    pixelRatio: 3,
    platform: 'ios',
    hasNotch: true,
    hasSafeArea: true,
    notchHeight: 47,
    safeAreaBottom: 34,
    borderRadius: 40,
  },
  {
    id: 'iphone-14-pro-max',
    name: 'iPhone 14 Pro Max',
    width: 430,
    height: 932,
    pixelRatio: 3,
    platform: 'ios',
    hasNotch: true,
    hasSafeArea: true,
    notchHeight: 54,
    safeAreaBottom: 34,
    borderRadius: 47,
  },
  {
    id: 'iphone-se',
    name: 'iPhone SE',
    width: 375,
    height: 667,
    pixelRatio: 2,
    platform: 'ios',
    hasNotch: false,
    hasSafeArea: false,
    notchHeight: 0,
    safeAreaBottom: 0,
    borderRadius: 20,
  },
  {
    id: 'samsung-s23',
    name: 'Samsung S23',
    width: 365,
    height: 780,
    pixelRatio: 3,
    platform: 'android',
    hasNotch: false,
    hasSafeArea: true,
    notchHeight: 24,
    safeAreaBottom: 16,
    borderRadius: 30,
  },
  {
    id: 'samsung-s23-ultra',
    name: 'Samsung S23 Ultra',
    width: 393,
    height: 853,
    pixelRatio: 3,
    platform: 'android',
    hasNotch: false,
    hasSafeArea: true,
    notchHeight: 24,
    safeAreaBottom: 16,
    borderRadius: 30,
  },
  {
    id: 'huawei-p60',
    name: 'Huawei P60',
    width: 373,
    height: 791,
    pixelRatio: 3,
    platform: 'harmony',
    hasNotch: false,
    hasSafeArea: true,
    notchHeight: 24,
    safeAreaBottom: 16,
    borderRadius: 30,
  },
  {
    id: 'xiaomi-13',
    name: 'Xiaomi 13',
    width: 393,
    height: 873,
    pixelRatio: 3,
    platform: 'android',
    hasNotch: false,
    hasSafeArea: true,
    notchHeight: 24,
    safeAreaBottom: 16,
    borderRadius: 28,
  },
  {
    id: 'ipad-pro-11',
    name: 'iPad Pro 11"',
    width: 834,
    height: 1194,
    pixelRatio: 2,
    platform: 'ios',
    hasNotch: false,
    hasSafeArea: true,
    notchHeight: 24,
    safeAreaBottom: 20,
    borderRadius: 16,
  },
  {
    id: 'ipad-mini',
    name: 'iPad Mini',
    width: 744,
    height: 1133,
    pixelRatio: 2,
    platform: 'ios',
    hasNotch: false,
    hasSafeArea: true,
    notchHeight: 24,
    safeAreaBottom: 20,
    borderRadius: 16,
  },
  {
    id: 'huawei-matepad',
    name: 'Huawei MatePad',
    width: 720,
    height: 1140,
    pixelRatio: 2,
    platform: 'harmony',
    hasNotch: false,
    hasSafeArea: true,
    notchHeight: 24,
    safeAreaBottom: 20,
    borderRadius: 16,
  },
]

const DEFAULT_DEVICE = DEVICE_LIST[0]

export function useMobileSimulator() {
  const [state, setState] = useState<SimulatorState>({
    device: DEFAULT_DEVICE,
    rotation: 'portrait',
    scale: 1,
    url: 'https://www.example.com',
    touchEventsEnabled: true,
    gesturesEnabled: true,
  })

  const setDevice = useCallback((device: DeviceConfig) => {
    setState(prev => ({ ...prev, device }))
  }, [])

  const setRotation = useCallback((rotation: 'portrait' | 'landscape') => {
    setState(prev => ({ ...prev, rotation }))
  }, [])

  const toggleRotation = useCallback(() => {
    setState(prev => ({
      ...prev,
      rotation: prev.rotation === 'portrait' ? 'landscape' : 'portrait',
    }))
  }, [])

  const setScale = useCallback((scale: number) => {
    const clampedScale = Math.max(0.5, Math.min(2, scale))
    setState(prev => ({ ...prev, scale: clampedScale }))
  }, [])

  const setUrl = useCallback((url: string) => {
    setState(prev => ({ ...prev, url }))
  }, [])

  const setTouchEventsEnabled = useCallback((enabled: boolean) => {
    setState(prev => ({ ...prev, touchEventsEnabled: enabled }))
  }, [])

  const setGesturesEnabled = useCallback((enabled: boolean) => {
    setState(prev => ({ ...prev, gesturesEnabled: enabled }))
  }, [])

  const viewportSize = useMemo(() => {
    const { device, rotation } = state
    if (rotation === 'portrait') {
      return { width: device.width, height: device.height }
    }
    return { width: device.height, height: device.width }
  }, [state.device, state.rotation])

  const refresh = useCallback(() => {
    const iframe = document.querySelector('#mobile-simulator-iframe') as HTMLIFrameElement
    if (iframe) {
      iframe.src = iframe.src
    }
  }, [])

  const goBack = useCallback(() => {
    const iframe = document.querySelector('#mobile-simulator-iframe') as HTMLIFrameElement
    if (iframe?.contentWindow) {
      try {
        iframe.contentWindow.history.back()
      } catch (e) {
        console.warn('Cannot navigate back:', e)
      }
    }
  }, [])

  return {
    state,
    deviceList: DEVICE_LIST,
    viewportSize,
    setDevice,
    setRotation,
    toggleRotation,
    setScale,
    setUrl,
    setTouchEventsEnabled,
    setGesturesEnabled,
    refresh,
    goBack,
  }
}
