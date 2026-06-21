import React, { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, InputNumber, Card, Tabs, Badge } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, BugOutlined, CodeOutlined, RobotOutlined, HistoryOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { RpaScript, RpaExecution, rpaApi } from '@/api/flow'
import { useAppStore } from '@/store/appStore'

const { TextArea } = Input
const { TabPane } = Tabs
const { Option } = Select

const RpaScriptList: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [scriptData, setScriptData] = useState<RpaScript[]>([])
  const [executionData, setExecutionData] = useState<RpaExecution[]>([])
  const [loading, setLoading] = useState(false)
  const [execLoading, setExecLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<RpaScript | null>(null)
  const [form] = Form.useForm()
  const [executorStatus, setExecutorStatus] = useState<boolean | null>(null)

  const loadScripts = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const res = await rpaApi.listScripts(currentApp.id)
      setScriptData(res.data || [])
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  const loadExecutions = async () => {
    setExecLoading(true)
    try {
      const res = await rpaApi.pageExecutions(1, 20)
      setExecutionData(res.data?.records || [])
    } catch (e) {
      console.error(e)
    } finally {
      setExecLoading(false)
    }
  }

  const checkExecutorHealth = async () => {
    try {
      const res = await rpaApi.checkExecutorHealth()
      setExecutorStatus(res.data?.status === 'UP')
    } catch (e) {
      setExecutorStatus(false)
    }
  }

  useEffect(() => {
    loadScripts()
    loadExecutions()
    checkExecutorHealth()
  }, [currentApp])

  const handleCreate = () => {
    setEditingItem(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: RpaScript) => {
    setEditingItem(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await rpaApi.deleteScript(id)
      message.success('删除成功')
      loadScripts()
    } catch (e) {
      console.error(e)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (!currentApp) return

      if (editingItem) {
        await rpaApi.updateScript(editingItem.id!, { ...editingItem, ...values, appId: currentApp.id })
        message.success('更新成功')
      } else {
        await rpaApi.createScript({ ...values, appId: currentApp.id })
        message.success('创建成功')
      }
      setModalVisible(false)
      loadScripts()
    } catch (e) {
      console.error(e)
    }
  }

  const handlePublish = async (id: number) => {
    try {
      await rpaApi.publishScript(id)
      message.success('发布成功')
      loadScripts()
    } catch (e) {
      console.error(e)
    }
  }

  const handleExecute = async (id: number) => {
    try {
      await rpaApi.executeScript(id, {}, 'MANUAL')
      message.success('已开始执行，请在执行记录中查看结果')
      loadExecutions()
    } catch (e) {
      message.error('执行失败')
      console.error(e)
    }
  }

  const handleValidate = async (id: number) => {
    try {
      await rpaApi.validateScript(id)
      message.success('脚本验证通过')
    } catch (e: any) {
      message.error(e?.response?.data?.message || '脚本验证失败')
    }
  }

  const scriptColumns = [
    { title: '脚本名称', dataIndex: 'scriptName', key: 'scriptName' },
    { title: '脚本编码', dataIndex: 'scriptCode', key: 'scriptCode' },
    { title: '目标URL', dataIndex: 'targetUrl', key: 'targetUrl', ellipsis: true },
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
    { title: '更新时间', dataIndex: 'updatedTime', key: 'updatedTime', width: 180 },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: RpaScript) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => navigate(`/rpa/designer/${record.id}`)}>
            设计
          </Button>
          <Button type="link" size="small" icon={<CheckCircleOutlined />} onClick={() => handleValidate(record.id!)}>
            验证
          </Button>
          <Button type="link" size="small" icon={<BugOutlined />} onClick={() => handleExecute(record.id!)}>
            运行
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

  const executionColumns = [
    { title: '执行编号', dataIndex: 'executionNo', key: 'executionNo', width: 200 },
    { title: '脚本ID', dataIndex: 'scriptId', key: 'scriptId', width: 80 },
    { title: '触发方式', dataIndex: 'triggerType', key: 'triggerType', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          SUCCESS: 'green',
          FAILED: 'red',
          RUNNING: 'blue',
          PENDING: 'orange',
        }
        const textMap: Record<string, string> = {
          SUCCESS: '成功',
          FAILED: '失败',
          RUNNING: '执行中',
          PENDING: '等待中',
        }
        return <Tag color={colorMap[status] || 'default'}>{textMap[status] || status}</Tag>
      },
    },
    { title: '耗时(ms)', dataIndex: 'duration', key: 'duration', width: 100 },
    { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 180 },
    { title: '错误信息', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <h2 style={{ margin: 0 }}>RPA自动化脚本</h2>
          <Badge
            status={executorStatus === null ? 'default' : executorStatus ? 'success' : 'error'}
            text={
              executorStatus === null
                ? '检测执行器状态...'
                : executorStatus
                ? '执行器运行正常'
                : '执行器不可用'
            }
          />
        </div>
        <Space>
          <Button icon={<HistoryOutlined />} onClick={loadExecutions}>
            刷新执行记录
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建脚本
          </Button>
        </Space>
      </div>

      <Tabs defaultActiveKey="scripts">
        <TabPane
          tab={
            <span>
              <RobotOutlined /> 脚本列表
            </span>
          }
          key="scripts"
        >
          <Card>
            <Table
              columns={scriptColumns}
              dataSource={scriptData}
              rowKey="id"
              loading={loading}
            />
          </Card>
        </TabPane>
        <TabPane
          tab={
            <span>
              <HistoryOutlined /> 执行记录
            </span>
          }
          key="executions"
        >
          <Card>
            <Table
              columns={executionColumns}
              dataSource={executionData}
              rowKey="id"
              loading={execLoading}
            />
          </Card>
        </TabPane>
      </Tabs>

      <Modal
        title={editingItem ? '编辑RPA脚本' : '新建RPA脚本'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="scriptName"
            label="脚本名称"
            rules={[{ required: true, message: '请输入脚本名称' }]}
          >
            <Input placeholder="请输入脚本名称" />
          </Form.Item>
          <Form.Item
            name="scriptCode"
            label="脚本编码"
            rules={[{ required: true, message: '请输入脚本编码' }]}
          >
            <Input placeholder="请输入脚本编码，如：login_robot" />
          </Form.Item>
          <Form.Item
            name="targetUrl"
            label="目标网站URL"
            rules={[{ required: true, message: '请输入目标网站URL' }]}
          >
            <Input placeholder="https://example.com" />
          </Form.Item>
          <Form.Item name="timeout" label="超时时间(秒)">
            <InputNumber style={{ width: '100%' }} min={10} max={3600} defaultValue={300} />
          </Form.Item>
          <Form.Item
            name="scriptType"
            label="脚本类型"
            rules={[{ required: true, message: '请选择脚本类型' }]}
          >
            <Select>
              <Option value="BROWSER">浏览器自动化</Option>
              <Option value="DATA_EXTRACT">数据抓取</Option>
              <Option value="FORM_FILL">表单填写</Option>
              <Option value="CUSTOM">自定义</Option>
            </Select>
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} placeholder="请输入脚本描述" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default RpaScriptList
