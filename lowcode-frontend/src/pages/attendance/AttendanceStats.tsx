import React, { useState, useEffect } from 'react'
import {
  Card,
  Table,
  DatePicker,
  Button,
  Space,
  Tag,
  Statistic,
  Row,
  Col,
  Modal,
  List,
  message,
  Select,
} from 'antd'
import { CalendarOutlined, TeamOutlined, ClockCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { attendanceApi, AttendanceStats, AttendanceRecord } from '@/api/attendance'
import { useAppStore } from '@/store/appStore'
import './Schedule.less'

const { RangePicker } = DatePicker
const { Option } = Select

const AttendanceStatsPage: React.FC = () => {
  const { currentApp } = useAppStore()
  const [stats, setStats] = useState<AttendanceStats[]>([])
  const [records, setRecords] = useState<AttendanceRecord[]>([])
  const [loading, setLoading] = useState(false)
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().startOf('month'),
    dayjs().endOf('month'),
  ])
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [selectedUser, setSelectedUser] = useState<AttendanceStats | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const loadStats = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const res = await attendanceApi.getAttendanceStats(
        currentApp.id,
        dateRange[0].format('YYYY-MM-DD'),
        dateRange[1].format('YYYY-MM-DD')
      )
      if (res.code === 0 || res.code === 200) {
        setStats(res.data || [])
      }
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadStats()
  }, [currentApp, dateRange])

  const handleViewDetail = async (record: AttendanceStats) => {
    if (!currentApp) return
    setSelectedUser(record)
    setDetailModalVisible(true)
    setDetailLoading(true)
    try {
      const res = await attendanceApi.getUserRecords(
        currentApp.id,
        record.userId,
        dateRange[0].format('YYYY-MM-DD'),
        dateRange[1].format('YYYY-MM-DD')
      )
      if (res.code === 0 || res.code === 200) {
        setRecords(res.data || [])
      }
    } catch (e) {
      console.error(e)
    } finally {
      setDetailLoading(false)
    }
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
      dataIndex: 'workDays',
      key: 'workDays',
      width: 100,
      render: (v: number) => <Tag color="green">{v} 天</Tag>,
    },
    {
      title: '总工时',
      dataIndex: 'totalHours',
      key: 'totalHours',
      width: 100,
      render: (v: number) => `${v?.toFixed(2) || 0} 小时`,
    },
    {
      title: '迟到次数',
      dataIndex: 'lateCount',
      key: 'lateCount',
      width: 100,
      render: (v: number) => (
        <Tag color={v > 0 ? 'orange' : 'default'}>{v} 次</Tag>
      ),
    },
    {
      title: '早退次数',
      dataIndex: 'earlyCount',
      key: 'earlyCount',
      width: 100,
      render: (v: number) => (
        <Tag color={v > 0 ? 'orange' : 'default'}>{v} 次</Tag>
      ),
    },
    {
      title: '缺勤次数',
      dataIndex: 'absentCount',
      key: 'absentCount',
      width: 100,
      render: (v: number) => (
        <Tag color={v > 0 ? 'red' : 'default'}>{v} 次</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: AttendanceStats) => (
        <Button type="link" onClick={() => handleViewDetail(record)}>
          查看详情
        </Button>
      ),
    },
  ]

  const totalWorkDays = stats.reduce((sum, s) => sum + (s.workDays || 0), 0)
  const totalHours = stats.reduce((sum, s) => sum + (s.totalHours || 0), 0)
  const totalLate = stats.reduce((sum, s) => sum + (s.lateCount || 0), 0)
  const totalEarly = stats.reduce((sum, s) => sum + (s.earlyCount || 0), 0)

  return (
    <div className="attendance-stats-page">
      <Card>
        <div className="stats-header">
          <h3 style={{ margin: 0 }}>考勤统计</h3>
          <Space>
            <RangePicker
              value={dateRange}
              onChange={(dates) => {
                if (dates && dates[0] && dates[1]) {
                  setDateRange([dates[0] as Dayjs, dates[1] as Dayjs])
                }
              }}
            />
            <Button type="primary" onClick={loadStats}>
              查询
            </Button>
          </Space>
        </div>

        <div className="stats-cards">
          <Row gutter={16}>
            <Col span={6}>
              <Card>
                <Statistic
                  title="员工总数"
                  value={stats.length}
                  prefix={<TeamOutlined />}
                  valueStyle={{ color: '#1677ff' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="总出勤天数"
                  value={totalWorkDays}
                  suffix="天"
                  prefix={<CalendarOutlined />}
                  valueStyle={{ color: '#52c41a' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="总工时"
                  value={totalHours.toFixed(1)}
                  suffix="小时"
                  prefix={<ClockCircleOutlined />}
                  valueStyle={{ color: '#722ed1' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="异常次数"
                  value={totalLate + totalEarly}
                  suffix="次"
                  prefix={<ExclamationCircleOutlined />}
                  valueStyle={{ color: '#fa8c16' }}
                />
              </Card>
            </Col>
          </Row>
        </div>

        <Table
          dataSource={stats}
          rowKey="userId"
          loading={loading}
          columns={columns}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title={`${selectedUser?.userName || ''} 考勤详情`}
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={null}
        width={700}
      >
        <div className="detail-modal">
          <List
            loading={detailLoading}
            dataSource={records}
            renderItem={(item) => (
              <div className="record-item">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <span style={{ fontWeight: 500 }}>{item.attendanceDate}</span>
                    <Tag
                      style={{ marginLeft: 8 }}
                      color={
                        item.status === 'NORMAL'
                          ? 'green'
                          : item.status === 'LATE' || item.status === 'EARLY'
                          ? 'orange'
                          : 'red'
                      }
                    >
                      {item.status === 'NORMAL'
                        ? '正常'
                        : item.status === 'LATE'
                        ? '迟到'
                        : item.status === 'EARLY'
                        ? '早退'
                        : item.status === 'ABSENT'
                        ? '缺勤'
                        : item.status}
                    </Tag>
                    {item.shiftType && (
                      <Tag color="blue">
                        {item.shiftType === 'MORNING'
                          ? '早班'
                          : item.shiftType === 'EVENING'
                          ? '晚班'
                          : item.shiftType === 'REST'
                          ? '休息'
                          : '全天'}
                      </Tag>
                    )}
                  </div>
                  <div style={{ color: '#999', fontSize: 13 }}>
                    工时 {item.workHours?.toFixed(2) || 0}h
                  </div>
                </div>
                <div style={{ marginTop: 8, fontSize: 13, color: '#666' }}>
                  <span>上班: {item.clockInTime ? dayjs(item.clockInTime).format('HH:mm:ss') : '--'}</span>
                  <span style={{ marginLeft: 16 }}>
                    下班: {item.clockOutTime ? dayjs(item.clockOutTime).format('HH:mm:ss') : '--'}
                  </span>
                  {item.lateMinutes ? (
                    <span style={{ marginLeft: 16, color: '#fa8c16' }}>迟到 {item.lateMinutes} 分钟</span>
                  ) : null}
                  {item.earlyMinutes ? (
                    <span style={{ marginLeft: 16, color: '#fa8c16' }}>早退 {item.earlyMinutes} 分钟</span>
                  ) : null}
                </div>
              </div>
            )}
          />
        </div>
      </Modal>
    </div>
  )
}

export default AttendanceStatsPage
