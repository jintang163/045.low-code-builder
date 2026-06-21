import React, { useState, useEffect, useCallback } from 'react'
import {
  Card,
  Button,
  Space,
  Tag,
  Spin,
  Empty,
  Row,
  Col,
  Typography,
  Divider,
  Alert,
  Input,
  InputNumber,
  Form,
  Select,
  message,
} from 'antd'
import {
  ExperimentOutlined,
  ReloadOutlined,
  EyeOutlined,
  ControlOutlined,
  ThunderboltOutlined,
  TrophyOutlined,
  ArrowLeftOutlined,
  UserOutlined,
  CopyOutlined,
} from '@ant-design/icons'
import { useNavigate, useSearchParams, useParams } from 'react-router-dom'
import {
  abtestApi,
  ABTestInfo,
  ABTestVariant,
  mockABTests,
} from '@/api/abtest'
import { pageApi, PageInfo, PageComponent } from '@/api/page'
import useABTest from '@/hooks/useABTest'

const { Title, Text, Paragraph } = Typography
const { Option } = Select

const renderRuntimeComponent = (component: PageComponent, onEvent?: (event: string, params: any) => void) => {
  try {
    const props = component.propsConfig ? JSON.parse(component.propsConfig) : {}
    const style = component.styleConfig ? JSON.parse(component.styleConfig) : {}

    const baseStyle: React.CSSProperties = {
      padding: 12,
      margin: 8,
      borderRadius: 6,
      background: '#fff',
      ...style,
    }

    const handleClick = () => {
      if (onEvent) onEvent('click', { componentId: component.componentId, componentName: component.componentName })
    }

    switch (component.componentType) {
      case 'INPUT':
        return <Input placeholder={props.placeholder || '请输入'} style={{ width: '100%', ...baseStyle }} />
      case 'TEXTAREA':
        return <Input.TextArea placeholder={props.placeholder || '请输入'} rows={props.rows || 3} style={{ width: '100%' }} />
      case 'NUMBER':
        return <InputNumber placeholder={props.placeholder || '请输入数字'} style={{ width: '100%', ...baseStyle }} />
      case 'SELECT':
        return (
          <Select placeholder={props.placeholder || '请选择'} style={{ width: '100%' }}>
            {(props.options || [{ label: '选项1', value: '1' }, { label: '选项2', value: '2' }]).map((opt: any) => (
              <Option key={opt.value} value={opt.value}>{opt.label}</Option>
            ))}
          </Select>
        )
      case 'DATE':
        return <Input placeholder="选择日期" style={{ width: '100%', ...baseStyle }} />
      case 'BUTTON':
        return (
          <Button type={props.type || 'primary'} style={baseStyle} onClick={handleClick}>
            {props.text || component.componentName || '按钮'}
          </Button>
        )
      case 'TITLE':
        return (
          <div style={{
            fontSize: props.level ? 24 - (props.level - 1) * 4 : 20,
            fontWeight: 'bold',
            ...baseStyle,
          }}>
            {props.text || component.componentName || '标题'}
          </div>
        )
      case 'TEXT':
        return (
          <div style={{ color: props.color || '#333', ...baseStyle }}>
            {props.text || component.componentName || '文本内容'}
          </div>
        )
      case 'IMAGE':
        return (
          <div style={{
            width: props.width || 200,
            height: props.height || 150,
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: 14,
            borderRadius: 8,
            ...baseStyle,
          }}>
            🖼️ {props.alt || component.componentName || '图片'}
          </div>
        )
      case 'LINK':
        return (
          <a style={baseStyle} onClick={handleClick}>
            {props.text || component.componentName || '链接'}
          </a>
        )
      case 'DIVIDER':
        return <Divider style={baseStyle}>{props.text || ''}</Divider>
      case 'CARD':
        return (
          <Card title={props.title || component.componentName || '卡片'} style={baseStyle}>
            <div style={{ color: '#999' }}>卡片内容区域 - 版本快照渲染</div>
          </Card>
        )
      case 'TABLE':
        return (
          <div style={{ background: '#fafafa', padding: 16, borderRadius: 4, ...baseStyle }}>
            <div style={{ color: '#666', textAlign: 'center', padding: 20 }}>
              📊 表格组件 - {component.componentName}
            </div>
          </div>
        )
      case 'FORM':
        return (
          <div style={{ background: '#fafafa', padding: 24, borderRadius: 4, ...baseStyle }}>
            <Title level={5} style={{ marginTop: 0 }}>📋 {component.componentName || '表单'}</Title>
            <Form layout="vertical">
              <Form.Item label="示例字段">
                <Input placeholder="请输入" />
              </Form.Item>
              <Form.Item>
                <Button type="primary" onClick={handleClick}>提交</Button>
              </Form.Item>
            </Form>
          </div>
        )
      case 'TABS':
        return (
          <Card style={baseStyle}>
            <Title level={5} style={{ marginTop: 0 }}>📑 {component.componentName || '标签页'}</Title>
            <Select defaultValue="1" style={{ width: 200 }}>
              <Option value="1">标签1</Option>
              <Option value="2">标签2</Option>
            </Select>
          </Card>
        )
      case 'GRID':
        return (
          <div style={{
            display: 'grid',
            gridTemplateColumns: `repeat(${props.columns || 2}, 1fr)`,
            gap: 8,
            padding: 12,
            border: '1px solid #f0f0f0',
            borderRadius: 6,
            ...baseStyle,
          }}>
            {Array.from({ length: props.columns || 2 }).map((_, i) => (
              <div key={i} style={{
                height: 80,
                background: '#f5f5f5',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#999',
                borderRadius: 4,
              }}>
                列{i + 1}
              </div>
            ))}
          </div>
        )
      case 'FLEX':
        return (
          <div style={{
            display: 'flex',
            gap: 8,
            padding: 12,
            border: '1px solid #f0f0f0',
            borderRadius: 6,
            ...baseStyle,
          }}>
            {Array.from({ length: 2 }).map((_, i) => (
              <div key={i} style={{
                flex: 1,
                height: 80,
                background: '#f5f5f5',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#999',
                borderRadius: 4,
              }}>
                子元素{i + 1}
              </div>
            ))}
          </div>
        )
      case 'LINECHART':
      case 'BARCHART':
      case 'PIECHART':
        return (
          <div style={{
            height: 260,
            background: 'linear-gradient(180deg, #f0f9ff 0%, #e0f2fe 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: 6,
            ...baseStyle,
          }}>
            <div style={{ textAlign: 'center', color: '#666' }}>
              <div style={{ fontSize: 40, marginBottom: 8 }}>
                {component.componentType === 'BARCHART' ? '📊' : component.componentType === 'PIECHART' ? '🥧' : '📈'}
              </div>
              <div style={{ fontWeight: 500 }}>{component.componentName || '图表'}</div>
              <div style={{ fontSize: 12, marginTop: 4, color: '#999' }}>快照版本渲染</div>
            </div>
          </div>
        )
      default:
        return (
          <div style={{
            padding: 20,
            background: '#fafafa',
            border: '1px dashed #d9d9d9',
            borderRadius: 6,
            textAlign: 'center',
            ...baseStyle,
          }}>
            <div style={{ fontSize: 24, marginBottom: 8 }}>📦</div>
            <div style={{ fontWeight: 500, color: '#666' }}>{component.componentName || component.componentType}</div>
            <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
              {component.componentType}
            </div>
          </div>
        )
    }
  } catch (e) {
    return (
      <Alert
        type="error"
        showIcon
        message={`组件渲染失败: ${component.componentName}`}
        description={(e as any).message}
      />
    )
  }
}

