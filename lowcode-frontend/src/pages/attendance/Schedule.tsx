import React, { useState, useEffect } from 'react'
import {
  Calendar,
  Badge,
  Button,
  Modal,
  Form,
  Select,
  Input,
  InputNumber,
  TimePicker,
  Tag,
  Space,
  Card,
  List,
  Avatar,
  message,
  Drawer,
  DatePicker,
  Popconfirm,
  Tooltip,
} from 'antd'
import {
  PlusOutlined,
  DeleteOutlined,
  TeamOutlined,
  CalendarOutlined,
  BatchJobOutlined,
} from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { attendanceApi, ShiftSchedule, ShiftConfig } from '@/api/attendance'
import { useAppStore } from '@/store/appStore'
import './Schedule.less'

const { Option } = Select
const { RangePicker } = DatePicker

const SHIFT_COLOR_MAP: Record<string, string> = {
  MORNING: 'blue',
  EVENING: 'orange',
  DOUBLE: 'purple',
  REST: 'green',
}

const ShiftSchedulePage: React.FC = () => {
  const { currentApp, userInfo } = useAppStore()
  const [shiftConfigs, setShiftConfigs] = useState<ShiftConfig[]>([])
  const [schedules, setSchedules] = useState<ShiftSchedule[]>([])
  const [loading, setLoading] = useState(false)
  const [currentMonth, setCurrentMonth] = useState<Dayjs>(dayjs())
  const [modalVisible, setModalVisible] = useState(false)
  const [selectedDate, setSelectedDate] = useState<Dayjs | null>(null)
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null)
  const [batchModalVisible, setBatchModalVisible] = useState(false)
  const [userListVisible, setUserListVisible] = useState(false)
  const [form] = Form.useForm()
  const [batchForm] = Form.useForm()
  const [draggedShift, setDraggedShift] = useState<string | null>(null)

  const mockUsers = [
    { id: 1, username: 'zhangsan', nickname: '张三', avatar: '' },
    { id: 2, username: 'lisi', nickname: '李四', avatar: '' },
    { id: 3, username: 'wangwu', nickname: '王五', avatar: '' },
    { id: 4, username: 'zhaoliu', nickname: '赵六', avatar: '' },
    { id: 5, username: 'sunqi', nickname: '孙七', avatar: '' },
  ]

  const loadShiftConfigs = async () => {
    if (!currentApp) return
    try {
      const res = await attendanceApi.getShiftConfigs(currentApp.id)
      if (res.code === 0 || res.code === 200) {
        setShiftConfigs(res.data || [])
      }
    } catch (e) {
      console.error(e)
    }
  }

  const loadSchedules = async (month: Dayjs) => {
    if (!currentApp) return
    setLoading(true)
    try {
      const startDate = month.startOf('month').format('YYYY-MM-DD')
      const endDate = month.endOf('month').format('YYYY-MM-DD')
      const res = await attendanceApi.getAppSchedules(currentApp.id, startDate, endDate)
      if (res.code === 0 || res.code === 200) {
        setSchedules(res.data || [])
      }
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadShiftConfigs()
    loadSchedules(currentMonth)
  }, [currentApp])

  const handleMonthChange = (value: Dayjs) => {
    setCurrentMonth(value)
    loadSchedules(value)
  }

  const handleDateSelect = (date: Dayjs) => {
    setSelectedDate(date)
    setSelectedUserId(null)
    form.resetFields()
    form.setFieldsValue({
      shiftDate: date.format('YYYY-MM-DD'),
      shiftType: 'MORNING',
    })
    setModalVisible(true)
  }

  const handleSaveSchedule = async () => {
    if (!currentApp || !selectedDate) return
    try {
      const values = await form.validateFields()
      const config = shiftConfigs.find((c) => c.shiftCode === values.shiftType)
      const data: Partial<ShiftSchedule> = {
        appId: currentApp.id,
        userId: values.userId || userInfo?.id || 1,
        userName: values.userName || userInfo?.nickname || '员工',
        shiftType: values.shiftType,
        shiftDate: selectedDate.format('YYYY-MM-DD'),
        workHours: config?.workHours || 8,
        hourlyWage: config?.hourlyWage || 20,
        remark: values.remark,
      }
      const res = await attendanceApi.saveSchedule(data)
      if (res.code === 0 || res.code === 200) {
        message.success('排班保存成功')
        setModalVisible(false)
        loadSchedules(currentMonth)
      }
    } catch (e: any) {
      message.error(e.message || '保存失败')
    }
  }

  const handleBatchSchedule = async () => {
    if (!currentApp) return
    try {
      const values = await batchForm.validateFields()
      const [startDate, endDate] = values.dateRange
      const data = {
        appId: currentApp.id,
        userIds: values.userIds,
        startDate: startDate.format('YYYY-MM-DD'),
        endDate: endDate.format('YYYY-MM-DD'),
        shiftType: values.shiftType,
      }
      const res = await attendanceApi.batchSchedule(data)
      if (res.code === 0 || res.code === 200) {
        message.success('批量排班成功')
        setBatchModalVisible(false)
        loadSchedules(currentMonth)
      }
    } catch (e: any) {
      message.error(e.message || '批量排班失败')
    }
  }

  const handleDeleteSchedule = async (userId: number, dateStr: string) => {
    if (!currentApp) return
    try {
      await attendanceApi.deleteSchedule(currentApp.id, userId, dateStr)
      message.success('删除成功')
      loadSchedules(currentMonth)
    } catch (e) {
      console.error(e)
    }
  }

  const getSchedulesByDate = (date: Dayjs) => {
    const dateStr = date.format('YYYY-MM-DD')
    return schedules.filter((s) => dayjs(s.shiftDate).format('YYYY-MM-DD') === dateStr)
  }

  const dateCellRender = (value: Dayjs) => {
    const daySchedules = getSchedulesByDate(value)
    const shiftMap: Record<string, string[]> = {}

    daySchedules.forEach((s) => {
      if (!shiftMap[s.shiftType]) shiftMap[s.shiftType] = []
      shiftMap[s.shiftType].push(s.userName || '员工')
    })

    return (
      <ul className="schedule-list">
        {Object.entries(shiftMap).map(([shiftType, names]) => (
          <li key={shiftType}>
            <Badge
              color={SHIFT_COLOR_MAP[shiftType] || 'default'}
              text={`${names.length}人 ${shiftType === 'MORNING' ? '早班' : shiftType === 'EVENING' ? '晚班' : shiftType === 'REST' ? '休息' : '全天'}`}
            />
          </li>
        ))}
      </ul>
    )
  }

  const handleCellClick = (date: Dayjs) => {
    setSelectedDate(date)
    setUserListVisible(true)
  }

  const handleShiftDragStart = (shiftType: string) => {
    setDraggedShift(shiftType)
  }

  const handleShiftDragEnd = () => {
    setDraggedShift(null)
  }

  const handleDayDrop = async (date: Dayjs, e: React.DragEvent) => {
    e.preventDefault()
    if (!draggedShift || !currentApp) return

    const config = shiftConfigs.find((c) => c.shiftCode === draggedShift)
    try {
      const data: Partial<ShiftSchedule> = {
        appId: currentApp.id,
        userId: userInfo?.id || 1,
        userName: userInfo?.nickname || '员工',
        shiftType: draggedShift,
        shiftDate: date.format('YYYY-MM-DD'),
        workHours: config?.workHours || 8,
        hourlyWage: config?.hourlyWage || 20,
      }
      await attendanceApi.saveSchedule(data)
      message.success('排班成功')
      loadSchedules(currentMonth)
    } catch (e) {
      console.error(e)
      message.error('排班失败')
    }
    setDraggedShift(null)
  }

  return (
    <div className="attendance-schedule-page">
      <Card
        title="排班管理"
        extra={
          <Space>
            <Button icon={<TeamOutlined />} onClick={() => setBatchModalVisible(true)}>
              批量排班
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => handleDateSelect(dayjs())}>
              新增排班
            </Button>
          </Space>
        }
      >
        <div className="schedule-toolbar">
          <div className="shift-legend">
            <span className="legend-title">班次图例（拖拽到日历排班）：</span>
            {shiftConfigs.map((config) => (
              <Tooltip key={config.shiftCode} title="拖拽到日历上排班">
                <Tag
                  color={config.shiftColor}
                  draggable
                  onDragStart={() => handleShiftDragStart(config.shiftCode)}
                  onDragEnd={handleShiftDragEnd}
                  className="shift-tag-draggable"
                >
                  {config.shiftName}
                </Tag>
              </Tooltip>
            ))}
          </div>
        </div>

        <div
          className="calendar-wrapper"
          onDragOver={(e) => e.preventDefault()}
        >
          <Calendar
            fullscreen
            cellRender={(current) => {
              return (
                <div
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => handleDayDrop(current, e)}
                  onClick={() => handleCellClick(current)}
                  className="calendar-cell"
                >
                  {dateCellRender(current)}
                </div>
              )
            }}
            onSelect={handleDateSelect}
            onPanelChange={handleMonthChange}
            value={currentMonth}
          />
        </div>
      </Card>

      <Modal
        title={selectedDate ? `${selectedDate.format('YYYY-MM-DD')} 排班` : '新增排班'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSaveSchedule}
        width={520}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="员工" name="userId" rules={[{ required: true, message: '请选择员工' }]}>
            <Select placeholder="选择员工">
              {mockUsers.map((user) => (
                <Option key={user.id} value={user.id}>
                  {user.nickname || user.username}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item label="班次" name="shiftType" rules={[{ required: true, message: '请选择班次' }]}>
            <Select placeholder="选择班次">
              {shiftConfigs.map((config) => (
                <Option key={config.shiftCode} value={config.shiftCode}>
                  <Tag color={config.shiftColor}>{config.shiftName}</Tag>
                  <span style={{ marginLeft: 8, color: '#999' }}>
                    {config.startTime} - {config.endTime}
                  </span>
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item label="排班日期" name="shiftDate">
            <Input disabled />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="批量排班"
        open={batchModalVisible}
        onCancel={() => setBatchModalVisible(false)}
        onOk={handleBatchSchedule}
        width={600}
      >
        <Form form={batchForm} layout="vertical">
          <Form.Item label="选择员工" name="userIds" rules={[{ required: true, message: '请选择员工' }]}>
            <Select mode="multiple" placeholder="选择多个员工">
              {mockUsers.map((user) => (
                <Option key={user.id} value={user.id}>
                  {user.nickname || user.username}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item label="排班日期范围" name="dateRange" rules={[{ required: true, message: '请选择日期范围' }]}>
            <RangePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="班次" name="shiftType" rules={[{ required: true, message: '请选择班次' }]}>
            <Select placeholder="选择班次">
              {shiftConfigs.map((config) => (
                <Option key={config.shiftCode} value={config.shiftCode}>
                  <Tag color={config.shiftColor}>{config.shiftName}</Tag>
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={selectedDate ? `${selectedDate.format('YYYY-MM-DD')} 排班详情` : '排班详情'}
        placement="right"
        width={400}
        open={userListVisible}
        onClose={() => setUserListVisible(false)}
        extra={
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => {
            setUserListVisible(false)
            handleDateSelect(selectedDate || dayjs())
          }}>
            添加
          </Button>
        }
      >
        {selectedDate && (
          <List
            dataSource={getSchedulesByDate(selectedDate)}
            renderItem={(item) => (
              <List.Item
                actions={[
                  <Popconfirm
                    title="确定删除这个排班吗？"
                    onConfirm={() => handleDeleteSchedule(item.userId, item.shiftDate)}
                  >
                    <DeleteOutlined style={{ color: '#ff4d4f' }} />
                  </Popconfirm>,
                ]}
              >
                <List.Item.Meta
                  avatar={<Avatar icon={<TeamOutlined />} />}
                  title={item.userName || '员工'}
                  description={
                    <Space>
                      <Tag color={SHIFT_COLOR_MAP[item.shiftType] || 'default'}>
                        {item.shiftType === 'MORNING' ? '早班' : item.shiftType === 'EVENING' ? '晚班' : item.shiftType === 'REST' ? '休息' : '全天'}
                      </Tag>
                      <span style={{ color: '#999', fontSize: 12 }}>
                        {item.startTime} - {item.endTime}
                      </span>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        )}
        {selectedDate && getSchedulesByDate(selectedDate).length === 0 && (
          <div style={{ textAlign: 'center', color: '#999', padding: '40px 0' }}>
            当天暂无排班
          </div>
        )}
      </Drawer>
    </div>
  )
}

export default ShiftSchedulePage
