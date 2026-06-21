import React, { useEffect, useCallback, useRef, useState } from 'react'
import { abtestApi, ABTestVariant } from '@/api/abtest'

export interface UseABTestOptions {
  testId: number
  autoTrackPageView?: boolean
}

export interface UseABTestResult {
  variant: ABTestVariant | null
  isLoading: boolean
  trackEvent: (eventType: string, eventKey: string, eventValue?: number, extraData?: Record<string, any>) => void
  trackPageView: () => void
  trackConversion: (eventKey?: string, eventValue?: number) => void
}

export const useABTest = (options: UseABTestOptions): UseABTestResult => {
  const { testId, autoTrackPageView = true } = options
  
  const [variant, setVariant] = useState<ABTestVariant | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const hasTrackedPageView = useRef(false)

  useEffect(() => {
    const loadVariant = async () => {
      if (!testId) return
      
      try {
        setIsLoading(true)
        const res = await abtestApi.allocate(testId)
        if (res.code === 0 && res.data) {
          setVariant(res.data)
        }
      } catch (e) {
        console.warn('A/B测试变体分配失败:', e)
      } finally {
        setIsLoading(false)
      }
    }

    loadVariant()
  }, [testId])

  const trackEvent = useCallback((eventType: string, eventKey: string, eventValue?: number, extraData?: Record<string, any>) => {
    if (!testId || !variant) return

    const eventData = {
      testId,
      variantId: variant.id,
      eventType,
      eventKey,
      eventValue,
      sessionId: sessionStorage.getItem('abtest_session_id') || generateSessionId(),
      pageUrl: window.location.href,
      timestamp: Date.now(),
      ...extraData,
    }

    abtestApi.recordEvent(eventData).catch((e) => {
      console.warn('A/B测试事件上报失败:', e)
    })
  }, [testId, variant])

  const trackPageView = useCallback(() => {
    if (hasTrackedPageView.current) return
    hasTrackedPageView.current = true
    trackEvent('VIEW', 'page_view')
  }, [trackEvent])

  const trackConversion = useCallback((eventKey = 'conversion', eventValue?: number) => {
    trackEvent('CONVERSION', eventKey, eventValue)
  }, [trackEvent])

  useEffect(() => {
    if (autoTrackPageView && variant && !isLoading) {
      trackPageView()
    }
  }, [autoTrackPageView, variant, isLoading, trackPageView])

  return {
    variant,
    isLoading,
    trackEvent,
    trackPageView,
    trackConversion,
  }
}

const generateSessionId = (): string => {
  const sessionId = 'sess_' + Math.random().toString(36).substring(2, 15)
  sessionStorage.setItem('abtest_session_id', sessionId)
  return sessionId
}

export default useABTest
