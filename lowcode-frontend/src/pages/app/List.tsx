import React, { useState, useEffect } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, Card, Row, Col, Spin } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, AppstoreOutlined, RocketOutlined, SettingOutlined, StopOutlined, DownloadOutlined } from '@ant-design/icons'
import { useAppStore } from '@/store/appStore'
import { appApi, AppInfo, AppGenerateConfig } from '@/api'

const AppList: React.FC = () => {
  const { setCurrentApp, currentApp } = useAppStore()
  const [data, setData] = useState<AppInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [viewMode, setViewMode] = useState<'card' | 'table'>('card')
  const [modalVisible, setModalVisible] = useState(false)
  const [generateModalVisible, setGenerateModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<AppInfo | null>(null)
  const [generatingApp, setGeneratingApp] = useState<AppInfo | null>(null)
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
    setCurrentApp({ id: app.id!, name: app.appName })
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
      packageName: 'com.lowcode.' + app.appCode,
      frontendFramework: 'react',
      backendFramework: 'springboot',
      dbType: 'MySQL',
      includeDocker: true,
      includeReadme: true,
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
      title: '操作',
      key: 'action',
      render: (_: any, record: AppInfo) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleSelectApp(record)}>
            切换
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
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
                    app.status === 1
                      ? <StopOutlined key="stop" onClick={(e) => { e.stopPropagation(); handleStop(app) }} />
                      : <RocketOutlined key="publish" onClick={(e) => { e.stopPropagation(); handlePublish(app) }} />,
                    <DownloadOutlined key="generate" onClick={(e) => { e.stopPropagation(); handleGenerate(app) }} />,
                    <DeleteOutlined key="delete" onClick={(e) => { e.stopPropagation(); handleDelete(app.id!) }} />,
                  ]}
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
        title="生成应用代码包"
        open={generateModalVisible}
        onOk={handleGenerateSubmit}
        onCancel={() => setGenerateModalVisible(false)}
        width={600}
      >
        <Form form={generateForm} layout="vertical">
          <Form.Item name="appId" hidden><Input /></Form.Item>
          <Form.Item name="appName" label="应用名称"><Input disabled /></Form.Item>
          <Form.Item name="appCode" label="应用编码"><Input disabled /></Form.Item>
          <Form.Item
            name="packageName"
            label="后端包名"
            rules={[{ required: true, message: '请输入包名' }]}
          >
            <Input placeholder="com.lowcode.xxx" />
          </Form.Item>
          <Form.Item
            name="frontendFramework"
            label="前端框架"
            rules={[{ required: true }]}
          >
            <Select>
              <Select.Option value="react">React + Ant Design</Select.Option>
              <Select.Option value="vue">Vue + Element UI</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="backendFramework"
            label="后端框架"
            rules={[{ required: true }]}
          >
            <Select>
              <Select.Option value="springboot">Spring Boot + MyBatis Plus</Select.Option>
            </Select>
          </Form.Item>
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
          <Form.Item name="includeDocker" label="包含Docker配置" valuePropName="checked">
            <Input type="checkbox" />
          </Form.Item>
          <Form.Item name="includeReadme" label="包含README文档" valuePropName="checked">
            <Input type="checkbox" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AppList
