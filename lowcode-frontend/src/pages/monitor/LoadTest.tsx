import React, { useState, useEffect, useCallback, useRef } from 'react'
import {
  Card,
  Form,
  Input,
  InputNumber,
  Select,
  Button,
  Space,
  List,
  Tag,
  Statistic,
  Row,
  Col,
  Progress,
  Alert,
  Empty,
  Modal,
  Tabs,
  Popconfirm,
  message,
  Divider,
  Typography,
  Tooltip,
  Cascader,
} from 'antd'
import {
  PlayCircleOutlined,
  StopOutlined,
  DeleteOutlined,
  ReloadOutlined,
  LineChartOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  InfoCircleOutlined,
  BarChartOutlined,
  DashboardOutlined,
  FileTextOutlined,
  AppstoreOutlined,
  GlobalOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import {
  loadTestApi,
  type LoadTestConfig,
  type LoadTestMetrics,
  type LoadTestReport,
  type LoadTestInfo,
} from '@/api/monitor'
import { appApi, type AppInfo } from '@/api'
import { pageApi, type PageInfo } from '@/api/page'

const { Title, Text } = Typography
const { Option } = Select

const LoadTestPage: React.FC = () => {
  const [form] = Form.useForm()
  const [tests, setTests] = useState<LoadTestInfo[]>([])
  const [selectedTestId, setSelectedTestId] = useState<string | null>(null)
  const [currentMetrics, setCurrentMetrics] = useState<LoadTestMetrics | null>(null)
  const [currentReport, setCurrentReport] = useState<LoadTestReport | null>(null)
  const [loading, setLoading] = useState(false)
  const [starting, setStarting] = useState(false)
  const [stopping, setStopping] = useState(false)
  const [configModalVisible, setConfigModalVisible] = useState(false)

  const [appList, setAppList] = useState<AppInfo[]>([])
  const [pageListMap, setPageListMap] = useState<Record<number, PageInfo[]>>({})
  const [loadingPages, setLoadingPages] = useState(false)
  const [targetMode, setTargetMode] = useState<'url' | 'page'>('url')

  const metricsPollingRef = useRef<number | null>(null)
  const listPollingRef = useRef<number | null>(null)

  const loadTestList = useCallback(async () => {
    try {
      const res = await loadTestApi.list()
      const list = (res as any)?.data || res || []
      setTests(list)
    } catch (e) {
      console.error('加载测试列表失败:', e)
    }
  }, [])

  const loadApps = useCallback(async () => {
    try {
      const res = await appApi.list()
      const list = (res as any)?.data || res || []
      setAppList(list)
    } catch (e) {
      console.error('加载应用列表失败:', e)
    }
  }, [])

  const loadPages = useCallback(async (appId: number) => {
    if (pageListMap[appId]) {
      return pageListMap[appId]
    }
    setLoadingPages(true)
    try {
      const res = await pageApi.list(appId)
      const list = (res as any)?.data || res || []
      setPageListMap(prev => ({ ...prev, [appId]: list }))
      return list
    } catch (e) {
      console.error('加载页面列表失败:', e)
      return []
    } finally {
      setLoadingPages(false)
    }
  }, [pageListMap])

  const loadMetrics = useCallback(async (testId: string) => {
    try {
      const res = await loadTestApi.getMetrics(testId)
      const metrics = (res as any)?.data || res
      setCurrentMetrics(metrics)
    } catch (e) {
      console.error('加载指标失败:', e)
    }
  }, [])

  const loadReport = useCallback(async (testId: string) => {
    try {
      setLoading(true)
      const res = await loadTestApi.getReport(testId)
      const report = (res as any)?.data || res
      setCurrentReport(report)
    } catch (e) {
      console.error('加载报告失败:', e)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadTestList()
    loadApps()
    listPollingRef.current = window.setInterval(loadTestList, 3000)
    return () => {
      if (listPollingRef.current) clearInterval(listPollingRef.current)
    }
  }, [loadTestList, loadApps])

  useEffect(() => {
    if (selectedTestId) {
      loadMetrics(selectedTestId)
      const selectedTest = tests.find(t => t.testId === selectedTestId)
      const isRunning = selectedTest?.status === 'RUNNING'
      if (isRunning) {
        metricsPollingRef.current = window.setInterval(() => {
          loadMetrics(selectedTestId)
          loadReport(selectedTestId)
        }, 1000)
      } else {
        loadReport(selectedTestId)
      }
    }
    return () => {
      if (metricsPollingRef.current) clearInterval(metricsPollingRef.current)
    }
  }, [selectedTestId, tests, loadMetrics, loadReport])

  const handleStart = async (values: any) => {
    setStarting(true)
    try {
      let targetUrl = values.targetUrl
      if (targetMode === 'page' && values.selectedPage) {
        const [appId, pageId] = values.selectedPage
        const pages = pageListMap[appId] || []
        const page = pages.find((p: PageInfo) => p.id === Number(pageId))
        if (page) {
          const baseUrl = window.location.origin
          const pagePath = page.pagePath || `/page/${pageId}`
          targetUrl = baseUrl + (pagePath.startsWith('/') ? pagePath : '/' + pagePath)
        }
      }

      const config: LoadTestConfig = {
        testName: values.testName,
        targetUrl,
        httpMethod: values.httpMethod || 'GET',
        virtualUsers: values.virtualUsers || 10,
        durationSeconds: values.durationSeconds || 60,
        rampUpSeconds: values.rampUpSeconds || 10,
        thinkTimeMs: values.thinkTimeMs ?? 1000,
        timeoutMs: values.timeoutMs || 30000,
        requestBody: values.requestBody,
        contentType: values.contentType || 'application/json',
        headers: values.headers ? parseHeaders(values.headers) : undefined,
      }

      const res = await loadTestApi.start(config)
      const metrics = (res as any)?.data || res
      setSelectedTestId(metrics.testId)
      message.success('压力测试已启动')
      setConfigModalVisible(false)
      form.resetFields()
      setTargetMode('url')
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || '启动失败'
      message.error(msg)
    } finally {
      setStarting(false)
    }
  }

  const handleStop = async (testId: string) => {
    setStopping(true)
    try {
      await loadTestApi.stop(testId)
      message.success('测试已停止')
      loadMetrics(testId)
      loadReport(testId)
      loadTestList()
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || '停止失败')
    } finally {
      setStopping(false)
    }
  }

  const handleDelete = async (testId: string) => {
    try {
      await loadTestApi.delete(testId)
      message.success('删除成功')
      if (selectedTestId === testId) {
        setSelectedTestId(null)
        setCurrentMetrics(null)
        setCurrentReport(null)
      }
      loadTestList()
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || '删除失败')
    }
  }

  const parseHeaders = (headerStr: string): Record<string, string> => {
    const headers: Record<string, string> = {}
    const lines = headerStr.trim().split('\n')
    lines.forEach(line => {
      const colonIndex = line.indexOf(':')
      if (colonIndex > 0) {
        const key = line.substring(0, colonIndex).trim()
        const value = line.substring(colonIndex + 1).trim()
        if (key && value) headers[key] = value
      }
    })
    return headers
  }

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return mins > 0 ? `${mins}分${secs}秒` : `${secs}秒`
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'RUNNING': return 'processing'
      case 'COMPLETED': return 'success'
      case 'STOPPED': return 'default'
      case 'READY': return 'warning'
      default: return 'default'
    }
  }

  const getStatusText = (status: string) => {
    switch (status) {
      case 'RUNNING': return '运行中'
      case 'COMPLETED': return '已完成'
      case 'STOPPED': return '已停止'
      case 'READY': return '准备中'
      default: return status
    }
  }

  const selectedTest = tests.find(t => t.testId === selectedTestId)
  const isRunning = selectedTest?.status === 'RUNNING'

  const pageOptions = appList.map(app => ({
    value: String(app.id),
    label: app.appName || app.name || `应用${app.id}`,
    children: (pageListMap[app.id!] || []).map(page => ({
      value: String(page.id),
      label: (
        <span>
          <FileTextOutlined style={{ marginRight: 6 }} />
          {page.pageName}
          {page.pagePath && <Tag color="blue" style={{ marginLeft: 6 }}>{page.pagePath}</Tag>}
        </span>
      ),
    })),
  }))

  const handlePageLoad = async (selectedOptions: any[]) => {
    if (selectedOptions.length === 1) {
      const appId = Number(selectedOptions[0].value)
      await loadPages(appId)
    }
  }

  const throughputOption = currentReport ? {
    title: { text: '吞吐量 (RPS)', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: '10%', right: '5%', bottom: '10%' },
    xAxis: {
      type: 'category',
      data: currentReport.throughputSeries?.map((_: any, i: number) => i + 's') || [],
    },
    yAxis: { type: 'value', name: 'RPS' },
    series: [{
      data: currentReport.throughputSeries?.map((s: any) => s.value) || [],
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(24, 144, 255, 0.2)' },
      lineStyle: { color: '#1890ff', width: 2 },
      itemStyle: { color: '#1890ff' },
    }],
  } : {}

  const responseTimeOption = currentReport ? {
    title: { text: '平均响应时间 (ms)', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis', formatter: '{b}: {c}ms' },
    grid: { left: '10%', right: '5%', bottom: '10%' },
    xAxis: {
      type: 'category',
      data: currentReport.responseTimeSeries?.map((_: any, i: number) => i + 's') || [],
    },
    yAxis: { type: 'value', name: 'ms' },
    series: [{
      data: currentReport.responseTimeSeries?.map((s: any) => s.value) || [],
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(82, 196, 26, 0.2)' },
      lineStyle: { color: '#52c41a', width: 2 },
      itemStyle: { color: '#52c41a' },
    }],
  } : {}

  const errorRateOption = currentReport ? {
    title: { text: '错误率 (%)', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
    grid: { left: '10%', right: '5%', bottom: '10%' },
    xAxis: {
      type: 'category',
      data: currentReport.errorRateSeries?.map((_: any, i: number) => i + 's') || [],
    },
    yAxis: { type: 'value', name: '%', max: 100 },
    series: [{
      data: currentReport.errorRateSeries?.map((s: any) => s.value) || [],
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(255, 77, 79, 0.2)' },
      lineStyle: { color: '#ff4d4f', width: 2 },
      itemStyle: { color: '#ff4d4f' },
    }],
  } : {}

  const distributionOption = currentReport?.responseTimeDistribution ? {
    title: { text: '响应时间分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis', formatter: '{b}: {c} 次' },
    grid: { left: '10%', right: '5%', bottom: '15%' },
    xAxis: {
      type: 'category',
      data: currentReport.responseTimeDistribution.bucketLabels || [],
      axisLabel: { rotate: 45, fontSize: 10 },
    },
    yAxis: { type: 'value', name: '请求数' },
    series: [{
      data: currentReport.responseTimeDistribution.buckets?.map((b: any) => Number(b)) || [],
      type: 'bar',
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#1890ff' },
            { offset: 1, color: '#91d5ff' },
          ],
        },
        borderRadius: [4, 4, 0, 0],
      },
      barMaxWidth: 30,
    }],
  } : {}

  const percentilesOption = currentReport?.responseTimeDistribution ? {
    title: { text: '响应时间百分位', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis', formatter: '{b}: {c}ms' },
    grid: { left: '15%', right: '10%' },
    xAxis: {
      type: 'category',
      data: ['Min', 'P50', 'P75', 'P90', 'P95', 'P99', 'Max'],
    },
    yAxis: { type: 'value', name: 'ms' },
    series: [{
      data: [
        currentReport.responseTimeDistribution.min,
        currentReport.responseTimeDistribution.p50,
        currentReport.responseTimeDistribution.p75,
        currentReport.responseTimeDistribution.p90,
        currentReport.responseTimeDistribution.p95,
        currentReport.responseTimeDistribution.p99,
        currentReport.responseTimeDistribution.max,
      ],
      type: 'bar',
      label: { show: true, position: 'top', formatter: '{c}ms' },
      itemStyle: {
        color: (params: any) => {
          const colors = ['#52c41a', '#73d13d', '#a0d911', '#faad14', '#fa8c16', '#f5222d', '#cf1322']
          return colors[params.dataIndex] || '#1890ff'
        },
        borderRadius: [4, 4, 0, 0],
      },
      barMaxWidth: 40,
    }],
  } : {}

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>
          <ThunderboltOutlined style={{ marginRight: 8 }} />
          性能压力测试
        </Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={loadTestList}>
            刷新
          </Button>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={() => setConfigModalVisible(true)}
          >
            新建测试
          </Button>
        </Space>
      </div>

      <div style={{ flex: 1, display: 'flex', gap: 16, minHeight: 0 }}>
        <Card
          title="测试列表"
          extra={<Tag color="blue">{tests.length}</Tag>}
          style={{ width: 320, flexShrink: 0 }}
          bodyStyle={{ padding: 0, overflowY: 'auto' }}
        >
          {tests.length === 0 ? (
            <Empty description="暂无测试记录" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ padding: '40px 0' }} />
          ) : (
            <List
              dataSource={tests}
              renderItem={(item: any) => (
                <List.Item
                  key={item.testId}
                  onClick={() => setSelectedTestId(item.testId)}
                  style={{
                    cursor: 'pointer',
                    padding: '12px 16px',
                    background: selectedTestId === item.testId ? '#e6f7ff' : undefined,
                    borderLeft: selectedTestId === item.testId ? '3px solid #1890ff' : undefined,
                  }}
                >
                  <List.Item.Meta
                    title={
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Text strong ellipsis style={{ maxWidth: 160 }}>{item.testName}</Text>
                        <Tag color={getStatusColor(item.status)} size="small">
                          {getStatusText(item.status)}
                        </Tag>
                      </div>
                    }
                    description={
                      <div>
                        <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                          {item.targetUrl?.length > 20 ? item.targetUrl.substring(0, 20) + '...' : item.targetUrl}
                        </Text>
                        <div style={{ marginTop: 4 }}>
                          <Tag color="default" size="small">{item.virtualUsers} VU</Tag>
                          <Tag color="default" size="small">{formatTime(item.duration || 0)}</Tag>
                        </div>
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          )}
        </Card>

        <Card
          style={{ flex: 1, overflow: 'auto' }}
          bodyStyle={{ padding: 0 }}
          title={
            selectedTest ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span>{selectedTest.testName}</span>
                <Tag color={getStatusColor(selectedTest.status)}>
                  {getStatusText(selectedTest.status)}
                </Tag>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {selectedTest.testId}
                </Text>
              </div>
            ) : (
              <span>请选择测试</span>
            )
          }
          extra={
            selectedTest && (
              <Space>
                {isRunning && (
                  <Button
                    danger
                    icon={<StopOutlined />}
                    onClick={() => handleStop(selectedTest.testId)}
                    loading={stopping}
                  >
                    停止测试
                  </Button>
                )}
                <Popconfirm
                  title="确定删除这个测试吗？"
                  onConfirm={() => handleDelete(selectedTest.testId)}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button danger icon={<DeleteOutlined />}>
                    删除
                  </Button>
                </Popconfirm>
              </Space>
            )
          }
        >
          {!selectedTest ? (
            <Empty
              description="请从左侧选择一个测试查看详情，或点击右上角「新建测试」创建新测试"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              style={{ padding: '80px 0' }}
            />
          ) : !currentMetrics ? (
            <Empty description="加载中..." style={{ padding: '80px 0' }} />
          ) : (
            <div style={{ padding: 16 }}>
              <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                <Col xs={12} sm={8} md={6} lg={4}>
                  <Card size="small">
                    <Statistic
                      title={
                        <span><DashboardOutlined style={{ marginRight: 4 }} />总请求</span>
                      }
                      value={currentMetrics.totalRequests}
                      valueStyle={{ fontSize: 20 }}
                    />
                  </Card>
                </Col>
                <Col xs={12} sm={8} md={6} lg={4}>
                  <Card size="small">
                    <Statistic
                      title={
                        <span><CheckCircleOutlined style={{ marginRight: 4, color: '#52c41a' }} />成功</span>
                      }
                      value={currentMetrics.successRequests}
                      valueStyle={{ color: '#52c41a', fontSize: 20 }}
                    />
                  </Card>
                </Col>
                <Col xs={12} sm={8} md={6} lg={4}>
                  <Card size="small">
                    <Statistic
                      title={
                        <span><WarningOutlined style={{ marginRight: 4, color: '#ff4d4f' }} />失败</span>
                      }
                      value={currentMetrics.failedRequests}
                      valueStyle={{ color: '#ff4d4f', fontSize: 20 }}
                    />
                  </Card>
                </Col>
                <Col xs={12} sm={8} md={6} lg={4}>
                  <Card size="small">
                    <Statistic
                      title={
                        <span><LineChartOutlined style={{ marginRight: 4 }} />成功率</span>
                      }
                      value={currentMetrics.successRate}
                      precision={2}
                      suffix="%"
                      valueStyle={{
                        color: currentMetrics.successRate >= 95 ? '#52c41a' : currentMetrics.successRate >= 80 ? '#faad14' : '#ff4d4f',
                        fontSize: 20,
                      }}
                    />
                    <Progress
                      percent={Number(currentMetrics.successRate.toFixed(0))}
                      size="small"
                      status={currentMetrics.successRate >= 95 ? 'success' : currentMetrics.successRate >= 80 ? 'normal' : 'exception'}
                      showInfo={false}
                      style={{ marginTop: 4 }}
                    />
                  </Card>
                </Col>
                <Col xs={12} sm={8} md={6} lg={4}>
                  <Card size="small">
                    <Statistic
                      title={
                        <span><ThunderboltOutlined style={{ marginRight: 4 }} />RPS</span>
                      }
                      value={currentMetrics.requestsPerSecond}
                      precision={1}
                      suffix="/s"
                      valueStyle={{ fontSize: 20 }}
                    />
                  </Card>
                </Col>
                <Col xs={12} sm={8} md={6} lg={4}>
                  <Card size="small">
                    <Statistic
                      title={
                        <span><BarChartOutlined style={{ marginRight: 4 }} />平均响应</span>
                      }
                      value={currentMetrics.avgResponseTime}
                      precision={0}
                      suffix="ms"
                      valueStyle={{
                        color: currentMetrics.avgResponseTime < 500 ? '#52c41a' : currentMetrics.avgResponseTime < 2000 ? '#faad14' : '#ff4d4f',
                        fontSize: 20,
                      }}
                    />
                  </Card>
                </Col>
              </Row>

              <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                <Col xs={12} sm={8}>
                  <Card size="small" title="最小响应">
                    <Text strong style={{ fontSize: 18 }}>{currentMetrics.minResponseTime}</Text>
                    <Text type="secondary"> ms</Text>
                  </Card>
                </Col>
                <Col xs={12} sm={8}>
                  <Card size="small" title="最大响应">
                    <Text strong style={{ fontSize: 18 }}>{currentMetrics.maxResponseTime}</Text>
                    <Text type="secondary"> ms</Text>
                  </Card>
                </Col>
                <Col xs={12} sm={8}>
                  <Card size="small" title="已运行">
                    <Text strong style={{ fontSize: 18 }}>{formatTime(currentMetrics.elapsedSeconds)}</Text>
                    {isRunning && currentMetrics.activeUsers !== undefined && (
                      <Tag color="processing" style={{ marginLeft: 8 }}>
                        {currentMetrics.activeUsers} 活跃用户
                      </Tag>
                    )}
                  </Card>
                </Col>
              </Row>

              {isRunning && (
                <Alert
                  type="info"
                  showIcon
                  icon={<InfoCircleOutlined spin />}
                  message="测试运行中"
                  description="数据每秒钟自动刷新，请耐心等待测试完成或手动停止测试查看完整报告。"
                  style={{ marginBottom: 16 }}
                />
              )}

              {currentReport && currentReport.bottleneckAnalysis && (
                <Alert
                  type={
                    currentReport.bottleneckAnalysis.performanceGrade?.startsWith('A') ? 'success'
                    : currentReport.bottleneckAnalysis.performanceGrade?.startsWith('B') ? 'info'
                    : currentReport.bottleneckAnalysis.performanceGrade?.startsWith('C') ? 'warning'
                    : 'error'
                  }
                  showIcon
                  message={
                    <span>
                      性能评级: <strong style={{ fontSize: 16 }}>{currentReport.bottleneckAnalysis.performanceGrade}</strong>
                      <span style={{ marginLeft: 16 }}>{currentReport.bottleneckAnalysis.overall}</span>
                    </span>
                  }
                  style={{ marginBottom: 16 }}
                />
              )}

              <Tabs
                items={[
                  {
                    key: 'realtime',
                    label: <><LineChartOutlined /> 实时监控</>,
                    children: (
                      <div>
                        <Row gutter={[16, 16]}>
                          <Col xs={24} lg={8}>
                            <Card size="small">
                              <ReactECharts option={throughputOption} style={{ height: 280 }} notMerge />
                            </Card>
                          </Col>
                          <Col xs={24} lg={8}>
                            <Card size="small">
                              <ReactECharts option={responseTimeOption} style={{ height: 280 }} notMerge />
                            </Card>
                          </Col>
                          <Col xs={24} lg={8}>
                            <Card size="small">
                              <ReactECharts option={errorRateOption} style={{ height: 280 }} notMerge />
                            </Card>
                          </Col>
                        </Row>
                      </div>
                    ),
                  },
                  {
                    key: 'distribution',
                    label: <><BarChartOutlined /> 响应时间分析</>,
                    children: currentReport && (
                      <div>
                        <Row gutter={[16, 16]}>
                          <Col xs={24} lg={12}>
                            <Card size="small">
                              <ReactECharts option={distributionOption} style={{ height: 320 }} notMerge />
                            </Card>
                          </Col>
                          <Col xs={24} lg={12}>
                            <Card size="small">
                              <ReactECharts option={percentilesOption} style={{ height: 320 }} notMerge />
                            </Card>
                          </Col>
                        </Row>

                        {currentReport.responseTimeDistribution && (
                          <Card size="small" title="百分位详情" style={{ marginTop: 16 }}>
                            <Row gutter={[16, 8]}>
                              <Col xs={6} sm={4}>
                                <Statistic title="P50" value={currentReport.responseTimeDistribution.p50} suffix="ms" />
                              </Col>
                              <Col xs={6} sm={4}>
                                <Statistic title="P75" value={currentReport.responseTimeDistribution.p75} suffix="ms" />
                              </Col>
                              <Col xs={6} sm={4}>
                                <Statistic title="P90" value={currentReport.responseTimeDistribution.p90} suffix="ms" />
                              </Col>
                              <Col xs={6} sm={4}>
                                <Statistic title="P95" value={currentReport.responseTimeDistribution.p95} suffix="ms" />
                              </Col>
                              <Col xs={6} sm={4}>
                                <Statistic title="P99" value={currentReport.responseTimeDistribution.p99} suffix="ms" />
                              </Col>
                              <Col xs={6} sm={4}>
                                <Statistic title="Avg" value={currentReport.responseTimeDistribution.avg} suffix="ms" />
                              </Col>
                            </Row>
                          </Card>
                        )}
                      </div>
                    ),
                  },
                  {
                    key: 'errors',
                    label: (
                      <>
                        <WarningOutlined style={{ color: '#ff4d4f' }} /> 错误详情
                        {currentReport?.errorDetails && currentReport.errorDetails.length > 0 && (
                          <Tag color="error" style={{ marginLeft: 4 }}>
                            {currentReport.errorDetails.length}
                          </Tag>
                        )}
                      </>
                    ),
                    children: currentReport && (
                      <div>
                        {currentReport.errorDetails && currentReport.errorDetails.length > 0 ? (
                          <List
                            dataSource={currentReport.errorDetails}
                            renderItem={(item: any) => (
                              <List.Item key={item.errorType}>
                                <List.Item.Meta
                                  title={
                                    <Space>
                                      <Tag color="error">{item.errorType}</Tag>
                                      <span style={{ color: '#ff4d4f' }}>{item.count} 次</span>
                                    </Space>
                                  }
                                  description={
                                    <div>
                                      <div style={{ color: '#666' }}>{item.message || '无详细错误信息'}</div>
                                      {item.sampleStatusCodes && item.sampleStatusCodes.length > 0 && (
                                        <div style={{ marginTop: 4 }}>
                                          <Text type="secondary">状态码示例: </Text>
                                          {item.sampleStatusCodes.map((code: number, i: number) => (
                                            <Tag key={i} color="default">{code}</Tag>
                                          ))}
                                        </div>
                                      )}
                                    </div>
                                  }
                                />
                              </List.Item>
                            )}
                          />
                        ) : (
                          <Empty
                            description="没有错误，所有请求都成功处理！"
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                            style={{ padding: '40px 0' }}
                          />
                        )}
                      </div>
                    ),
                  },
                  {
                    key: 'analysis',
                    label: <><InfoCircleOutlined /> 瓶颈分析</>,
                    children: currentReport?.bottleneckAnalysis && (
                      <div>
                        {currentReport.bottleneckAnalysis.warnings && currentReport.bottleneckAnalysis.warnings.length > 0 && (
                          <Card size="small" title="告警信息" style={{ marginBottom: 16 }}>
                            <List
                              dataSource={currentReport.bottleneckAnalysis.warnings}
                              renderItem={(item: string) => (
                                <List.Item>
                                  <WarningOutlined style={{ color: '#faad14', marginRight: 8 }} />
                                  {item}
                                </List.Item>
                              )}
                            />
                          </Card>
                        )}

                        {currentReport.bottleneckAnalysis.suggestions && currentReport.bottleneckAnalysis.suggestions.length > 0 && (
                          <Card size="small" title="优化建议">
                            <List
                              dataSource={currentReport.bottleneckAnalysis.suggestions}
                              renderItem={(item: string) => (
                                <List.Item>
                                  <InfoCircleOutlined style={{ color: '#1890ff', marginRight: 8 }} />
                                  {item}
                                </List.Item>
                              )}
                            />
                          </Card>
                        )}

                        {(!currentReport.bottleneckAnalysis.warnings || currentReport.bottleneckAnalysis.warnings.length === 0) &&
                         (!currentReport.bottleneckAnalysis.suggestions || currentReport.bottleneckAnalysis.suggestions.length === 0) && (
                          <Empty
                            description="性能表现优秀，没有发现明显瓶颈"
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                            style={{ padding: '40px 0' }}
                          />
                        )}
                      </div>
                    ),
                  },
                ]}
              />
            </div>
          )}
        </Card>
      </div>

      <Modal
        title="新建压力测试"
        open={configModalVisible}
        onCancel={() => { setConfigModalVisible(false); form.resetFields(); setTargetMode('url') }}
        footer={null}
        width={720}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleStart}
          initialValues={{
            httpMethod: 'GET',
            virtualUsers: 10,
            durationSeconds: 60,
            rampUpSeconds: 10,
            thinkTimeMs: 1000,
            timeoutMs: 30000,
            contentType: 'application/json',
          }}
        >
          <Row gutter={16}>
            <Col xs={24}>
              <Form.Item
                label="测试名称"
                name="testName"
                rules={[{ required: true, message: '请输入测试名称' }]}
              >
                <Input placeholder="如：首页接口压测" />
              </Form.Item>
            </Col>
          </Row>

          <Card
            size="small"
            title="目标设置"
            style={{ marginBottom: 16 }}
            extra={
              <Select
                size="small"
                value={targetMode}
                onChange={(val: any) => setTargetMode(val)}
                style={{ width: 120 }}
              >
                <Option value="url">手动输入URL</Option>
                <Option value="page">选择平台页面</Option>
              </Select>
            }
          >
            {targetMode === 'url' ? (
              <Form.Item
                label="目标URL"
                name="targetUrl"
                rules={[
                  { required: true, message: '请输入目标URL' },
                  { type: 'url', message: '请输入有效的URL' },
                ]}
                style={{ marginBottom: 0 }}
              >
                <Input placeholder="https://api.example.com/users" prefix={<GlobalOutlined />} />
              </Form.Item>
            ) : (
              <Form.Item
                label="选择页面"
                name="selectedPage"
                rules={[{ required: true, message: '请选择目标页面' }]}
                style={{ marginBottom: 0 }}
              >
                <Cascader
                  options={pageOptions}
                  placeholder="请选择应用和页面"
                  loadData={handlePageLoad}
                  loading={loadingPages}
                  expandTrigger="hover"
                  style={{ width: '100%' }}
                />
              </Form.Item>
            )}
          </Card>

          <Row gutter={16}>
            <Col xs={12} sm={8}>
              <Form.Item label="请求方法" name="httpMethod">
                <Select>
                  <Option value="GET">GET</Option>
                  <Option value="POST">POST</Option>
                  <Option value="PUT">PUT</Option>
                  <Option value="DELETE">DELETE</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col xs={12} sm={8}>
              <Form.Item
                label="虚拟用户数"
                name="virtualUsers"
                tooltip="同时模拟的并发用户数量（最大500）"
                rules={[
                  { required: true, message: '请输入虚拟用户数' },
                  { type: 'number', min: 1, max: 500, message: '1-500之间' },
                ]}
              >
                <InputNumber min={1} max={500} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={12} sm={8}>
              <Form.Item
                label="测试时长 (秒)"
                name="durationSeconds"
                tooltip="测试持续时间（最大3600秒=1小时）"
                rules={[
                  { required: true, message: '请输入测试时长' },
                  { type: 'number', min: 5, max: 3600, message: '5-3600秒之间' },
                ]}
              >
                <InputNumber min={5} max={3600} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={12} sm={8}>
              <Form.Item
                label="用户递增时长 (秒)"
                name="rampUpSeconds"
                tooltip="在指定时间内逐步将并发用户增加到目标值"
              >
                <InputNumber min={0} max={300} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={12} sm={8}>
              <Form.Item
                label="请求间隔 (毫秒)"
                name="thinkTimeMs"
                tooltip="每个虚拟用户两次请求之间的等待时间"
              >
                <InputNumber min={0} max={10000} step={100} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={12} sm={8}>
              <Form.Item
                label={<span><ClockCircleOutlined style={{ marginRight: 4 }} />超时时间 (毫秒)</span>}
                name="timeoutMs"
                tooltip="单个请求的连接超时和读取超时时间"
              >
                <InputNumber min={1000} max={300000} step={1000} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={12} sm={8}>
              <Form.Item
                label="Content-Type"
                name="contentType"
              >
                <Select>
                  <Option value="application/json">application/json</Option>
                  <Option value="application/x-www-form-urlencoded">x-www-form-urlencoded</Option>
                  <Option value="text/plain">text/plain</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Divider style={{ margin: '12px 0' }} />

          <Title level={5}>高级配置</Title>

          <Row gutter={16}>
            <Col xs={24}>
              <Tooltip title="每行一个Header，格式：Key: Value">
                <Form.Item label="请求头" name="headers" tooltip="每行一个Header，格式：Key: Value">
                  <Input.TextArea
                    rows={3}
                    placeholder={"Authorization: Bearer xxx\nX-Custom: value"}
                  />
                </Form.Item>
              </Tooltip>
            </Col>
            <Col xs={24}>
              <Form.Item label="请求体" name="requestBody">
                <Input.TextArea
                  rows={4}
                  placeholder='{"username": "test", "password": "123456"}'
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right', marginTop: 16 }}>
            <Space>
              <Button onClick={() => { setConfigModalVisible(false); form.resetFields(); setTargetMode('url') }}>取消</Button>
              <Button type="primary" htmlType="submit" icon={<PlayCircleOutlined />} loading={starting}>
                开始测试
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default LoadTestPage
