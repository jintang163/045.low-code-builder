import React, { useRef, useCallback, useEffect } from 'react'

export interface TouchPoint {
  x: number
  y: number
  identifier: number
}

export interface GestureEvent {
  type: 'swipe' | 'pinch' | 'tap' | 'doubletap' | 'longpress'
  direction?: 'left' | 'right' | 'up' | 'down'
  scale?: number
  velocity?: number
}

interface TouchEventHandlerProps {
  children: React.ReactNode
  enabled?: boolean
  gesturesEnabled?: boolean
  onTouchStart?: (touches: TouchPoint[]) => void
  onTouchMove?: (touches: TouchPoint[]) => void
  onTouchEnd?: (touches: TouchPoint[]) => void
  onGesture?: (gesture: GestureEvent) => void
  style?: React.CSSProperties
  className?: string
}

const TouchEventHandler: React.FC<TouchEventHandlerProps> = ({
  children,
  enabled = true,
  gesturesEnabled = true,
  onTouchStart,
  onTouchMove,
  onTouchEnd,
  onGesture,
  style,
  className,
}) => {
  const containerRef = useRef<HTMLDivElement>(null)
  const touchStartRef = useRef<Map<number, TouchPoint>>(new Map())
  const lastTouchEndRef = useRef<{ time: number; point: TouchPoint } | null>(null)
  const longPressTimerRef = useRef<number | null>(null)

  const getRelativePosition = useCallback((clientX: number, clientY: number): TouchPoint => {
    if (!containerRef.current) {
      return { x: clientX, y: clientY, identifier: 0 }
    }
    const rect = containerRef.current.getBoundingClientRect()
    return {
      x: clientX - rect.left,
      y: clientY - rect.top,
      identifier: 0,
    }
  }, [])

  const createTouchEvent = useCallback((
    type: string,
    touches: TouchPoint[],
    target: EventTarget
  ): TouchEvent => {
    const touchList = {
      length: touches.length,
      item: (index: number) => ({
        clientX: touches[index]?.x || 0,
        clientY: touches[index]?.y || 0,
        identifier: touches[index]?.identifier || 0,
        target,
      }),
      [Symbol.iterator]: function* () {
        for (let i = 0; i < this.length; i++) {
          yield this.item(i)
        }
      },
    } as unknown as TouchList

    const event = new Event(type, { bubbles: true, cancelable: true }) as TouchEvent
    Object.defineProperties(event, {
      touches: { value: touchList },
      targetTouches: { value: touchList },
      changedTouches: { value: touchList },
    })
    return event
  }, [])

  const dispatchTouchEvent = useCallback((
    type: 'touchstart' | 'touchmove' | 'touchend',
    touches: TouchPoint[],
    target: EventTarget
  ) => {
    const event = createTouchEvent(type, touches, target)
    target.dispatchEvent(event)
  }, [createTouchEvent])

  const detectGesture = useCallback((
    startTouches: Map<number, TouchPoint>,
    endTouches: Map<number, TouchPoint>,
    duration: number
  ): GestureEvent | null => {
    if (!gesturesEnabled) return null

    const startPoints = Array.from(startTouches.values())
    const endPoints = Array.from(endTouches.values())

    if (startPoints.length === 1 && endPoints.length === 1) {
      const start = startPoints[0]
      const end = endPoints[0]
      const dx = end.x - start.x
      const dy = end.y - start.y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < 10) {
        if (duration < 200) {
          const now = Date.now()
          const lastTouch = lastTouchEndRef.current
          if (lastTouch && now - lastTouch.time < 300) {
            lastTouchEndRef.current = null
            return { type: 'doubletap' }
          }
          lastTouchEndRef.current = { time: now, point: end }
          return { type: 'tap' }
        }
        return null
      }

      const velocity = distance / duration
      const absDx = Math.abs(dx)
      const absDy = Math.abs(dy)

      if (absDx > absDy) {
        return {
          type: 'swipe',
          direction: dx > 0 ? 'right' : 'left',
          velocity,
        }
      } else {
        return {
          type: 'swipe',
          direction: dy > 0 ? 'down' : 'up',
          velocity,
        }
      }
    }

    if (startPoints.length >= 2 && endPoints.length >= 2) {
      const startDistance = Math.hypot(
        startPoints[0].x - startPoints[1].x,
        startPoints[0].y - startPoints[1].y
      )
      const endDistance = Math.hypot(
        endPoints[0].x - endPoints[1].x,
        endPoints[0].y - endPoints[1].y
      )
      const scale = endDistance / startDistance
      if (Math.abs(scale - 1) > 0.1) {
        return { type: 'pinch', scale }
      }
    }

    return null
  }, [gesturesEnabled])

  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if (!enabled) return
    if (e.button !== 0) return

    e.preventDefault()
    const point = getRelativePosition(e.clientX, e.clientY)
    touchStartRef.current.clear()
    touchStartRef.current.set(0, { ...point, startTime: Date.now() } as any)
    onTouchStart?.([point])

    const target = e.target as EventTarget
    dispatchTouchEvent('touchstart', [point], target)

    if (gesturesEnabled && longPressTimerRef.current === null) {
      longPressTimerRef.current = window.setTimeout(() => {
        onGesture?.({ type: 'longpress' })
      }, 500)
    }
  }, [enabled, getRelativePosition, onTouchStart, dispatchTouchEvent, gesturesEnabled, onGesture])

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!enabled) return
    if (touchStartRef.current.size === 0) return

    e.preventDefault()
    const point = getRelativePosition(e.clientX, e.clientY)
    onTouchMove?.([point])

    const target = e.target as EventTarget
    dispatchTouchEvent('touchmove', [point], target)

    if (longPressTimerRef.current !== null) {
      const startPoint = touchStartRef.current.get(0)
      if (startPoint) {
        const dx = point.x - startPoint.x
        const dy = point.y - startPoint.y
        if (Math.sqrt(dx * dx + dy * dy) > 10) {
          clearTimeout(longPressTimerRef.current)
          longPressTimerRef.current = null
        }
      }
    }
  }, [enabled, getRelativePosition, onTouchMove, dispatchTouchEvent])

  const handleMouseUp = useCallback((e: React.MouseEvent) => {
    if (!enabled) return
    if (touchStartRef.current.size === 0) return

    e.preventDefault()
    const point = getRelativePosition(e.clientX, e.clientY)
    const endTouches = new Map<number, TouchPoint>()
    endTouches.set(0, point)

    const startPoint = touchStartRef.current.get(0)
    const duration = startPoint && (startPoint as any).startTime
      ? Date.now() - (startPoint as any).startTime
      : 0

    onTouchEnd?.([point])

    const target = e.target as EventTarget
    dispatchTouchEvent('touchend', [point], target)

    if (gesturesEnabled) {
      const gesture = detectGesture(touchStartRef.current, endTouches, duration)
      if (gesture) {
        onGesture?.(gesture)
      }
    }

    touchStartRef.current.clear()
    if (longPressTimerRef.current !== null) {
      clearTimeout(longPressTimerRef.current)
      longPressTimerRef.current = null
    }
  }, [enabled, getRelativePosition, onTouchEnd, dispatchTouchEvent, gesturesEnabled, detectGesture, onGesture])

  const handleMouseLeave = useCallback((e: React.MouseEvent) => {
    if (touchStartRef.current.size > 0) {
      handleMouseUp(e)
    }
  }, [handleMouseUp])

  useEffect(() => {
    return () => {
      if (longPressTimerRef.current !== null) {
        clearTimeout(longPressTimerRef.current)
      }
    }
  }, [])

  return (
    <div
      ref={containerRef}
      style={{
        ...style,
        touchAction: 'none',
        userSelect: 'none',
        cursor: enabled ? 'pointer' : 'default',
      }}
      className={className}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseLeave}
    >
      {children}
    </div>
  )
}

export default TouchEventHandler
