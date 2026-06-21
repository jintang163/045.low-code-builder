import React, { useEffect, useState } from 'react'
import {
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  InputNumber,
  Switch,
  DatePicker,
  List,
  Tooltip,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  CopyOutlined,
  MailOutlined,
  ClockCircleOutlined,
  FileExcelOutlined,
  BarChartOutlined,
  TableOutlined,
  AppstoreOutlined,
  PlayCircleOutlined,
  StopOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { ReportInfo, reportApi, ScheduleConfig } from '@/api/report'
import { useAppStore } from '@/store/appStore'
import dayjs from 'dayjs'

const { Option } = Select
const { RangePicker } = DatePicker

const mockReports: ReportInfo[] = [
  {
    id: 1,
    appId: 1,
    reportName: '销售数据报表',
    reportCode: 'sales_report',
    reportType: 'comprehensive',
    description: '销售数据综合报表，包含销售额、订单量、客单价等指标',
    status: 1,
    version: '1.0.0',
    createdTime: '2024-01-15 10:30:00',
    updatedTime: '2024-01-20 14:20:00',
    components: [],
  },
  {
    id: 2,
    appId: 1,
    reportName: '用户行为分析报表',
    reportCode: 'user_behavior_report',
    reportType: 'chart',
    description: '用户活跃度、留存率、转化率等数据分析',
    status: 1,
    version: '1.2.0',
    createdTime: '2024-01-10 09:00:00',
    updatedTime: '2024-01-18 16:45:00',
    components: [],
  },
  {
    id: 3,
    appId: 1,
    reportName: '库存明细表',
    reportCode: 'inventory_detail',
    reportType: 'table',
    description: '库存明细交叉表，按仓库、品类分组汇总',
    status: 0,
    version: '0.9.0',
    createdTime: '2024-01-05 11:20:00',
    updatedTime: '2024-01-12 10:15:00',
    components: [],
  },
  {
    id: 4,
    appId: 1,
    reportName: '财务月报',
    reportCode: 'finance_monthly',
    reportType: 'comprehensive',
    description: '月度财务报表，包含收入、支出、利润等',
    status: 1,
    version: '2.0.0',
    createdTime: '2023-12-01 08:00:00',
    updatedTime: '2024-01-02 09:30:00',
    components: [],
  },
]

const ReportList: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [data, setData] = useState<ReportInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<ReportInfo | null>(null)
  const [form] = Form.useForm()
  const [scheduleModalVisible, setScheduleModalVisible] = useState(false)
  const [currentReport, setCurrentReport] = useState<ReportInfo | null>(null)
  const [emailModalVisible, setEmailModalVisible] = useState(false)
  const [emailForm] = Form.useForm()
  const [scheduleList, setScheduleList] = useState<ScheduleConfig[]>([])
  const [scheduleLoading, setScheduleLoading] = useState(false)
  const [scheduleFormVisible, setScheduleFormVisible] = useState(false)
  const [scheduleForm] = Form.useForm()
  const [editingSchedule, setEditingSchedule] = useState<ScheduleConfig | null>(null)
  const [emailLoading, setEmailLoading] = useState(false)
  const [deleteLoading, setDeleteLoading] = useState<number | null>(null)
  const [copyLoading, setCopyLoading] = useState<number | null>(null)

  const loadData = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const res: any = await reportApi.list(currentApp.id!)
      if (res.code === 0 || res.code === 200) {
        setData(res.data || [])
      } else {
        setData(mockReports)
      }
    } catch (e) {
      console.error(e)
      setData(mockReports)
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
    form.setFieldsValue({
      reportType: 'comprehensive',
    })
    setModalVisible(true)
  }

  const handleEdit = (record: ReportInfo) => {
    setEditingItem(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    setDeleteLoading(id)
    try {
      const res: any = await reportApi.delete(id)
      if (res.code === 0 || res.code === 200) {
        message.success('删除成功')
        setData(prev => prev.filter(item => item.id !== id))
      } else {
        message.error(res.message || '删除失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('删除失败: ' + (e.message || ''))
    } finally {
      setDeleteLoading(null)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (!currentApp) return

      if (editingItem) {
        const res: any = await reportApi.update({
          ...editingItem,
          ...values,
        })
        if (res.code === 0 || res.code === 200) {
          setData(prev => prev.map(item => 
            item.id === editingItem.id ? { ...item, ...values } : item
          ))
          message.success('更新成功')
          setModalVisible(false)
        } else {
          message.error(res.message || '更新失败')
        }
      } else {
        const newReport: ReportInfo = {
          ...values,
          appId: currentApp.id!,
          status: 0,
          version: '1.0.0',
          components: [],
        }
        const res: any = await reportApi.save(newReport)
        if (res.code === 0 || res.code === 200) {
          setData(prev => [res.data, ...prev])
          message.success('创建成功')
          setModalVisible(false)
        } else {
          message.error(res.message || '创建失败')
        }
      }
    } catch (e: any) {
      console.error(e)
      message.error('操作失败: ' + (e.message || ''))
    }
  }

  const handleDesign = (id: number) => {
    navigate(`/report/designer/${id}`)
  }

  const handlePreview = (id: number) => {
    navigate(`/report/preview/${id}`)
  }

  const handleCopy = async (record: ReportInfo) => {
    setCopyLoading(record.id!)
    try {
      const newName = record.reportName + ' - 副本'
      const newCode = record.reportCode + '_copy'
      const res: any = await reportApi.copy(record.id!, newName, newCode)
      if (res.code === 0 || res.code === 200) {
        setData(prev => [res.data, ...prev])
        message.success('复制成功')
      } else {
        message.error(res.message || '复制失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('复制失败: ' + (e.message || ''))
    } finally {
      setCopyLoading(null)
    }
  }

  const loadSchedules = async (reportId: number) => {
    setScheduleLoading(true)
    try {
      const res: any = await reportApi.getSchedules(reportId)
      if (res.code === 0 || res.code === 200) {
        setScheduleList(res.data || [])
      }
    } catch (e) {
      console.error(e)
    } finally {
      setScheduleLoading(false)
    }
  }

  const handleSchedule = (record: ReportInfo) => {
    setCurrentReport(record)
    setScheduleModalVisible(true)
    loadSchedules(record.id!)
  }

  const handleAddSchedule = () => {
    setEditingSchedule(null)
    scheduleForm.resetFields()
    scheduleForm.setFieldsValue({
      enabled: true,
      sendType: 'daily',
      sendTime: '09:00',
      exportFormat: 'excel',
      subject: currentReport?.reportName + ' - 定时报表',
      recipients: [],
    })
    setScheduleFormVisible(true)
  }

  const handleEditSchedule = (schedule: ScheduleConfig) => {
    setEditingSchedule(schedule)
    scheduleForm.setFieldsValue(schedule)
    setScheduleFormVisible(true)
  }

  const handleScheduleSubmit = async () => {
    try {
      const values = await scheduleForm.validateFields()
      if (!currentReport) return

      const scheduleData: ScheduleConfig = {
        ...values,
        emailConfig: {
          recipients: values.recipients || [],
          subject: values.subject || '',
          content: values.content || '',
          attachReport: values.attachReport !== false,
        },
      }

      const res: any = await reportApi.saveSchedule(currentReport.id!, scheduleData)
      if (res.code === 0 || res.code === 200) {
        message.success('保存成功')
        setScheduleFormVisible(false)
        loadSchedules(currentReport.id!)
      } else {
        message.error(res.message || '保存失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('保存失败: ' + (e.message || ''))
    }
  }

  const handleDeleteSchedule = async (scheduleId: number) => {
    if (!currentReport) return
    try {
      const res: any = await reportApi.deleteSchedule(currentReport.id!, scheduleId)
      if (res.code === 0 || res.code === 200) {
        message.success('删除成功')
        loadSchedules(currentReport.id!)
      } else {
        message.error(res.message || '删除失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('删除失败: ' + (e.message || ''))
    }
  }

  const handleRunSchedule = async (scheduleId: number) => {
    if (!currentReport) return
    try {
      const res: any = await reportApi.runSchedule(currentReport.id!, scheduleId)
      if (res.code === 0 || res.code === 200) {
        message.success('执行成功')
      } else {
        message.error(res.message || '执行失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('执行失败: ' + (e.message || ''))
    }
  }

  const handleSendEmail = (record: ReportInfo) => {
    setCurrentReport(record)
    emailForm.resetFields()
    emailForm.setFieldsValue({
      subject: record.reportName,
      exportFormat: 'excel',
      attachReport: true,
      recipients: [],
    })
    setEmailModalVisible(true)
  }

  const handleSendEmailSubmit = async () => {
    if (!currentReport) return
    setEmailLoading(true)
    try {
      const values = await emailForm.validateFields()
      const emailConfig = {
        recipients: values.recipients || [],
        subject: values.subject || '',
        content: values.content || '',
        attachReport: values.attachReport !== false,
      }
      const res: any = await reportApi.sendEmail(currentReport.id!, emailConfig)
      if (res.code === 0 || res.code === 200) {
        message.success('邮件发送成功')
        setEmailModalVisible(false)
      } else {
        message.error(res.message || '发送失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('邮件发送失败: ' + (e.message || ''))
    } finally {
      setEmailLoading(false)
    }
  }

  const getReportTypeIcon = (type: string) => {
    switch (type) {
      case 'table':
        return <TableOutlined />
      case 'chart':
        return <BarChartOutlined />
      case 'comprehensive':
        return <AppstoreOutlined />
      default:
        return <BarChartOutlined />
    }
  }

  const getReportTypeName = (type: string) => {
    switch (type) {
      case 'table':
        return '表格报表'
      case 'chart':
        return '图表报表'
      case 'comprehensive':
        return '综合报表'
      default:
        return '报表'
    }
  }

  const getReportTypeColor = (type: string) => {
    switch (type) {
      case 'table':
        return 'blue'
      case 'chart':
        return 'green'
      case 'comprehensive':
        return 'purple'
      default:
        return 'default'
    }
  }

  const getSendTypeText = (type: string) => {
    switch (type) {
      case 'daily': return '每天'
      case 'weekly': return '每周'
      case 'monthly': return '每月'
      case 'custom': return '自定义'
      default: return type
    }
  }

  const columns = [
    { title: '报表名称', dataIndex: 'reportName', key: 'reportName', width: 200 },
    { title: '报表编码', dataIndex: 'reportCode', key: 'reportCode', width: 180 },
    {
      title: '报表类型',
      dataIndex: 'reportType',
      key: 'reportType',
      width: 120,
      render: (type: string) => (
        <Tag color={getReportTypeColor(type)} icon={getReportTypeIcon(type)}>
          {getReportTypeName(type)}
        </Tag>
      ),
    },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: '版本', dataIndex: 'version', key: 'version', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'orange'}>
          {status === 1 ? '已发布' : '草稿'}
        </Tag>
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedTime',
      key: 'updatedTime',
      width: 180,
      render: (time: string) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_: any, record: ReportInfo) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleDesign(record.id!)}>
            设计
          </Button>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handlePreview(record.id!)}>
            预览
          </Button>
          <Button type="link" size="small" icon={<ClockCircleOutlined />} onClick={() => handleSchedule(record)}>
            定时
          </Button>
          <Button type="link" size="small" icon={<MailOutlined />} onClick={() => handleSendEmail(record)}>
            邮件
          </Button>
          <Button 
            type="link" 
            size="small" 
            icon={<CopyOutlined />} 
            loading={copyLoading === record.id}
            onClick={() => handleCopy(record)}
          >
            复制
          </Button>
          <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id!)}>
            <Button 
              type="link" 
              size="small" 
              danger 
              icon={<DeleteOutlined />}
              loading={deleteLoading === record.id}
            >
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
        <h2 style={{ margin: 0 }}>报表管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建报表
        </Button>
      </div>

      <Card>
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1200 }}
        />
      </Card>

      <Modal
        title={editingItem ? '编辑报表' : '新建报表'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="reportName"
                label="报表名称"
                rules={[{ required: true, message: '请输入报表名称' }]}
              >
                <Input placeholder="请输入报表名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="reportCode"
                label="报表编码"
                rules={[{ required: true, message: '请输入报表编码' }]}
              >
                <Input placeholder="请输入报表编码" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="reportType"
            label="报表类型"
            rules={[{ required: true, message: '请选择报表类型' }]}
          >
            <Select>
              <Option value="table">表格报表</Option>
              <Option value="chart">图表报表</Option>
              <Option value="comprehensive">综合报表</Option>
            </Select>
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="定时发送设置"
        open={scheduleModalVisible}
        onCancel={() => setScheduleModalVisible(false)}
        footer={null}
        width={700}
      >
        <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontWeight: 500 }}>定时任务列表</span>
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={handleAddSchedule}>
            新增任务
          </Button>
        </div>
        <List
          loading={scheduleLoading}
          dataSource={scheduleList}
          locale={{ emptyText: '暂无定时任务' }}
          renderItem={(item: any, idx) => (
            <List.Item
              key={idx}
              style={{ padding: '12px 0', borderBottom: '1px solid #f0f0f0' }}
              actions={[
                <Tooltip title="立即执行">
                  <Button type="link" size="small" icon={<PlayCircleOutlined />} onClick={() => handleRunSchedule(item.id)} />
                </Tooltip>,
                <Tooltip title="编辑">
                  <Button type="link" size="small" icon={<SettingOutlined />} onClick={() => handleEditSchedule(item)} />
                </Tooltip>,
                <Popconfirm title="确定删除?" onConfirm={() => handleDeleteSchedule(item.id)}>
                  <Button type="link" size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>,
              ]}
            >
              <Space>
                <Tag color={item.enabled ? 'green' : 'default'}>
                  {item.enabled ? '已启用' : '已禁用'}
                </Tag>
                <span style={{ fontWeight: 500 }}>{item.subject || '定时报表'}</span>
                <Tag color="blue">{getSendTypeText(item.sendType)}</Tag>
                <span style={{ color: '#999', fontSize: 12 }}>
                  {item.sendTime || '-'}
                </span>
                <span style={{ color: '#999', fontSize: 12 }}>
                  格式: {item.exportFormat || 'excel'}
                </span>
              </Space>
            </List.Item>
          )}
        />
      </Modal>

      <Modal
        title={editingSchedule ? '编辑定时任务' : '新增定时任务'}
        open={scheduleFormVisible}
        onOk={handleScheduleSubmit}
        onCancel={() => setScheduleFormVisible(false)}
        width={600}
      >
        <Form form={scheduleForm} layout="vertical">
          <Form.Item name="enabled" label="启用定时发送" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="sendType" label="发送频率" rules={[{ required: true, message: '请选择发送频率' }]}>
            <Select>
              <Option value="daily">每天</Option>
              <Option value="weekly">每周</Option>
              <Option value="monthly">每月</Option>
              <Option value="custom">自定义</Option>
            </Select>
          </Form.Item>
          <Form.Item name="sendTime" label="发送时间" rules={[{ required: true, message: '请输入发送时间' }]}>
            <Input type="time" />
          </Form.Item>
          <Form.Item name="subject" label="邮件主题" rules={[{ required: true, message: '请输入邮件主题' }]}>
            <Input placeholder="请输入邮件主题" />
          </Form.Item>
          <Form.Item name="recipients" label="收件人邮箱" rules={[{ required: true, message: '请输入收件人邮箱' }]}>
            <Select mode="tags" placeholder="输入邮箱地址后按回车" />
          </Form.Item>
          <Form.Item name="content" label="邮件内容">
            <Input.TextArea rows={3} placeholder="请输入邮件内容" />
          </Form.Item>
          <Form.Item name="exportFormat" label="导出格式">
            <Select>
              <Option value="excel">Excel</Option>
              <Option value="pdf">PDF</Option>
              <Option value="image">图片</Option>
            </Select>
          </Form.Item>
          <Form.Item name="attachReport" label="包含报表附件" valuePropName="checked">
            <Switch defaultChecked />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="发送邮件"
        open={emailModalVisible}
        onOk={handleSendEmailSubmit}
        onCancel={() => setEmailModalVisible(false)}
        width={600}
        confirmLoading={emailLoading}
      >
        <Form form={emailForm} layout="vertical">
          <Form.Item
            name="recipients"
            label="收件人"
            rules={[{ required: true, message: '请输入收件人邮箱' }]}
          >
            <Select mode="tags" placeholder="输入邮箱地址后按回车" />
          </Form.Item>
          <Form.Item
            name="subject"
            label="邮件主题"
            rules={[{ required: true, message: '请输入邮件主题' }]}
          >
            <Input placeholder="请输入邮件主题" />
          </Form.Item>
          <Form.Item name="content" label="邮件内容">
            <Input.TextArea rows={4} placeholder="请输入邮件内容" />
          </Form.Item>
          <Form.Item name="exportFormat" label="附件格式">
            <Select>
              <Option value="excel">Excel</Option>
              <Option value="pdf">PDF</Option>
              <Option value="image">图片</Option>
            </Select>
          </Form.Item>
          <Form.Item name="attachReport" label="包含报表附件" valuePropName="checked">
            <Switch defaultChecked />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ReportList
