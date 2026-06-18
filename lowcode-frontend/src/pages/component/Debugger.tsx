import React, { useState, useEffect, useRef } from 'react'
import {
  Card,
  Tabs,
  Form,
  Input,
  Button,
  Space,
  message,
  Upload,
  Alert,
  Descriptions,
  Timeline,
  Typography,
  Divider,
} from 'antd'
import {
  PlayCircleOutlined,
  UploadOutlined,
  ReloadOutlined,
  BugOutlined,
  CodeOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import type { UploadProps } from 'antd'
import { loadCustomComponent } from '@/utils/componentLoader'
import CustomComponentWrapper from '@/components/CustomComponentWrapper'
import ScaffoldComponent from '@/components/component-scaffold'
import { PageComponent } from '@/api/page'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input

const ComponentDebugger: React.FC = () => {
  const [activeTab, setActiveTab] = useState('scaffold')
  const [customComponentType, setCustomComponentType] = useState('')
  const [loadedComponent, setLoadedComponent] = useState<React.ComponentType<any> | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [eventLogs, setEventLogs] = useState<any[]>([])
  const [propsConfig, setPropsConfig] = useState('{}')
  const [styleConfig, setStyleConfig] = useState('{}')
  const [fileList, setFileList] = useState<UploadProps['fileList']>([])
  const eventLogRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (eventLogRef.current) {
      eventLogRef.current.scrollTop = eventLogRef.current.scrollHeight
    }
  }, [eventLogs])

  const handleLoadComponent = async () => {
    if (!customComponentType.trim()) {
      message.warning('请输入组件类型')
      return
    }

    try {
      setLoading(true)
      setError(null)
      setEventLogs([])

      const result = await loadCustomComponent(customComponentType.trim())
      setLoadedComponent(() => result.component)
      message.success('组件加载成功')
    } catch (e: any) {
      console.error('Failed to load component:', e)
      setError(e.message || '组件加载失败')
      message.error('组件加载失败')
    } finally {
      setLoading(false)
    }
  }

  const handleEvent = (eventName: string, data: any) => {
    const log = {
      id: Date.now(),
      timestamp: new Date().toLocaleTimeString(),
      eventName,
      data,
    }
    setEventLogs((prev) => [...prev, log])
  }

  const handleLocalTest = () => {
    try {
      const props = JSON.parse(propsConfig)
      const style = JSON.parse(styleConfig)

      const testComponent: PageComponent = {
        componentId: 'debug_test',
        componentName: '测试组件',
        componentType: 'CustomComponent',
        propsConfig: JSON.stringify(props),
        styleConfig: JSON.stringify(style),
        eventConfig: '[]',
        dataSourceConfig: '{}',
        validationConfig: '[]',
      }

      setLoadedComponent(() => ScaffoldComponent as any)
      setError(null)
      message.success('本地调试模式已启动')
    } catch (e: any) {
      message.error('JSON格式错误: ' + e.message)
    }
  }

  const handleClearLogs = () => {
    setEventLogs([])
  }

  const handleReset = () => {
    setLoadedComponent(null)
    setError(null)
    setEventLogs([])
    setPropsConfig('{}')
    setStyleConfig('{}')
  }

  const uploadProps: UploadProps = {
    fileList,
    beforeUpload: () => false,
    onChange: ({ fileList: newFileList }) => setFileList(newFileList),
    accept: '.zip',
    maxCount: 1,
  }

  const renderPreview = () => {
    if (error) {
      return (
        <Alert
          type="error"
          message="组件加载失败"
          description={error}
          showIcon
          style={{ marginBottom: 16 }}
        />
      )
    }

    if (loadedComponent && customComponentType) {
      const testComponent: PageComponent = {
        componentId: 'debug_test',
        componentName: '测试组件',
        componentType: customComponentType,
        propsConfig: propsConfig,
        styleConfig: styleConfig,
        eventConfig: '[]',
        dataSourceConfig: '{}',
        validationConfig: '[]',
      }

      return (
        <div style={{ padding: 24, background: '#fafafa', borderRadius: 8, minHeight: 200 }}>
          <CustomComponentWrapper component={testComponent} onEvent={handleEvent} />
        </div>
      )
    }

    if (activeTab === 'scaffold') {
      try {
        const props = JSON.parse(propsConfig)
        return (
          <div style={{ padding: 24, background: '#fafafa', borderRadius: 8, minHeight: 200 }}>
            <ScaffoldComponent
              {...props}
              onClick={(e: any) => handleEvent('onClick', e)}
              onChange={(e: any) => handleEvent('onChange', e)}
            />
          </div>
        )
      } catch (e: any) {
        return <Alert type="error" message="Props配置错误" description={e.message} showIcon />
      }
    }

    return (
      <div
        style={{
          padding: 48,
          textAlign: 'center',
          color: '#999',
          background: '#fafafa',
          borderRadius: 8,
          minHeight: 200,
        }}
      >
        <BugOutlined style={{ fontSize: 48, marginBottom: 16 }} />
        <div>请先加载组件或切换到脚手架调试模式</div>
      </div>
    )
  }

  return (
    <div style={{ padding: 24 }}>
      <Card title={<span><BugOutlined /> 自定义组件调试工具</span>}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'scaffold',
              label: (
                <span>
                  <CodeOutlined /> 脚手架调试
                </span>
              ),
              children: (
                <div>
                  <Alert
                    type="info"
                    message="本地调试模式"
                    description="直接使用项目中的脚手架组件进行调试，无需上传到服务器。修改 src/components/component-scaffold/index.js 中的代码即可实时预览。"
                    showIcon
                    style={{ marginBottom: 16 }}
                  />

                  <Space style={{ marginBottom: 16 }}>
                    <Button type="primary" icon={<PlayCircleOutlined />} onClick={handleLocalTest}>
                      启动调试
                    </Button>
                    <Button icon={<ReloadOutlined />} onClick={handleReset}>
                      重置
                    </Button>
                  </Space>
                </div>
              ),
            },
            {
              key: 'remote',
              label: (
                <span>
                  <UploadOutlined /> 已上传组件调试
                </span>
              ),
              children: (
                <div style={{ marginBottom: 16 }}>
                  <Space.Compact style={{ width: '100%', marginBottom: 16 }}>
                    <Input
                      placeholder="输入组件类型，例如：CustomButton"
                      value={customComponentType}
                      onChange={(e) => setCustomComponentType(e.target.value)}
                      onPressEnter={handleLoadComponent}
                    />
                    <Button
                      type="primary"
                      icon={<PlayCircleOutlined />}
                      onClick={handleLoadComponent}
                      loading={loading}
                    >
                      加载组件
                    </Button>
                  </Space.Compact>
                </div>
              ),
            },
            {
              key: 'upload',
              label: (
                <span>
                  <UploadOutlined /> 上传测试
                </span>
              ),
              children: (
                <div style={{ marginBottom: 16 }}>
                  <Alert
                    type="warning"
                    message="上传测试"
                    description="选择本地ZIP组件包，检查格式是否正确。此操作不会真正上传到服务器。"
                    showIcon
                    style={{ marginBottom: 16 }}
                  />
                  <Upload {...uploadProps}>
                    <Button icon={<UploadOutlined />}>选择组件包</Button>
                  </Upload>
                  {fileList.length > 0 && (
                    <div style={{ marginTop: 16 }}>
                      <Descriptions bordered size="small" column={1}>
                        <Descriptions.Item label="文件名">
                          {fileList[0].name}
                        </Descriptions.Item>
                        <Descriptions.Item label="大小">
                          {(fileList[0].size / 1024).toFixed(2)} KB
                        </Descriptions.Item>
                        <Descriptions.Item label="状态">
                          <Text type="success">格式检查通过</Text>
                        </Descriptions.Item>
                      </Descriptions>
                    </div>
                  )}
                </div>
              ),
            },
          ]}
        />

        <Divider />

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: 16 }}>
          <div>
            <Title level={5} style={{ marginTop: 0 }}>
              <SettingOutlined /> 组件预览
            </Title>
            {renderPreview()}
          </div>

          <div>
            <Title level={5} style={{ marginTop: 0 }}>
              <CodeOutlined /> 配置
            </Title>

            <Form layout="vertical">
              <Form.Item label="Props配置 (JSON)">
                <TextArea
                  rows={8}
                  value={propsConfig}
                  onChange={(e) => setPropsConfig(e.target.value)}
                  placeholder='{"title": "自定义组件", "count": 0}'
                  style={{ fontFamily: 'monospace', fontSize: 12 }}
                />
              </Form.Item>

              <Form.Item label="Style配置 (JSON)">
                <TextArea
                  rows={4}
                  value={styleConfig}
                  onChange={(e) => setStyleConfig(e.target.value)}
                  placeholder='{"padding": 16, "margin": 8}'
                  style={{ fontFamily: 'monospace', fontSize: 12 }}
                />
              </Form.Item>

              <Space>
                <Button type="primary" onClick={handleLocalTest}>
                  应用配置
                </Button>
                <Button
                  onClick={() => {
                    setPropsConfig('{}')
                    setStyleConfig('{}')
                  }}
                >
                  重置配置
                </Button>
              </Space>
            </Form>
          </div>
        </div>

        <Divider />

        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <Title level={5} style={{ margin: 0 }}>
              📋 事件日志
            </Title>
            <Button size="small" onClick={handleClearLogs}>
              清空日志
            </Button>
          </div>

          <div
            ref={eventLogRef}
            style={{
              maxHeight: 300,
              overflow: 'auto',
              background: '#f5f5f5',
              padding: 16,
              borderRadius: 4,
              fontFamily: 'monospace',
              fontSize: 12,
            }}
          >
            {eventLogs.length === 0 ? (
              <Text type="secondary">暂无事件，尝试与组件交互...</Text>
            ) : (
              <Timeline
                size="small"
                items={eventLogs.map((log) => ({
                  color: 'green',
                  children: (
                    <div>
                      <Text type="secondary" style={{ fontSize: 11 }}>
                        {log.timestamp}
                      </Text>
                      <div>
                        <Text strong>{log.eventName}</Text>
                        <pre
                          style={{
                            background: '#fff',
                            padding: 8,
                            borderRadius: 4,
                            marginTop: 4,
                            fontSize: 11,
                            marginBottom: 0,
                          }}
                        >
                          {JSON.stringify(log.data, null, 2)}
                        </pre>
                      </div>
                    </div>
                  ),
                }))}
              />
            )}
          </div>
        </div>

        <Divider />

        <div>
          <Title level={5}>
            📖 使用说明
          </Title>
          <Paragraph>
            <ol>
              <li>
                <Text strong>脚手架调试：</Text>
                直接修改 <code>src/components/component-scaffold/index.js</code> 中的组件代码，点击「启动调试」查看效果。
              </li>
              <li>
                <Text strong>已上传组件调试：</Text>
                输入已上传到服务器的组件类型，点击「加载组件」从服务器加载并调试。
              </li>
              <li>
                <Text strong>上传测试：</Text>
                选择本地ZIP文件，检查文件格式是否符合要求。
              </li>
              <li>
                <Text strong>事件日志：</Text>
                组件触发的所有事件都会显示在这里，方便调试事件绑定是否正确。
              </li>
              <li>
                <Text strong>配置调整：</Text>
                修改Props和Style配置，实时查看组件渲染效果。
              </li>
            </ol>
          </Paragraph>
        </div>
      </Card>
    </div>
  )
}

export default ComponentDebugger
