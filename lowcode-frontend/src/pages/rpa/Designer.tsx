import React, { useState, useEffect, useCallback, useRef } from 'react'
import {
  Layout,
  Button,
  Space,
  Form,
  Input,
  Select,
  InputNumber,
  Modal,
  message,
  Drawer,
  Divider,
  Card,
  Table,
  Tag,
  Tooltip,
  Tabs,
  Badge,
  Empty,
  Alert,
} from 'antd'
import {
  SaveOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  DeleteOutlined,
  ArrowLeftOutlined,
  VideoCameraOutlined,
  VideoCameraAddOutlined,
  PauseOutlined,
  StopOutlined,
  UpOutlined,
  DownOutlined,
  EditOutlined,
  CopyOutlined,
  EyeOutlined,
  CodeOutlined,
  CheckCircleOutlined,
  RobotOutlined,
  BulbOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { RpaScript, RpaStep, rpaApi } from '@/api/flow'
import { useAppStore } from '@/store/appStore'

const { Header, Sider, Content } = Layout
const { Option } = Select
const { TextArea } = Input
const { TabPane } = Tabs

const actionTypes = [
  { type: 'navigate', name: '打开网址', icon: '🌐', color: '#1677ff', category: 'navigation' },
  { type: 'click', name: '点击元素', icon: '👆', color: '#52c41a', category: 'interaction' },
  { type: 'input', name: '输入文本', icon: '⌨️', color: '#722ed1', category: 'interaction' },
  { type: 'select', name: '下拉选择', icon: '📋', color: '#13c2c2', category: 'interaction' },
  { type: 'hover', name: '鼠标悬停', icon: '🖱️', color: '#eb2f96', category: 'interaction' },
  { type: 'press', name: '按键操作', icon: '⌨️', color: '#fa8c16', category: 'interaction' },
  { type: 'check', name: '勾选复选框', icon: '☑️', color: '#52c41a', category: 'interaction' },
  { type: 'uncheck', name: '取消勾选', icon: '🔲', color: '#ff4d4f', category: 'interaction' },
  { type: 'scroll', name: '滚动页面', icon: '📜', color: '#8c8c8c', category: 'page' },
  { type: 'wait', name: '等待', icon: '⏳', color: '#faad14', category: 'page' },
  { type: 'extract', name: '抓取数据', icon: '📥', color: '#2f54eb', category: 'data' },
  { type: 'screenshot', name: '截图', icon: '📷', color: '#a0d911', category: 'data' },
]

const getActionInfo = (type: string) => {
  return actionTypes.find(a => a.type === type) || { name: type, icon: '❓', color: '#999' }
}

interface StepFormProps {
  step: RpaStep
  index: number
  onSave: (step: RpaStep) => void
  onCancel: () => void
}

const StepForm: React.FC<StepFormProps> = ({ step, index, onSave, onCancel }) => {
  const [form] = Form.useForm()

  useEffect(() => {
    form.setFieldsValue(step)
  }, [step, form])

  const action = Form.useWatch('action', form)

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      onSave(values)
    } catch (e) {
      console.error(e)
    }
  }

  return (
    <Form form={form} layout="vertical" onFinish={handleSubmit}>
      <Form.Item name="action" label="操作类型" rules={[{ required: true }]}>
        <Select>
          {actionTypes.map(a => (
            <Option key={a.type} value={a.type}>
              {a.icon} {a.name}
            </Option>
          ))}
        </Select>
      </Form.Item>

      {['click', 'input', 'select', 'hover', 'press', 'check', 'uncheck', 'extract'].includes(action) && (
        <Form.Item
          name="selector"
          label="CSS选择器"
          rules={[{ required: true, message: '请输入CSS选择器' }]}
          extra="如: #username, .btn-login, button[type='submit']"
        >
          <Input placeholder="输入CSS选择器" />
        </Form.Item>
      )}

      {action === 'navigate' && (
        <Form.Item name="url" label="目标URL" rules={[{ required: true, message: '请输入URL' }]}>
          <Input placeholder="https://example.com" />
        </Form.Item>
      )}

      {['input', 'press'].includes(action) && (
        <Form.Item name="value" label={action === 'press' ? '按键(如: Enter)' : '输入值'}>
          <Input placeholder="支持变量: ${变量名}" />
        </Form.Item>
      )}

      {action === 'select' && (
        <Form.Item name="value" label="选择的值">
          <Input placeholder="下拉选项的值" />
        </Form.Item>
      )}

      {action === 'extract' && (
        <>
          <Form.Item name="fieldName" label="字段名称" rules={[{ required: true }]}>
            <Input placeholder="保存结果的字段名" />
          </Form.Item>
          <Form.Item name="extractType" label="提取类型">
            <Select defaultValue="text">
              <Option value="text">文本内容</Option>
              <Option value="value">输入框值</Option>
              <Option value="attribute">元素属性</Option>
              <Option value="html">HTML内容</Option>
              <Option value="all_text">所有匹配文本</Option>
            </Select>
          </Form.Item>
          <Form.Item name="attribute" label="属性名" extra="当提取类型为属性时需要填写">
            <Input placeholder="如: href, src, class" />
          </Form.Item>
        </>
      )}

      {action === 'scroll' && (
        <>
          <Form.Item name="direction" label="滚动方向">
            <Select defaultValue="down">
              <Option value="down">向下</Option>
              <Option value="up">向上</Option>
              <Option value="right">向右</Option>
              <Option value="left">向左</Option>
            </Select>
          </Form.Item>
          <Form.Item name="pixels" label="滚动像素">
            <InputNumber style={{ width: '100%' }} min={1} max={10000} defaultValue={500} />
          </Form.Item>
        </>
      )}

      {action === 'wait' && (
        <Form.Item name="seconds" label="等待秒数">
          <InputNumber style={{ width: '100%' }} min={0.1} max={60} step={0.5} defaultValue={2} />
        </Form.Item>
      )}

      {action === 'screenshot' && (
        <>
          <Form.Item name="name" label="截图名称">
            <Input placeholder="screenshot_1" />
          </Form.Item>
          <Form.Item name="fullPage" label="整页截图" valuePropName="checked">
            <Select>
              <Option value={true}>是</Option>
              <Option value={false}>否</Option>
            </Select>
          </Form.Item>
        </>
      )}

      <Form.Item name="timeout" label="超时时间(毫秒)">
        <InputNumber style={{ width: '100%' }} min={1000} max={120000} step={1000} defaultValue={30000} />
      </Form.Item>

      <Form.Item>
        <Space>
          <Button type="primary" htmlType="submit">
            保存步骤 {index + 1}
          </Button>
          <Button onClick={onCancel}>取消</Button>
        </Space>
      </Form.Item>
    </Form>
  )
}

