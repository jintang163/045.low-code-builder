import React, { useState, useEffect } from 'react'
import {
  Table, Button, Space, Tag, Modal, Form, Input, Select, InputNumber,
  Switch, message, Popconfirm, Card, Row, Col, Tabs, Tooltip, Badge, Progress,
} from 'antd'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined,
  ReloadOutlined, ApiOutlined, DatabaseOutlined, HeartOutlined,
  SettingOutlined, EyeOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { dataSourceApi, DataSource, PoolStatus } from '@/api/dataModel'
import { useAppStore } from '@/store/appStore'

const { Option } = Select
const { TabPane } = Tabs

const dbTypeOptions = [
  { label: 'MySQL', value: 'mysql', port: 3306 },
  { label: 'Oracle', value: 'oracle', port: 1521 },
  { label: 'SQL Server', value: 'sqlserver', port: 1433 },
  { label: 'PostgreSQL', value: 'postgresql', port: 5432 },
  { label: '达梦', value: 'dm', port: 5236 },
  { label: 'REST API', value: 'rest_api', port: 0 },
]

const authTypeOptions = [
  { label: '无认证', value: 'NONE' },
  { label: 'Bearer Token', value: 'BEARER' },
  { label: 'Basic Auth', value: 'BASIC' },
  { label: 'API Key', value: 'API_KEY' },
]

