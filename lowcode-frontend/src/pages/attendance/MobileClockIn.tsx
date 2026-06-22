import React, { useState, useEffect } from 'react'
import { Button, message, Modal, Tabs, List, Tag, Card, Descriptions, Spin, Alert } from 'antd'
import {
  EnvironmentOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  HomeOutlined,
  UserOutlined,
  ReloadOutlined,
  SafetyOutlined,
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
  locatedAt?: number
}

const MobileClockInPage: React.FC = () => {
  const { currentApp, userInfo } = useAppStore()
  const [currentTime, setCurrentTime] = useState(dayjs())
  const [location, setLocation] = useState<LocationInfo | null>(null)
  const [locating, setLocating] = useState(false)
  const [locationError, setLocationError] = useState<string | null>(null)
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

  const validateLocationFreshness = (loc: LocationInfo | null): boolean => {
    if (!loc) return false
    if (!loc.locatedAt) return false
    const age = Date.now() - loc.locatedAt
    return age < 5 * 60 * 1000
  }

  const canClockIn = (): { ok: boolean; reason?: string } => {
    if (locating) {
      return { ok: false, reason: '正在定位中...' }
    }
    if (!location) {
      return { ok: false, reason: '无法获取定位，请先点击右上角刷新定位' }
    }
    if (!validateLocationFreshness(location)) {
      return { ok: false, reason: '定位已过期，请刷新定位' }
    }
    if (!location.inRange) {
      return { ok: false, reason: `不在考勤范围内，距离最近考勤点${location.distance?.toFixed(0) || 0}米` }
    }
    return { ok: true }
  }

  const getLocation = async () => {
    setLocationError(null)
    if (!navigator.geolocation) {
      setLocationError('当前浏览器不支持GPS定位，请使用支持的浏览器')
      return null
    }

    setLocating(true)
    try {
      const pos: GeolocationPosition = await new Promise((resolve, reject) => {
        const timeoutId = setTimeout(() => {
          reject(new Error('定位超时，请检查GPS设置'))
        }, 15000)
        navigator.geolocation.getCurrentPosition(
          (p) => {
            clearTimeout(timeoutId)
            resolve(p)
          },
          (err) => {
            clearTimeout(timeoutId)
            reject(err)
          },
          { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
        )
      })

      const loc: LocationInfo = {
        latitude: pos.coords.latitude,
        longitude: pos.coords.longitude,
        locatedAt: Date.now(),
      }
      setLocation(loc)
      return loc
    } catch (err: any) {
      let msg = '定位失败'
      switch (err?.code) {
        case 1:
          msg = '请允许浏览器获取位置权限'
          break
        case 2:
          msg = '无法获取位置，请开启GPS定位'
          break
        case 3:
          msg = '定位超时，请重试'
          break
        default:
          msg = err?.message || '定位失败'
      }
      setLocationError(msg)
      message.error(msg)
      return null
    } finally {
      setLocating(false)
    }
  }

  const checkDistance = async (lat: number, lng: number) => {
    if (!currentApp) return
    try {
      const res = await attendanceApi.checkLocation(currentApp.id, lat, lng)
      if (res.code === 0 || res.code === 200) {
        setLocation((prev) => ({
          ...prev!,
          latitude: lat,
          longitude: lng,
          distance: res.data?.distance,
          inRange: res.data?.inRange,
          locatedAt: Date.now(),
        }))
      }
    } catch (e) {
      console.error(e)
    }
  }

  const refreshLocation = async () => {
    const pos = await getLocation()
    if (pos) {
      await checkDistance(pos.latitude, pos.longitude)
    }
  }

  const loadTodayInfo = async () => {
    if (!currentApp) return
    try {
      const today = dayjs().format('YYYY-MM-DD')
      const [scheduleRes, recordRes] = await Promise.all([
        attendanceApi.getUserSchedules(currentApp.id, userInfo?.id || 1, today, today),
        attendanceApi.getTodayRecord(currentApp.id),
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
      const res = await attendanceApi.getUserSchedules(
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
    refreshLocation()
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
    const check = canClockIn()
    if (!check.ok) {
      message.warning(check.reason || '无法打卡')
      return
    }

    setClockingIn(true)
    try {
      const apiMethod = type === 'IN' ? attendanceApi.clockIn : attendanceApi.clockOut
      const res = await apiMethod({
        appId: currentApp.id,
        latitude: location!.latitude,
        longitude: location!.longitude,
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
  const clockInCheck = canClockIn()

  return (
    <div className="mobile-clockin-page">
      <div className="clockin-container">
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          centered
          tabBarExtraContent={
            <Button
              type="text"
              icon={<ReloadOutlined spin={locating} />}
              onClick={refreshLocation}
              style={{ color: '#fff' }}
            >
              刷新定位
            </Button>
          }
        >
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
                style={{
                  cursor: clockInCheck.ok ? 'pointer' : 'not-allowed',
                  opacity: clockInCheck.ok ? 1 : 0.6,
                  background: !clockInCheck.ok
                    ? 'linear-gradient(135deg, #999 0%, #bbb 100%)'
                    : undefined,
                }}
                onClick={() => {
                  if (!todayRecord?.clockInTime) {
                    handleClockIn('IN')
                  } else if (!todayRecord?.clockOutTime) {
                    handleClockIn('OUT')
                  }
                }}
              >
                <Spin spinning={clockingIn}>
                  {!clockInCheck.ok && !todayRecord?.clockInTime && !todayRecord?.clockOutTime ? (
                    <>
                      <WarningOutlined style={{ fontSize: 40, marginBottom: 8 }} />
                      <div className="clockin-time" style={{ fontSize: 20 }}>
                        {clockInCheck.reason?.slice(0, 8)}
                      </div>
                      <div className="clockin-status">点击刷新定位</div>
                    </>
                  ) : !todayRecord?.clockInTime ? (
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

            {locationError && (
              <Alert
                style={{ marginTop: 16 }}
                type="error"
                showIcon
                icon={<WarningOutlined />}
                message={locationError}
                action={
                  <Button size="small" type="primary" onClick={refreshLocation}>
                    重试
                  </Button>
                }
              />
            )}

            {!locationError && (
              <div className="location-info">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>
                    <EnvironmentOutlined /> 考勤地点
                  </span>
                  <Button size="small" type="link" icon={<ReloadOutlined spin={locating} />} onClick={refreshLocation}>
                    刷新定位
                  </Button>
                </div>
                {locating ? (
                  <div className="location-text">定位中...</div>
                ) : location ? (
                  <>
                    <div className="location-text">
                      当前位置距离考勤点 {location.distance?.toFixed(0) || 0} 米
                      {location.locatedAt && (
                        <span style={{ color: '#999', fontSize: 11 }}>
                          {' '}（{dayjs(location.locatedAt).format('HH:mm:ss')} 更新）
                        </span>
                      )}
                    </div>
                    <div style={{ marginTop: 4, fontSize: 12 }}>
                      {location.inRange ? (
                        <span style={{ color: '#52c41a' }}>
                          <SafetyOutlined /> 在考勤范围内，可以打卡
                        </span>
                      ) : (
                        <span style={{ color: '#ff4d4f' }}>
                          <WarningOutlined /> 不在考勤范围内，无法打卡
                        </span>
                      )}
                    </div>
                  </>
                ) : (
                  <div className="location-text" style={{ color: '#ff4d4f' }}>
                    <WarningOutlined /> 无法获取定位，请点击右上角刷新
                  </div>
                )}
              </div>
            )}
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
