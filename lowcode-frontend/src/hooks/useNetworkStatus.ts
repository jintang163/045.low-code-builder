import { useState, useEffect, useCallback } from 'react'
import { getNetworkDetector, NetworkStatus } from '@/utils/offline/networkDetector'

export function useNetworkStatus() {
  const [networkStatus, setNetworkStatus] = useState<NetworkStatus>(() => {
    if (typeof window !== 'undefined') {
      return getNetworkDetector().getStatus()
    }
    return 'online'
  })

  const [isOnline, setIsOnline] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      return getNetworkDetector().isOnline()
    }
    return true
  })

  useEffect(() => {
    const detector = getNetworkDetector()

    const handleStatusChange = (status: NetworkStatus) => {
      setNetworkStatus(status)
      setIsOnline(status === 'online')
    }

    const unsubscribe = detector.subscribe(handleStatusChange)

    return () => {
      unsubscribe()
    }
  }, [])

  const checkNow = useCallback(async () => {
    const detector = getNetworkDetector()
    return await detector.checkNow()
  }, [])

  return {
    isOnline,
    networkStatus,
    checkNow,
  }
}

export default useNetworkStatus