interface ABTestRuntimeProps {}

const ABTestRuntime: React.FC<ABTestRuntimeProps> = () => {
  const { id } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()

  const [abTest, setAbTest] = useState<ABTestInfo | null>(null)
  const [loading, setLoading] = useState(false)
  const [pageLoading, setPageLoading] = useState(false)
  const [pageInfo, setPageInfo] = useState<PageInfo | null>(null)
  const [components, setComponents] = useState<PageComponent[]>([])
  const [manualUserId, setManualUserId] = useState<number | undefined>(
    searchParams.get('userId') ? Number(searchParams.get('userId')) : undefined
  )
  const [forceVariantId, setForceVariantId] = useState<number | undefined>(
    searchParams.get('variantId') ? Number(searchParams.get('variantId')) : undefined
  )
  const [eventLog, setEventLog] = useState<Array<{ time: string; type: string; msg: string }>>([])

  const testId = Number(id)
  const currentUserId = manualUserId || Math.floor(Math.random() * 100000) + 1

  const { variant, isLoading, trackEvent, trackPageView, trackConversion } = useABTest({
    testId,
    autoTrackPageView: false,
  })

  const addLog = (type: string, msg: string) => {
    const time = new Date().toLocaleTimeString()
    setEventLog(prev => [{ time, type, msg }, ...prev].slice(0, 50))
  }

  const loadTest = useCallback(async () => {
    if (!id) return
    setLoading(true)
    try {
      const res: any = await abtestApi.get(Number(id))
      if (res.code === 0 || res.code === 200) {
        setAbTest(res.data)
        addLog('info', `已加载测试: ${res.data?.testName}`)
      } else {
        const mock = mockABTests.find(t => t.id === Number(id))
        if (mock) {
          setAbTest(mock)
          addLog('info', `已加载测试(本地Mock): ${mock.testName}`)
        }
      }
    } catch (e: any) {
      console.error(e)
      const mock = mockABTests.find(t => t.id === Number(id))
      if (mock) {
        setAbTest(mock)
        addLog('warn', `加载失败，使用Mock数据: ${mock.testName}`)
      }
    } finally {
      setLoading(false)
    }
  }, [id])

  const loadPageBySnapshot = useCallback(async (snapshotId: number, variantName: string) => {
    if (!abTest?.resourceId) return
    setPageLoading(true)
    addLog('info', `加载快照: snapshotId=${snapshotId}, 变体=${variantName}`)
    try {
      const res: any = await pageApi.previewBySnapshot(abTest.resourceId, snapshotId)
      if (res.code === 0 || res.code === 200) {
        const data = res.data || {}
        const comps: PageComponent[] = data.components || data.pageComponents || []
        setPageInfo({
          id: abTest.resourceId,
          appId: abTest.appId,
          pageName: data.pageName || abTest.testName,
          pageCode: data.pageCode || abTest.testCode,
          version: data.version || variant?.version,
          components: comps,
        } as PageInfo)
        setComponents(comps)
        addLog('success', `快照页面组件已加载: ${comps.length} 个组件`)
      } else {
        generateMockComponents(snapshotId, variantName)
      }
    } catch (e: any) {
      console.error(e)
      generateMockComponents(snapshotId, variantName)
    } finally {
      setPageLoading(false)
    }
  }, [abTest, variant])

  const generateMockComponents = (snapshotId: number, variantName: string) => {
    const isControl = variantName.includes('原始') || variantName.includes('对照')
    const mockComponents: PageComponent[] = isControl ? [
      {
        id: 1,
        componentId: 'title_1',
        componentName: '页面标题',
        componentType: 'TITLE',
        propsConfig: JSON.stringify({ level: 2, text: `欢迎使用 ${abTest?.testName || '示例页面'} (对照组)` }),
        styleConfig: JSON.stringify({ color: '#1677ff' }),
      },
      {
        id: 2,
        componentId: 'text_1',
        componentName: '说明文本',
        componentType: 'TEXT',
        propsConfig: JSON.stringify({ text: '这是对照组的页面内容，使用蓝色按钮风格。页面版本基于原始设计。' }),
        styleConfig: JSON.stringify({ color: '#666' }),
      },
      {
        id: 3,
        componentId: 'image_1',
        componentName: '主图',
        componentType: 'IMAGE',
        propsConfig: JSON.stringify({ alt: '产品主图', width: 400, height: 200 }),
        styleConfig: JSON.stringify({}),
      },
      {
        id: 4,
        componentId: 'button_1',
        componentName: '立即购买 (蓝色)',
        componentType: 'BUTTON',
        propsConfig: JSON.stringify({ type: 'primary', text: '立即购买 (对照组蓝色)' }),
        styleConfig: JSON.stringify({}),
      },
    ] : [
      {
        id: 1,
        componentId: 'title_1',
        componentName: '页面标题',
        componentType: 'TITLE',
        propsConfig: JSON.stringify({ level: 2, text: `🎉 ${abTest?.testName || '示例页面'} (实验组优化版)` }),
        styleConfig: JSON.stringify({ color: '#52c41a' }),
      },
      {
        id: 2,
        componentId: 'text_1',
        componentName: '说明文本',
        componentType: 'TEXT',
        propsConfig: JSON.stringify({ text: '这是实验组优化后的页面内容，使用绿色按钮和更吸引人的文案，转化率显著提升！' }),
        styleConfig: JSON.stringify({ color: '#333', fontWeight: 500 }),
      },
      {
        id: 3,
        componentId: 'image_1',
        componentName: '主图',
        componentType: 'IMAGE',
        propsConfig: JSON.stringify({ alt: '产品主图-优化版', width: 400, height: 200 }),
        styleConfig: JSON.stringify({}),
      },
      {
        id: 4,
        componentId: 'button_1',
        componentName: '限时优惠 (绿色)',
        componentType: 'BUTTON',
        propsConfig: JSON.stringify({ type: 'primary', text: '🎁 限时优惠立即购买 (实验组绿色)' }),
        styleConfig: JSON.stringify({}),
      },
      {
        id: 5,
        componentId: 'chart_1',
        componentName: '销量趋势图',
        componentType: 'BARCHART',
        propsConfig: JSON.stringify({}),
        styleConfig: JSON.stringify({}),
      },
    ]
    setComponents(mockComponents)
    addLog('success', `生成Mock组件(变体区分): ${mockComponents.length} 个组件`)
  }

  useEffect(() => {
    loadTest()
  }, [loadTest])

  useEffect(() => {
    if (!variant || !abTest) return
    const snapshotId = forceVariantId
      ? (abTest.variants?.find(v => v.id === forceVariantId)?.snapshotId || variant.snapshotId)
      : variant.snapshotId
    const finalVariant = forceVariantId
      ? abTest.variants?.find(v => v.id === forceVariantId) || variant
      : variant
    if (snapshotId) {
      loadPageBySnapshot(snapshotId, finalVariant.variantName)
    } else {
      addLog('warn', '变体无关联快照ID，使用默认渲染')
      generateMockComponents(0, finalVariant.variantName)
    }
  }, [variant, abTest, forceVariantId, loadPageBySnapshot])

  useEffect(() => {
    if (!variant || isLoading) return
    trackPageView()
    addLog('event', '已自动上报 PAGE_VIEW 事件')
  }, [variant, isLoading, trackPageView])

  const handleComponentEvent = (event: string, params: any) => {
    addLog('event', `组件事件: ${event} - ${params.componentName}`)
    if (event === 'click') {
      trackEvent('CLICK', params.componentId || params.componentName || 'button_click')
      addLog('event', `已上报 CLICK 事件: ${params.componentName}`)
    }
  }

  const handleSimulateConversion = () => {
    trackConversion('simulated_purchase', 99)
    addLog('event', '已上报 CONVERSION 事件: simulated_purchase')
    message.success('转化事件已上报')
  }

  const handleReallocate = () => {
    const uid = manualUserId || Math.floor(Math.random() * 100000) + 1
    setManualUserId(uid)
    setSearchParams({ userId: String(uid) })
    addLog('info', `重新分配流量，userId=${uid}`)
    setTimeout(() => window.location.reload(), 300)
  }

  const currentVariant = forceVariantId
    ? abTest?.variants?.find(v => v.id === forceVariantId)
    : variant

  return (
    <div style={{ background: '#f5f5f5', minHeight: '100vh', padding: 16 }}>
      <Card style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <Space wrap>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(`/abtest/detail/${id}`)}>
              返回详情
            </Button>
            <div style={{ borderLeft: '1px solid #e8e8e8', height: 24, margin: '0 8px' }} />
            <Title level={4} style={{ margin: 0 }}>
              <ExperimentOutlined style={{ marginRight: 8, color: '#1677ff' }} />
              A/B 测试运行时预览
            </Title>
            {abTest && (
              <Tag color="blue">{abTest.testCode}</Tag>
            )}
          </Space>
          <Space wrap>
            <Space>
              <Text type="secondary">用户ID:</Text>
              <InputNumber
                min={1}
                value={manualUserId}
                onChange={setManualUserId}
                placeholder="自定义userId"
                style={{ width: 140 }}
              />
              <Button icon={<ReloadOutlined />} onClick={handleReallocate}>
                重新分配
              </Button>
            </Space>
            {abTest?.variants && (
              <Space>
                <Text type="secondary">强制变体:</Text>
                <Select
                  allowClear
                  placeholder="选择变体"
                  value={forceVariantId}
                  onChange={setForceVariantId}
                  style={{ width: 220 }}
                >
                  {abTest.variants.map(v => (
                    <Option key={v.id} value={v.id}>
                      {v.variantType === 'control' ? '(对照) ' : '(实验) '}{v.variantName}
                    </Option>
                  ))}
                </Select>
              </Space>
            )}
          </Space>
        </div>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={17}>
          <Card
            title={
              <Space>
                <EyeOutlined />
                <span>页面渲染（按变体快照）</span>
                {currentVariant && (
                  currentVariant.variantType === 'control' ? (
                    <Tag color="blue" icon={<ControlOutlined />}>对照组: {currentVariant.variantName}</Tag>
                  ) : (
                    <Tag color="green" icon={<ThunderboltOutlined />}>实验组: {currentVariant.variantName}</Tag>
                  )
                )}
                {currentVariant?.snapshotId && (
                  <Tag color="purple">快照ID: {currentVariant.snapshotId}</Tag>
                )}
                {currentVariant?.version && (
                  <Tag color="cyan">版本: {currentVariant.version}</Tag>
                )}
              </Space>
            }
            loading={isLoading || loading || pageLoading}
          >
            {components.length === 0 ? (
              <Empty description="暂无页面组件，请等待变体分配" />
            ) : (
              <div style={{ background: '#fff', padding: 24, borderRadius: 8, minHeight: 400 }}>
                {components.map(comp => (
                  <div key={comp.id || comp.componentId}>
                    {renderRuntimeComponent(comp, handleComponentEvent)}
                  </div>
                ))}
                <Divider />
                <div style={{ textAlign: 'center', padding: 16 }}>
                  <Space>
                    <Button
                      type="primary"
                      size="large"
                      icon={<TrophyOutlined />}
                      onClick={handleSimulateConversion}
                    >
                      🎯 模拟转化事件
                    </Button>
                    <Button
                      icon={<CopyOutlined />}
                      onClick={() => {
                        message.success('已复制变体信息')
                        addLog('info', `变体: ${currentVariant?.variantName}, snapshotId=${currentVariant?.snapshotId}`)
                      }}
                    >
                      复制变体信息
                    </Button>
                  </Space>
                </div>
              </div>
            )}
          </Card>
        </Col>

        <Col xs={24} lg={7}>
          <Card
            title={
              <Space>
                <ExperimentOutlined />
                <span>运行时信息</span>
              </Space>
            }
            style={{ marginBottom: 16 }}
            size="small"
          >
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <div>
                <Text type="secondary">测试名称:</Text>
                <div style={{ fontWeight: 500 }}>{abTest?.testName || '-'}</div>
              </div>
              <div>
                <Text type="secondary">测试状态:</Text>
                {' '}
                {abTest?.status === 0 && <Tag color="default">草稿</Tag>}
                {abTest?.status === 1 && <Tag color="green" icon={<EyeOutlined />}>运行中</Tag>}
                {abTest?.status === 2 && <Tag color="orange">已暂停</Tag>}
                {abTest?.status === 3 && <Tag color="blue">已结束</Tag>}
              </div>
              <div>
                <Text type="secondary">用户ID:</Text>
                <Tag color="purple" icon={<UserOutlined />}>{currentUserId}</Tag>
              </div>
              <div>
                <Text type="secondary">分配变体:</Text>
                {' '}
                {isLoading ? (
                  <Spin size="small" />
                ) : currentVariant ? (
                  currentVariant.variantType === 'control' ? (
                    <Tag color="blue">对照组</Tag>
                  ) : (
                    <Tag color="green">实验组</Tag>
                  )
                ) : (
                  <Tag color="default">未分配</Tag>
                )}
              </div>
              <div>
                <Text type="secondary">快照ID:</Text>
                <span style={{ fontFamily: 'monospace' }}>{currentVariant?.snapshotId || '-'}</span>
              </div>
              <div>
                <Text type="secondary">版本:</Text>
                <span style={{ fontFamily: 'monospace' }}>{currentVariant?.version || '-'}</span>
              </div>
              <div>
                <Text type="secondary">组件数量:</Text>
                <span>{components.length}</span>
              </div>
              <div>
                <Text type="secondary">资源类型:</Text>
                <Tag>{abTest?.resourceType || '-'}</Tag>
              </div>
              <div>
                <Text type="secondary">资源ID:</Text>
                <span style={{ fontFamily: 'monospace' }}>{abTest?.resourceId || '-'}</span>
              </div>
            </Space>
          </Card>

          <Card
            title={
              <Space>
                <span>📋 事件日志</span>
                <Tag color="blue">{eventLog.length}</Tag>
              </Space>
            }
            size="small"
            style={{ maxHeight: 400, overflow: 'auto' }}
          >
            {eventLog.length === 0 ? (
              <Empty description="暂无事件" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ padding: 20 }} />
            ) : (
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                {eventLog.map((log, i) => (
                  <div key={i} style={{ fontSize: 12, lineHeight: 1.8 }}>
                    <span style={{ color: '#999', marginRight: 6 }}>[{log.time}]</span>
                    {log.type === 'event' && <Tag color="green" style={{ fontSize: 10 }}>EVENT</Tag>}
                    {log.type === 'info' && <Tag color="blue" style={{ fontSize: 10 }}>INFO</Tag>}
                    {log.type === 'success' && <Tag color="green" style={{ fontSize: 10 }}>OK</Tag>}
                    {log.type === 'warn' && <Tag color="orange" style={{ fontSize: 10 }}>WARN</Tag>}
                    <span style={{ marginLeft: 4, color: '#333' }}>{log.msg}</span>
                  </div>
                ))}
              </Space>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default ABTestRuntime
