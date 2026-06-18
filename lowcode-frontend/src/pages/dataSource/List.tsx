import React, { useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined } from '@ant-design/icons'

interface DataSource {
  id?: number
  sourceName: string
  sourceCode: string
  dbType: string
  host: string
  port: number
  database: string
  username: string
  password?: string
  status?: string
}

const mockData: DataSource[] = [
  { id: 1, sourceName: '主数据库', sourceCode: 'main_db', dbType: 'MySQL', host: 'localhost', port: 3306, database: 'lowcode_platform', username: 'root', status: 'CONNECTED' },
  { id: 2, sourceName: '业务数据库', sourceCode: 'biz_db', dbType: 'MySQL', host: 'localhost', port: 3306, database: 'business', username: 'root', status: 'CONNECTED' },
]

const DataSourceList: React.FC = () => {
  const [data, setData] = useState<DataSource[]>(mockData)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<DataSource | null>(null)
  const [form] = Form.useForm()

  const handleCreate = () => {
    setEditingItem(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: DataSource) => {
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
        const newItem: DataSource = { ...values, id: Date.now(), status: 'DISCONNECTED' }
        setData([...data, newItem])
        message.success('创建成功')
      }
      setModalVisible(false)
    } catch (e) {
      console.error(e)
    }
  }

  const handleTestConnection = (record: DataSource) => {
    message.success(`测试连接成功: ${record.sourceName}`)
    setData(data.map(item => item.id === record.id ? { ...item, status: 'CONNECTED' } : item))
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
          <Button type="link" size="small" icon={<PlayCircleOutlined />} onClick={() => handleTestConnection(record)}>
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
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建数据源
        </Button>
      </div>

      <Card>
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
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
          <Form.Item
            name="host"
            label="主机地址"
            rules={[{ required: true, message: '请输入主机地址' }]}
          >
            <Input placeholder="localhost" />
          </Form.Item>
          <Form.Item
            name="port"
            label="端口"
            rules={[{ required: true, message: '请输入端口' }]}
          >
            <Input type="number" placeholder="3306" />
          </Form.Item>
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
