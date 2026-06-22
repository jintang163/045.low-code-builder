import React, { useState, useEffect } from 'react'
import { Button, message, Modal, Tabs, List, Tag, Card, Descriptions, Spin } from 'antd'
import {
  EnvironmentOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  HomeOutlined,
  UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { attendanceApi, AttendanceRecord, ShiftSchedule } from '@/api/attendance'
import { useAppStore } from '@/store/appStore'
import './Schedule.less'

const { TabPane } = Tabs

interface LocationInfo {
  latitude: number
  longitude: number
  address?: string
  distance?: number
  inRange?: boolean
}

const MobileClockInPage: React.FC = () => {
  const { currentApp, userInfo } = useAppStore()
  const [currentTime, setCurrentTime] = useState(dayjs())
  const [location, setLocation] = useState<LocationInfo | null>(null)
  const [locating, setLocating] = useState(false)
  const [todayRecord, setTodayRecord] = useState<AttendanceRecord | null>(null)
  const [todaySchedule, setTodaySchedule] = useState<ShiftSchedule | null>(null)
  const [activeTab, setActiveTab] = useState('clockin')
  const [scheduleList, setScheduleList] = useState<ShiftSchedule[]>([])
  const [recordList, setRecordList] = useState<AttendanceRecord[]>([])
  const [loading, setLoading] = useState(false)
  const [clockingIn, setClockingIn] = useState(false)

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(dayjs())
    }, 1000)
    return () => clearInterval(timer)
  }, [])

  const getLocation = (): Promise<{ latitude: number; longitude: number }> => {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('浏览器不支持定位功能'))
        return
      }
      setLocating(true)
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setLocating(false)
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          })
        },
        (error) => {
          setLocating(false)
          reject(error)
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
      )
    })
  }

  const checkDistance = async (lat: number, lng: number) => {
    if (!currentApp) return
    try {
      const res = await attendanceApi.checkLocation(currentApp.id, lat, lng)
      if (res.code === 0 || res.code === 200) {
        setLocation({
          latitude: lat,
          longitude: lng,
          distance: res.data?.distance,
          inRange: res.data?.inRange,
        })
      }
    } catch (e) {
      console.error(e)
      setLocation({
        latitude: lat,
        longitude: lng,
        inRange: true,
        distance: 0,
      })
    }
  }

  const loadTodayInfo = async () => {
    if (!currentApp) return
    try {
      const today = dayjs().format('YYYY-MM-DD')
      const [scheduleRes, recordRes] = await Promise.all([
        attendanceApi.getUserSchedule(currentApp.id, userInfo?.id || 1, today, today),
        attendanceApi.getTodayRecord(currentApp.id, userInfo?.id || 1),
      ])
      if (scheduleRes.code === 0 || scheduleRes.code === 200) {
        const schedules = scheduleRes.data || []
        if (schedules.length > 0) {
          setTodaySchedule(schedules[0])
        }
      }
      if (recordRes.code === 0 || recordRes.code === 200) {
        setTodayRecord(recordRes.data)
      }
    } catch (e) {
      console.error(e)
    }
  }

  const loadUserRecords = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const startDate = dayjs().startOf('month').format('YYYY-MM-DD')
      const endDate = dayjs().endOf('month').format('YYYY-MM-DD')
      const res = await attendanceApi.getUserRecords(
        currentApp.id,
        userInfo?.id || 1,
        startDate,
        endDate
      )
      if (res.code === 0 || res.code === 200) {
        setRecordList(res.data || [])
      }
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  const loadUserSchedules = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const startDate = dayjs().startOf('month').format('YYYY-MM-DD')
      const endDate = dayjs().endOf('month').format('YYYY-MM-DD')
      const res = await attendanceApi.getUserSchedule(
        currentApp.id,
        userInfo?.id || 1,
        startDate,
        endDate
      )
      if (res.code === 0 || res.code === 200) {
        setScheduleList(res.data || [])
      }
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTodayInfo()
    getLocation()
      .then((pos) => {
        checkDistance(pos.latitude, pos.longitude)
      })
      .catch((err) => {
        console.warn('定位失败:', err)
        message.warning('定位失败，无法验证考勤地点')
      })
  }, [currentApp])

  useEffect(() => {
    if (activeTab === 'schedule') {
      loadUserSchedules()
    } else if (activeTab === 'records') {
      loadUserRecords()
    }
  }, [activeTab])

  const handleClockIn = async (type: 'IN' | 'OUT') => {
    if (!currentApp) return
    if (!location?.inRange) {
      message.warning('您不在考勤范围内，无法打卡')
      return
    }

    setClockingIn(true)
    try {
      const res = await attendanceApi.clockIn({
        appId: currentApp.id,
        clockType: type,
        latitude: location.latitude,
        longitude: location.longitude,
      })
      if (res.code === 0 || res.code === 200) {
        message.success(type === 'IN' ? '上班打卡成功' : '下班打卡成功')
        loadTodayInfo()
      }
    } catch (e: any) {
      message.error(e.message || '打卡失败')
    } finally {
      setClockingIn(false)
    }
  }

  const getShiftTypeName = (type: string) => {
    const map: Record<string, string> = {
      MORNING: '早班',
      EVENING: '晚班',
      DOUBLE: '全天',
      REST: '休息',
    }
    return map[type] || type
  }

  const getStatusInfo = (status: string) => {
    const map: Record<string, { text: string; color: string }> = {
      NORMAL: { text: '正常', color: 'green' },
      LATE: { text: '迟到', color: 'orange' },
      EARLY: { text: '早退', color: 'orange' },
      ABSENT: { text: '缺勤', color: 'red' },
    }
    return map[status] || { text: status, color: 'default' }
  }

  const isTodayRest = todaySchedule?.shiftType === 'REST'

  return (
    <div className="mobile-clockin-page">
      <div className="clockin-container">
        <Tabs activeKey={activeTab} onChange={setActiveTab} centered>
          <TabPane tab="打卡" key="clockin" />
          <TabPane tab="排班" key="schedule" />
          <TabPane tab="考勤" key="records" />
        </Tabs>

        {activeTab === 'clockin' && (
          <div>
            <div style={{ textAlign: 'center', marginTop: 20 }}>
              <div style={{ fontSize: 36, fontWeight: 'bold', color: '#333' }}>
                {currentTime.format('HH:mm:ss')}
              </div>
              <div style={{ color: '#999', marginTop: 4 }}>
                {currentTime.format('YYYY年MM月DD日 dddd')}
              </div>
            </div>

            {todaySchedule && (
              <Card size="small" style={{ marginTop: 20 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: 500 }}>今日排班</span>
                  <Tag color={isTodayRest ? 'green' : 'blue'}>
                    {getShiftTypeName(todaySchedule.shiftType)}
                  </Tag>
                </div>
                {!isTodayRest && (
                  <div style={{ color: '#666', fontSize: 13, marginTop: 8 }}>
                    <ClockCircleOutlined /> {todaySchedule.startTime} - {todaySchedule.endTime}
                    <span style={{ marginLeft: 12 }}>
                      工时 {todaySchedule.workHours}h
                    </span>
                  </div>
                )}
              </Card>
            )}

            {!isTodayRest && (
              <div
                className="clockin-circle"
                onClick={() => {
                  if (!todayRecord?.clockInTime) {
                    handleClockIn('IN')
                  } else if (!todayRecord?.clockOutTime) {
                    handleClockIn('OUT')
                  }
                }}
              >
                <Spin spinning={clockingIn}>
                  {!todayRecord?.clockInTime ? (
                    <>
                      <div className="clockin-time">上班打卡</div>
                      <div className="clockin-status">点击打卡</div>
                    </>
                  ) : !todayRecord?.clockOutTime ? (
                    <>
                      <div className="clockin-time">下班打卡</div>
                      <div className="clockin-status">
                        {dayjs(todayRecord.clockInTime).format('HH:mm')} 已上班
                      </div>
                    </>
                  ) : (
                    <>
                      <CheckCircleOutlined style={{ fontSize: 48, marginBottom: 8 }} />
                      <div className="clockin-time">已完成打卡</div>
                    </>
                  )}
                </Spin>
              </div>
            )}

            {todayRecord && (
              <div className="info-section">
                <div className="info-item">
                  <span className="info-label">上班打卡</span>
                  <span className="info-value">
                    {todayRecord.clockInTime
                      ? dayjs(todayRecord.clockInTime).format('HH:mm:ss')
                      : '--'}
                  </span>
                </div>
                <div className="info-item">
                  <span className="info-label">下班打卡</span>
                  <span className="info-value">
                    {todayRecord.clockOutTime
                      ? dayjs(todayRecord.clockOutTime).format('HH:mm:ss')
                      : '--'}
                  </span>
                </div>
                <div className="info-item">
                  <span className="info-label">考勤状态</span>
                  <span className="info-value">
                    <Tag color={getStatusInfo(todayRecord.status || '').color}>
                      {getStatusInfo(todayRecord.status || '').text}
                    </Tag>
                  </span>
                </div>
                {todayRecord.workHours && (
                  <div className="info-item">
                    <span className="info-label">今日工时</span>
                    <span className="info-value" style={{ color: '#1677ff' }}>
                      {todayRecord.workHours.toFixed(2)} 小时
                    </span>
                  </div>
                )}
              </div>
            )}

            <div className="location-info">
              <div>
                <EnvironmentOutlined /> 考勤地点
              </div>
              {locating ? (
                <div className="location-text">定位中...</div>
              ) : location ? (
                <>
                  <div className="location-text">
                    当前位置距离考勤点 {location.distance?.toFixed(0) || 0} 米
                  </div>
                  <div style={{ marginTop: 4, fontSize: 12 }}>
                    {location.inRange ? (
                      <span style={{ color: '#52c41a' }}>✓ 在考勤范围内</span>
                    ) : (
                      <span style={{ color: '#ff4d4f' }}>✗ 不在考勤范围内</span>
                    )}
                  </div>
                </>
              ) : (
                <div className="location-text">定位失败</div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'schedule' && (
          <Spin spinning={loading}>
            <List
              dataSource={scheduleList}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={<CalendarOutlined style={{ fontSize: 20, color: '#1677ff' }} />}
                    title={item.shiftDate}
                    description={
                      <div>
                        <Tag color={item.shiftType === 'REST' ? 'green' : 'blue'}>
                          {getShiftTypeName(item.shiftType)}
                        </Tag>
                        {item.shiftType !== 'REST' && (
                          <span style={{ marginLeft: 8, color: '#999', fontSize: 12 }}>
                            {item.startTime} - {item.endTime}
                          </span>
                        )}
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          </Spin>
        )}

        {activeTab === 'records' && (
          <Spin spinning={loading}>
            <List
              dataSource={recordList}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    title={item.attendanceDate}
                    description={
                      <div>
                        <Tag color={getStatusInfo(item.status || '').color}>
                          {getStatusInfo(item.status || '').text}
                        </Tag>
                        <span style={{ marginLeft: 8, color: '#999', fontSize: 12 }}>
                          上班: {item.clockInTime ? dayjs(item.clockInTime).format('HH:mm') : '--'}
                          {' / '}
                          下班: {item.clockOutTime ? dayjs(item.clockOutTime).format('HH:mm') : '--'}
                        </span>
                        <div style={{ marginTop: 4, color: '#666', fontSize: 12 }}>
                          工时: {item.workHours?.toFixed(2) || 0}h
                          {item.lateMinutes ? ` | 迟到${item.lateMinutes}分` : ''}
                          {item.earlyMinutes ? ` | 早退${item.earlyMinutes}分` : ''}
                        </div>
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          </Spin>
        )}
      </div>
    </div>
  )
}

export default MobileClockInPage
