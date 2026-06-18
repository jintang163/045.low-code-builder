import { message, Modal } from 'antd'
import { request } from './request'

export interface EventAction {
  eventType: string
  actionType: string
  actionConfig?: string
}

export const executeEventAction = async (
  action: EventAction,
  eventData: any,
  context?: {
    navigate?: (path: string) => void
    formRef?: any
    [key: string]: any
  }
) => {
  try {
    const config = action.actionConfig ? JSON.parse(action.actionConfig) : {}

    switch (action.actionType) {
      case 'navigate':
        if (config.url) {
          if (context?.navigate) {
            context.navigate(config.url)
          } else {
            window.location.href = config.url
          }
          message.success('页面跳转成功')
        }
        break

      case 'api':
        if (config.url) {
          const method = config.method || 'GET'
          const params = config.params || {}
          const data = config.data || eventData

          const res = await request({
            url: config.url,
            method,
            params: method === 'GET' ? { ...params, ...eventData } : params,
            data: method !== 'GET' ? { ...data, ...eventData } : undefined,
          })

          if (config.successMessage) {
            message.success(config.successMessage)
          }
          return res
        }
        break

      case 'submit':
        if (context?.formRef) {
          await context.formRef.submit()
          message.success('表单提交成功')
        }
        break

      case 'reset':
        if (context?.formRef) {
          context.formRef.resetFields()
          message.success('表单已重置')
        }
        break

      case 'modal':
        if (config.title || config.content) {
          Modal.info({
            title: config.title || '提示',
            content: config.content || JSON.stringify(eventData, null, 2),
            okText: config.okText || '确定',
          })
        }
        break

      case 'message':
        const messageType = config.type || 'success'
        const messageContent = config.content || `事件触发: ${action.eventType}`
        ;(message as any)[messageType](messageContent)
        break

      case 'confirm':
        return new Promise((resolve) => {
          Modal.confirm({
            title: config.title || '确认',
            content: config.content || '确定要执行此操作吗？',
            okText: config.okText || '确定',
            cancelText: config.cancelText || '取消',
            onOk: () => {
              if (config.onSuccessMessage) {
                message.success(config.onSuccessMessage)
              }
              resolve(true)
            },
            onCancel: () => {
              resolve(false)
            },
          })
        })

      case 'download':
        if (config.url) {
          const link = document.createElement('a')
          link.href = config.url
          link.download = config.filename || ''
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          message.success('开始下载')
        }
        break

      case 'print':
        window.print()
        break

      case 'custom':
        if (config.code) {
          try {
            const fn = new Function('eventData', 'context', 'message', 'Modal', 'request', config.code)
            await fn(eventData, context, message, Modal, request)
          } catch (e: any) {
            console.error('Custom code execution error:', e)
            message.error('自定义代码执行失败: ' + e.message)
          }
        }
        break

      default:
        console.log('Unhandled action type:', action.actionType, 'with data:', eventData)
    }
  } catch (e: any) {
    console.error('Event action execution error:', e)
    message.error('动作执行失败: ' + e.message)
  }
}

export const executeComponentEvents = async (
  eventConfig: string | undefined,
  eventName: string,
  eventData: any,
  context?: any
) => {
  if (!eventConfig) return

  try {
    const events: EventAction[] = JSON.parse(eventConfig)
    const matchingEvents = events.filter(e => e.eventType === eventName)

    for (const action of matchingEvents) {
      await executeEventAction(action, eventData, context)
    }
  } catch (e) {
    console.error('Failed to execute component events:', e)
  }
}
