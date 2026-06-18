import React, { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, Switch, message, Popconfirm } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, CodeOutlined, EyeOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { PageInfo, pageApi } from '@/api/page'
import { useAppStore } from '@/store/appStore'

const PageList: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [data, setData] = useState<PageInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<PageInfo | null>(null)
  const [form] = Form.useForm()
  const [codeModalVisible, setCodeModalVisible] = useState(false)
  const [codeContent, setCodeContent] = useState('')

  const loadData = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const res = await pageApi.list(currentApp.id)
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

  const handleEdit = (record: PageInfo) => {
    setEditingItem(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await pageApi.delete(id)
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
        await pageApi.update({ ...editingItem, ...values })
        message.success('更新成功')
      } else {
        await pageApi.save({ ...values, appId: currentApp.id })
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
      await pageApi.publish(id)
      message.success('发布成功')
      loadData()
    } catch (e) {
      console.error(e)
    }
  }

  const handleViewCode = async (id: number) => {
    try {
      const res = await pageApi.generateCode(id)
      setCodeContent(res.data || '')
      setCodeModalVisible(true)
    } catch (e) {
      console.error(e)
    }
  }

  const columns = [
    { title: '页面名称', dataIndex: 'pageName', key: 'pageName' },
    { title: '页面编码', dataIndex: 'pageCode', key: 'pageCode' },
    { title: '页面路径', dataIndex: 'pagePath', key: 'pagePath' },
    { title: '布局类型', dataIndex: 'layoutType', key: 'layoutType' },
    {
      title: '首页',
      dataIndex: 'isHome',
      key: 'isHome',
      render: (isHome: number) => (isHome === 1 ? <Tag color="blue">是</Tag> : <Tag>否</Tag>),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'orange'}>
          {status === 1 ? '已发布' : '草稿'}
        </Tag>
      ),
    },
    { title: '版本', dataIndex: 'version', key: 'version' },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: PageInfo) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => navigate(`/page/designer/${record.id}`)}>
            设计
          </Button>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewCode(record.id!)}>
            预览
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
        <h2>页面设计</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建页面
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
        title={editingItem ? '编辑页面' : '新建页面'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="pageName"
            label="页面名称"
            rules={[{ required: true, message: '请输入页面名称' }]}
          >
            <Input placeholder="请输入页面名称" />
          </Form.Item>
          <Form.Item
            name="pageCode"
            label="页面编码"
            rules={[{ required: true, message: '请输入页面编码' }]}
          >
            <Input placeholder="请输入页面编码" />
          </Form.Item>
          <Form.Item
            name="pagePath"
            label="页面路径"
            rules={[{ required: true, message: '请输入页面路径' }]}
          >
            <Input placeholder="如: /user/list" />
          </Form.Item>
          <Form.Item
            name="layoutType"
            label="布局类型"
            rules={[{ required: true, message: '请选择布局类型' }]}
          >
            <Select>
              <Select.Option value="FULL">全屏</Select.Option>
              <Select.Option value="SIDEBAR">侧边栏</Select.Option>
              <Select.Option value="TOP">顶部导航</Select.Option>
              <Select.Option value="FREE">自由布局</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="isHome" label="设为首页" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="页面代码"
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

export default PageList
