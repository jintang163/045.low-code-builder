import React, { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, CodeOutlined, BugOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { BusinessLogic, logicApi } from '@/api/flow'
import { useAppStore } from '@/store/appStore'

const LogicList: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [data, setData] = useState<BusinessLogic[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<BusinessLogic | null>(null)
  const [form] = Form.useForm()
  const [codeModalVisible, setCodeModalVisible] = useState(false)
  const [codeContent, setCodeContent] = useState('')

  const loadData = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const res = await logicApi.list(currentApp.id)
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

  const handleEdit = (record: BusinessLogic) => {
    setEditingItem(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await logicApi.delete(id)
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
        await logicApi.update({ ...editingItem, ...values })
        message.success('更新成功')
      } else {
        await logicApi.save({ ...values, appId: currentApp.id })
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
      await logicApi.publish(id)
      message.success('发布成功')
      loadData()
    } catch (e) {
      console.error(e)
    }
  }

  const handleViewCode = async (id: number) => {
    try {
      const res = await logicApi.generateCode(id)
      setCodeContent(res.data || '')
      setCodeModalVisible(true)
    } catch (e) {
      console.error(e)
    }
  }

  const columns = [
    { title: '逻辑名称', dataIndex: 'logicName', key: 'logicName' },
    { title: '逻辑编码', dataIndex: 'logicCode', key: 'logicCode' },
    { title: '触发类型', dataIndex: 'triggerType', key: 'triggerType' },
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
      render: (_: any, record: BusinessLogic) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => navigate(`/logic/designer/${record.id}`)}>
            设计
          </Button>
          <Button type="link" size="small" icon={<BugOutlined />} onClick={() => navigate(`/logic/designer/${record.id}`)}>
            调试
          </Button>
          <Button type="link" size="small" icon={<CodeOutlined />} onClick={() => handleViewCode(record.id!)}>
            代码
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
        <h2>业务逻辑</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建逻辑
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
        title={editingItem ? '编辑逻辑' : '新建逻辑'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="logicName"
            label="逻辑名称"
            rules={[{ required: true, message: '请输入逻辑名称' }]}
          >
            <Input placeholder="请输入逻辑名称" />
          </Form.Item>
          <Form.Item
            name="logicCode"
            label="逻辑编码"
            rules={[{ required: true, message: '请输入逻辑编码' }]}
          >
            <Input placeholder="请输入逻辑编码" />
          </Form.Item>
          <Form.Item
            name="triggerType"
            label="触发类型"
            rules={[{ required: true, message: '请选择触发类型' }]}
          >
            <Select>
              <Select.Option value="API">API触发</Select.Option>
              <Select.Option value="TIMER">定时触发</Select.Option>
              <Select.Option value="EVENT">事件触发</Select.Option>
              <Select.Option value="MANUAL">手动触发</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="生成代码"
        open={codeModalVisible}
        onCancel={() => setCodeModalVisible(false)}
        footer={null}
        width={800}
      >
        <pre style={{ background: '#f5f5f5', padding: 16, borderRadius: 4, overflow: 'auto' }}>
          {codeContent}
        </pre>
      </Modal>
    </div>
  )
}

export default LogicList
