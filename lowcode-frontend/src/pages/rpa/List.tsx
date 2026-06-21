import React, { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, InputNumber, Card, Tabs, Badge, Switch, DatePicker, Descriptions } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, BugOutlined, CodeOutlined, RobotOutlined, HistoryOutlined, CheckCircleOutlined, ClockCircleOutlined, BellOutlined, BellFilled } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { RpaScript, RpaExecution, rpaApi } from '@/api/flow'
import { useAppStore } from '@/store/appStore'
import dayjs from 'dayjs'

const { TextArea } = Input
const { TabPane } = Tabs
const { Option } = Select
const { RangePicker } = DatePicker

const RpaScriptList: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [scriptData, setScriptData] = useState<RpaScript[]>([])
  const [executionData, setExecutionData] = useState<RpaExecution[]>([])
  const [loading, setLoading] = useState(false)
  const [execLoading, setExecLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [scheduleModalVisible, setScheduleModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<RpaScript | null>(null)
  const [scheduleItem, setScheduleItem] = useState<RpaScript | null>(null)
  const [form] = Form.useForm()
  const [scheduleForm] = Form.useForm()
  const [executorStatus, setExecutorStatus] = useState<boolean | null>(null)
  const [nextExecutionPreview, setNextExecutionPreview] = useState<any>(null)

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

  const handleOpenSchedule = (record: RpaScript) => {
    setScheduleItem(record)
    scheduleForm.resetFields()
    if (record.cronExpression) {
      scheduleForm.setFieldsValue({
        cronExpression: record.cronExpression,
        scheduleParams: record.scheduleParams,
        scheduleEnabled: record.scheduleEnabled === 1,
      })
    } else {
      scheduleForm.setFieldsValue({
        scheduleEnabled: false,
      })
    }
    setNextExecutionPreview(null)
    setScheduleModalVisible(true)
  }

  const handleCronChange = async (cron: string) => {
    if (cron && cron.length >= 5) {
      try {
        const res = await rpaApi.calculateNextExecution(cron)
        setNextExecutionPreview(res.data)
      } catch (e) {
        setNextExecutionPreview({ valid: false })
      }
    } else {
      setNextExecutionPreview(null)
    }
  }

  const handleEnableSchedule = async () => {
    if (!scheduleItem) return
    try {
      const values = await scheduleForm.validateFields()
      await rpaApi.enableSchedule(scheduleItem.id!, values.cronExpression, values.scheduleParams)
      message.success('定时调度已启用')
      setScheduleModalVisible(false)
      loadScripts()
    } catch (e: any) {
      message.error(e?.response?.data?.message || '启用定时调度失败')
    }
  }

  const handleDisableSchedule = async (id: number) => {
    try {
      await rpaApi.disableSchedule(id)
      message.success('定时调度已禁用')
      loadScripts()
    } catch (e) {
      message.error('禁用定时调度失败')
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
    {
      title: '定时调度',
      key: 'schedule',
      width: 120,
      render: (_: any, record: RpaScript) => (
        <Space>
          <Badge
            status={record.scheduleEnabled === 1 ? 'processing' : 'default'}
            text={record.scheduleEnabled === 1 ? '已启用' : '未启用'}
          />
          {record.cronExpression && (
            <Tag color="blue" style={{ fontSize: 11 }}>
              {record.cronExpression}
            </Tag>
          )}
        </Space>
      ),
    },
    {
      title: '执行统计',
      key: 'stats',
      width: 150,
      render: (_: any, record: RpaScript) => (
        <div style={{ fontSize: 12 }}>
          <div>总执行: {record.executeCount || 0}</div>
          <div style={{ color: '#52c41a' }}>成功: {record.successCount || 0}</div>
          <div style={{ color: '#ff4d4f' }}>失败: {record.failCount || 0}</div>
        </div>
      ),
    },
    {
      title: '下次执行',
      dataIndex: 'nextExecuteTime',
      key: 'nextExecuteTime',
      width: 180,
      render: (time: string) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    { title: '更新时间', dataIndex: 'updatedTime', key: 'updatedTime', width: 180 },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: RpaScript) => (
        <Space wrap>
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
          <Button
            type="link"
            size="small"
            icon={record.scheduleEnabled === 1 ? <BellFilled /> : <BellOutlined />}
            onClick={() => handleOpenSchedule(record)}
            style={{ color: record.scheduleEnabled === 1 ? '#1677ff' : undefined }}
          >
            定时
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

      <Modal
        title={
          <Space>
            <ClockCircleOutlined />
            定时调度配置 - {scheduleItem?.scriptName}
          </Space>
        }
        open={scheduleModalVisible}
        onOk={handleEnableSchedule}
        onCancel={() => setScheduleModalVisible(false)}
        width={600}
        okText="启用定时"
        cancelText="关闭"
      >
        <Form form={scheduleForm} layout="vertical">
          <Descriptions column={1} size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="脚本编码">{scheduleItem?.scriptCode}</Descriptions.Item>
            <Descriptions.Item label="当前状态">
              <Tag color={scheduleItem?.status === 'PUBLISHED' ? 'green' : 'orange'}>
                {scheduleItem?.status === 'PUBLISHED' ? '已发布（可配置定时）' : '草稿（请先发布）'}
              </Tag>
            </Descriptions.Item>
          </Descriptions>

          <Alert
            message="Cron表达式说明"
            type="info"
            showIcon
            description={
              <div>
                <p style={{ margin: 0, fontSize: 12 }}>格式：秒 分 时 日 月 周</p>
                <p style={{ margin: '4px 0 0 0', fontSize: 12 }}>
                  常用：0 0 2 * * ? (每天2点) | 0 */30 * * * ? (每30分钟) | 0 0 9 ? * MON-FRI (工作日9点)
                </p>
              </div>
            }
            style={{ marginBottom: 16 }}
          />

          <Form.Item
            name="cronExpression"
            label="Cron表达式"
            rules={[
              { required: true, message: '请输入Cron表达式' },
              { min: 5, message: 'Cron表达式至少5个字符' },
            ]}
          >
            <Select
              mode="tags"
              style={{ width: '100%' }}
              placeholder="输入或选择Cron表达式，如：0 0 2 * * ?"
              onChange={handleCronChange}
              tokenSeparators={[',']}
              options={[
                { value: '0 0 2 * * ?', label: '每天 02:00 执行' },
                { value: '0 0 9 * * ?', label: '每天 09:00 执行' },
                { value: '0 0 */1 * * ?', label: '每小时执行' },
                { value: '0 */30 * * * ?', label: '每30分钟执行' },
                { value: '0 */15 * * * ?', label: '每15分钟执行' },
                { value: '0 0 9 ? * MON-FRI', label: '工作日 09:00 执行' },
                { value: '0 0 12 * * 6,7', label: '周末 12:00 执行' },
                { value: '0 0 1 L * ?', label: '每月1号 01:00 执行' },
              ]}
            />
          </Form.Item>

          {nextExecutionPreview && (
            <Alert
              type={nextExecutionPreview.valid ? 'success' : 'error'}
              showIcon
              message={
                nextExecutionPreview.valid
                  ? `下次执行时间：${dayjs(nextExecutionPreview.nextExecutionTime).format('YYYY-MM-DD HH:mm:ss')}`
                  : 'Cron表达式无效，请检查格式'
              }
              style={{ marginBottom: 16 }}
            />
          )}

          <Form.Item
            name="scheduleParams"
            label="执行参数 (JSON)"
            help="脚本执行时传入的参数，如：{\"username\": \"test\"}"
          >
            <TextArea
              rows={4}
              placeholder='{"username": "test", "password": "123456"}'
            />
          </Form.Item>

          {scheduleItem?.scheduleEnabled === 1 && (
            <Form.Item>
              <Button
                danger
                block
                icon={<BellOutlined />}
                onClick={() => {
                  handleDisableSchedule(scheduleItem!.id!)
                  setScheduleModalVisible(false)
                }}
              >
                禁用当前定时任务
              </Button>
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  )
}

export default RpaScriptList
