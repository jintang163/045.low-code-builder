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
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { ReportInfo, reportApi } from '@/api/report'
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

  const loadData = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      setData(mockReports)
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
    try {
      message.success('删除成功')
      setData(prev => prev.filter(item => item.id !== id))
    } catch (e) {
      console.error(e)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (!currentApp) return

      if (editingItem) {
        setData(prev => prev.map(item => 
          item.id === editingItem.id ? { ...item, ...values } : item
        ))
        message.success('更新成功')
      } else {
        const newReport: ReportInfo = {
          ...values,
          id: Date.now(),
          appId: currentApp.id,
          status: 0,
          version: '1.0.0',
          components: [],
          createdTime: new Date().toISOString(),
          updatedTime: new Date().toISOString(),
        }
        setData(prev => [newReport, ...prev])
        message.success('创建成功')
      }
      setModalVisible(false)
    } catch (e) {
      console.error(e)
    }
  }

  const handleDesign = (id: number) => {
    navigate(`/report/designer/${id}`)
  }

  const handlePreview = (id: number) => {
    navigate(`/report/preview/${id}`)
  }

  const handleCopy = async (record: ReportInfo) => {
    try {
      const newReport: ReportInfo = {
        ...record,
        id: Date.now(),
        reportName: record.reportName + ' - 副本',
        reportCode: record.reportCode + '_copy',
        status: 0,
        version: '1.0.0',
        createdTime: new Date().toISOString(),
        updatedTime: new Date().toISOString(),
      }
      setData(prev => [newReport, ...prev])
      message.success('复制成功')
    } catch (e) {
      console.error(e)
    }
  }

  const handleSchedule = (record: ReportInfo) => {
    setCurrentReport(record)
    setScheduleModalVisible(true)
  }

  const handleSendEmail = (record: ReportInfo) => {
    setCurrentReport(record)
    emailForm.resetFields()
    setEmailModalVisible(true)
  }

  const handleSendEmailSubmit = async () => {
    try {
      const values = await emailForm.validateFields()
      message.success('邮件发送成功')
      setEmailModalVisible(false)
    } catch (e) {
      console.error(e)
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
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm'),
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
          <Button type="link" size="small" icon={<CopyOutlined />} onClick={() => handleCopy(record)}>
            复制
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
        onOk={() => {
          message.success('定时任务设置成功')
          setScheduleModalVisible(false)
        }}
        onCancel={() => setScheduleModalVisible(false)}
        width={600}
      >
        <Form layout="vertical">
          <Form.Item name="enabled" label="启用定时发送" valuePropName="checked">
            <Switch defaultChecked />
          </Form.Item>
          <Form.Item name="sendType" label="发送频率">
            <Select defaultValue="daily">
              <Option value="daily">每天</Option>
              <Option value="weekly">每周</Option>
              <Option value="monthly">每月</Option>
              <Option value="custom">自定义</Option>
            </Select>
          </Form.Item>
          <Form.Item name="sendTime" label="发送时间">
            <Input type="time" defaultValue="09:00" />
          </Form.Item>
          <Form.Item name="recipients" label="收件人邮箱">
            <Select mode="tags" placeholder="输入邮箱地址后按回车" />
          </Form.Item>
          <Form.Item name="subject" label="邮件主题">
            <Input placeholder="请输入邮件主题" defaultValue={currentReport?.reportName + ' - 定时报表'} />
          </Form.Item>
          <Form.Item name="exportFormat" label="导出格式">
            <Select defaultValue="excel">
              <Option value="excel">Excel</Option>
              <Option value="pdf">PDF</Option>
              <Option value="image">图片</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="发送邮件"
        open={emailModalVisible}
        onOk={handleSendEmailSubmit}
        onCancel={() => setEmailModalVisible(false)}
        width={600}
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
            <Select defaultValue="excel">
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
