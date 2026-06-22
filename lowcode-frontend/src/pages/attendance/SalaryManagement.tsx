import React, { useState, useEffect } from 'react'
import {
  Card,
  Table,
  DatePicker,
  Button,
  Space,
  Tag,
  Modal,
  InputNumber,
  Form,
  message,
  Statistic,
  Row,
  Col,
  Divider,
  Descriptions,
} from 'antd'
import {
  DollarOutlined,
  FileExcelOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { attendanceApi, SalaryRecord } from '@/api/attendance'
import { useAppStore } from '@/store/appStore'
import './Schedule.less'

const { MonthPicker } = DatePicker

const SalaryManagementPage: React.FC = () => {
  const { currentApp } = useAppStore()
  const [list, setList] = useState<SalaryRecord[]>([])
  const [loading, setLoading] = useState(false)
  const [salaryMonth, setSalaryMonth] = useState<Dayjs>(dayjs())
  const [generateModalVisible, setGenerateModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentSalary, setCurrentSalary] = useState<SalaryRecord | null>(null)
  const [form] = Form.useForm()

  const loadData = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const res = await attendanceApi.getSalaryByMonth(
        currentApp.id,
        salaryMonth.format('YYYY-MM')
      )
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
    loadData()
  }, [currentApp, salaryMonth])

  const handleGenerate = () => {
    form.resetFields()
    form.setFieldsValue({
      salaryMonth: salaryMonth.format('YYYY-MM'),
      defaultHourlyWage: 20,
    })
    setGenerateModalVisible(true)
  }

  const handleGenerateSubmit = async () => {
    if (!currentApp) return
    try {
      const values = await form.validateFields()
      const res = await attendanceApi.generateSalary({
        appId: currentApp.id,
        salaryMonth: salaryMonth.format('YYYY-MM'),
        defaultHourlyWage: values.defaultHourlyWage,
      })
      if (res.code === 0 || res.code === 200) {
        message.success('工资生成成功')
        setGenerateModalVisible(false)
        loadData()
      }
    } catch (e: any) {
      message.error(e.message || '生成失败')
    }
  }

  const handleExport = async () => {
    if (!currentApp) return
    try {
      const res = await attendanceApi.exportSalary(currentApp.id, salaryMonth.format('YYYY-MM'))
      const blob = res.data as Blob
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `工资表_${salaryMonth.format('YYYY-MM')}.xlsx`
      a.click()
      window.URL.revokeObjectURL(url)
      message.success('导出成功')
    } catch (e) {
      console.error(e)
      message.error('导出失败')
    }
  }

  const handleMarkPaid = async () => {
    if (!currentApp) return
    try {
      await attendanceApi.markPaid(currentApp.id, salaryMonth.format('YYYY-MM'))
      message.success('已标记为发薪')
      loadData()
    } catch (e: any) {
      message.error(e.message || '操作失败')
    }
  }

  const handleViewDetail = (record: SalaryRecord) => {
    setCurrentSalary(record)
    setDetailModalVisible(true)
  }

  const columns = [
    {
      title: '员工',
      dataIndex: 'userName',
      key: 'userName',
      width: 120,
    },
    {
      title: '出勤天数',
      dataIndex: 'totalWorkDays',
      key: 'totalWorkDays',
      width: 100,
      render: (v: number) => `${v || 0} 天`,
    },
    {
      title: '总工时',
      dataIndex: 'totalWorkHours',
      key: 'totalWorkHours',
      width: 100,
      render: (v: number) => `${v?.toFixed(2) || 0} h`,
    },
    {
      title: '基本工资',
      dataIndex: 'baseSalary',
      key: 'baseSalary',
      width: 110,
      render: (v: number) => <span style={{ color: '#52c41a' }}>¥{v?.toFixed(2) || 0}</span>,
    },
    {
      title: '扣款合计',
      dataIndex: 'deductionTotal',
      key: 'deductionTotal',
      width: 110,
      render: (v: number) => <span style={{ color: '#ff4d4f' }}>-¥{v?.toFixed(2) || 0}</span>,
    },
    {
      title: '实发工资',
      dataIndex: 'netSalary',
      key: 'netSalary',
      width: 120,
      render: (v: number) => (
        <span style={{ color: '#1677ff', fontWeight: 'bold', fontSize: 16 }}>
          ¥{v?.toFixed(2) || 0}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (v: string) =>
        v === 'PAID' ? (
          <Tag color="success" icon={<CheckCircleOutlined />}>已发薪</Tag>
        ) : (
          <Tag color="orange">未发薪</Tag>
        ),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: SalaryRecord) => (
        <Button type="link" onClick={() => handleViewDetail(record)}>
          详情
        </Button>
      ),
    },
  ]

  const totalNetSalary = list.reduce((sum, s) => sum + (s.netSalary || 0), 0)
  const totalBaseSalary = list.reduce((sum, s) => sum + (s.baseSalary || 0), 0)
  const totalDeduction = list.reduce((sum, s) => sum + (s.deductionTotal || 0), 0)

  return (
    <div className="salary-management-page">
      <Card>
        <div className="salary-header">
          <h3 style={{ margin: 0 }}>工资管理</h3>
          <Space>
            <MonthPicker value={salaryMonth} onChange={(date) => date && setSalaryMonth(date)} />
            <Button icon={<ThunderboltOutlined />} type="primary" onClick={handleGenerate}>
              生成工资
            </Button>
            <Button icon={<FileExcelOutlined />} onClick={handleExport}>
              导出Excel
            </Button>
            <Button icon={<CheckCircleOutlined />} onClick={handleMarkPaid}>
              标记发薪
            </Button>
          </Space>
        </div>

        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={8}>
            <Card size="small">
              <Statistic
                title="员工人数"
                value={list.length}
                prefix={<DollarOutlined />}
                valueStyle={{ color: '#1677ff' }}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small">
              <Statistic
                title="应发工资总额"
                value={totalBaseSalary.toFixed(2)}
                prefix="¥"
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small">
              <Statistic
                title="实发工资总额"
                value={totalNetSalary.toFixed(2)}
                prefix="¥"
                valueStyle={{ color: '#722ed1' }}
              />
            </Card>
          </Col>
        </Row>

        <Table
          dataSource={list}
          rowKey="id"
          loading={loading}
          columns={columns}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title="生成工资"
        open={generateModalVisible}
        onCancel={() => setGenerateModalVisible(false)}
        onOk={handleGenerateSubmit}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="工资月份" name="salaryMonth">
            <Input disabled />
          </Form.Item>
          <Form.Item label="默认时薪（元/小时）" name="defaultHourlyWage" rules={[{ required: true, message: '请输入时薪' }]}>
            <InputNumber min={0} step={1} style={{ width: '100%' }} />
          </Form.Item>
          <div style={{ color: '#999', fontSize: 12 }}>
            系统将根据考勤记录自动计算工资，包含基本工资、加班费、请假扣款、迟到早退扣款等。
          </div>
        </Form>
      </Modal>

      <Modal
        title="工资详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={null}
        width={600}
      >
        {currentSalary && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="员工姓名">{currentSalary.userName}</Descriptions.Item>
            <Descriptions.Item label="工资月份">{currentSalary.salaryMonth}</Descriptions.Item>
            <Descriptions.Item label="出勤天数">{currentSalary.totalWorkDays} 天</Descriptions.Item>
            <Descriptions.Item label="总工时">{currentSalary.totalWorkHours?.toFixed(2)} 小时</Descriptions.Item>
            <Descriptions.Item label="基本工资">¥{currentSalary.baseSalary?.toFixed(2)}</Descriptions.Item>
            <Descriptions.Item label="时薪">¥{currentSalary.hourlyWage?.toFixed(2)}/h</Descriptions.Item>
            <Descriptions.Item label="加班费">¥{currentSalary.overtimePay?.toFixed(2)}</Descriptions.Item>
            <Descriptions.Item label="奖金">¥{currentSalary.bonus?.toFixed(2)}</Descriptions.Item>
            <Descriptions.Item label="补贴">¥{currentSalary.subsidy?.toFixed(2)}</Descriptions.Item>
            <Descriptions.Item label="请假天数">{currentSalary.leaveDays} 天</Descriptions.Item>
            <Descriptions.Item label="请假扣款" style={{ color: '#ff4d4f' }}>
              -¥{currentSalary.leaveDeduction?.toFixed(2)}
            </Descriptions.Item>
            <Descriptions.Item label="迟到扣款" style={{ color: '#ff4d4f' }}>
              -¥{currentSalary.lateDeduction?.toFixed(2)}
            </Descriptions.Item>
            <Descriptions.Item label="早退扣款" style={{ color: '#ff4d4f' }}>
              -¥{currentSalary.earlyDeduction?.toFixed(2)}
            </Descriptions.Item>
            <Descriptions.Item label="缺勤扣款" style={{ color: '#ff4d4f' }}>
              -¥{currentSalary.absentDeduction?.toFixed(2)}
            </Descriptions.Item>
            <Descriptions.Item label="扣款合计" style={{ color: '#ff4d4f' }}>
              -¥{currentSalary.deductionTotal?.toFixed(2)}
            </Descriptions.Item>
            <Descriptions.Item label="实发工资" style={{ color: '#1677ff', fontWeight: 'bold' }}>
              ¥{currentSalary.netSalary?.toFixed(2)}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default SalaryManagementPage
