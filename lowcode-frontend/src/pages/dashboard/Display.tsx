import React, { useState, useEffect, useRef, useCallback } from 'react'
import {
  Button,
  Space,
  Tooltip,
  Drawer,
  Switch,
  Slider,
  Select,
  Tag,
  message,
  List,
  Modal,
  Form,
  Input,
  InputNumber,
  Divider,
} from 'antd'
import {
  FullscreenOutlined,
  FullscreenExitOutlined,
  ReloadOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  SettingOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined,
  ShareAltOutlined,
  DesktopOutlined,
  ClockCircleOutlined,
  EyeOutlined,
  CopyOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import BaseChart, { ChartType, ChartDataConfig, ChartStyleConfig } from '@/components/chart/BaseChart'
import IndicatorCard from '@/components/chart/IndicatorCard'
import { dashboardApi, screenApi, DashboardInfo, DashboardComponent, CarouselConfig } from '@/api/dashboard'
import dayjs from 'dayjs'
import { v4 as uuidv4 } from 'uuid'

const { Option } = Select

const DashboardDisplay: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const isShareMode = searchParams.get('share') === 'true'

  const [dashboard, setDashboard] = useState<DashboardInfo | null>(null)
  const [components, setComponents] = useState<DashboardComponent[]>([])
  const [loading, setLoading] = useState(false)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [showToolbar, setShowToolbar] = useState(true)
  const [settingsVisible, setSettingsVisible] = useState(false)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [refreshInterval, setRefreshInterval] = useState(30)
  const [carouselEnabled, setCarouselEnabled] = useState(false)
  const [carouselInterval, setCarouselInterval] = useState(10)
  const [currentPage, setCurrentPage] = useState(0)
  const [carouselPages, setCarouselPages] = useState<any[]>([])
  const [isPlaying, setIsPlaying] = useState(false)
  const [scale, setScale] = useState(1)
  const [showPageIndicator, setShowPageIndicator] = useState(true)
  const [shareModalVisible, setShareModalVisible] = useState(false)
  const [shareUrl, setShareUrl] = useState('')
  const [currentTime, setCurrentTime] = useState(dayjs().format('YYYY-MM-DD HH:mm:ss'))

  const containerRef = useRef<HTMLDivElement>(null)
  const toolbarTimerRef = useRef<any>(null)
  const carouselTimerRef = useRef<any>(null)
  const refreshTimerRef = useRef<any>(null)

  const mockData = [
    { category: '电子产品', month: '1月', sales: 12000, orders: 150, profit: 3600, target: 10000 },
    { category: '服装', month: '1月', sales: 8000, orders: 200, profit: 2400, target: 7000 },
    { category: '食品', month: '1月', sales: 5000, orders: 300, profit: 1500, target: 4500 },
    { category: '家居', month: '1月', sales: 6500, orders: 120, profit: 1950, target: 6000 },
    { category: '电子产品', month: '2月', sales: 15000, orders: 180, profit: 4500, target: 12000 },
    { category: '服装', month: '2月', sales: 9500, orders: 240, profit: 2850, target: 8000 },
    { category: '食品', month: '2月', sales: 6000, orders: 350, profit: 1800, target: 5000 },
    { category: '家居', month: '2月', sales: 7200, orders: 135, profit: 2160, target: 6500 },
  ]

  const mockComponents: DashboardComponent[] = [
    {
      id: uuidv4(),
      componentType: 'text',
      componentName: '大标题',
      positionX: 0,
      positionY: 10,
      width: 1920,
      height: 80,
      propsConfig: JSON.stringify({
        content: '运营数据大屏',
        fontSize: 48,
        fontWeight: 'bold',
        color: '#00e5ff',
        textAlign: 'center',
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 0,
    },
    {
      id: uuidv4(),
      componentType: 'indicator',
      componentName: '总销售额',
      positionX: 100,
      positionY: 120,
      width: 320,
      height: 140,
      propsConfig: JSON.stringify({
        title: '总销售额',
        value: 125680,
        unit: '元',
        trend: 'up',
        trendValue: 12.5,
        color: '#00e5ff',
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 1,
    },
    {
      id: uuidv4(),
      componentType: 'indicator',
      componentName: '订单总数',
      positionX: 460,
      positionY: 120,
      width: 320,
      height: 140,
      propsConfig: JSON.stringify({
        title: '订单总数',
        value: 3856,
        unit: '单',
        trend: 'up',
        trendValue: 8.3,
        color: '#52c41a',
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 2,
    },
    {
      id: uuidv4(),
      componentType: 'indicator',
      componentName: '用户数',
      positionX: 820,
      positionY: 120,
      width: 320,
      height: 140,
      propsConfig: JSON.stringify({
        title: '活跃用户',
        value: 12580,
        unit: '人',
        trend: 'up',
        trendValue: 15.2,
        color: '#722ed1',
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 3,
    },
    {
      id: uuidv4(),
      componentType: 'indicator',
      componentName: '转化率',
      positionX: 1180,
      positionY: 120,
      width: 320,
      height: 140,
      propsConfig: JSON.stringify({
        title: '转化率',
        value: 23.6,
        unit: '%',
        trend: 'down',
        trendValue: 2.1,
        color: '#faad14',
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 4,
    },
    {
      id: uuidv4(),
      componentType: 'indicator',
      componentName: '客单价',
      positionX: 1540,
      positionY: 120,
      width: 320,
      height: 140,
      propsConfig: JSON.stringify({
        title: '客单价',
        value: 325.8,
        unit: '元',
        trend: 'up',
        trendValue: 5.7,
        color: '#eb2f96',
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 5,
    },
    {
      id: uuidv4(),
      componentType: 'bar',
      componentName: '品类销售柱状图',
      positionX: 80,
      positionY: 300,
      width: 700,
      height: 400,
      propsConfig: JSON.stringify({
        title: '各品类销售对比',
        xField: 'category',
        yFields: ['sales', 'target'],
        seriesField: '',
        showLegend: true,
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 6,
    },
    {
      id: uuidv4(),
      componentType: 'line',
      componentName: '销售趋势折线图',
      positionX: 820,
      positionY: 300,
      width: 700,
      height: 400,
      propsConfig: JSON.stringify({
        title: '月度销售趋势',
        xField: 'month',
        yFields: ['sales', 'profit'],
        seriesField: 'category',
        showLegend: true,
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 7,
    },
    {
      id: uuidv4(),
      componentType: 'doughnut',
      componentName: '品类占比环形图',
      positionX: 1560,
      positionY: 300,
      width: 360,
      height: 400,
      propsConfig: JSON.stringify({
        title: '品类销售占比',
        categoryField: 'category',
        valueField: 'sales',
        showLegend: true,
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 8,
    },
    {
      id: uuidv4(),
      componentType: 'table',
      componentName: '销售明细表格',
      positionX: 80,
      positionY: 740,
      width: 1000,
      height: 300,
      propsConfig: JSON.stringify({
        title: '销售明细',
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 9,
    },
    {
      id: uuidv4(),
      componentType: 'gauge',
      componentName: '完成率仪表盘',
      positionX: 1120,
      positionY: 740,
      width: 350,
      height: 300,
      propsConfig: JSON.stringify({
        title: '目标完成率',
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 10,
    },
    {
      id: uuidv4(),
      componentType: 'radar',
      componentName: '能力雷达图',
      positionX: 1510,
      positionY: 740,
      width: 400,
      height: 300,
      propsConfig: JSON.stringify({
        title: '综合能力评估',
        categoryField: 'category',
        valueField: 'sales',
        showLegend: false,
      }),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      sortOrder: 11,
    },
  ]

  useEffect(() => {
    loadDashboard()
    setCurrentTime(dayjs().format('YYYY-MM-DD HH:mm:ss'))
    const timer = setInterval(() => {
      setCurrentTime(dayjs().format('YYYY-MM-DD HH:mm:ss'))
    }, 1000)
    return () => clearInterval(timer)
  }, [id])

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    try {
      const mockDashboard: DashboardInfo = {
        id: Number(id),
        appId: 1,
        dashboardName: '运营数据大屏',
        dashboardCode: 'operation_dashboard',
        description: '运营核心指标综合展示',
        status: 1,
        version: '1.0.0',
        width: 1920,
        height: 1080,
        backgroundColor: '#0a0e27',
        autoRefresh: true,
        refreshInterval: 30,
        components: mockComponents,
        carouselConfig: {
          enabled: false,
          pages: [],
          interval: 10,
          autoPlay: true,
          showIndicator: true,
          transitionEffect: 'fade',
        },
      }
      setDashboard(mockDashboard)
      setComponents(mockComponents)
      setAutoRefresh(mockDashboard.autoRefresh || false)
      setRefreshInterval(mockDashboard.refreshInterval || 30)

      const pages = [
        { id: '1', pageName: '总览页面', componentIds: mockComponents.map(c => c.id) },
        { id: '2', pageName: '销售页面', componentIds: mockComponents.filter(c => ['bar', 'line', 'doughnut'].includes(c.componentType)).map(c => c.id) },
        { id: '3', pageName: '指标页面', componentIds: mockComponents.filter(c => c.componentType === 'indicator').map(c => c.id) },
      ]
      setCarouselPages(pages)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    if (autoRefresh && refreshInterval > 0) {
      refreshTimerRef.current = setInterval(() => {
        refreshData()
      }, refreshInterval * 1000)
    }
    return () => {
      if (refreshTimerRef.current) {
        clearInterval(refreshTimerRef.current)
      }
    }
  }, [autoRefresh, refreshInterval])

  useEffect(() => {
    if (carouselEnabled && isPlaying && carouselPages.length > 1) {
      carouselTimerRef.current = setInterval(() => {
        setCurrentPage(prev => (prev + 1) % carouselPages.length)
      }, carouselInterval * 1000)
    }
    return () => {
      if (carouselTimerRef.current) {
        clearInterval(carouselTimerRef.current)
      }
    }
  }, [carouselEnabled, isPlaying, carouselInterval, carouselPages.length])

  const refreshData = () => {
    setLoading(true)
    setTimeout(() => {
      setLoading(false)
    }, 500)
  }

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      containerRef.current?.requestFullscreen()
      setIsFullscreen(true)
    } else {
      document.exitFullscreen()
      setIsFullscreen(false)
    }
  }

  const handleMouseMove = () => {
    setShowToolbar(true)
    if (toolbarTimerRef.current) {
      clearTimeout(toolbarTimerRef.current)
    }
    toolbarTimerRef.current = setTimeout(() => {
      if (isFullscreen) {
        setShowToolbar(false)
      }
    }, 3000)
  }

  const handleShare = async () => {
    try {
      const url = `${window.location.origin}/screen/display/${id}?share=true&code=${Date.now()}`
      setShareUrl(url)
      setShareModalVisible(true)
    } catch (e) {
      console.error(e)
    }
  }

  const handlePrevPage = () => {
    setCurrentPage(prev => (prev - 1 + carouselPages.length) % carouselPages.length)
  }

  const handleNextPage = () => {
    setCurrentPage(prev => (prev + 1) % carouselPages.length)
  }

  const togglePlay = () => {
    setIsPlaying(prev => !prev)
  }

  const getVisibleComponents = () => {
    if (!carouselEnabled || carouselPages.length === 0) {
      return components
    }
    const currentPageConfig = carouselPages[currentPage]
    if (!currentPageConfig) return components
    return components.filter(c => currentPageConfig.componentIds.includes(c.id))
  }

  const visibleComponents = getVisibleComponents()

  const renderComponent = (comp: DashboardComponent) => {
    const props = comp.propsConfig ? JSON.parse(comp.propsConfig) : {}

    const chartTypes = ['line', 'bar', 'pie', 'doughnut', 'area', 'barStack', 'lineBar', 'radar', 'gauge', 'funnel', 'heatmap', 'scatter', 'chart']
    if (chartTypes.includes(comp.componentType)) {
      const chartType = (comp.componentType === 'chart' ? 'bar' : comp.componentType) as ChartType
      const dataConfig: ChartDataConfig = {
        xField: props.xField || 'category',
        yField: props.yFields || ['sales'],
        seriesField: props.seriesField,
        valueField: props.valueField || 'sales',
        categoryField: props.categoryField || 'category',
      }
      const styleConfig: ChartStyleConfig = {
        theme: 'dark',
        title: props.title ? { text: props.title, textStyle: { color: '#00e5ff', fontSize: 16 } } : undefined,
        legend: { show: props.showLegend !== false, position: 'top', textStyle: { color: '#ccc', fontSize: 12 } },
        tooltip: { show: true, trigger: chartType === 'pie' || chartType === 'doughnut' ? 'item' : 'axis' },
        animation: props.animation !== false,
      }
      return (
        <BaseChart
          type={chartType}
          data={mockData}
          dataConfig={dataConfig}
          styleConfig={styleConfig}
          height="100%"
        />
      )
    }

    if (comp.componentType === 'indicator') {
      return (
        <IndicatorCard
          title={props.title || '指标名称'}
          value={props.value || 0}
          unit={props.unit}
          trend={props.trend}
          trendValue={props.trendValue}
          color={props.color || '#00e5ff'}
          theme="dark"
          size="large"
          style={{ height: '100%' }}
        />
      )
    }

    if (comp.componentType === 'text') {
      return (
        <div style={{
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: props.textAlign === 'center' ? 'center' : props.textAlign === 'right' ? 'flex-end' : 'flex-start',
          fontSize: props.fontSize || 16,
          fontWeight: props.fontWeight || 'normal',
          color: props.color || '#fff',
        }}>
          {props.content || '文本内容'}
        </div>
      )
    }

    if (comp.componentType === 'table') {
      return (
        <div style={{
          height: '100%',
          background: 'rgba(0, 229, 255, 0.05)',
          border: '1px solid rgba(0, 229, 255, 0.2)',
          borderRadius: 4,
          padding: 16,
          overflow: 'auto',
        }}>
          <div style={{ color: '#00e5ff', marginBottom: 12, fontSize: 16, fontWeight: 500 }}>
            {props.title || '数据表格'}
          </div>
          <table style={{ width: '100%', color: '#ccc', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(0, 229, 255, 0.2)' }}>
                <th style={{ textAlign: 'left', padding: '8px 12px' }}>品类</th>
                <th style={{ textAlign: 'right', padding: '8px 12px' }}>销售额</th>
                <th style={{ textAlign: 'right', padding: '8px 12px' }}>订单数</th>
                <th style={{ textAlign: 'right', padding: '8px 12px' }}>利润</th>
                <th style={{ textAlign: 'right', padding: '8px 12px' }}>目标</th>
              </tr>
            </thead>
            <tbody>
              {mockData.slice(0, 6).map((row, idx) => (
                <tr key={idx} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                  <td style={{ padding: '8px 12px' }}>{row.category}</td>
                  <td style={{ textAlign: 'right', padding: '8px 12px', color: '#00e5ff' }}>¥{row.sales.toLocaleString()}</td>
                  <td style={{ textAlign: 'right', padding: '8px 12px' }}>{row.orders}</td>
                  <td style={{ textAlign: 'right', padding: '8px 12px', color: '#52c41a' }}>¥{row.profit.toLocaleString()}</td>
                  <td style={{ textAlign: 'right', padding: '8px 12px' }}>¥{row.target.toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )
    }

    return (
      <div style={{ padding: 16, color: '#666', textAlign: 'center' }}>
        {comp.componentName}
      </div>
    )
  }

  if (!dashboard) {
    return (
      <div style={{
        width: '100vw',
        height: '100vh',
        background: '#0a0e27',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#00e5ff',
      }}>
        加载中...
      </div>
    )
  }

  return (
    <div
      ref={containerRef}
      style={{
        width: '100vw',
        height: '100vh',
        background: `linear-gradient(135deg, ${dashboard.backgroundColor} 0%, #1a1f3a 50%, ${dashboard.backgroundColor} 100%)`,
        overflow: 'hidden',
        position: 'relative',
      }}
      onMouseMove={handleMouseMove}
    >
      <div
        style={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: `translate(-50%, -50%) scale(${scale})`,
          transformOrigin: 'center center',
          width: dashboard.width,
          height: dashboard.height,
        }}
      >
        {visibleComponents.map(comp => (
          <div
            key={comp.id}
            style={{
              position: 'absolute',
              left: comp.positionX,
              top: comp.positionY,
              width: comp.width,
              height: comp.height,
              zIndex: comp.zIndex || 1,
            }}
          >
            {renderComponent(comp)}
          </div>
        ))}
      </div>

      <div
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          padding: '16px 24px',
          background: 'linear-gradient(180deg, rgba(0,0,0,0.6) 0%, transparent 100%)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          opacity: showToolbar ? 1 : 0,
          transition: 'opacity 0.3s',
          zIndex: 1000,
        }}
      >
        <Space>
          {!isShareMode && (
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/screen')}
              style={{ background: 'rgba(0,0,0,0.5)', color: '#00e5ff', borderColor: 'rgba(0, 229, 255, 0.3)' }}
            >
              返回
            </Button>
          )}
          <Tag color="cyan" style={{ marginLeft: 8 }}>
            <EyeOutlined /> {dashboard.dashboardName}
          </Tag>
          {autoRefresh && (
            <Tag color="green">
              <ReloadOutlined spin={loading} /> {refreshInterval}s 自动刷新
            </Tag>
          )}
        </Space>
        <Space>
          <Tag color="default" style={{ marginRight: 8, fontFamily: 'monospace' }}>
            <ClockCircleOutlined /> {currentTime}
          </Tag>
          {carouselEnabled && carouselPages.length > 1 && (
            <>
              <Button
                icon={<ArrowLeftOutlined />}
                onClick={handlePrevPage}
                style={{ background: 'rgba(0,0,0,0.5)', color: '#00e5ff', borderColor: 'rgba(0, 229, 255, 0.3)' }}
              />
              <Button
                icon={isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                onClick={togglePlay}
                style={{ background: 'rgba(0,0,0,0.5)', color: isPlaying ? '#faad14' : '#52c41a', borderColor: 'rgba(0, 229, 255, 0.3)' }}
              >
                {isPlaying ? '暂停' : '播放'}
              </Button>
              <Button
                icon={<ArrowRightOutlined />}
                onClick={handleNextPage}
                style={{ background: 'rgba(0,0,0,0.5)', color: '#00e5ff', borderColor: 'rgba(0, 229, 255, 0.3)' }}
              />
              <Tag color="blue">
                {currentPage + 1} / {carouselPages.length}
              </Tag>
            </>
          )}
          <Button
            icon={<ReloadOutlined spin={loading} />}
            onClick={refreshData}
            style={{ background: 'rgba(0,0,0,0.5)', color: '#00e5ff', borderColor: 'rgba(0, 229, 255, 0.3)' }}
          >
            刷新
          </Button>
          <Button
            icon={<ShareAltOutlined />}
            onClick={handleShare}
            style={{ background: 'rgba(0,0,0,0.5)', color: '#00e5ff', borderColor: 'rgba(0, 229, 255, 0.3)' }}
          >
            分享
          </Button>
          <Button
            icon={<SettingOutlined />}
            onClick={() => setSettingsVisible(true)}
            style={{ background: 'rgba(0,0,0,0.5)', color: '#00e5ff', borderColor: 'rgba(0, 229, 255, 0.3)' }}
          >
            设置
          </Button>
          <Button
            type="primary"
            icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
            onClick={toggleFullscreen}
            style={{ background: '#00e5ff', borderColor: '#00e5ff' }}
          >
            {isFullscreen ? '退出全屏' : '全屏'}
          </Button>
        </Space>
      </div>

      {carouselEnabled && showPageIndicator && carouselPages.length > 1 && (
        <div
          style={{
            position: 'fixed',
            bottom: 30,
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            gap: 12,
            zIndex: 1000,
            opacity: showToolbar ? 1 : 0,
            transition: 'opacity 0.3s',
          }}
        >
          {carouselPages.map((page, idx) => (
            <div
              key={page.id}
              onClick={() => setCurrentPage(idx)}
              style={{
                width: idx === currentPage ? 32 : 12,
                height: 12,
                borderRadius: 6,
                background: idx === currentPage ? '#00e5ff' : 'rgba(255,255,255,0.3)',
                cursor: 'pointer',
                transition: 'all 0.3s',
                boxShadow: idx === currentPage ? '0 0 10px rgba(0, 229, 255, 0.5)' : 'none',
              }}
              title={page.pageName}
            />
          ))}
        </div>
      )}

      <Drawer
        title="显示设置"
        placement="right"
        width={320}
        open={settingsVisible}
        onClose={() => setSettingsVisible(false)}
        styles={{ body: { padding: 16 } }}
      >
        <div style={{ marginBottom: 20 }}>
          <div style={{ color: '#00e5ff', marginBottom: 12, fontWeight: 500 }}>
            <ReloadOutlined /> 自动刷新
          </div>
          <Form layout="vertical" size="small">
            <Form.Item label="启用自动刷新" valuePropName="checked">
              <Switch checked={autoRefresh} onChange={setAutoRefresh} />
            </Form.Item>
            {autoRefresh && (
              <Form.Item label="刷新间隔(秒)">
                <InputNumber 
                  style={{ width: '100%' }} 
                  min={1} 
                  max={3600}
                  value={refreshInterval}
                  onChange={(val) => setRefreshInterval(val || 30)}
                />
              </Form.Item>
            )}
          </Form>
        </div>

        <Divider style={{ borderColor: 'rgba(0, 229, 255, 0.1)', margin: '16px 0' }} />

        <div style={{ marginBottom: 20 }}>
          <div style={{ color: '#00e5ff', marginBottom: 12, fontWeight: 500 }}>
            <PlayCircleOutlined /> 轮播设置
          </div>
          <Form layout="vertical" size="small">
            <Form.Item label="启用页面轮播" valuePropName="checked">
              <Switch checked={carouselEnabled} onChange={setCarouselEnabled} />
            </Form.Item>
            {carouselEnabled && (
              <>
                <Form.Item label="轮播间隔(秒)">
                  <InputNumber 
                    style={{ width: '100%' }} 
                    min={1} 
                    max={300}
                    value={carouselInterval}
                    onChange={(val) => setCarouselInterval(val || 10)}
                  />
                </Form.Item>
                <Form.Item label="显示页面指示器" valuePropName="checked">
                  <Switch checked={showPageIndicator} onChange={setShowPageIndicator} />
                </Form.Item>
                <Form.Item label="切换效果">
                  <Select defaultValue="fade">
                    <Option value="fade">淡入淡出</Option>
                    <Option value="slide">滑动</Option>
                    <Option value="zoom">缩放</Option>
                  </Select>
                </Form.Item>
              </>
            )}
          </Form>
        </div>

        <Divider style={{ borderColor: 'rgba(0, 229, 255, 0.1)', margin: '16px 0' }} />

        <div>
          <div style={{ color: '#00e5ff', marginBottom: 12, fontWeight: 500 }}>
            <DesktopOutlined /> 显示缩放
          </div>
          <Slider
            min={0.5}
            max={2}
            step={0.05}
            value={scale}
            onChange={setScale}
            tooltip={{ formatter: (val) => `${Math.round(val! * 100)}%` }}
          />
          <div style={{ textAlign: 'center', color: '#888', fontSize: 12 }}>
            {Math.round(scale * 100)}%
          </div>
        </div>

        {carouselEnabled && carouselPages.length > 0 && (
          <>
            <Divider style={{ borderColor: 'rgba(0, 229, 255, 0.1)', margin: '16px 0' }} />
            <div>
              <div style={{ color: '#00e5ff', marginBottom: 12, fontWeight: 500 }}>
                轮播页面列表
              </div>
              <List
                size="small"
                dataSource={carouselPages}
                renderItem={(page, idx) => (
                  <List.Item
                    style={{
                      background: idx === currentPage ? 'rgba(0, 229, 255, 0.1)' : 'transparent',
                      borderRadius: 4,
                      cursor: 'pointer',
                    }}
                    onClick={() => setCurrentPage(idx)}
                  >
                    <Space>
                      <Tag color={idx === currentPage ? 'cyan' : 'default'}>{idx + 1}</Tag>
                      <span>{page.pageName}</span>
                    </Space>
                  </List.Item>
                )}
              />
            </div>
          </>
        )}
      </Drawer>

      <Modal
        title="分享大屏"
        open={shareModalVisible}
        onCancel={() => setShareModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setShareModalVisible(false)}>
            关闭
          </Button>,
        ]}
        centered
      >
        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 8, color: '#666' }}>分享链接</div>
          <Input.TextArea 
            value={shareUrl} 
            readOnly 
            rows={2}
            style={{ fontFamily: 'monospace', fontSize: 12 }}
          />
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Button 
            type="primary" 
            icon={<CopyOutlined />}
            onClick={() => {
              navigator.clipboard?.writeText(shareUrl)
              message.success('链接已复制')
            }}
          >
            复制链接
          </Button>
        </div>
      </Modal>

      {loading && (
        <div style={{
          position: 'fixed',
          top: 80,
          right: 24,
          zIndex: 2000,
        }}>
          <Tag color="cyan" style={{ padding: '4px 12px' }}>
            <ReloadOutlined spin /> 数据刷新中...
          </Tag>
        </div>
      )}
    </div>
  )
}

export default DashboardDisplay
