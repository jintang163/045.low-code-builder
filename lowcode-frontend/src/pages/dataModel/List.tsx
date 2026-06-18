import React, { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, CodeOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { DataModel, dataModelApi } from '@/api/dataModel'
import { useAppStore } from '@/store/appStore'

const DataModelList: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [data, setData] = useState<DataModel[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<DataModel | null>(null)
  const [form] = Form.useForm()
  const [sqlModalVisible, setSqlModalVisible] = useState(false)
  const [sqlContent, setSqlContent] = useState('')

  const loadData = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const res = await dataModelApi.list(currentApp.id)
      setData(res.data || [])
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [currentApp])

  const handleCreate = () => {
    setEditingItem(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: DataModel) => {
    setEditingItem(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await dataModelApi.delete(id)
      message.success('删除成功')
      loadData()
    } catch (e) {
      console.error(e)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (!currentApp) return

      if (editingItem) {
        await dataModelApi.update({ ...editingItem, ...values })
        message.success('更新成功')
      } else {
        await dataModelApi.save({ ...values, appId: currentApp.id })
        message.success('创建成功')
      }
      setModalVisible(false)
      loadData()
    } catch (e) {
      console.error(e)
    }
  }

  const handlePublish = async (id: number) => {
    try {
      await dataModelApi.publish(id)
      message.success('发布成功')
      loadData()
    } catch (e) {
      console.error(e)
    }
  }

  const handleViewSql = async (id: number) => {
    try {
      const res = await dataModelApi.createSql(id)
      setSqlContent(res.data || '')
      setSqlModalVisible(true)
    } catch (e) {
      console.error(e)
    }
  }

  const columns = [
    { title: '模型名称', dataIndex: 'modelName', key: 'modelName' },
    { title: '模型编码', dataIndex: 'modelCode', key: 'modelCode' },
    { title: '表名', dataIndex: 'tableName', key: 'tableName' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'PUBLISHED' ? 'green' : 'orange'}>
          {status === 'PUBLISHED' ? '已发布' : '草稿'}
        </Tag>
      ),
    },
    { title: '版本', dataIndex: 'version', key: 'version' },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: DataModel) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => navigate(`/dataModel/designer/${record.id}`)}>
            设计
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button type="link" size="small" icon={<CodeOutlined />} onClick={() => handleViewSql(record.id!)}>
            SQL
          </Button>
          <Button type="link" size="small" icon={<PlayCircleOutlined />} onClick={() => handlePublish(record.id!)}>
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
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h2>数据模型</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建模型
        </Button>
      </div>

      <Card>
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
        />
      </Card>

      <Modal
        title={editingItem ? '编辑模型' : '新建模型'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="modelName"
            label="模型名称"
            rules={[{ required: true, message: '请输入模型名称' }]}
          >
            <Input placeholder="请输入模型名称" />
          </Form.Item>
          <Form.Item
            name="modelCode"
            label="模型编码"
            rules={[{ required: true, message: '请输入模型编码' }]}
          >
            <Input placeholder="请输入模型编码" />
          </Form.Item>
          <Form.Item
            name="tableName"
            label="表名"
            rules={[{ required: true, message: '请输入表名' }]}
          >
            <Input placeholder="请输入表名" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="建表SQL"
        open={sqlModalVisible}
        onCancel={() => setSqlModalVisible(false)}
        footer={null}
        width={800}
      >
        <pre style={{ background: '#f5f5f5', padding: 16, borderRadius: 4, overflow: 'auto' }}>
          {sqlContent}
        </pre>
      </Modal>
    </div>
  )
}

export default DataModelList
