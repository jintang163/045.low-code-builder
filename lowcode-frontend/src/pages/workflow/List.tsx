import React, { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, message, Popconfirm, Card } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, CloudUploadOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { WorkflowDefinition, workflowApi } from '@/api/flow'
import { useAppStore } from '@/store/appStore'

const WorkflowList: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [data, setData] = useState<WorkflowDefinition[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<WorkflowDefinition | null>(null)
  const [form] = Form.useForm()

  const loadData = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const res = await workflowApi.list(currentApp.id)
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

  const handleEdit = (record: WorkflowDefinition) => {
    setEditingItem(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await workflowApi.delete(id)
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
        await workflowApi.update({ ...editingItem, ...values })
        message.success('更新成功')
      } else {
        await workflowApi.save({ ...values, appId: currentApp.id })
        message.success('创建成功')
      }
      setModalVisible(false)
      loadData()
    } catch (e) {
      console.error(e)
    }
  }

  const handleDeploy = async (id: number) => {
    try {
      await workflowApi.deploy(id)
      message.success('部署成功')
      loadData()
    } catch (e) {
      console.error(e)
    }
  }

  const columns = [
    { title: '流程名称', dataIndex: 'processName', key: 'processName' },
    { title: '流程编码', dataIndex: 'processKey', key: 'processKey' },
    { title: '描述', dataIndex: 'processDesc', key: 'processDesc' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'orange'}>
          {status === 1 ? '已部署' : '草稿'}
        </Tag>
      ),
    },
    { title: '版本', dataIndex: 'version', key: 'version' },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: WorkflowDefinition) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => navigate(`/workflow/designer/${record.id}`)}>
            设计
          </Button>
          <Button type="link" size="small" icon={<CloudUploadOutlined />} onClick={() => handleDeploy(record.id!)}>
            部署
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
        <h2>工作流</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建流程
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
        title={editingItem ? '编辑流程' : '新建流程'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="processName"
            label="流程名称"
            rules={[{ required: true, message: '请输入流程名称' }]}
          >
            <Input placeholder="请输入流程名称" />
          </Form.Item>
          <Form.Item
            name="processKey"
            label="流程编码"
            rules={[{ required: true, message: '请输入流程编码' }]}
          >
            <Input placeholder="请输入流程编码" />
          </Form.Item>
          <Form.Item name="processDesc" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default WorkflowList
