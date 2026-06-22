import React, { useState, useEffect } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, Card, Row, Col, Spin, Radio, Descriptions, Alert, List, Divider } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, AppstoreOutlined, RocketOutlined, SettingOutlined, StopOutlined, DownloadOutlined, SyncOutlined, CloudDownloadOutlined, HistoryOutlined } from '@ant-design/icons'
import { useAppStore } from '@/store/appStore'
import { appApi, AppInfo, AppGenerateConfig } from '@/api'
import { templateApi, UpdateCheckResult, UpdateResult, TemplateVersion } from '@/api/template'

const AppList: React.FC = () => {
  const { setCurrentApp, currentApp } = useAppStore()
  const [data, setData] = useState<AppInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [viewMode, setViewMode] = useState<'card' | 'table'>('card')
  const [modalVisible, setModalVisible] = useState(false)
  const [generateModalVisible, setGenerateModalVisible] = useState(false)
  const [updateModalVisible, setUpdateModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<AppInfo | null>(null)
  const [generatingApp, setGeneratingApp] = useState<AppInfo | null>(null)
  const [updatingApp, setUpdatingApp] = useState<AppInfo | null>(null)
  const [updateCheckResult, setUpdateCheckResult] = useState<UpdateCheckResult | null>(null)
  const [updateMode, setUpdateMode] = useState<'incremental' | 'full'>('incremental')
  const [checkingUpdate, setCheckingUpdate] = useState(false)
  const [updating, setUpdating] = useState(false)
  const [form] = Form.useForm()
  const [generateForm] = Form.useForm()

  const fetchData = async () => {
    try {
      setLoading(true)
      const res: any = await appApi.list()
      if (res.code === 0 || res.code === 200) {
        setData(res.data || [])
      }
    } catch (e: any) {
      message.error('获取应用列表失败: ' + e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  const handleCreate = () => {
    setEditingItem(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: AppInfo) => {
    setEditingItem(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      const res: any = await appApi.delete(id)
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
      if (editingItem) {
        const res: any = await appApi.update({ ...editingItem, ...values })
        if (res.code === 0 || res.code === 200) {
          message.success('更新成功')
          fetchData()
          setModalVisible(false)
        }
      } else {
        const res: any = await appApi.create(values)
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

  const handleSelectApp = (app: AppInfo) => {
    setCurrentApp(app)
    message.success(`已切换到应用: ${app.appName}`)
  }

  const handlePublish = async (app: AppInfo) => {
    try {
      const res: any = await appApi.publish(app.id!)
      if (res.code === 0 || res.code === 200) {
        message.success(`应用 ${app.appName} 已发布`)
        fetchData()
      }
    } catch (e: any) {
      message.error('发布失败: ' + e.message)
    }
  }

  const handleStop = async (app: AppInfo) => {
    try {
      const res: any = await appApi.stop(app.id!)
      if (res.code === 0 || res.code === 200) {
        message.success(`应用 ${app.appName} 已停止`)
        fetchData()
      }
    } catch (e: any) {
      message.error('停止失败: ' + e.message)
    }
  }

  const handleGenerate = (app: AppInfo) => {
    setGeneratingApp(app)
    generateForm.setFieldsValue({
      appId: app.id,
      appName: app.appName,
      appCode: app.appCode,
      appDesc: app.appDesc,
      version: '1.0.0',
      author: 'lowcode',
      basePackage: 'com.lowcode.' + app.appCode,
      moduleName: app.appCode,
      packageName: 'com.lowcode.' + app.appCode,
      frontendFramework: 'react',
      backendFramework: 'springboot',
      dbType: 'MySQL',
      dbHost: 'localhost',
      dbPort: 3306,
      dbName: app.appCode,
      dbUsername: 'root',
      dbPassword: '123456',
      redisHost: 'localhost',
      redisPort: 6379,
      redisDatabase: 0,
      includeBackend: true,
      includeFrontend: true,
      generateDocker: true,
      generateK8s: true,
      generateSdk: true,
      generateReadme: true,
      sdkLanguage: 'java',
      namespace: 'default',
      replicas: 2,
      cpuRequest: '200m',
      memoryRequest: '512Mi',
      cpuLimit: '500m',
      memoryLimit: '1Gi',
    })
    setGenerateModalVisible(true)
  }

  const handleGenerateSubmit = async () => {
    try {
      const values = await generateForm.validateFields()
      const res: any = await appApi.generate(values)
      if (res.code === 0 || res.code === 200) {
        message.success('应用代码生成成功！')
        setGenerateModalVisible(false)
        window.open(`/api${appApi.download(values.appCode)}`, '_blank')
      }
    } catch (e: any) {
      if (e.message) message.error('生成失败: ' + e.message)
    }
  }

  const handleCheckUpdate = async (app: AppInfo) => {
    try {
      setCheckingUpdate(true)
      setUpdatingApp(app)
      const res: any = await templateApi.checkUpdate(app.id!)
      if (res.code === 0 || res.code === 200) {
        setUpdateCheckResult(res.data)
        setUpdateModalVisible(true)
      }
    } catch (e: any) {
      message.error('检查更新失败: ' + e.message)
    } finally {
      setCheckingUpdate(false)
    }
  }

  const handleUpdateApp = async () => {
    if (!updatingApp) return
    try {
      setUpdating(true)
      const res: any = await templateApi.updateApp(updatingApp.id!, updateMode)
      if (res.code === 0 || res.code === 200) {
        const data: UpdateResult = res.data
        message.success(data.message + `（新增${data.added || 0}个，跳过${data.skipped || 0}个）`)
        setUpdateModalVisible(false)
        fetchData()
      }
    } catch (e: any) {
      message.error('更新失败: ' + e.message)
    } finally {
      setUpdating(false)
    }
  }

  const columns = [
    { title: '应用名称', dataIndex: 'appName', key: 'appName' },
    { title: '应用编码', dataIndex: 'appCode', key: 'appCode' },
    { title: '描述', dataIndex: 'appDesc', key: 'appDesc' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'orange'}>
          {status === 1 ? '运行中' : '已停止'}
        </Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'createdTime', key: 'createdTime' },
    {
      title: '模板版本',
      dataIndex: 'templateVersion',
      key: 'templateVersion',
      render: (v: string) => v ? `v${v}` : '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: AppInfo) => (
        <Space wrap>
          <Button type="link" size="small" onClick={() => handleSelectApp(record)}>
            切换
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          {record.templateId && (
            <Button type="link" size="small" icon={<SyncOutlined />} onClick={() => handleCheckUpdate(record)}>
              检查更新
            </Button>
          )}
          {record.status === 1 ? (
            <Button type="link" size="small" icon={<StopOutlined />} onClick={() => handleStop(record)}>
              停止
            </Button>
          ) : (
            <Button type="link" size="small" icon={<RocketOutlined />} onClick={() => handlePublish(record)}>
              发布
            </Button>
          )}
          <Button type="link" size="small" icon={<DownloadOutlined />} onClick={() => handleGenerate(record)}>
            生成
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
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>应用管理</h2>
        <Space>
          <Button.Group>
            <Button type={viewMode === 'card' ? 'primary' : 'default'} onClick={() => setViewMode('card')}>卡片</Button>
            <Button type={viewMode === 'table' ? 'primary' : 'default'} onClick={() => setViewMode('table')}>列表</Button>
          </Button.Group>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建应用
          </Button>
        </Space>
      </div>

      {currentApp && (
        <div style={{ marginBottom: 16, padding: 12, background: '#e6f7ff', borderRadius: 4, border: '1px solid #91d5ff' }}>
          当前应用: <strong>{currentApp.appName}</strong>
        </div>
      )}

      <Spin spinning={loading}>
        {viewMode === 'card' ? (
          <Row gutter={[16, 16]}>
            {data.map(app => (
              <Col span={8} key={app.id}>
                <Card
                  hoverable
                  onClick={() => handleSelectApp(app)}
                  style={{ border: currentApp?.id === app.id ? '2px solid #1677ff' : undefined }}
                  actions={[
                    <SettingOutlined key="edit" onClick={(e) => { e.stopPropagation(); handleEdit(app) }} />,
                    app.templateId && <SyncOutlined key="update" onClick={(e) => { e.stopPropagation(); handleCheckUpdate(app) }} />,
                    app.status === 1
                      ? <StopOutlined key="stop" onClick={(e) => { e.stopPropagation(); handleStop(app) }} />
                      : <RocketOutlined key="publish" onClick={(e) => { e.stopPropagation(); handlePublish(app) }} />,
                    <DownloadOutlined key="generate" onClick={(e) => { e.stopPropagation(); handleGenerate(app) }} />,
                    <DeleteOutlined key="delete" onClick={(e) => { e.stopPropagation(); handleDelete(app.id!) }} />,
                  ].filter(Boolean)}
                >
                  <Card.Meta
                    avatar={<AppstoreOutlined style={{ fontSize: 32, color: '#1677ff' }} />}
                    title={
                      <Space>
                        {app.appName}
                        <Tag color={app.status === 1 ? 'green' : 'orange'}>
                          {app.status === 1 ? '运行中' : '已停止'}
                        </Tag>
                      </Space>
                    }
                    description={app.appDesc || '暂无描述'}
                  />
                </Card>
              </Col>
            ))}
          </Row>
        ) : (
          <Card>
            <Table
              columns={columns}
              dataSource={data}
              rowKey="id"
              loading={loading}
            />
          </Card>
        )}
      </Spin>

      <Modal
        title={editingItem ? '编辑应用' : '新建应用'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="appName"
            label="应用名称"
            rules={[{ required: true, message: '请输入应用名称' }]}
          >
            <Input placeholder="请输入应用名称" />
          </Form.Item>
          <Form.Item
            name="appCode"
            label="应用编码"
            rules={[{ required: true, message: '请输入应用编码' }]}
          >
            <Input placeholder="请输入应用编码" />
          </Form.Item>
          <Form.Item name="appDesc" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="导出应用代码包"
        open={generateModalVisible}
        onOk={handleGenerateSubmit}
        onCancel={() => setGenerateModalVisible(false)}
        width={720}
        okText="导出代码"
      >
        <Form form={generateForm} layout="vertical">
          <Form.Item name="appId" hidden><Input /></Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="appName" label="应用名称"><Input disabled /></Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="appCode" label="应用编码"><Input disabled /></Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="version"
                label="版本号"
                rules={[{ required: true, message: '请输入版本号' }]}
              >
                <Input placeholder="1.0.0" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="author" label="作者">
                <Input placeholder="lowcode" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="basePackage"
                label="后端包名"
                rules={[{ required: true, message: '请输入包名' }]}
              >
                <Input placeholder="com.lowcode.xxx" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="moduleName" label="模块名">
                <Input placeholder="xxx" />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">代码生成选项</Divider>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="includeBackend" label="后端代码" valuePropName="checked">
                <Input type="checkbox" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="includeFrontend" label="前端代码" valuePropName="checked">
                <Input type="checkbox" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="generateReadme" label="README文档" valuePropName="checked">
                <Input type="checkbox" />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">数据库配置</Divider>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                name="dbType"
                label="数据库类型"
                rules={[{ required: true }]}
              >
                <Select>
                  <Select.Option value="MySQL">MySQL</Select.Option>
                  <Select.Option value="PostgreSQL">PostgreSQL</Select.Option>
                  <Select.Option value="DAMENG">达梦数据库</Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="dbHost" label="数据库主机">
                <Input placeholder="localhost" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="dbPort" label="端口">
                <Input type="number" placeholder="3306" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="dbName" label="数据库名">
                <Input placeholder="数据库名" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="dbUsername" label="用户名">
                <Input placeholder="root" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="dbPassword" label="密码">
                <Input.Password placeholder="密码" />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">Redis配置</Divider>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="redisHost" label="Redis主机">
                <Input placeholder="localhost" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="redisPort" label="端口">
                <Input type="number" placeholder="6379" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="redisDatabase" label="数据库">
                <Input type="number" placeholder="0" />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">部署配置</Divider>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="generateDocker" label="Docker配置" valuePropName="checked">
                <Input type="checkbox" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="generateK8s" label="K8s部署文件" valuePropName="checked">
                <Input type="checkbox" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="generateSdk" label="生成SDK" valuePropName="checked">
                <Input type="checkbox" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item noStyle shouldUpdate={(prevValues, curValues) => prevValues.generateK8s !== curValues.generateK8s}>
            {({ getFieldValue }) => getFieldValue('generateK8s') ? (
              <div style={{ padding: '12px', background: '#f5f5f5', borderRadius: 4, marginBottom: 16 }}>
                <h4 style={{ margin: '0 0 12px 0' }}>K8s 配置</h4>
                <Row gutter={16}>
                  <Col span={8}>
                    <Form.Item name="namespace" label="命名空间">
                      <Input placeholder="default" />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="replicas" label="副本数">
                      <Input type="number" placeholder="2" />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="cpuRequest" label="CPU请求">
                      <Input placeholder="200m" />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={16}>
                  <Col span={8}>
                    <Form.Item name="memoryRequest" label="内存请求">
                      <Input placeholder="512Mi" />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="cpuLimit" label="CPU限制">
                      <Input placeholder="500m" />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="memoryLimit" label="内存限制">
                      <Input placeholder="1Gi" />
                    </Form.Item>
                  </Col>
                </Row>
              </div>
            ) : null}
          </Form.Item>

          <Form.Item noStyle shouldUpdate={(prevValues, curValues) => prevValues.generateSdk !== curValues.generateSdk}>
            {({ getFieldValue }) => getFieldValue('generateSdk') ? (
              <div style={{ padding: '12px', background: '#f5f5f5', borderRadius: 4, marginBottom: 16 }}>
                <h4 style={{ margin: '0 0 12px 0' }}>SDK 配置</h4>
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item name="sdkLanguage" label="SDK语言">
                      <Select>
                        <Select.Option value="java">Java</Select.Option>
                        <Select.Option value="javascript">JavaScript</Select.Option>
                        <Select.Option value="typescript">TypeScript</Select.Option>
                        <Select.Option value="python">Python</Select.Option>
                      </Select>
                    </Form.Item>
                  </Col>
                </Row>
              </div>
            ) : null}
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={<Space><CloudDownloadOutlined /> 模板更新</Space>}
        open={updateModalVisible}
        onOk={handleUpdateApp}
        onCancel={() => setUpdateModalVisible(false)}
        confirmLoading={updating}
        okText="更新应用"
        okButtonProps={{ disabled: !updateCheckResult?.hasUpdate }}
        width={600}
      >
        {updatingApp && (
          <div>
            <Alert
              type={updateCheckResult?.hasUpdate ? 'info' : 'success'}
              showIcon
              message={updateCheckResult?.message || '检查完成'}
              style={{ marginBottom: 16 }}
            />

            <Descriptions size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="应用名称">{updatingApp.appName}</Descriptions.Item>
              <Descriptions.Item label="应用编码">{updatingApp.appCode}</Descriptions.Item>
              <Descriptions.Item label="当前版本">
                <Tag color="blue">{updateCheckResult?.currentVersion || '-'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="最新版本">
                <Tag color="green">{updateCheckResult?.latestVersion || '-'}</Tag>
              </Descriptions.Item>
              {updateCheckResult?.templateName && (
                <Descriptions.Item label="来源模板" span={2}>
                  {updateCheckResult.templateName}
                </Descriptions.Item>
              )}
            </Descriptions>

            {updateCheckResult?.changeLogs && updateCheckResult.changeLogs.length > 0 && (
              <div style={{ marginBottom: 16 }}>
                <h4 style={{ marginBottom: 8 }}><HistoryOutlined /> 更新日志</h4>
                <List
                  size="small"
                  bordered
                  dataSource={updateCheckResult.changeLogs}
                  renderItem={(item: string) => <List.Item>{item}</List.Item>}
                />
              </div>
            )}

            {updateCheckResult?.diff && (
              <div style={{ marginBottom: 16 }}>
                <h4 style={{ marginBottom: 8 }}>更新内容预览</h4>
                <Row gutter={[8, 8]}>
                  {updateCheckResult.diff.dataSources && (
                    <Col span={12}>
                      <Tag color="blue">数据源: +{updateCheckResult.diff.dataSources.added}</Tag>
                    </Col>
                  )}
                  {updateCheckResult.diff.dataModels && (
                    <Col span={12}>
                      <Tag color="green">数据模型: +{updateCheckResult.diff.dataModels.added}</Tag>
                    </Col>
                  )}
                  {updateCheckResult.diff.pages && (
                    <Col span={12}>
                      <Tag color="orange">页面: +{updateCheckResult.diff.pages.added}</Tag>
                    </Col>
                  )}
                  {updateCheckResult.diff.businessLogics && (
                    <Col span={12}>
                      <Tag color="purple">业务逻辑: +{updateCheckResult.diff.businessLogics.added}</Tag>
                    </Col>
                  )}
                  {updateCheckResult.diff.workflows && (
                    <Col span={12}>
                      <Tag color="red">工作流: +{updateCheckResult.diff.workflows.added}</Tag>
                    </Col>
                  )}
                </Row>
              </div>
            )}

            {updateCheckResult?.hasUpdate && (
              <div>
                <h4 style={{ marginBottom: 8 }}>更新模式</h4>
                <Radio.Group
                  value={updateMode}
                  onChange={(e) => setUpdateMode(e.target.value)}
                  style={{ width: '100%' }}
                >
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <Radio value="incremental" style={{ display: 'flex', alignItems: 'flex-start' }}>
                      <div>
                        <div><strong>增量更新（推荐）</strong></div>
                        <div style={{ color: '#666', fontSize: 12 }}>
                          仅新增模板中新增的资源，跳过已存在的资源，保护您的自定义修改不被覆盖
                        </div>
                      </div>
                    </Radio>
                    <Radio value="full" style={{ display: 'flex', alignItems: 'flex-start' }}>
                      <div>
                        <div><strong>全量覆盖</strong></div>
                        <div style={{ color: '#666', fontSize: 12 }}>
                          删除所有现有资源并重新创建，将丢失您的所有自定义修改
                        </div>
                      </div>
                    </Radio>
                  </Space>
                </Radio.Group>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  )
}

export default AppList
