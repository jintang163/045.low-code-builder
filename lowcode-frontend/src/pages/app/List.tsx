import React, { useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, Card, Row, Col } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, AppstoreOutlined, RocketOutlined, SettingOutlined } from '@ant-design/icons'
import { useAppStore } from '@/store/appStore'

interface AppInfo {
  id?: number
  appName: string
  appCode: string
  description?: string
  status?: string
  createdAt?: string
}

const mockData: AppInfo[] = [
  { id: 1, appName: '示例应用', appCode: 'demo_app', description: '示例应用，用于演示功能', status: 'RUNNING', createdAt: '2024-01-01 10:00:00' },
  { id: 2, appName: 'OA系统', appCode: 'oa_system', description: '企业OA办公系统', status: 'STOPPED', createdAt: '2024-01-05 14:30:00' },
  { id: 3, appName: 'CRM系统', appCode: 'crm_system', description: '客户关系管理系统', status: 'RUNNING', createdAt: '2024-01-10 09:15:00' },
]

const AppList: React.FC = () => {
  const { setCurrentApp, currentApp } = useAppStore()
  const [data, setData] = useState<AppInfo[]>(mockData)
  const [viewMode, setViewMode] = useState<'card' | 'table'>('card')
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<AppInfo | null>(null)
  const [form] = Form.useForm()

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

  const handleDelete = (id: number) => {
    setData(data.filter(item => item.id !== id))
    message.success('删除成功')
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingItem) {
        setData(data.map(item => item.id === editingItem.id ? { ...item, ...values } : item))
        message.success('更新成功')
      } else {
        const newItem: AppInfo = { ...values, id: Date.now(), status: 'STOPPED', createdAt: new Date().toISOString() }
        setData([...data, newItem])
        message.success('创建成功')
      }
      setModalVisible(false)
    } catch (e) {
      console.error(e)
    }
  }

  const handleSelectApp = (app: AppInfo) => {
    setCurrentApp({ id: app.id!, name: app.appName })
    message.success(`已切换到应用: ${app.appName}`)
  }

  const handlePublish = (app: AppInfo) => {
    setData(data.map(item => item.id === app.id ? { ...item, status: 'RUNNING' } : item))
    message.success(`应用 ${app.appName} 已发布`)
  }

  const columns = [
    { title: '应用名称', dataIndex: 'appName', key: 'appName' },
    { title: '应用编码', dataIndex: 'appCode', key: 'appCode' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'RUNNING' ? 'green' : 'orange'}>
          {status === 'RUNNING' ? '运行中' : '已停止'}
        </Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
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
          <Button type="link" size="small" icon={<RocketOutlined />} onClick={() => handlePublish(record)}>
            发布
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
          当前应用: <strong>{currentApp.name}</strong>
        </div>
      )}

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
                  <RocketOutlined key="publish" onClick={(e) => { e.stopPropagation(); handlePublish(app) }} />,
                  <DeleteOutlined key="delete" onClick={(e) => { e.stopPropagation(); handleDelete(app.id!) }} />,
                ]}
              >
                <Card.Meta
                  avatar={<AppstoreOutlined style={{ fontSize: 32, color: '#1677ff' }} />}
                  title={
                    <Space>
                      {app.appName}
                      <Tag color={app.status === 'RUNNING' ? 'green' : 'orange'}>
                        {app.status === 'RUNNING' ? '运行中' : '已停止'}
                      </Tag>
                    </Space>
                  }
                  description={app.description || '暂无描述'}
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
          />
        </Card>
      )}

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
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AppList
