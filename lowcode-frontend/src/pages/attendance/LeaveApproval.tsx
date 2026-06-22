import React, { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Select,
  DatePicker,
  TimePicker,
  Input,
  Upload,
  message,
  Tabs,
  Badge,
  Avatar,
} from 'antd'
import {
  PlusOutlined,
  CheckOutlined,
  CloseOutlined,
  UploadOutlined,
  UserOutlined,
} from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { attendanceApi, LeaveRequest } from '@/api/attendance'
import { useAppStore } from '@/store/appStore'
import './Schedule.less'

const { Option } = Select
const { TextArea } = Input
const { TabPane } = Tabs

const LeaveApprovalPage: React.FC = () => {
  const { currentApp, userInfo } = useAppStore()
  const [list, setList] = useState<LeaveRequest[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [form] = Form.useForm()
  const [activeTab, setActiveTab] = useState('pending')
  const [approvalModalVisible, setApprovalModalVisible] = useState(false)
  const [currentLeave, setCurrentLeave] = useState<LeaveRequest | null>(null)
  const [approvalAction, setApprovalAction] = useState<'approve' | 'reject'>('approve')

  const loadData = async (status?: string) => {
    if (!currentApp) return
    setLoading(true)
    try {
      let res
      if (status === 'pending') {
        res = await attendanceApi.getPendingLeaves(currentApp.id)
      } else if (status === 'my') {
        res = await attendanceApi.getMyLeaves(currentApp.id)
      } else {
        res = await attendanceApi.getLeavesByStatus(currentApp.id, status)
      }
      if (res.code === 0 || res.code === 200) {
        setList(res.data || [])
      }
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData(activeTab === 'all' ? undefined : activeTab === 'my' ? undefined : activeTab)
  }, [currentApp, activeTab])

  const handleApply = () => {
    form.resetFields()
    setModalVisible(true)
  }

  const handleSubmit = async () => {
    if (!currentApp) return
    try {
      const values = await form.validateFields()
      const data: Partial<LeaveRequest> = {
        appId: currentApp.id,
        leaveType: values.leaveType,
        startDate: values.dateRange[0].format('YYYY-MM-DD'),
        endDate: values.dateRange[1].format('YYYY-MM-DD'),
        reason: values.reason,
      }
      const res = await attendanceApi.createLeave(data)
      if (res.code === 0 || res.code === 200) {
        message.success('请假申请提交成功')
        setModalVisible(false)
        loadData(activeTab === 'all' ? undefined : activeTab)
      }
    } catch (e: any) {
      message.error(e.message || '提交失败')
    }
  }

  const handleApproval = (record: LeaveRequest, action: 'approve' | 'reject') => {
    setCurrentLeave(record)
    setApprovalAction(action)
    setApprovalModalVisible(true)
  }

  const handleApprovalSubmit = async () => {
    if (!currentLeave) return
    try {
      const values = await form.validateFields()
      const res = await attendanceApi.approveLeave({
        id: currentLeave.id,
        status: approvalAction === 'approve' ? 'APPROVED' : 'REJECTED',
        approvalRemark: values.approvalRemark,
      })
      if (res.code === 0 || res.code === 200) {
        message.success('审批成功')
        setApprovalModalVisible(false)
        loadData(activeTab === 'all' ? undefined : activeTab)
      }
    } catch (e: any) {
      message.error(e.message || '审批失败')
    }
  }

  const columns = [
    {
      title: '申请人',
      dataIndex: 'userName',
      key: 'userName',
      width: 120,
      render: (v: string) => (
        <Space>
          <Avatar icon={<UserOutlined />} size="small" />
          {v || '员工'}
        </Space>
      ),
    },
    {
      title: '请假类型',
      dataIndex: 'leaveType',
      key: 'leaveType',
      width: 100,
      render: (v: string) => {
        const typeMap: Record<string, { label: string; color: string }> = {
          SICK: { label: '病假', color: 'red' },
          PERSONAL: { label: '事假', color: 'orange' },
          ANNUAL: { label: '年假', color: 'green' },
          MARRIAGE: { label: '婚假', color: 'pink' },
          MATERNITY: { label: '产假', color: 'purple' },
          OTHER: { label: '其他', color: 'default' },
        }
        const t = typeMap[v] || { label: v, color: 'default' }
        return <Tag color={t.color}>{t.label}</Tag>
      },
    },
    {
      title: '请假日期',
      dataIndex: 'startDate',
      key: 'date',
      width: 200,
      render: (_: any, record: LeaveRequest) =>
        `${record.startDate} ~ ${record.endDate}（${record.leaveDays}天）`,
    },
    {
      title: '请假原因',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (v: string) => {
        const statusMap: Record<string, { label: string; color: string }> = {
          PENDING: { label: '待审批', color: 'processing' },
          APPROVED: { label: '已批准', color: 'success' },
          REJECTED: { label: '已拒绝', color: 'error' },
          CANCELLED: { label: '已取消', color: 'default' },
        }
        const s = statusMap[v] || { label: v, color: 'default' }
        return <Badge status={s.color as any} text={s.label} />
      },
    },
    {
      title: '申请时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (v: string) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_: any, record: LeaveRequest) =>
        record.status === 'PENDING' ? (
          <Space>
            <Button type="primary" size="small" icon={<CheckOutlined />} onClick={() => handleApproval(record, 'approve')}>
              批准
            </Button>
            <Button danger size="small" icon={<CloseOutlined />} onClick={() => handleApproval(record, 'reject')}>
              拒绝
            </Button>
          </Space>
        ) : (
          <Button type="link" size="small">
            查看详情
          </Button>
        ),
    },
  ]

  const pendingCount = activeTab === 'pending' ? list.length : 0

  return (
    <div className="leave-approval-page">
      <Card
        title="请假管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleApply}>
            申请请假
          </Button>
        }
      >
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab={<span>待审批 {pendingCount > 0 && <Badge count={pendingCount} />}</span>} key="pending" />
          <TabPane tab="我发起的" key="my" />
          <TabPane tab="全部" key="all" />
        </Tabs>

        <Table
          dataSource={list}
          rowKey="id"
          loading={loading}
          columns={columns}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title="申请请假"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmit}
        width={520}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="请假类型" name="leaveType" rules={[{ required: true, message: '请选择请假类型' }]}>
            <Select placeholder="请选择">
              <Option value="SICK">病假</Option>
              <Option value="PERSONAL">事假</Option>
              <Option value="ANNUAL">年假</Option>
              <Option value="MARRIAGE">婚假</Option>
              <Option value="MATERNITY">产假</Option>
              <Option value="OTHER">其他</Option>
            </Select>
          </Form.Item>
          <Form.Item label="请假日期" name="dateRange" rules={[{ required: true, message: '请选择日期' }]}>
            <DatePicker.RangePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="请假原因" name="reason" rules={[{ required: true, message: '请输入请假原因' }]}>
            <TextArea rows={4} placeholder="请输入请假原因" />
          </Form.Item>
          <Form.Item label="附件">
            <Upload beforeUpload={() => false}>
              <Button icon={<UploadOutlined />}>上传附件（病假条等）</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={approvalAction === 'approve' ? '批准请假' : '拒绝请假'}
        open={approvalModalVisible}
        onCancel={() => setApprovalModalVisible(false)}
        onOk={handleApprovalSubmit}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="审批意见" name="approvalRemark">
            <TextArea rows={3} placeholder="请输入审批意见（选填）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default LeaveApprovalPage