const RpaDesigner: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [script, setScript] = useState<RpaScript | null>(null)
  const [form] = Form.useForm()
  const [steps, setSteps] = useState<RpaStep[]>([])
  const [editingStepIndex, setEditingStepIndex] = useState<number | null>(null)
  const [recording, setRecording] = useState(false)
  const [recordModalVisible, setRecordModalVisible] = useState(false)
  const [recordTargetUrl, setRecordTargetUrl] = useState('')
  const [codeVisible, setCodeVisible] = useState(false)
  const [executing, setExecuting] = useState(false)
  const [execResult, setExecResult] = useState<any>(null)

  const recorderRef = useRef<any>(null)

  const loadScript = useCallback(async () => {
    if (!id || id === 'undefined') {
      setScript({
        appId: currentApp?.id || 1,
        scriptName: '',
        scriptCode: '',
        scriptType: 'BROWSER',
        targetUrl: '',
        timeout: 300,
        status: 'DRAFT',
        description: '',
      })
      setSteps([])
      form.setFieldsValue({
        scriptName: '',
        scriptCode: '',
        targetUrl: '',
        timeout: 300,
        scriptType: 'BROWSER',
        description: '',
      })
      return
    }
    try {
      const res = await rpaApi.getScript(Number(id))
      const scriptData = res.data
      setScript(scriptData)
      form.setFieldsValue(scriptData)
      if (scriptData?.scriptContent) {
        try {
          const content = JSON.parse(scriptData.scriptContent)
          setSteps(content.steps || [])
        } catch (e) {
          console.error('解析脚本内容失败', e)
        }
      }
    } catch (e) {
      console.error(e)
    }
  }, [id, currentApp, form])

  useEffect(() => {
    loadScript()
  }, [loadScript])

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      if (!script) return

      const scriptContent = JSON.stringify({ steps })
      const data = {
        ...script,
        ...values,
        scriptContent,
      }

      if (script.id) {
        await rpaApi.updateScript(script.id, data)
        message.success('保存成功')
      } else {
        const res = await rpaApi.createScript(data)
        setScript(res.data)
        navigate(`/rpa/designer/${res.data.id}`, { replace: true })
        message.success('创建成功')
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handleAddStep = (action: string) => {
    const newStep: RpaStep = { action }
    setSteps([...steps, newStep])
    setEditingStepIndex(steps.length)
  }

  const handleEditStep = (index: number) => {
    setEditingStepIndex(index)
  }

  const handleSaveStep = (step: RpaStep) => {
    if (editingStepIndex === null) return
    const newSteps = [...steps]
    newSteps[editingStepIndex] = step
    setSteps(newSteps)
    setEditingStepIndex(null)
  }

  const handleDeleteStep = (index: number) => {
    const newSteps = steps.filter((_, i) => i !== index)
    setSteps(newSteps)
    if (editingStepIndex === index) {
      setEditingStepIndex(null)
    }
  }

  const handleMoveStep = (index: number, direction: 'up' | 'down') => {
    if (direction === 'up' && index === 0) return
    if (direction === 'down' && index === steps.length - 1) return

    const newSteps = [...steps]
    const targetIndex = direction === 'up' ? index - 1 : index + 1
    ;[newSteps[index], newSteps[targetIndex]] = [newSteps[targetIndex], newSteps[index]]
    setSteps(newSteps)
  }

  const handleCopyStep = (index: number) => {
    const stepToCopy = steps[index]
    const newStep = { ...stepToCopy }
    const newSteps = [...steps]
    newSteps.splice(index + 1, 0, newStep)
    setSteps(newSteps)
  }

  const handleStartRecording = () => {
    if (!recordTargetUrl) {
      message.warning('请输入要录制的网站URL')
      return
    }
    setRecording(true)
    setRecordModalVisible(false)
    message.info('录制模式已启动，请在新窗口中完成操作。注意：由于浏览器安全限制，完整录制功能需要安装Chrome扩展。此演示模式将引导您手动添加步骤。')

    setTimeout(() => {
      Modal.info({
        title: '录制引导',
        icon: <BulbOutlined />,
        content: (
          <div>
            <p>录制功能说明：</p>
            <ol>
              <li>在左侧面板选择操作类型手动添加步骤</li>
              <li>或使用浏览器开发者工具获取元素选择器</li>
              <li>推荐安装Chrome扩展获得完整录制体验</li>
            </ol>
          </div>
        ),
        onOk() {
          setRecording(false)
        },
      })
    }, 500)
  }

  const handleStopRecording = () => {
    setRecording(false)
    message.success('录制已停止')
  }

  const handleValidate = async () => {
    if (!script?.id) {
      message.warning('请先保存脚本')
      return
    }
    try {
      await rpaApi.validateScript(script.id)
      message.success('脚本验证通过')
    } catch (e: any) {
      Modal.error({
        title: '脚本验证失败',
        content: e?.response?.data?.message || '未知错误',
      })
    }
  }

  const handleExecute = async () => {
    if (!script?.id) {
      message.warning('请先保存脚本')
      return
    }
    setExecuting(true)
    setExecResult(null)
    try {
      const res = await rpaApi.executeScript(script.id, {}, 'MANUAL')
      setExecResult(res.data)
      message.success('执行完成')
    } catch (e: any) {
      message.error('执行失败: ' + (e?.response?.data?.message || e.message))
    } finally {
      setExecuting(false)
    }
  }

  const stepColumns = [
    {
      title: '序号',
      key: 'index',
      width: 60,
      render: (_: any, __: any, index: number) => index + 1,
    },
    {
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      width: 120,
      render: (action: string) => {
        const info = getActionInfo(action)
        return (
          <Tag color={info.color}>
            <span style={{ marginRight: 4 }}>{info.icon}</span>
            {info.name}
          </Tag>
        )
      },
    },
    {
      title: '选择器/URL',
      key: 'target',
      render: (_: any, record: RpaStep) => record.selector || record.url || '-',
    },
    {
      title: '值',
      dataIndex: 'value',
      key: 'value',
      render: (value: string) => value || '-',
    },
    {
      title: '字段名',
      dataIndex: 'fieldName',
      key: 'fieldName',
      render: (v: string) => v || '-',
    },
    {
      title: '操作',
      key: 'actions',
      width: 180,
      render: (_: any, __: any, index: number) => (
        <Space size="small">
          <Tooltip title="上移">
            <Button
              type="text"
              size="small"
              icon={<UpOutlined />}
              onClick={() => handleMoveStep(index, 'up')}
              disabled={index === 0}
            />
          </Tooltip>
          <Tooltip title="下移">
            <Button
              type="text"
              size="small"
              icon={<DownOutlined />}
              onClick={() => handleMoveStep(index, 'down')}
              disabled={index === steps.length - 1}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEditStep(index)} />
          </Tooltip>
          <Tooltip title="复制">
            <Button type="text" size="small" icon={<CopyOutlined />} onClick={() => handleCopyStep(index)} />
          </Tooltip>
          <Tooltip title="删除">
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleDeleteStep(index)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <Layout style={{ height: '100%' }}>
      <Header
        style={{
          background: '#fff',
          borderBottom: '1px solid #e8e8e8',
          padding: '0 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          height: 56,
          minHeight: 56,
        }}
      >
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/rpa')}>
            返回
          </Button>
          <Form form={form} layout="inline">
            <Form.Item name="scriptName" label="脚本名称" rules={[{ required: true }]}>
              <Input placeholder="请输入脚本名称" style={{ width: 180 }} />
            </Form.Item>
            <Form.Item name="scriptCode" label="脚本编码" rules={[{ required: true }]}>
              <Input placeholder="请输入脚本编码" style={{ width: 150 }} />
            </Form.Item>
            <Form.Item name="targetUrl" label="目标URL">
              <Input placeholder="https://example.com" style={{ width: 220 }} />
            </Form.Item>
            {script?.status && (
              <Badge
                status={script.status === 'PUBLISHED' ? 'success' : 'warning'}
                text={script.status === 'PUBLISHED' ? '已发布' : '草稿'}
              />
            )}
          </Form>
        </Space>
        <Space>
          {recording ? (
            <Button danger icon={<StopOutlined />} onClick={handleStopRecording}>
              停止录制
            </Button>
          ) : (
            <Button
              type="primary"
              icon={<VideoCameraAddOutlined />}
              onClick={() => setRecordModalVisible(true)}
              style={{ background: '#eb2f96', borderColor: '#eb2f96' }}
            >
              录制操作
            </Button>
          )}
          <Button icon={<CheckCircleOutlined />} onClick={handleValidate}>
            验证
          </Button>
          <Button icon={<CodeOutlined />} onClick={() => setCodeVisible(true)}>
            查看代码
          </Button>
          <Button
            icon={<PlayCircleOutlined />}
            onClick={handleExecute}
            loading={executing}
            type="primary"
          >
            试运行
          </Button>
          <Button icon={<SaveOutlined />} type="primary" onClick={handleSave}>
            保存
          </Button>
        </Space>
      </Header>

      <Layout>
        <Sider
          width={240}
          style={{
            background: '#fff',
            borderRight: '1px solid #e8e8e8',
            overflow: 'auto',
          }}
        >
          <div style={{ padding: 12 }}>
            <div style={{ marginBottom: 12, fontWeight: 'bold', color: '#666' }}>
              <SettingOutlined /> 添加操作步骤
            </div>
            {['navigation', 'interaction', 'page', 'data'].map(cat => (
              <div key={cat} style={{ marginBottom: 16 }}>
                <div style={{ fontSize: 12, color: '#999', marginBottom: 8, paddingLeft: 4 }}>
                  {cat === 'navigation' && '导航操作'}
                  {cat === 'interaction' && '交互操作'}
                  {cat === 'page' && '页面操作'}
                  {cat === 'data' && '数据操作'}
                </div>
                {actionTypes
                  .filter(a => a.category === cat)
                  .map(a => (
                    <Tooltip key={a.type} title={`添加: ${a.name}`}>
                      <Button
                        block
                        style={{
                          marginBottom: 6,
                          textAlign: 'left',
                          borderLeft: `3px solid ${a.color}`,
                        }}
                        onClick={() => handleAddStep(a.type)}
                      >
                        <span style={{ marginRight: 8 }}>{a.icon}</span>
                        {a.name}
                      </Button>
                    </Tooltip>
                  ))}
              </div>
            ))}
          </div>
        </Sider>

        <Content style={{ background: '#f5f5f5', padding: 16, overflow: 'auto' }}>
          {recording && (
            <Alert
              message="录制模式"
              description="正在录制浏览器操作，点击左侧面板添加步骤，或使用Chrome扩展自动捕获操作。"
              type="warning"
              showIcon
              icon={<VideoCameraOutlined spin />}
              style={{ marginBottom: 16 }}
              closable
            />
          )}

          {execResult && (
            <Card title="执行结果" style={{ marginBottom: 16 }}>
              <Tabs defaultActiveKey="summary">
                <TabPane tab="摘要" key="summary">
                  <p><strong>执行编号：</strong>{execResult.executionNo}</p>
                  <p><strong>状态：</strong>
                    <Tag color={execResult.status === 'SUCCESS' ? 'green' : 'red'}>
                      {execResult.status === 'SUCCESS' ? '成功' : '失败'}
                    </Tag>
                  </p>
                  <p><strong>耗时：</strong>{execResult.duration}ms</p>
                  {execResult.errorMessage && (
                    <p><strong>错误信息：</strong>{execResult.errorMessage}</p>
                  )}
                </TabPane>
                {execResult.outputResult && (
                  <TabPane tab="输出数据" key="output">
                    <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, maxHeight: 300, overflow: 'auto' }}>
                      {JSON.stringify(JSON.parse(execResult.outputResult), null, 2)}
                    </pre>
                  </TabPane>
                )}
              </Tabs>
            </Card>
          )}

          <Card
            title={
              <Space>
                <RobotOutlined />
                操作步骤
                <Tag color="blue">{steps.length} 个步骤</Tag>
              </Space>
            }
            extra={
              <Space>
                <Button icon={<PlusOutlined />} onClick={() => handleAddStep('click')}>
                  添加步骤
                </Button>
              </Space>
            }
          >
            {steps.length === 0 ? (
              <Empty
                description={
                  <div>
                    <p>还没有操作步骤</p>
                    <p style={{ color: '#999', fontSize: 12 }}>从左侧面板选择操作类型开始创建</p>
                  </div>
                }
              />
            ) : (
              <Table
                dataSource={steps}
                columns={stepColumns}
                rowKey={(record, index) => index + '_' + record.action}
                pagination={false}
                size="small"
                onRow={(_, index) => ({
                  style: editingStepIndex === index ? { background: '#e6f4ff' } : {},
                })}
              />
            )}
          </Card>
        </Content>

        <Sider
          width={360}
          style={{ background: '#fff', borderLeft: '1px solid #e8e8e8', overflow: 'auto' }}
        >
          {editingStepIndex !== null ? (
            <div style={{ padding: 16 }}>
              <div style={{ marginBottom: 16, fontWeight: 'bold' }}>
                编辑步骤 {editingStepIndex + 1}
              </div>
              <StepForm
                step={steps[editingStepIndex]}
                index={editingStepIndex}
                onSave={handleSaveStep}
                onCancel={() => setEditingStepIndex(null)}
              />
            </div>
          ) : (
            <div style={{ textAlign: 'center', padding: '60px 20px', color: '#999' }}>
              <EyeOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />
              <div style={{ marginTop: 16 }}>请选择步骤进行编辑</div>
              <div style={{ fontSize: 12, marginTop: 8 }}>点击步骤列表中的编辑按钮</div>
            </div>
          )}
        </Sider>
      </Layout>

      <Modal
        title={
          <Space>
            <VideoCameraOutlined />
            启动录制
          </Space>
        }
        open={recordModalVisible}
        onOk={handleStartRecording}
        onCancel={() => setRecordModalVisible(false)}
        okText="开始录制"
        cancelText="取消"
      >
        <div style={{ marginBottom: 16 }}>
          <Alert
            type="info"
            showIcon
            message="录制说明"
            description={
              <div>
                <p>1. 在下方输入要自动化操作的网站URL</p>
                <p>2. 系统将引导您完成操作步骤的录制</p>
                <p>3. 推荐安装Chrome扩展以获得完整的自动录制体验</p>
              </div>
            }
            style={{ marginBottom: 16 }}
          />
          <Input
            addonBefore="目标URL"
            placeholder="https://example.com"
            value={recordTargetUrl}
            onChange={e => setRecordTargetUrl(e.target.value)}
          />
        </div>
      </Modal>

      <Drawer
        title="生成的脚本代码"
        placement="right"
        width={600}
        open={codeVisible}
        onClose={() => setCodeVisible(false)}
      >
        <pre
          style={{
            background: '#1e1e1e',
            color: '#d4d4d4',
            padding: 16,
            borderRadius: 4,
            fontFamily: 'Consolas, monospace',
            fontSize: 12,
            overflow: 'auto',
            minHeight: '80vh',
          }}
        >
          {JSON.stringify({ steps }, null, 2)}
        </pre>
      </Drawer>
    </Layout>
  )
}

export default RpaDesigner
