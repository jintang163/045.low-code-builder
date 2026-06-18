import React, { useState, useRef, useEffect } from 'react'
import { Input, Button, Spin, Empty, Tag, Tooltip, message } from 'antd'
import {
  SendOutlined,
  RobotOutlined,
  UserOutlined,
  BulbOutlined,
  DeleteOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { AiChatMessage, aiPageApi, AiPageGenerateVO, PageInfo } from '@/api/page'

const { TextArea } = Input

interface AiPagePanelProps {
  sessionId: string
  onSessionIdChange: (sessionId: string) => void
  onPageGenerated: (pageData: { pageName: string; components: any[] }) => void
  currentPage?: PageInfo | null
  appId?: number
}

const suggestionPrompts = [
  '创建商品管理列表，支持搜索和分页',
  '做一个用户登录页面，包含用户名和密码',
  '生成一个数据统计仪表盘，包含各种图表',
  '制作一个产品详情页，包含图片、描述和规格',
]

const AiPagePanel: React.FC<AiPagePanelProps> = ({
  sessionId,
  onSessionIdChange,
  onPageGenerated,
  currentPage,
  appId,
}) => {
  const [messages, setMessages] = useState<AiChatMessage[]>([])
  const [inputValue, setInputValue] = useState('')
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    scrollToBottom()
  }, [messages, loading])

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  const handleSend = async () => {
    if (!inputValue.trim() || loading) return

    const userMessage = inputValue.trim()
    setInputValue('')
    setLoading(true)
    setGenerating(true)

    const newUserMessage: AiChatMessage = { role: 'user', content: userMessage }
    const newMessages = [...messages, newUserMessage]
    setMessages(newMessages)

    try {
      let currentPageJson = ''
      if (currentPage && currentPage.components && currentPage.components.length > 0) {
        currentPageJson = JSON.stringify({
          pageName: currentPage.pageName,
          components: currentPage.components,
        })
      }

      const res = await aiPageApi.generate({
        sessionId,
        userMessage,
        appId,
        currentPageJson,
        history: messages.length > 0 ? messages : undefined,
      })

      const data: AiPageGenerateVO = res.data
      if (data?.success) {
        onSessionIdChange(data.sessionId)

        if (data.pageJson) {
          try {
            const pageData = JSON.parse(data.pageJson)
            if (pageData.components && Array.isArray(pageData.components)) {
              onPageGenerated({
                pageName: pageData.pageName || currentPage?.pageName || 'AI生成页面',
                components: pageData.components,
              })
            }
          } catch (e) {
            console.error('Parse generated page JSON failed:', e)
          }
        }

        const assistantMessage: AiChatMessage = {
          role: 'assistant',
          content: data.replyMessage,
        }
        setMessages([...newMessages, assistantMessage])
      } else {
        message.error(data?.errorMessage || '生成页面失败')
        const errorMessage: AiChatMessage = {
          role: 'assistant',
          content: data?.replyMessage || '抱歉，生成页面时出现错误，请重试。',
        }
        setMessages([...newMessages, errorMessage])
      }
    } catch (e: any) {
      console.error('AI generate page failed:', e)
      message.error('请求失败：' + (e.message || '请稍后重试'))
      const errorMessage: AiChatMessage = {
        role: 'assistant',
        content: '抱歉，网络请求失败，请稍后重试。',
      }
      setMessages([...newMessages, errorMessage])
    } finally {
      setLoading(false)
      setGenerating(false)
    }
  }

  const handleClearChat = async () => {
    try {
      if (sessionId) {
        await aiPageApi.clearSession(sessionId)
      }
      const res = await aiPageApi.createSession()
      onSessionIdChange(res.data || '')
      setMessages([])
      message.success('已开启新对话')
    } catch (e) {
      console.error(e)
    }
  }

  const handleSuggestionClick = (prompt: string) => {
    setInputValue(prompt)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: '#fafafa' }}>
      <div
        style={{
          padding: '12px 16px',
          borderBottom: '1px solid #e8e8e8',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: '#fff',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <ThunderboltOutlined style={{ color: '#1677ff', fontSize: 16 }} />
          <span style={{ fontWeight: 500, fontSize: 14 }}>AI 页面助手</span>
          {generating && (
            <Tag color="processing" style={{ marginLeft: 8 }}>
              生成中
            </Tag>
          )}
        </div>
        <Tooltip title="开启新对话">
          <Button type="text" size="small" icon={<DeleteOutlined />} onClick={handleClearChat} />
        </Tooltip>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
        {messages.length === 0 ? (
          <Empty
            image={<BulbOutlined style={{ fontSize: 48, color: '#1677ff' }} />}
            description={
              <div style={{ color: '#666' }}>
                <div style={{ marginBottom: 12 }}>描述您想要的页面，AI 会自动生成</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {suggestionPrompts.map((prompt, idx) => (
                    <Button
                      key={idx}
                      type="dashed"
                      size="small"
                      onClick={() => handleSuggestionClick(prompt)}
                      style={{ textAlign: 'left', height: 'auto', padding: '8px 12px' }}
                    >
                      {prompt}
                    </Button>
                  ))}
                </div>
              </div>
            }
            style={{ marginTop: 40 }}
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {messages.map((msg, idx) => (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  gap: 8,
                  justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                }}
              >
                {msg.role === 'assistant' && (
                  <div
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: '50%',
                      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#fff',
                      flexShrink: 0,
                    }}
                  >
                    <RobotOutlined />
                  </div>
                )}
                <div
                  style={{
                    maxWidth: '75%',
                    padding: '10px 14px',
                    borderRadius: 12,
                    background: msg.role === 'user' ? '#1677ff' : '#fff',
                    color: msg.role === 'user' ? '#fff' : '#333',
                    border: msg.role === 'user' ? 'none' : '1px solid #e8e8e8',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                    fontSize: 13,
                    lineHeight: 1.6,
                  }}
                >
                  {msg.content}
                </div>
                {msg.role === 'user' && (
                  <div
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: '50%',
                      background: '#e6f4ff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#1677ff',
                      flexShrink: 0,
                    }}
                  >
                    <UserOutlined />
                  </div>
                )}
              </div>
            ))}
            {loading && (
              <div style={{ display: 'flex', gap: 8 }}>
                <div
                  style={{
                    width: 32,
                    height: 32,
                    borderRadius: '50%',
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#fff',
                    flexShrink: 0,
                  }}
                >
                  <RobotOutlined />
                </div>
                <div
                  style={{
                    padding: '14px 18px',
                    borderRadius: 12,
                    background: '#fff',
                    border: '1px solid #e8e8e8',
                  }}
                >
                  <Spin size="small" />
                  <span style={{ marginLeft: 8, color: '#999', fontSize: 13 }}>AI 正在生成页面...</span>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      <div
        style={{
          padding: 12,
          borderTop: '1px solid #e8e8e8',
          background: '#fff',
        }}
      >
        <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
          <TextArea
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="描述您想要的页面，例如：创建商品管理列表..."
            autoSize={{ minRows: 2, maxRows: 5 }}
            disabled={loading}
            style={{ resize: 'none' }}
          />
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSend}
            loading={loading}
            disabled={!inputValue.trim()}
            style={{ height: 40 }}
          >
            发送
          </Button>
        </div>
        <div style={{ fontSize: 11, color: '#999', marginTop: 6, textAlign: 'center' }}>
          Enter 发送，Shift+Enter 换行。生成后可在画布上继续拖拽调整。
        </div>
      </div>
    </div>
  )
}

export default AiPagePanel
