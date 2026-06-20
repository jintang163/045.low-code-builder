import React, { useEffect, useState, useCallback, useRef } from 'react'
import {
  Row,
  Col,
  Card,
  Tabs,
  Form,
  Input,
  Select,
  Button,
  Space,
  Steps,
  Progress,
  Tag,
  Table,
  Modal,
  message,
  Switch,
  InputNumber,
  Tooltip,
  Slider,
  Badge,
  DatePicker,
  Empty,
  Spin,
  Divider,
  Collapse,
  Alert,
  Descriptions,
} from 'antd'
import {
  RocketOutlined,
  CloudServerOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  RollbackOutlined,
  EyeOutlined,
  FileTextOutlined,
  CopyOutlined,
  ApiOutlined,
  GlobalOutlined,
  DashboardOutlined,
  CpuOutlined,
  GoldOutlined,
  DatabaseOutlined,
  NodeIndexOutlined,
  ExperimentOutlined,
  EnvironmentOutlined,
  SafetyCertificateOutlined,
  ThunderboltOutlined,
  ContainerOutlined,
  CloudUploadOutlined,
  BuildOutlined,
  AuditOutlined,
  SendOutlined,
  LeftCircleOutlined,
  ClockCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'
import {
  deployApi,
  CloudConfig,
  AppService,
  DeploySpec,
  DeployTask,
  DeployStatus,
  DeployProgressEvent,
  DeployRequest,
  DeployResourceInfo,
} from '@/api/deploy'

const { Option } = Select
const { RangePicker } = DatePicker
const { TextArea } = Input

const DEPLOY_STEPS = [
  { key: 'ENV_CHECK', title: '环境检查', icon: <AuditOutlined /> },
  { key: 'BUILD_IMAGE', title: '构建镜像', icon: <BuildOutlined /> },
  { key: 'PUSH_REGISTRY', title: '推送仓库', icon: <CloudUploadOutlined /> },
  { key: 'DEPLOY_K8S', title: '部署K8s', icon: <SendOutlined /> },
  { key: 'SERVICE_READY', title: '服务就绪', icon: <CheckCircleOutlined /> },
]

const DEPLOY_PRESETS: Record<
  string,
  DeploySpec & { name: string; desc: string; icon: React.ReactNode }
> = {
  DEV: {
    name: '开发版',
    desc: '适合开发测试环境',
    icon: <ExperimentOutlined />,
    replicas: 1,
    cpuRequest: '100m',
    memoryRequest: '128Mi',
    cpuLimit: '500m',
    memoryLimit: '512Mi',
    enableHpa: false,
    rolloutStrategy: 'RollingUpdate',
    maxSurge: '25%',
    maxUnavailable: '25%',
  },
  STANDARD: {
    name: '标准版',
    desc: '适合中小规模生产',
    icon: <GoldOutlined />,
    replicas: 2,
    cpuRequest: '500m',
    memoryRequest: '512Mi',
    cpuLimit: '2',
    memoryLimit: '2Gi',
    enableHpa: true,
    minReplicas: 2,
    maxReplicas: 5,
    cpuThreshold: 70,
    rolloutStrategy: 'RollingUpdate',
    maxSurge: '25%',
    maxUnavailable: '0',
  },
  ENTERPRISE: {
    name: '企业版',
    desc: '适合高可用生产环境',
    icon: <SafetyCertificateOutlined />,
    replicas: 3,
    cpuRequest: '1',
    memoryRequest: '1Gi',
    cpuLimit: '4',
    memoryLimit: '4Gi',
    enableHpa: true,
    minReplicas: 3,
    maxReplicas: 10,
    cpuThreshold: 60,
    rolloutStrategy: 'RollingUpdate',
    maxSurge: '50%',
    maxUnavailable: '0',
  },
}

const statusMap: Record<DeployStatus, { color: string; text: string; icon: React.ReactNode }> = {
  PENDING: { color: 'default', text: '等待中', icon: <ClockCircleOutlined /> },
  BUILDING: { color: 'processing', text: '构建中', icon: <BuildOutlined spin /> },
  PUSHING: { color: 'processing', text: '推送中', icon: <CloudUploadOutlined spin /> },
  DEPLOYING: { color: 'processing', text: '部署中', icon: <RocketOutlined spin /> },
  RUNNING: { color: 'processing', text: '运行中', icon: <PlayCircleOutlined spin /> },
  SUCCESS: { color: 'success', text: '成功', icon: <CheckCircleOutlined /> },
  FAILED: { color: 'error', text: '失败', icon: <CloseCircleOutlined /> },
  ROLLING_BACK: { color: 'warning', text: '回滚中', icon: <RollbackOutlined spin /> },
  ROLLED_BACK: { color: 'warning', text: '已回滚', icon: <RollbackOutlined /> },
}

const platformMap: Record<string, { text: string; color: string; icon: React.ReactNode }> = {
  ALIYUN: { text: '阿里云', color: '#ff6a00', icon: <CloudServerOutlined /> },
  TENCENT: { text: '腾讯云', color: '#006eff', icon: <CloudServerOutlined /> },
  CUSTOM: { text: '自定义', color: '#722ed1', icon: <SettingOutlined /> },
}

const DeployCenter: React.FC = () => {
  const [activeTab, setActiveTab] = useState('deploy')
  const [selectedTask, setSelectedTask] = useState<DeployTask | null>(null)

  return (
    <div
      style={{
        padding: 16,
        background: 'linear-gradient(135deg, #0a0e27 0%, #1a1f3a 50%, #0d1117 100%)',
        minHeight: 'calc(100vh - 96px)',
        color: '#fff',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ color: '#00e5ff', margin: 0, fontSize: 24, fontWeight: 'bold' }}>
          <RocketOutlined /> 应用部署中心
        </h2>
        <Space>
          <span style={{ color: '#aaa' }}>{dayjs().format('YYYY-MM-DD HH:mm:ss')}</span>
        </Space>
      </div>

      <Card
        style={{
          background: '#0d111788',
          border: '1px solid #00e5ff33',
          borderRadius: 8,
        }}
        styles={{ body: { padding: 0 } }}
      >
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          tabBarStyle={{
            borderBottom: '1px solid #00e5ff33',
            padding: '0 16px',
            marginBottom: 0,
          }}
          style={{ color: '#fff' }}
          items={[
            {
              key: 'deploy',
              label: (
                <span>
                  <RocketOutlined /> 部署中心
                </span>
              ),
              children: <DeployTab />,
            },
            {
              key: 'cloud',
              label: (
                <span>
                  <CloudServerOutlined /> 云配置
                </span>
              ),
              children: <CloudConfigTab />,
            },
            {
              key: 'history',
              label: (
                <span>
                  <HistoryOutlined /> 任务历史
                </span>
              ),
              children: (
                <HistoryTab
                  onSelectTask={(task) => {
                    setSelectedTask(task)
                    setActiveTab('resource')
                  }}
                />
              ),
            },
            {
              key: 'resource',
              label: (
                <span>
                  <DashboardOutlined /> 资源看板
                  {selectedTask && <Badge status="processing" style={{ marginLeft: 8 }} />}
                </span>
              ),
              children: (
                <ResourceTab
                  task={selectedTask}
                  onBack={() => {
                    setSelectedTask(null)
                    setActiveTab('history')
                  }}
                />
              ),
            },
            {
              key: 'help',
              label: (
                <span>
                  <InfoCircleOutlined /> 帮助
                </span>
              ),
              children: <HelpTab />,
            },
          ]}
        />
      </Card>
    </div>
  )
}

const DeployTab: React.FC = () => {
  const [form] = Form.useForm<DeployRequest>()
  const [apps, setApps] = useState<AppService[]>([])
  const [cloudConfigs, setCloudConfigs] = useState<CloudConfig[]>([])
  const [presets, setPresets] = useState<DeploySpec[]>([])
  const [selectedPreset, setSelectedPreset] = useState<string>('STANDARD')
  const [deploying, setDeploying] = useState(false)
  const [currentTask, setCurrentTask] = useState<DeployTask | null>(null)
  const [events, setEvents] = useState<DeployProgressEvent[]>([])
  const [testLoading, setTestLoading] = useState<Record<number, boolean>>({})
  const wsRef = useRef<WebSocket | null>(null)
  const logContainerRef = useRef<HTMLDivElement>(null)

  const fetchApps = useCallback(async () => {
    try {
      const res = await deployApi.listApps()
      setApps(res.data)
    } catch (e) {
      console.error('加载应用列表失败', e)
    }
  }, [])

  const fetchCloudConfigs = useCallback(async () => {
    try {
      const res = await deployApi.listCloudConfigs()
      setCloudConfigs(res.data)
    } catch (e) {
      console.error('加载云配置失败', e)
    }
  }, [])

  const fetchPresets = useCallback(async () => {
    try {
      const res = await deployApi.getPresets()
      setPresets(res.data)
    } catch (e) {
      setPresets(Object.values(DEPLOY_PRESETS))
    }
  }, [])

  useEffect(() => {
    fetchApps()
    fetchCloudConfigs()
    fetchPresets()
    form.setFieldsValue({
      version: `1.0.0-${dayjs().format('YYYYMMDDHHmmss')}`,
      spec: { ...DEPLOY_PRESETS.STANDARD },
    })
  }, [fetchApps, fetchCloudConfigs, fetchPresets, form])

  useEffect(() => {
    const preset = DEPLOY_PRESETS[selectedPreset]
    if (preset) {
      form.setFieldsValue({ spec: { ...preset } })
    }
  }, [selectedPreset, form])

  useEffect(() => {
    if (logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight
    }
  }, [events])

  const connectWebSocket = useCallback((taskId: string) => {
    if (wsRef.current) {
      wsRef.current.close()
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/deploy/${taskId}`
    const ws = new WebSocket(wsUrl)
    wsRef.current = ws

    ws.onmessage = (event) => {
      try {
        const data: DeployProgressEvent = JSON.parse(event.data)
        setEvents((prev) => [...prev, data])
        setCurrentTask((prev) =>
          prev
            ? {
                ...prev,
                status: data.status,
                progress: data.progress,
                currentStep: data.step,
              }
            : prev
        )
      } catch (e) {
        console.error('WebSocket消息解析失败', e)
      }
    }

    ws.onerror = (error) => {
      console.error('WebSocket连接错误', error)
    }

    ws.onclose = () => {
      console.log('WebSocket连接已关闭')
    }

    return () => {
      ws.close()
    }
  }, [])

  const handleSelectApp = (serviceName: string) => {
    const app = apps.find((a) => a.serviceName === serviceName)
    if (app) {
      form.setFieldsValue({
        serviceName: app.serviceName,
        displayName: app.displayName,
        modulePath: app.modulePath,
      })
    }
  }

  const handleTestCloudConfig = async (id: number) => {
    setTestLoading((prev) => ({ ...prev, [id]: true }))
    try {
      const res = await deployApi.testCloudConfig(id)
      if (res.data) {
        message.success('连接测试成功')
      } else {
        message.error('连接测试失败')
      }
    } catch (e) {
      message.error('连接测试失败')
    } finally {
      setTestLoading((prev) => ({ ...prev, [id]: false }))
    }
  }

  const fetchTask = async (taskId: string) => {
    try {
      const res = await deployApi.getTask(taskId)
      setCurrentTask(res.data)
      if (
        res.data.progress < 100 &&
        !['SUCCESS', 'FAILED', 'ROLLED_BACK'].includes(res.data.status)
      ) {
        setTimeout(() => fetchTask(taskId), 3000)
      }
    } catch (e) {
      console.error('获取任务状态失败', e)
    }
  }

  const handleSubmitDeploy = async () => {
    try {
      const values = await form.validateFields()
      setDeploying(true)
      setEvents([])
      const res = await deployApi.submitDeploy(values)
      const task = res.data
      setCurrentTask(task)
      connectWebSocket(task.taskId)
      message.success('部署任务已提交')
      void fetchTask(task.taskId)
    } catch (e) {
      console.error('部署提交失败', e)
    } finally {
      setDeploying(false)
    }
  }

  const handleFetchEvents = async (taskId: string) => {
    try {
      const res = await deployApi.getTaskEvents(taskId)
      setEvents(res.data)
    } catch (e) {
      console.error('获取事件日志失败', e)
    }
  }

  const currentStepIndex = currentTask
    ? DEPLOY_STEPS.findIndex((s) => s.key === currentTask.currentStep)
    : -1

  const spec = Form.useWatch('spec', form) || DEPLOY_PRESETS.STANDARD

  return (
    <Row gutter={[16, 16]} style={{ padding: 16 }}>
      <Col span={12}>
        <Card
          title={<span style={{ color: '#00e5ff' }}>部署参数配置</span>}
          style={{
            background: '#0d111788',
            border: '1px solid #00e5ff33',
            borderRadius: 8,
          }}
          styles={{ body: { padding: 16 } }}
          size="small"
        >
          <Form form={form} layout="vertical" style={{ color: '#fff' }}>
            <Form.Item
              name="serviceName"
              label={<span style={{ color: '#ccc' }}>应用服务</span>}
              rules={[{ required: true, message: '请选择应用服务' }]}
            >
              <Select
                placeholder="请选择应用服务"
                onChange={(value) => handleSelectApp(value)}
                style={{ background: '#1a1f3a' }}
                listHeight={200}
                optionRender={(option) => {
                  const app = apps.find((a) => a.serviceName === option.value)
                  return (
                    <Space>
                      <ContainerOutlined style={{ color: '#00e5ff' }} />
                      <span>{app?.displayName || app?.serviceName}</span>
                      <Tag color="blue" style={{ marginLeft: 'auto' }}>
                        {app?.imageName}:{app?.imageTag}
                      </Tag>
                    </Space>
                  )
                }}
                options={apps.map((a) => ({
                  value: a.serviceName,
                  label: a.displayName || a.serviceName,
                }))}
              />
            </Form.Item>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="displayName"
                  label={<span style={{ color: '#ccc' }}>显示名称</span>}
                  rules={[{ required: true, message: '请输入显示名称' }]}
                >
                  <Input placeholder="例如：用户服务" style={{ background: '#1a1f3a' }} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="modulePath"
                  label={<span style={{ color: '#ccc' }}>模块路径</span>}
                  rules={[{ required: true, message: '请输入模块路径' }]}
                >
                  <Input placeholder="例如：/modules/user" style={{ background: '#1a1f3a' }} />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item label={<span style={{ color: '#ccc' }}>云平台配置</span>} required>
              <Space.Compact style={{ width: '100%' }}>
                <Form.Item
                  name="cloudConfigId"
                  noStyle
                  rules={[{ required: true, message: '请选择云配置' }]}
                >
                  <Select
                    placeholder="选择云配置"
                    style={{ width: '80%', background: '#1a1f3a' }}
                    options={cloudConfigs.map((c) => ({
                      value: c.id,
                      label: (
                        <Space>
                          <span style={{ color: platformMap[c.platform]?.color }}>
                            {platformMap[c.platform]?.icon}
                          </span>
                          <span>{c.description || c.region}</span>
                          <Tag color={platformMap[c.platform]?.color}>
                            {platformMap[c.platform]?.text}
                          </Tag>
                        </Space>
                      ),
                    }))}
                  />
                </Form.Item>
                <Button
                  icon={<ApiOutlined />}
                  loading={testLoading[form.getFieldValue('cloudConfigId')]}
                  onClick={() => {
                    const id = form.getFieldValue('cloudConfigId')
                    if (id) handleTestCloudConfig(id)
                  }}
                >
                  测试
                </Button>
              </Space.Compact>
            </Form.Item>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="version"
                  label={<span style={{ color: '#ccc' }}>版本号</span>}
                  rules={[{ required: true, message: '请输入版本号' }]}
                >
                  <Input placeholder="例如：1.0.0" style={{ background: '#1a1f3a' }} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="domain" label={<span style={{ color: '#ccc' }}>域名绑定(可选)</span>}>
                  <Input
                    prefix={<GlobalOutlined />}
                    placeholder="例如：api.example.com"
                    style={{ background: '#1a1f3a' }}
                  />
                </Form.Item>
              </Col>
            </Row>

            <Divider style={{ borderColor: '#00e5ff33', margin: '12px 0' }}>
              <span style={{ color: '#888', fontSize: 12 }}>
                <NodeIndexOutlined /> 部署规格
              </span>
            </Divider>

            <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
              {Object.entries(DEPLOY_PRESETS).map(([key, preset]) => (
                <Col span={8} key={key}>
                  <div
                    onClick={() => setSelectedPreset(key)}
                    style={{
                      cursor: 'pointer',
                      padding: 12,
                      borderRadius: 8,
                      border: `2px solid ${selectedPreset === key ? '#00e5ff' : '#ffffff22'}`,
                      background: selectedPreset === key ? '#00e5ff22' : '#1a1f3a66',
                      transition: 'all 0.3s',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
                      <span
                        style={{
                          fontSize: 24,
                          color: selectedPreset === key ? '#00e5ff' : '#888',
                          marginRight: 8,
                        }}
                      >
                        {preset.icon}
                      </span>
                      <div>
                        <div
                          style={{
                            fontWeight: 'bold',
                            color: selectedPreset === key ? '#00e5ff' : '#fff',
                          }}
                        >
                          {preset.name}
                        </div>
                        <div style={{ fontSize: 11, color: '#888' }}>{preset.desc}</div>
                      </div>
                    </div>
                    <Space direction="vertical" size={0} style={{ fontSize: 11, color: '#aaa' }}>
                      <div>
                        <CpuOutlined /> CPU: {preset.cpuRequest} ~ {preset.cpuLimit}
                      </div>
                      <div>
                        <DatabaseOutlined /> 内存: {preset.memoryRequest} ~ {preset.memoryLimit}
                      </div>
                      <div>
                        <ContainerOutlined /> 实例: {preset.replicas}
                        {preset.enableHpa
                          ? ` (HPA ${preset.minReplicas}-${preset.maxReplicas})`
                          : ''}
                      </div>
                    </Space>
                  </div>
                </Col>
              ))}
            </Row>

            <Collapse
              bordered={false}
              ghost
              size="small"
              style={{ background: 'transparent', color: '#fff' }}
              items={[
                {
                  key: '1',
                  label: (
                    <span style={{ color: '#888' }}>
                      <SettingOutlined /> 高级配置
                    </span>
                  ),
                  children: (
                    <Space direction="vertical" size={12} style={{ width: '100%' }}>
                      <Row gutter={16}>
                        <Col span={12}>
                          <Form.Item
                            name={['spec', 'enableHpa']}
                            label={<span style={{ color: '#ccc' }}>启用HPA自动扩缩容</span>}
                            valuePropName="checked"
                          >
                            <Switch />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item
                            name={['spec', 'rolloutStrategy']}
                            label={<span style={{ color: '#ccc' }}>滚动策略</span>}
                          >
                            <Select style={{ background: '#1a1f3a' }}>
                              <Option value="RollingUpdate">滚动更新</Option>
                              <Option value="Recreate">重建</Option>
                            </Select>
                          </Form.Item>
                        </Col>
                      </Row>

                      {spec?.enableHpa && (
                        <Row gutter={16}>
                          <Col span={8}>
                            <Form.Item
                              name={['spec', 'minReplicas']}
                              label={<span style={{ color: '#ccc' }}>最小副本数</span>}
                            >
                              <InputNumber
                                min={1}
                                style={{ width: '100%', background: '#1a1f3a' }}
                              />
                            </Form.Item>
                          </Col>
                          <Col span={8}>
                            <Form.Item
                              name={['spec', 'maxReplicas']}
                              label={<span style={{ color: '#ccc' }}>最大副本数</span>}
                            >
                              <InputNumber
                                min={1}
                                style={{ width: '100%', background: '#1a1f3a' }}
                              />
                            </Form.Item>
                          </Col>
                          <Col span={8}>
                            <Form.Item
                              name={['spec', 'cpuThreshold']}
                              label={<span style={{ color: '#ccc' }}>CPU阈值(%)</span>}
                            >
                              <InputNumber
                                min={1}
                                max={100}
                                style={{ width: '100%', background: '#1a1f3a' }}
                              />
                            </Form.Item>
                          </Col>
                        </Row>
                      )}

                      {spec?.rolloutStrategy === 'RollingUpdate' && (
                        <Row gutter={16}>
                          <Col span={12}>
                            <Form.Item
                              name={['spec', 'maxSurge']}
                              label={<span style={{ color: '#ccc' }}>最大超出</span>}
                            >
                              <Input placeholder="25% 或 2" style={{ background: '#1a1f3a' }} />
                            </Form.Item>
                          </Col>
                          <Col span={12}>
                            <Form.Item
                              name={['spec', 'maxUnavailable']}
                              label={<span style={{ color: '#ccc' }}>最大不可用</span>}
                            >
                              <Input placeholder="25% 或 1" style={{ background: '#1a1f3a' }} />
                            </Form.Item>
                          </Col>
                        </Row>
                      )}

                      <Form.Item
                        label={<span style={{ color: '#ccc' }}>环境变量 (KEY=VALUE，每行一个)</span>}
                      >
                        <TextArea
                          rows={4}
                          placeholder="NODE_ENV=production\nLOG_LEVEL=info"
                          style={{ background: '#1a1f3a', fontFamily: 'monospace' }}
                          onChange={(e) => {
                            const lines = e.target.value.split('\n').filter(Boolean)
                            const envVars: Record<string, string> = {}
                            lines.forEach((line) => {
                              const [k, ...v] = line.split('=')
                              if (k) envVars[k.trim()] = v.join('=').trim()
                            })
                            form.setFieldValue(['spec', 'envVars'], envVars)
                          }}
                        />
                      </Form.Item>
                    </Space>
                  ),
                },
              ]}
            />

            <Button
              type="primary"
              size="large"
              block
              icon={<RocketOutlined />}
              loading={deploying}
              onClick={handleSubmitDeploy}
              style={{
                marginTop: 16,
                height: 48,
                fontSize: 16,
                fontWeight: 'bold',
                background: 'linear-gradient(135deg, #00e5ff 0%, #0066ff 100%)',
                border: 'none',
                boxShadow: '0 4px 20px rgba(0, 229, 255, 0.4)',
              }}
            >
              一键部署
            </Button>
          </Form>
        </Card>
      </Col>

      <Col span={12}>
        <Card
          title={
            <Space>
              <span style={{ color: '#ffa940' }}>部署进度面板</span>
              {currentTask && (
                <Tag color={statusMap[currentTask.status]?.color}>
                  {statusMap[currentTask.status]?.icon}
                  {statusMap[currentTask.status]?.text}
                </Tag>
              )}
            </Space>
          }
          extra={
            currentTask && (
              <Space>
                <Tooltip title="查看任务">
                  <Button
                    size="small"
                    icon={<EyeOutlined />}
                    onClick={() => handleFetchEvents(currentTask.taskId)}
                  />
                </Tooltip>
              </Space>
            )
          }
          style={{
            background: '#0d111788',
            border: '1px solid #ffa94033',
            borderRadius: 8,
          }}
          styles={{ body: { padding: 16 } }}
          size="small"
        >
          {!currentTask ? (
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '60px 0',
                color: '#888',
              }}
            >
              <RocketOutlined style={{ fontSize: 64, opacity: 0.3 }} />
              <div style={{ marginTop: 16 }}>暂无部署任务</div>
              <div style={{ fontSize: 12, marginTop: 8 }}>配置左侧参数后点击【一键部署】开始</div>
            </div>
          ) : (
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  padding: 16,
                  background: '#1a1f3a66',
                  borderRadius: 8,
                  border: '1px solid #00e5ff22',
                }}
              >
                <Progress
                  type="circle"
                  percent={currentTask.progress}
                  size={100}
                  strokeColor={{
                    '0%': '#00e5ff',
                    '100%': '#00ff88',
                  }}
                  trailColor="#ffffff11"
                />
                <div style={{ flex: 1, marginLeft: 16 }}>
                  <div style={{ fontSize: 16, fontWeight: 'bold', color: '#fff' }}>
                    {currentTask.deployName}
                  </div>
                  <div style={{ fontSize: 12, color: '#888', marginTop: 4 }}>
                    <Space>
                      <span>任务ID: {currentTask.taskId}</span>
                      <CopyOutlined
                        style={{ cursor: 'pointer' }}
                        onClick={() => {
                          navigator.clipboard?.writeText(currentTask.taskId)
                          message.success('已复制')
                        }}
                      />
                    </Space>
                  </div>
                  <div style={{ fontSize: 12, color: '#aaa', marginTop: 4 }}>
                    当前步骤: {currentTask.currentStep}
                  </div>
                  {currentTask.errorMessage && (
                    <Alert
                      type="error"
                      message={currentTask.errorMessage}
                      showIcon
                      style={{ marginTop: 8 }}
                    />
                  )}
                </div>
              </div>

              <Steps
                direction="vertical"
                size="small"
                current={currentStepIndex >= 0 ? currentStepIndex + 1 : 0}
                status={currentTask.status === 'FAILED' ? 'error' : undefined}
                items={DEPLOY_STEPS.map((step) => ({
                  title: <span style={{ color: '#fff' }}>{step.title}</span>,
                  icon: step.icon,
                  description: (
                    <span style={{ fontSize: 11, color: '#888' }}>
                      {events.find((e) => e.step === step.key)?.message || '等待执行...'}
                    </span>
                  ),
                }))}
              />

              <div>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: 8,
                  }}
                >
                  <Space>
                    <FileTextOutlined style={{ color: '#ffa940' }} />
                    <span style={{ color: '#ffa940', fontWeight: 500 }}>实时日志</span>
                  </Space>
                  <Space>
                    <Button
                      size="small"
                      icon={<ReloadOutlined />}
                      onClick={() => handleFetchEvents(currentTask.taskId)}
                    />
                  </Space>
                </div>
                <div
                  ref={logContainerRef}
                  style={{
                    background: '#000',
                    borderRadius: 8,
                    padding: 12,
                    height: 240,
                    overflowY: 'auto',
                    fontFamily: 'Consolas, Monaco, monospace',
                    fontSize: 12,
                    lineHeight: 1.6,
                    border: '1px solid #ffffff11',
                  }}
                >
                  {events.length === 0 ? (
                    <div style={{ color: '#666' }}>等待日志输出...</div>
                  ) : (
                    events.map((event, i) => (
                      <div key={i} style={{ display: 'flex', alignItems: 'flex-start' }}>
                        <span style={{ color: '#555', marginRight: 8, flexShrink: 0 }}>
                          [{dayjs(event.timestamp).format('HH:mm:ss')}]
                        </span>
                        <span
                          style={{
                            color:
                              event.logLevel === 'ERROR'
                                ? '#ff6b6b'
                                : event.logLevel === 'WARN'
                                ? '#ffd93d'
                                : '#aaa',
                            marginRight: 8,
                            flexShrink: 0,
                            fontWeight: 'bold',
                          }}
                        >
                          [{event.logLevel}]
                        </span>
                        <span
                          style={{
                            color: event.logLevel === 'ERROR' ? '#ff6b6b' : '#e0e0e0',
                          }}
                        >
                          {event.message}
                        </span>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </Space>
          )}
        </Card>
      </Col>
    </Row>
  )
}

const CloudConfigTab: React.FC = () => {
  const [configs, setConfigs] = useState<CloudConfig[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingConfig, setEditingConfig] = useState<CloudConfig | null>(null)
  const [testLoading, setTestLoading] = useState<Record<number, boolean>>({})
  const [form] = Form.useForm()

  const fetchConfigs = useCallback(async () => {
    setLoading(true)
    try {
      const res = await deployApi.listCloudConfigs()
      setConfigs(res.data)
    } catch (e) {
      console.error('加载云配置失败', e)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchConfigs()
  }, [fetchConfigs])

  const openAddModal = () => {
    setEditingConfig(null)
    form.resetFields()
    form.setFieldsValue({ platform: 'ALIYUN' })
    setModalVisible(true)
  }

  const openEditModal = (config: CloudConfig) => {
    setEditingConfig(config)
    form.setFieldsValue(config)
    setModalVisible(true)
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      const data = editingConfig ? { ...editingConfig, ...values } : values
      await deployApi.saveCloudConfig(data)
      message.success(editingConfig ? '更新成功' : '创建成功')
      setModalVisible(false)
      fetchConfigs()
    } catch (e) {
      console.error(e)
    }
  }

  const handleDelete = async (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '删除后无法恢复，确认要删除该云配置吗？',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deployApi.deleteCloudConfig(id)
          message.success('删除成功')
          fetchConfigs()
        } catch (e) {
          message.error('删除失败')
        }
      },
    })
  }

  const handleTest = async (id: number) => {
    setTestLoading((prev) => ({ ...prev, [id]: true }))
    try {
      const res = await deployApi.testCloudConfig(id)
      if (res.data) {
        message.success('连接测试成功')
      } else {
        message.error('连接测试失败')
      }
    } catch (e) {
      message.error('连接测试失败')
    } finally {
      setTestLoading((prev) => ({ ...prev, [id]: false }))
    }
  }

  const columns = [
    {
      title: '平台',
      dataIndex: 'platform',
      key: 'platform',
      width: 120,
      render: (v: string) => {
        const p = platformMap[v]
        return (
          <Tag color={p?.color} icon={p?.icon}>
            {p?.text || v}
          </Tag>
        )
      },
    },
    {
      title: '地域',
      dataIndex: 'region',
      key: 'region',
      width: 140,
      render: (v: string) => (
        <Space>
          <EnvironmentOutlined style={{ color: '#00e5ff' }} />
          <span style={{ fontFamily: 'monospace' }}>{v}</span>
        </Space>
      ),
    },
    {
      title: '集群ID',
      dataIndex: 'clusterId',
      key: 'clusterId',
      width: 200,
      render: (v: string) => (
        <span style={{ fontFamily: 'monospace', color: '#aaa', fontSize: 12 }}>{v}</span>
      ),
    },
    {
      title: '镜像仓库',
      dataIndex: 'registryUrl',
      key: 'registryUrl',
      ellipsis: true,
      render: (v: string) => <span style={{ color: '#888' }}>{v}</span>,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      render: (v: string) => v || <span style={{ color: '#666' }}>--</span>,
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: CloudConfig) => (
        <Space>
          <Button
            size="small"
            icon={<ApiOutlined />}
            loading={testLoading[record.id!]}
            onClick={() => handleTest(record.id!)}
          >
            测试
          </Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEditModal(record)} />
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id!)}
          />
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Space>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索地域/集群/描述"
            allowClear
            style={{ width: 300, background: '#1a1f3a' }}
          />
        </Space>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={openAddModal}
          style={{ background: 'linear-gradient(135deg, #00e5ff 0%, #0066ff 100%)' }}
        >
          新增云配置
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={configs}
        rowKey="id"
        loading={loading}
        style={{ color: '#fff' }}
        pagination={{ pageSize: 10, style: { color: '#fff' } }}
        locale={{ emptyText: <Empty description="暂无云配置" /> }}
      />

      <Modal
        title={editingConfig ? '编辑云配置' : '新增云配置'}
        open={modalVisible}
        onOk={handleSave}
        onCancel={() => setModalVisible(false)}
        width={720}
        okText="保存"
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="platform" label="云平台" rules={[{ required: true }]}>
                <Select style={{ background: '#fff' }}>
                  <Option value="ALIYUN">
                    <Space>
                      <span style={{ color: '#ff6a00' }}>
                        <CloudServerOutlined />
                      </span>
                      阿里云 ACK
                    </Space>
                  </Option>
                  <Option value="TENCENT">
                    <Space>
                      <span style={{ color: '#006eff' }}>
                        <CloudServerOutlined />
                      </span>
                      腾讯云 TKE
                    </Space>
                  </Option>
                  <Option value="CUSTOM">
                    <Space>
                      <span style={{ color: '#722ed1' }}>
                        <SettingOutlined />
                      </span>
                      自定义集群
                    </Space>
                  </Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="region" label="地域" rules={[{ required: true }]}>
                <Input placeholder="例如：cn-hangzhou / ap-beijing" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="accessKey" label="AccessKey ID" rules={[{ required: true }]}>
                <Input placeholder="AccessKey ID" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="accessSecret" label="AccessKey Secret" rules={[{ required: true }]}>
                <Input.Password placeholder="AccessKey Secret" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="clusterId" label="K8s集群ID" rules={[{ required: true }]}>
                <Input placeholder="集群ID" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="registryUrl" label="镜像仓库地址" rules={[{ required: true }]}>
                <Input placeholder="例如：registry.cn-hangzhou.aliyuncs.com" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="registryNamespace" label="命名空间">
                <Input placeholder="命名空间(可选)" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="registryUsername" label="仓库用户名">
                <Input placeholder="用户名(可选)" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="registryPassword" label="仓库密码">
                <Input.Password placeholder="密码(可选)" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="vpcId" label="VPC ID">
                <Input placeholder="VPC ID(可选)" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="securityGroupId" label="安全组ID">
                <Input placeholder="安全组ID(可选)" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="description" label="配置描述">
            <TextArea rows={2} placeholder="描述信息，便于识别" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

interface HistoryTabProps {
  onSelectTask: (task: DeployTask) => void
}

const HistoryTab: React.FC<HistoryTabProps> = ({ onSelectTask }) => {
  const [tasks, setTasks] = useState<DeployTask[]>([])
  const [loading, setLoading] = useState(false)
  const [statusFilter, setStatusFilter] = useState<DeployStatus | undefined>()
  const [serviceName, setServiceName] = useState('')
  const [detailVisible, setDetailVisible] = useState(false)
  const [selectedTask, setSelectedTask] = useState<DeployTask | null>(null)

  const fetchTasks = useCallback(async () => {
    setLoading(true)
    try {
      const res = await deployApi.listTasks({ status: statusFilter, serviceName })
      setTasks(res.data?.list || res.data || [])
    } catch (e) {
      console.error('加载任务列表失败', e)
    } finally {
      setLoading(false)
    }
  }, [statusFilter, serviceName])

  useEffect(() => {
    fetchTasks()
  }, [fetchTasks])

  const handleViewDetail = (task: DeployTask) => {
    setSelectedTask(task)
    setDetailVisible(true)
  }

  const handleRollback = (task: DeployTask) => {
    Modal.confirm({
      title: '确认回滚',
      content: `确认将 ${task.deployName} 回滚到上一个版本？`,
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deployApi.rollback(task.taskId)
          message.success('回滚任务已提交')
          fetchTasks()
        } catch (e) {
          message.error('回滚失败')
        }
      },
    })
  }

  const columns = [
    {
      title: '任务ID',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 180,
      render: (v: string) => (
        <span style={{ fontFamily: 'monospace', color: '#00e5ff', fontSize: 12 }}>{v}</span>
      ),
    },
    {
      title: '服务',
      dataIndex: 'deployName',
      key: 'deployName',
      render: (v: string, record: DeployTask) => (
        <Space>
          <ContainerOutlined style={{ color: '#1890ff' }} />
          <span style={{ color: '#fff' }}>{v}</span>
          <Tag color="blue">{record.version}</Tag>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (v: DeployStatus) => {
        const s = statusMap[v]
        return (
          <Tag color={s?.color} icon={s?.icon}>
            {s?.text}
          </Tag>
        )
      },
    },
    {
      title: '进度',
      dataIndex: 'progress',
      key: 'progress',
      width: 180,
      render: (v: number, record: DeployTask) => (
        <Progress
          percent={v}
          size="small"
          status={
            record.status === 'FAILED'
              ? 'exception'
              : record.status === 'SUCCESS'
              ? 'success'
              : 'active'
          }
          strokeColor={{
            '0%': '#00e5ff',
            '100%': '#00ff88',
          }}
        />
      ),
    },
    {
      title: '版本',
      dataIndex: 'version',
      key: 'version',
      width: 120,
      render: (v: string) => (
        <Tag color="purple" style={{ fontFamily: 'monospace' }}>
          {v}
        </Tag>
      ),
    },
    {
      title: '开始时间',
      dataIndex: 'startedAt',
      key: 'startedAt',
      width: 160,
      render: (v: string) => (
        <span style={{ color: '#888', fontSize: 12 }}>
          {dayjs(v).format('YYYY-MM-DD HH:mm:ss')}
        </span>
      ),
    },
    {
      title: '耗时',
      key: 'duration',
      width: 100,
      render: (_: any, record: DeployTask) => {
        const end = record.finishedAt ? dayjs(record.finishedAt) : dayjs()
        const duration = end.diff(dayjs(record.startedAt), 'second')
        return (
          <span style={{ color: '#aaa' }}>
            <ThunderboltOutlined /> {duration}s
          </span>
        )
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_: any, record: DeployTask) => (
        <Space>
          <Tooltip title="查看详情">
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
            />
          </Tooltip>
          <Tooltip title="查看日志">
            <Button
              size="small"
              icon={<FileTextOutlined />}
              onClick={() => onSelectTask(record)}
            />
          </Tooltip>
          <Tooltip title="资源看板">
            <Button
              size="small"
              icon={<DashboardOutlined />}
              onClick={() => onSelectTask(record)}
            />
          </Tooltip>
          {record.status === 'SUCCESS' && (
            <Tooltip title="回滚">
              <Button
                size="small"
                danger
                icon={<RollbackOutlined />}
                onClick={() => handleRollback(record)}
              />
            </Tooltip>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Space>
          <Select
            placeholder="状态筛选"
            allowClear
            style={{ width: 160 }}
            value={statusFilter}
            onChange={setStatusFilter}
            options={Object.entries(statusMap).map(([k, v]) => ({
              value: k as DeployStatus,
              label: (
                <Space>
                  <Tag color={v.color} icon={v.icon}>
                    {v.text}
                  </Tag>
                </Space>
              ),
            }))}
          />
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索服务名"
            allowClear
            style={{ width: 240 }}
            value={serviceName}
            onChange={(e) => setServiceName(e.target.value)}
            onPressEnter={fetchTasks}
          />
          <RangePicker style={{ width: 280 }} />
          <Button icon={<SearchOutlined />} onClick={fetchTasks} type="primary">
            搜索
          </Button>
        </Space>
        <Button icon={<ReloadOutlined />} onClick={fetchTasks}>
          刷新
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={tasks}
        rowKey="taskId"
        loading={loading}
        style={{ color: '#fff' }}
        pagination={{ pageSize: 10, style: { color: '#fff' } }}
        locale={{ emptyText: <Empty description="暂无部署任务" /> }}
      />

      <Modal
        title="任务详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={640}
      >
        {selectedTask && (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Descriptions column={2} size="small" bordered>
              <Descriptions.Item label="任务ID">
                <code>{selectedTask.taskId}</code>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={statusMap[selectedTask.status]?.color}>
                  {statusMap[selectedTask.status]?.text}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="服务名">{selectedTask.deployName}</Descriptions.Item>
              <Descriptions.Item label="版本">
                <Tag color="purple">{selectedTask.version}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="开始时间">
                {dayjs(selectedTask.startedAt).format('YYYY-MM-DD HH:mm:ss')}
              </Descriptions.Item>
              <Descriptions.Item label="完成时间">
                {selectedTask.finishedAt
                  ? dayjs(selectedTask.finishedAt).format('YYYY-MM-DD HH:mm:ss')
                  : '--'}
              </Descriptions.Item>
              <Descriptions.Item label="进度" span={2}>
                <Progress
                  percent={selectedTask.progress}
                  status={
                    selectedTask.status === 'FAILED'
                      ? 'exception'
                      : selectedTask.status === 'SUCCESS'
                      ? 'success'
                      : 'active'
                  }
                />
              </Descriptions.Item>
              {selectedTask.errorMessage && (
                <Descriptions.Item label="错误信息" span={2}>
                  <span style={{ color: '#ff4d4f' }}>{selectedTask.errorMessage}</span>
                </Descriptions.Item>
              )}
              <Descriptions.Item label="实例数" span={2}>
                <Space>
                  <Tag>副本: {selectedTask.spec.replicas}</Tag>
                  <Tag>
                    CPU: {selectedTask.spec.cpuRequest} ~ {selectedTask.spec.cpuLimit}
                  </Tag>
                  <Tag>
                    内存: {selectedTask.spec.memoryRequest} ~ {selectedTask.spec.memoryLimit}
                  </Tag>
                </Space>
              </Descriptions.Item>
            </Descriptions>
          </Space>
        )}
      </Modal>
    </div>
  )
}

interface ResourceTabProps {
  task: DeployTask | null
  onBack: () => void
}

const ResourceTab: React.FC<ResourceTabProps> = ({ task, onBack }) => {
  const [resource, setResource] = useState<DeployResourceInfo | null>(null)
  const [loading, setLoading] = useState(false)
  const [replicas, setReplicas] = useState(1)
  const [logs, setLogs] = useState<string[]>([])
  const [podName, setPodName] = useState('')
  const [logLines, setLogLines] = useState(200)
  const logContainerRef = useRef<HTMLDivElement>(null)

  const fetchResource = useCallback(async () => {
    if (!task) return
    setLoading(true)
    try {
      const res = await deployApi.getResource(task.taskId)
      setResource(res.data)
      setReplicas(res.data.currentReplicas || res.data.replicas || 1)
    } catch (e) {
      console.error('获取资源信息失败', e)
    } finally {
      setLoading(false)
    }
  }, [task])

  const fetchLogs = useCallback(async () => {
    if (!task) return
    try {
      const res = await deployApi.getLogs({
        taskId: task.taskId,
        podName,
        lines: logLines,
      })
      setLogs(res.data)
    } catch (e) {
      console.error('获取日志失败', e)
    }
  }, [task, podName, logLines])

  useEffect(() => {
    if (task) {
      fetchResource()
    }
  }, [task, fetchResource])

  useEffect(() => {
    if (logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight
    }
  }, [logs])

  const handleScale = async () => {
    if (!task) return
    try {
      await deployApi.scale({ taskId: task.taskId, replicas })
      message.success(`已设置副本数为 ${replicas}`)
      fetchResource()
    } catch (e) {
      message.error('扩缩容失败')
    }
  }

  const resourcePieOption = resource
    ? {
        tooltip: { trigger: 'item' },
        legend: {
          bottom: 0,
          textStyle: { color: '#fff' },
        },
        series: [
          {
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '40%'],
            avoidLabelOverlap: false,
            label: {
              show: true,
              color: '#fff',
              formatter: '{b}: {c}',
            },
            data: [
              {
                value: resource.readyReplicas || 0,
                name: '就绪',
                itemStyle: { color: '#52c41a' },
              },
              {
                value: Math.max(0, (resource.replicas || 0) - (resource.readyReplicas || 0)),
                name: '未就绪',
                itemStyle: { color: '#faad14' },
              },
            ],
          },
        ],
      }
    : {}

  const hpaOption = resource
    ? {
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, top: 30, bottom: 30 },
        xAxis: {
          type: 'category',
          data: ['当前', '目标'],
          axisLine: { lineStyle: { color: '#ffa94033' } },
          axisLabel: { color: '#ffa940' },
        },
        yAxis: {
          type: 'value',
          max: 100,
          axisLine: { lineStyle: { color: '#ffa94033' } },
          axisLabel: { color: '#ffa940', formatter: '{value}%' },
          splitLine: { lineStyle: { color: '#ffa94022' } },
        },
        series: [
          {
            type: 'bar',
            barWidth: 40,
            data: [
              {
                value: resource.currentCpuUtilization || 0,
                itemStyle: {
                  color:
                    (resource.currentCpuUtilization || 0) >
                    (resource.targetCpuUtilization || 80)
                      ? '#ff4d4f'
                      : '#00e5ff',
                },
              },
              {
                value: resource.targetCpuUtilization || 80,
                itemStyle: { color: '#ffa940' },
              },
            ],
            label: {
              show: true,
              position: 'top',
              color: '#fff',
              formatter: '{c}%',
            },
          },
        ],
      }
    : {}

  if (!task) {
    return (
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          padding: '80px 0',
          color: '#888',
        }}
      >
        <DashboardOutlined style={{ fontSize: 64, opacity: 0.3 }} />
        <div style={{ marginTop: 16, fontSize: 16 }}>请先从任务历史中选择一个部署任务</div>
        <Button style={{ marginTop: 16 }} onClick={onBack}>
          <HistoryOutlined /> 前往任务历史
        </Button>
      </div>
    )
  }

  return (
    <div style={{ padding: 16 }}>
      <div style={{ marginBottom: 16 }}>
        <Button icon={<LeftCircleOutlined />} onClick={onBack} style={{ marginRight: 12 }}>
          返回任务列表
        </Button>
        <Space>
          <Tag color={statusMap[task.status]?.color} icon={statusMap[task.status]?.icon}>
            {statusMap[task.status]?.text}
          </Tag>
          <span style={{ color: '#fff', fontWeight: 500 }}>{task.deployName}</span>
          <Tag color="purple">{task.version}</Tag>
          <span style={{ color: '#888', fontSize: 12 }}>{task.taskId}</span>
        </Space>
      </div>

      <Spin spinning={loading}>
        <Row gutter={[16, 16]}>
          <Col span={6}>
            <Card
              title={<span style={{ color: '#52c41a' }}>Pod 状态</span>}
              style={{
                background: '#0d111788',
                border: '1px solid #52c41a33',
                borderRadius: 8,
              }}
              styles={{ body: { padding: 12 } }}
              size="small"
            >
              <ReactECharts option={resourcePieOption} style={{ height: 200 }} theme="dark" />
              <div
                style={{
                  textAlign: 'center',
                  marginTop: 8,
                  color: '#fff',
                  fontSize: 20,
                  fontWeight: 'bold',
                }}
              >
                <span style={{ color: '#52c41a' }}>{resource?.readyReplicas || 0}</span>
                <span style={{ color: '#888' }}> / </span>
                <span>{resource?.replicas || 0}</span>
                <span style={{ fontSize: 12, color: '#888', marginLeft: 4 }}>就绪/期望</span>
              </div>
            </Card>
          </Col>

          <Col span={6}>
            <Card
              title={<span style={{ color: '#1890ff' }}>Service 信息</span>}
              style={{
                background: '#0d111788',
                border: '1px solid #1890ff33',
                borderRadius: 8,
              }}
              styles={{ body: { padding: 16 } }}
              size="small"
            >
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#888' }}>名称:</span>
                  <span style={{ color: '#fff', fontFamily: 'monospace' }}>
                    {resource?.serviceName || '--'}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#888' }}>类型:</span>
                  <Tag color="blue">{resource?.serviceType || 'ClusterIP'}</Tag>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#888' }}>端口:</span>
                  <Tag color="geekblue">{resource?.servicePort || '--'}</Tag>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#888' }}>命名空间:</span>
                  <span style={{ fontFamily: 'monospace', color: '#aaa' }}>
                    {resource?.namespace || 'default'}
                  </span>
                </div>
              </Space>
            </Card>
          </Col>

          <Col span={6}>
            <Card
              title={<span style={{ color: '#722ed1' }}>Ingress 域名</span>}
              style={{
                background: '#0d111788',
                border: '1px solid #722ed133',
                borderRadius: 8,
              }}
              styles={{ body: { padding: 16 } }}
              size="small"
            >
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#888' }}>Ingress:</span>
                  <span style={{ color: '#fff', fontFamily: 'monospace' }}>
                    {resource?.ingressName || '--'}
                  </span>
                </div>
                <div>
                  <span style={{ color: '#888', display: 'block', marginBottom: 4 }}>域名:</span>
                  {resource?.host ? (
                    <Tag color="purple" icon={<GlobalOutlined />}>
                      {resource.host}
                    </Tag>
                  ) : (
                    <span style={{ color: '#666' }}>--</span>
                  )}
                </div>
              </Space>
            </Card>
          </Col>

          <Col span={6}>
            <Card
              title={<span style={{ color: '#eb2f96' }}>HPA 状态</span>}
              style={{
                background: '#0d111788',
                border: '1px solid #eb2f9633',
                borderRadius: 8,
              }}
              styles={{ body: { padding: 12 } }}
              size="small"
            >
              <ReactECharts option={hpaOption} style={{ height: 200 }} theme="dark" />
              <div
                style={{
                  textAlign: 'center',
                  color: '#fff',
                  fontSize: 14,
                  marginTop: 4,
                }}
              >
                当前副本: <span style={{ color: '#eb2f96', fontWeight: 'bold' }}>{resource?.currentReplicas || resource?.replicas || 0}</span>
              </div>
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col span={12}>
            <Card
              title={
                <Space>
                  <span style={{ color: '#00e5ff' }}>
                    <RiseOutlined /> 扩缩容控制
                  </span>
                </Space>
              }
              style={{
                background: '#0d111788',
                border: '1px solid #00e5ff33',
                borderRadius: 8,
              }}
              styles={{ body: { padding: 20 } }}
              size="small"
            >
              <Space direction="vertical" size={20} style={{ width: '100%' }}>
                <div>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      marginBottom: 12,
                    }}
                  >
                    <span style={{ color: '#ccc' }}>副本数量</span>
                    <span style={{ color: '#00e5ff', fontWeight: 'bold', fontSize: 24 }}>
                      {replicas}
                    </span>
                  </div>
                  <Slider
                    min={1}
                    max={20}
                    value={replicas}
                    onChange={setReplicas}
                    marks={{
                      1: '1',
                      3: '3',
                      5: '5',
                      10: '10',
                      15: '15',
                      20: '20',
                    }}
                    tooltip={{ formatter: (v) => `${v} 副本` }}
                    style={{ color: '#00e5ff' }}
                  />
                </div>
                <Space>
                  <Button
                    type="primary"
                    onClick={handleScale}
                    icon={<RiseOutlined />}
                    style={{
                      background: 'linear-gradient(135deg, #00e5ff 0%, #0066ff 100%)',
                    }}
                  >
                    应用扩缩容
                  </Button>
                  <Button icon={<ReloadOutlined />} onClick={fetchResource}>
                    刷新资源
                  </Button>
                </Space>
              </Space>
            </Card>
          </Col>

          <Col span={12}>
            <Card
              title={
                <Space>
                  <span style={{ color: '#ffa940' }}>
                    <FileTextOutlined /> Pod 日志查看器
                  </span>
                </Space>
              }
              style={{
                background: '#0d111788',
                border: '1px solid #ffa94033',
                borderRadius: 8,
              }}
              styles={{ body: { padding: 16 } }}
              size="small"
            >
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <Space>
                  <Select
                    placeholder="选择Pod"
                    style={{ width: 240 }}
                    value={podName || undefined}
                    onChange={setPodName}
                    allowClear
                    options={
                      resource
                        ? Array.from({ length: resource.replicas || 1 }).map((_, i) => ({
                            value: `${resource.deploymentName || 'pod'}-${i}`,
                            label: `${resource.deploymentName || 'pod'}-${i}`,
                          }))
                        : []
                    }
                  />
                  <Select
                    placeholder="行数"
                    style={{ width: 120 }}
                    value={logLines}
                    onChange={setLogLines}
                    options={[
                      { value: 100, label: '100行' },
                      { value: 200, label: '200行' },
                      { value: 500, label: '500行' },
                      { value: 1000, label: '1000行' },
                    ]}
                  />
                  <Button icon={<ReloadOutlined />} onClick={fetchLogs} type="primary">
                    加载日志
                  </Button>
                </Space>
                <div
                  ref={logContainerRef}
                  style={{
                    background: '#000',
                    borderRadius: 8,
                    padding: 12,
                    height: 280,
                    overflowY: 'auto',
                    fontFamily: 'Consolas, Monaco, monospace',
                    fontSize: 11,
                    lineHeight: 1.5,
                    border: '1px solid #ffffff11',
                  }}
                >
                  {logs.length === 0 ? (
                    <div style={{ color: '#666' }}>请选择Pod并加载日志...</div>
                  ) : (
                    logs.map((line, i) => (
                      <div key={i} style={{ color: '#c0c0c0', whiteSpace: 'pre-wrap' }}>
                        {line}
                      </div>
                    ))
                  )}
                </div>
              </Space>
            </Card>
          </Col>
        </Row>
      </Spin>
    </div>
  )
}

const HelpTab: React.FC = () => {
  return (
    <div style={{ padding: 24 }}>
      <Row gutter={[24, 24]}>
        <Col span={12}>
          <Card
            title={
              <Space>
                <RocketOutlined style={{ color: '#00e5ff' }} />
                <span style={{ color: '#00e5ff' }}>快速开始</span>
              </Space>
            }
            style={{
              background: '#0d111788',
              border: '1px solid #00e5ff33',
              borderRadius: 8,
            }}
            styles={{ body: { padding: 20 } }}
          >
            <Steps
              direction="vertical"
              size="small"
              current={-1}
              items={[
                {
                  title: <span style={{ color: '#fff' }}>配置云平台</span>,
                  description: (
                    <span style={{ color: '#aaa' }}>
                      在「云配置」Tab 中添加阿里云/腾讯云/自定义集群配置，包括 AccessKey、集群ID、镜像仓库等信息。
                    </span>
                  ),
                },
                {
                  title: <span style={{ color: '#fff' }}>注册应用服务</span>,
                  description: (
                    <span style={{ color: '#aaa' }}>
                      在部署中心选择要部署的应用服务，或通过API注册新的服务。
                    </span>
                  ),
                },
                {
                  title: <span style={{ color: '#fff' }}>配置部署参数</span>,
                  description: (
                    <span style={{ color: '#aaa' }}>
                      选择部署规格（开发版/标准版/企业版），配置版本号、域名、环境变量等。
                    </span>
                  ),
                },
                {
                  title: <span style={{ color: '#fff' }}>一键部署</span>,
                  description: (
                    <span style={{ color: '#aaa' }}>
                      点击「一键部署」按钮，系统将自动完成构建镜像→推送仓库→部署K8s→服务就绪整个流程。
                    </span>
                  ),
                },
                {
                  title: <span style={{ color: '#fff' }}>监控与运维</span>,
                  description: (
                    <span style={{ color: '#aaa' }}>
                      在「任务历史」查看部署记录，在「资源看板」进行扩缩容、查看日志等操作。
                    </span>
                  ),
                },
              ]}
            />
          </Card>
        </Col>

        <Col span={12}>
          <Card
            title={
              <Space>
                <InfoCircleOutlined style={{ color: '#ffa940' }} />
                <span style={{ color: '#ffa940' }}>常见问题</span>
              </Space>
            }
            style={{
              background: '#0d111788',
              border: '1px solid #ffa94033',
              borderRadius: 8,
            }}
            styles={{ body: { padding: 16 } }}
          >
            <Collapse
              bordered={false}
              ghost
              style={{ color: '#fff' }}
              items={[
                {
                  key: '1',
                  label: <span style={{ color: '#fff' }}>如何获取阿里云 ACK 集群 ID？</span>,
                  children: (
                    <span style={{ color: '#aaa' }}>
                      登录阿里云控制台 → 容器服务 ACK → 集群列表 → 点击目标集群 → 基本信息中可查看集群 ID。
                    </span>
                  ),
                },
                {
                  key: '2',
                  label: <span style={{ color: '#fff' }}>部署失败如何排查？</span>,
                  children: (
                    <span style={{ color: '#aaa' }}>
                      1. 查看实时日志中的错误信息；
                      2. 检查云配置是否正确，可点击「测试」按钮验证；
                      3. 确认镜像仓库权限配置正确；
                      4. 在资源看板中查看 Pod 状态和日志。
                    </span>
                  ),
                },
                {
                  key: '3',
                  label: <span style={{ color: '#fff' }}>HPA 自动扩缩容如何工作？</span>,
                  children: (
                    <span style={{ color: '#aaa' }}>
                      HPA（Horizontal Pod Autoscaler）会根据 CPU 使用率自动调整 Pod 副本数。
                      当平均 CPU 使用率超过阈值时自动扩容，低于阈值时自动缩容（在 min/max 副本范围内）。
                    </span>
                  ),
                },
                {
                  key: '4',
                  label: <span style={{ color: '#fff' }}>如何回滚到上一个版本？</span>,
                  children: (
                    <span style={{ color: '#aaa' }}>
                      在「任务历史」中找到成功的部署任务，点击操作列的「回滚」按钮。
                      系统会创建一个新的回滚任务，将服务恢复到该版本。
                    </span>
                  ),
                },
                {
                  key: '5',
                  label: <span style={{ color: '#fff' }}>支持哪些部署策略？</span>,
                  children: (
                    <span style={{ color: '#aaa' }}>
                      目前支持 RollingUpdate（滚动更新，推荐）和 Recreate（重建）两种策略。
                      滚动更新会逐步替换旧版本 Pod，保证服务不中断；
                      重建策略会先删除所有旧 Pod 再启动新 Pod，可能造成短暂停机。
                    </span>
                  ),
                },
              ]}
            />
          </Card>
        </Col>
      </Row>

      <Row style={{ marginTop: 24 }}>
        <Col span={24}>
          <Card
            title={
              <Space>
                <ApiOutlined style={{ color: '#722ed1' }} />
                <span style={{ color: '#722ed1' }}>部署规格说明</span>
              </Space>
            }
            style={{
              background: '#0d111788',
              border: '1px solid #722ed133',
              borderRadius: 8,
            }}
            styles={{ body: { padding: 16 } }}
          >
            <Table
              pagination={false}
              style={{ color: '#fff' }}
              columns={[
                { title: '规格', dataIndex: 'name', key: 'name', width: 120 },
                { title: '适用场景', dataIndex: 'scene', key: 'scene' },
                { title: '副本数', dataIndex: 'replicas', key: 'replicas', width: 100 },
                { title: 'CPU(请求/限制)', dataIndex: 'cpu', key: 'cpu', width: 160 },
                { title: '内存(请求/限制)', dataIndex: 'memory', key: 'memory', width: 180 },
                { title: 'HPA', dataIndex: 'hpa', key: 'hpa', width: 160 },
              ]}
              dataSource={[
                {
                  key: 'dev',
                  name: (
                    <Tag color="blue" icon={<ExperimentOutlined />}>
                      开发版
                    </Tag>
                  ),
                  scene: '开发/测试环境，非核心业务',
                  replicas: 1,
                  cpu: '100m / 500m',
                  memory: '128Mi / 512Mi',
                  hpa: <Tag color="default">不启用</Tag>,
                },
                {
                  key: 'standard',
                  name: (
                    <Tag color="orange" icon={<GoldOutlined />}>
                      标准版
                    </Tag>
                  ),
                  scene: '中小规模生产环境，一般业务系统',
                  replicas: 2,
                  cpu: '500m / 2',
                  memory: '512Mi / 2Gi',
                  hpa: (
                    <Tag color="green">
                      启用 (2-5副本, 70%阈值)
                    </Tag>
                  ),
                },
                {
                  key: 'enterprise',
                  name: (
                    <Tag color="purple" icon={<SafetyCertificateOutlined />}>
                      企业版
                    </Tag>
                  ),
                  scene: '高可用生产环境，核心业务系统',
                  replicas: 3,
                  cpu: '1 / 4',
                  memory: '1Gi / 4Gi',
                  hpa: (
                    <Tag color="green">
                      启用 (3-10副本, 60%阈值)
                    </Tag>
                  ),
                },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default DeployCenter
