import React, { useState, useEffect, useRef, useCallback } from 'react'
import { Spin, Alert } from 'antd'
import { loadCustomComponent, getComponentSchema } from '@/utils/componentLoader'
import { PageComponent } from '@/api/page'

interface CustomComponentWrapperProps {
  component: PageComponent
  onEvent?: (eventName: string, data: any) => void
}

const CustomComponentWrapper: React.FC<CustomComponentWrapperProps> = ({
  component,
  onEvent,
}) => {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [LoadedComponent, setLoadedComponent] = useState<React.ComponentType<any> | null>(null)
  const [schema, setSchema] = useState<any>(null)
  const componentRef = useRef<any>(null)

  useEffect(() => {
    const loadComponent = async () => {
      try {
        setLoading(true)
        setError(null)
        const result = await loadCustomComponent(component.componentType)
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
  }, [component.componentType])

  const handleEvent = useCallback(
    (eventName: string, data: any) => {
      if (onEvent) {
        onEvent(eventName, data)
      }
    },
    [onEvent]
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
