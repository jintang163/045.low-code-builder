import React, { useState, useEffect } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, Card, Spin, Row, Col } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { dataSourceApi, DataSource } from '@/api/dataModel'
import { useAppStore } from '@/store/appStore'

const DataSourceList: React.FC = () => {
  const { currentApp } = useAppStore()
  const [data, setData] = useState<DataSource[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<DataSource | null>(null)
  const [testLoading, setTestLoading] = useState<string | null>(null)
  const [form] = Form.useForm()

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

  const handleCreate = () => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({
      dbType: 'MySQL',
      port: 3306,
      host: 'localhost',
    })
    setModalVisible(true)
  }

  const handleEdit = (record: DataSource) => {
    setEditingItem(record)
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

  const columns = [
    { title: '数据源名称', dataIndex: 'sourceName', key: 'sourceName' },
    { title: '数据源编码', dataIndex: 'sourceCode', key: 'sourceCode' },
    { title: '数据库类型', dataIndex: 'dbType', key: 'dbType' },
    { title: '主机', dataIndex: 'host', key: 'host' },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: '数据库', dataIndex: 'database', key: 'database' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'CONNECTED' ? 'green' : 'red'}>
          {status === 'CONNECTED' ? '已连接' : '未连接'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: DataSource) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<PlayCircleOutlined />}
            loading={testLoading === record.id + ''}
            onClick={() => handleTestConnection(record)}
          >
            测试
          </Button>
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
        <h2>数据源</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate} disabled={!currentApp}>
          新建数据源
        </Button>
      </div>

      {!currentApp && (
        <div style={{ marginBottom: 16, padding: 12, background: '#fffbe6', borderRadius: 4, border: '1px solid #ffe58f' }}>
          请先在【应用管理】中选择一个应用
        </div>
      )}

      <Card>
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
        />
      </Card>

      <Modal
        title={editingItem ? '编辑数据源' : '新建数据源'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="sourceName"
            label="数据源名称"
            rules={[{ required: true, message: '请输入数据源名称' }]}
          >
            <Input placeholder="请输入数据源名称" />
          </Form.Item>
          <Form.Item
            name="sourceCode"
            label="数据源编码"
            rules={[{ required: true, message: '请输入数据源编码' }]}
          >
            <Input placeholder="请输入数据源编码" />
          </Form.Item>
          <Form.Item
            name="dbType"
            label="数据库类型"
            rules={[{ required: true, message: '请选择数据库类型' }]}
          >
            <Select>
              <Select.Option value="MySQL">MySQL</Select.Option>
              <Select.Option value="PostgreSQL">PostgreSQL</Select.Option>
              <Select.Option value="DAMENG">达梦数据库</Select.Option>
            </Select>
          </Form.Item>
          <Row gutter={16}>
            <Col span={14}>
              <Form.Item
                name="host"
                label="主机地址"
                rules={[{ required: true, message: '请输入主机地址' }]}
              >
                <Input placeholder="localhost" />
              </Form.Item>
            </Col>
            <Col span={10}>
              <Form.Item
                name="port"
                label="端口"
                rules={[{ required: true, message: '请输入端口' }]}
              >
                <Input type="number" placeholder="3306" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="database"
            label="数据库名"
            rules={[{ required: true, message: '请输入数据库名' }]}
          >
            <Input placeholder="请输入数据库名" />
          </Form.Item>
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="root" />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
            rules={[{ required: !editingItem, message: '请输入密码' }]}
          >
            <Input.Password placeholder="请输入密码" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default DataSourceList
