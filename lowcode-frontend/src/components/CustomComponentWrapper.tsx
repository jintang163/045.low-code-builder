import React, { useState, useEffect, useRef, useCallback } from 'react'
import { Spin, Alert, message } from 'antd'
import { loadCustomComponent, getComponentSchema } from '@/utils/componentLoader'
import { PageComponent } from '@/api/page'
import { executeComponentEvents } from '@/utils/eventExecutor'

interface CustomComponentWrapperProps {
  component: PageComponent
  onEvent?: (eventName: string, data: any) => void
  executeActions?: boolean
  eventContext?: any
}

const CustomComponentWrapper: React.FC<CustomComponentWrapperProps> = ({
  component,
  onEvent,
  executeActions = false,
  eventContext,
}) => {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [LoadedComponent, setLoadedComponent] = useState<React.ComponentType<any> | null>(null)
  const [schema, setSchema] = useState<any>(null)
  const [versionDeprecated, setVersionDeprecated] = useState(false)
  const componentRef = useRef<any>(null)

  useEffect(() => {
    const loadComponent = async () => {
      try {
        setLoading(true)
        setError(null)
        setVersionDeprecated(false)

        const result = await loadCustomComponent(component.componentType, component.componentVersion)

        if (result.versionInfo?.deprecated) {
          setVersionDeprecated(true)
          setError(`组件版本 ${component.componentVersion} 已被废弃`)
          setLoading(false)
          return
        }

        setLoadedComponent(() => result.component)
        setSchema(result.schema)
      } catch (e: any) {
        console.error('Failed to load custom component:', e)
        setError(e.message || '组件加载失败')
      } finally {
        setLoading(false)
      }
    }

    loadComponent()
  }, [component.componentType, component.componentVersion])

  const handleEvent = useCallback(
    async (eventName: string, data: any) => {
      if (onEvent) {
        onEvent(eventName, data)
      }

      if (executeActions) {
        await executeComponentEvents(
          component.eventConfig,
          eventName,
          data,
          eventContext
        )
      }
    },
    [onEvent, executeActions, component.eventConfig, eventContext]
  )

  const wrapEventHandlers = useCallback(
    (props: any): any => {
      if (!schema?.exposedEvents) return props

      const wrappedProps = { ...props }

      schema.exposedEvents.forEach((eventName: string) => {
        const originalHandler = props[eventName]
        wrappedProps[eventName] = (...args: any[]) => {
          handleEvent(eventName, args)
          if (originalHandler && typeof originalHandler === 'function') {
            return originalHandler(...args)
          }
        }
      })

      return wrappedProps
    },
    [schema, handleEvent]
  )

  if (loading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin tip="加载组件中..." />
      </div>
    )
  }

  if (error) {
    return (
      <Alert
        type="error"
        message="组件加载失败"
        description={error}
        showIcon
      />
    )
  }

  if (!LoadedComponent) {
    return (
      <Alert
        type="warning"
        message="组件不可用"
        description={`组件 ${component.componentType} 无法加载`}
        showIcon
      />
    )
  }

  const props = component.propsConfig ? JSON.parse(component.propsConfig) : {}
  const style = component.styleConfig ? JSON.parse(component.styleConfig) : {}
  const wrappedProps = wrapEventHandlers(props)

  const baseStyle: React.CSSProperties = {
    padding: 8,
    margin: 4,
    minHeight: 40,
    ...style,
  }

  return (
    <LoadedComponent
      ref={componentRef}
      {...wrappedProps}
      style={baseStyle}
      __componentId={component.componentId}
      __componentType={component.componentType}
    />
  )
}

export const CustomComponentPreview: React.FC<{
  componentType: string
  defaultProps?: any
}> = ({ componentType, defaultProps = {} }) => {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [LoadedComponent, setLoadedComponent] = useState<React.ComponentType<any> | null>(null)

  useEffect(() => {
    const loadComponent = async () => {
      try {
        setLoading(true)
        setError(null)
        const result = await loadCustomComponent(componentType)
        setLoadedComponent(() => result.component)
      } catch (e: any) {
        console.error('Failed to load custom component:', e)
        setError(e.message || '组件加载失败')
      } finally {
        setLoading(false)
      }
    }

    loadComponent()
  }, [componentType])

  if (loading) {
    return (
      <div style={{ padding: 16, textAlign: 'center', color: '#999' }}>
        <Spin size="small" /> 加载中...
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ padding: 16, textAlign: 'center', color: '#ff4d4f' }}>
        ⚠️ 加载失败
      </div>
    )
  }

  if (!LoadedComponent) {
    return (
      <div style={{ padding: 16, textAlign: 'center', color: '#999' }}>
        📦 {componentType}
      </div>
    )
  }

  return <LoadedComponent {...defaultProps} />
}

export const useCustomComponentSchema = (componentType: string) => {
  const [schema, setSchema] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const cachedSchema = getComponentSchema(componentType)
    if (cachedSchema) {
      setSchema(cachedSchema)
      return
    }

    const loadSchema = async () => {
      try {
        setLoading(true)
        const result = await loadCustomComponent(componentType)
        setSchema(result.schema)
      } catch (e: any) {
        setError(e.message || '加载组件Schema失败')
      } finally {
        setLoading(false)
      }
    }

    loadSchema()
  }, [componentType])

  return { schema, loading, error }
}

export default CustomComponentWrapper