const DataSourceList: React.FC = () => {
  const { currentApp } = useAppStore()
  const navigate = useNavigate()
  const [data, setData] = useState<DataSource[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [poolModalVisible, setPoolModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<DataSource | null>(null)
  const [testLoading, setTestLoading] = useState<string | null>(null)
  const [poolStatus, setPoolStatus] = useState<PoolStatus | null>(null)
  const [poolLoading, setPoolLoading] = useState(false)
  const [form] = Form.useForm()
  const [poolForm] = Form.useForm()
  const [sourceType, setSourceType] = useState<string>('DATABASE')

  const fetchData = async () => {
    if (!currentApp?.id) return
    try {
      setLoading(true)
      const res: any = await dataSourceApi.list(currentApp.id)
      if (res.code === 0 || res.code === 200) {
        setData(res.data || [])
      }
    } catch (e: any) {
      message.error('获取数据源列表失败: ' + e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [currentApp])

  const handleDbTypeChange = (dbType: string) => {
    const opt = dbTypeOptions.find(o => o.value === dbType)
    if (opt) {
      form.setFieldsValue({ port: opt.port })
      if (dbType === 'rest_api') {
        setSourceType('REST_API')
        form.setFieldsValue({ sourceType: 'REST_API' })
      } else {
        setSourceType('DATABASE')
        form.setFieldsValue({ sourceType: 'DATABASE' })
      }
    }
  }

  const handleCreate = () => {
    setEditingItem(null)
    form.resetFields()
    setSourceType('DATABASE')
    form.setFieldsValue({
      dbType: 'mysql',
      port: 3306,
      host: 'localhost',
      sourceType: 'DATABASE',
      initialSize: 2,
      minIdle: 2,
      maxActive: 10,
      maxWait: 60000,
      timeBetweenEvictionRunsMillis: 60000,
      minEvictableIdleTimeMillis: 600000,
      maxLifetime: 1800000,
      connectionTimeout: 30000,
      testWhileIdle: true,
      testOnBorrow: false,
      testOnReturn: false,
      connectTimeout: 5000,
      readTimeout: 10000,
      restApiMethod: 'GET',
      restApiAuthType: 'NONE',
    })
    setModalVisible(true)
  }

  const handleEdit = (record: DataSource) => {
    setEditingItem(record)
    setSourceType(record.sourceType || (record.dbType === 'rest_api' ? 'REST_API' : 'DATABASE'))
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      const res: any = await dataSourceApi.delete(id)
      if (res.code === 0 || res.code === 200) {
        message.success('删除成功')
        fetchData()
      }
    } catch (e: any) {
      message.error('删除失败: ' + e.message)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const dataWithApp: DataSource = {
        ...values,
        appId: currentApp?.id || 0,
      }
      if (editingItem) {
        const res: any = await dataSourceApi.update({ ...editingItem, ...dataWithApp })
        if (res.code === 0 || res.code === 200) {
          message.success('更新成功')
          fetchData()
          setModalVisible(false)
        }
      } else {
        const res: any = await dataSourceApi.save(dataWithApp)
        if (res.code === 0 || res.code === 200) {
          message.success('创建成功')
          fetchData()
          setModalVisible(false)
        }
      }
    } catch (e: any) {
      if (e.message) message.error(e.message)
    }
  }

  const handleTestConnection = async (record: DataSource) => {
    try {
      setTestLoading(record.id + '')
      const res: any = await dataSourceApi.testConnection(record)
      if (res.code === 0 || res.code === 200) {
        if (res.data) {
          message.success(`测试连接成功: ${record.sourceName}`)
          fetchData()
        } else {
          message.error('测试连接失败')
        }
      }
    } catch (e: any) {
      message.error('测试连接失败: ' + e.message)
    } finally {
      setTestLoading(null)
    }
  }

  const handleHealthCheck = async (id: number) => {
    try {
      const res: any = await dataSourceApi.healthCheck(id)
      if (res.code === 0 || res.code === 200) {
        if (res.data?.healthy) {
          message.success('健康检查通过')
        } else {
          message.warning('健康检查未通过')
        }
        fetchData()
      }
    } catch (e: any) {
      message.error('健康检查失败: ' + e.message)
    }
  }

  const handleViewPoolStatus = async (id: number) => {
    try {
      setPoolLoading(true)
      setPoolModalVisible(true)
      const res: any = await dataSourceApi.getPoolStatus(id)
      if (res.code === 0 || res.code === 200) {
        setPoolStatus(res.data)
      }
    } catch (e: any) {
      message.error('获取连接池状态失败: ' + e.message)
    } finally {
      setPoolLoading(false)
    }
  }

  const handleRefreshPool = async (id: number) => {
    try {
      const res: any = await dataSourceApi.refreshPool(id)
      if (res.code === 0 || res.code === 200) {
        message.success('连接池已刷新')
      }
    } catch (e: any) {
      message.error('刷新连接池失败: ' + e.message)
    }
  }

  const handleOpenDesigner = (record: DataSource) => {
    navigate(`/dataSource/designer/${record.id}`)
  }

  const columns = [
    { title: '数据源名称', dataIndex: 'sourceName', key: 'sourceName' },
    { title: '数据源编码', dataIndex: 'sourceCode', key: 'sourceCode' },
    {
      title: '类型',
      key: 'type',
      render: (_: any, record: DataSource) => (
        <Space>
          <Tag color={record.dbType === 'rest_api' ? 'blue' : 'green'}>
            {record.sourceType === 'REST_API' ? 'REST API' : '数据库'}
          </Tag>
          <Tag>{dbTypeOptions.find(o => o.value === record.dbType)?.label || record.dbType}</Tag>
        </Space>
      ),
    },
    {
      title: '连接信息',
      key: 'connInfo',
      render: (_: any, record: DataSource) => (
        record.dbType === 'rest_api'
          ? <Tooltip title={record.restApiUrl}><span style={{ maxWidth: 200, display: 'inline-block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{record.restApiUrl}</span></Tooltip>
          : `${record.host}:${record.port}/${record.database}`
      ),
    },
    {
      title: '健康状态',
      key: 'health',
      render: (_: any, record: DataSource) => {
        const status = record.healthCheckStatus
        if (status === 'HEALTHY') return <Badge status="success" text="健康" />
        if (status === 'UNHEALTHY') return <Badge status="error" text="异常" />
        return <Badge status="default" text="未检查" />
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: DataSource) => (
        <Space size="small" wrap>
          <Tooltip title="设计器">
            <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleOpenDesigner(record)} />
          </Tooltip>
          <Button
            type="link"
            size="small"
            icon={<PlayCircleOutlined />}
            loading={testLoading === record.id + ''}
            onClick={() => handleTestConnection(record)}
          >
            测试
          </Button>
          <Tooltip title="健康检查">
            <Button type="link" size="small" icon={<HeartOutlined />} onClick={() => handleHealthCheck(record.id!)} />
          </Tooltip>
          {record.dbType !== 'rest_api' && (
            <>
              <Tooltip title="连接池">
                <Button type="link" size="small" icon={<SettingOutlined />} onClick={() => handleViewPoolStatus(record.id!)} />
              </Tooltip>
              <Tooltip title="刷新连接池">
                <Button type="link" size="small" icon={<ReloadOutlined />} onClick={() => handleRefreshPool(record.id!)} />
              </Tooltip>
            </>
          )}
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h2>数据源连接器</h2>
        <Space>
          <Button icon={<ApiOutlined />} onClick={() => navigate('/dataSource/designer')}>
            虚拟视图
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate} disabled={!currentApp}>
            新建数据源
          </Button>
        </Space>
      </div>

      {!currentApp && (
        <div style={{ marginBottom: 16, padding: 12, background: '#fffbe6', borderRadius: 4, border: '1px solid #ffe58f' }}>
          请先在【应用管理】中选择一个应用
        </div>
      )}

      <Card>
        <Table columns={columns} dataSource={data} rowKey="id" loading={loading} />
      </Card>

      <Modal
        title={editingItem ? '编辑数据源' : '新建数据源'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={720}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Tabs defaultActiveKey="basic">
            <TabPane tab="基本信息" key="basic">
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="sourceName" label="数据源名称" rules={[{ required: true, message: '请输入' }]}>
                    <Input placeholder="请输入数据源名称" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="sourceCode" label="数据源编码" rules={[{ required: true, message: '请输入' }]}>
                    <Input placeholder="请输入数据源编码" />
                  </Form.Item>
                </Col>
              </Row>
              <Form.Item name="sourceType" label="数据源类型" rules={[{ required: true }]}>
                <Select onChange={(v) => setSourceType(v)}>
                  <Option value="DATABASE">数据库</Option>
                  <Option value="REST_API">REST API</Option>
                </Select>
              </Form.Item>
              <Form.Item name="dbType" label="数据库类型" rules={[{ required: true, message: '请选择' }]}>
                <Select onChange={handleDbTypeChange}>
                  {dbTypeOptions.map(opt => (
                    <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                  ))}
                </Select>
              </Form.Item>

              {sourceType !== 'REST_API' && (
                <>
                  <Row gutter={16}>
                    <Col span={14}>
                      <Form.Item name="host" label="主机地址" rules={[{ required: true, message: '请输入' }]}>
                        <Input placeholder="localhost" />
                      </Form.Item>
                    </Col>
                    <Col span={10}>
                      <Form.Item name="port" label="端口" rules={[{ required: true, message: '请输入' }]}>
                        <InputNumber style={{ width: '100%' }} placeholder="3306" />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Form.Item name="database" label="数据库名" rules={[{ required: true, message: '请输入' }]}>
                    <Input placeholder="请输入数据库名" />
                  </Form.Item>
                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入' }]}>
                        <Input placeholder="root" />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item name="password" label="密码" rules={[{ required: !editingItem, message: '请输入' }]}>
                        <Input.Password placeholder="请输入密码" />
                      </Form.Item>
                    </Col>
                  </Row>
                </>
              )}

              {sourceType === 'REST_API' && (
                <>
                  <Form.Item name="restApiUrl" label="API地址" rules={[{ required: true, message: '请输入' }]}>
                    <Input placeholder="https://api.example.com/data" />
                  </Form.Item>
                  <Row gutter={16}>
                    <Col span={8}>
                      <Form.Item name="restApiMethod" label="请求方法">
                        <Select>
                          <Option value="GET">GET</Option>
                          <Option value="POST">POST</Option>
                          <Option value="PUT">PUT</Option>
                          <Option value="DELETE">DELETE</Option>
                        </Select>
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name="restApiAuthType" label="认证方式">
                        <Select>
                          {authTypeOptions.map(opt => (
                            <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                          ))}
                        </Select>
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name="connectTimeout" label="连接超时(ms)">
                        <InputNumber style={{ width: '100%' }} min={1000} step={1000} />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Form.Item name="restApiAuthToken" label="认证Token">
                    <Input.Password placeholder="Bearer Token / API Key" />
                  </Form.Item>
                  <Form.Item name="restApiHeaders" label="自定义请求头(JSON)">
                    <Input.TextArea rows={2} placeholder='{"X-Custom-Header": "value"}' />
                  </Form.Item>
                  <Form.Item name="restApiBody" label="请求体">
                    <Input.TextArea rows={3} placeholder='{"key": "value"}' />
                  </Form.Item>
                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item name="connectTimeout" label="连接超时(ms)">
                        <InputNumber style={{ width: '100%' }} min={1000} step={1000} />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item name="readTimeout" label="读取超时(ms)">
                        <InputNumber style={{ width: '100%' }} min={1000} step={1000} />
                      </Form.Item>
                    </Col>
                  </Row>
                </>
              )}
            </TabPane>

            {sourceType !== 'REST_API' && (
              <TabPane tab="连接池配置" key="pool">
                <Row gutter={16}>
                  <Col span={8}>
                    <Form.Item name="initialSize" label="初始连接数">
                      <InputNumber style={{ width: '100%' }} min={1} max={50} />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="minIdle" label="最小空闲连接">
                      <InputNumber style={{ width: '100%' }} min={1} max={50} />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="maxActive" label="最大活跃连接">
                      <InputNumber style={{ width: '100%' }} min={1} max={100} />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={16}>
                  <Col span={8}>
                    <Form.Item name="maxWait" label="最大等待(ms)">
                      <InputNumber style={{ width: '100%' }} min={1000} step={5000} />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="connectionTimeout" label="连接超时(ms)">
                      <InputNumber style={{ width: '100%' }} min={1000} step={5000} />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="maxLifetime" label="最大生命周期(ms)">
                      <InputNumber style={{ width: '100%' }} min={60000} step={60000} />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={16}>
                  <Col span={8}>
                    <Form.Item name="timeBetweenEvictionRunsMillis" label="检测间隔(ms)">
                      <InputNumber style={{ width: '100%' }} min={10000} step={10000} />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="minEvictableIdleTimeMillis" label="最小空闲时间(ms)">
                      <InputNumber style={{ width: '100%' }} min={60000} step={60000} />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="validationQuery" label="验证SQL">
                      <Input placeholder="SELECT 1" />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={16}>
                  <Col span={8}>
                    <Form.Item name="testWhileIdle" label="空闲时检测" valuePropName="checked">
                      <Switch />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="testOnBorrow" label="借用时检测" valuePropName="checked">
                      <Switch />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="testOnReturn" label="归还时检测" valuePropName="checked">
                      <Switch />
                    </Form.Item>
                  </Col>
                </Row>
              </TabPane>
            )}
          </Tabs>
        </Form>
      </Modal>

      <Modal
        title="连接池状态"
        open={poolModalVisible}
        onCancel={() => setPoolModalVisible(false)}
        footer={null}
        width={500}
      >
        {poolLoading ? (
          <div style={{ textAlign: 'center', padding: 20 }}>加载中...</div>
        ) : poolStatus ? (
          <div>
            <Row gutter={16}>
              <Col span={12}>
                <Card size="small" title="活跃连接">
                  <div style={{ fontSize: 24, fontWeight: 'bold' }}>{poolStatus.activeConnections}</div>
                  <Progress
                    percent={poolStatus.maxActive ? Math.round((poolStatus.activeConnections / poolStatus.maxActive) * 100) : 0}
                    size="small"
                    status={poolStatus.activeConnections === poolStatus.maxActive ? 'exception' : 'active'}
                  />
                </Card>
              </Col>
              <Col span={12}>
                <Card size="small" title="空闲连接">
                  <div style={{ fontSize: 24, fontWeight: 'bold', color: '#52c41a' }}>{poolStatus.idleConnections}</div>
                </Card>
              </Col>
            </Row>
            <Row gutter={16} style={{ marginTop: 16 }}>
              <Col span={12}>
                <Card size="small" title="总连接数">
                  <div style={{ fontSize: 24 }}>{poolStatus.totalConnections}</div>
                </Card>
              </Col>
              <Col span={12}>
                <Card size="small" title="等待线程">
                  <div style={{ fontSize: 24, color: poolStatus.threadsAwaitingConnection > 0 ? '#faad14' : '#52c41a' }}>
                    {poolStatus.threadsAwaitingConnection}
                  </div>
                </Card>
              </Col>
            </Row>
            <div style={{ marginTop: 16, color: '#999' }}>
              连接池名称: {poolStatus.poolName} | 状态: {poolStatus.closed ? '已关闭' : '运行中'}
            </div>
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: 20, color: '#999' }}>暂无连接池状态</div>
        )}
      </Modal>
    </div>
  )
}

export default DataSourceList
