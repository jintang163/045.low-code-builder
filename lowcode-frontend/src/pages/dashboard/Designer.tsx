import React, { useState, useEffect, useCallback, useRef } from 'react'
import {
  Layout,
  Button,
  Space,
  Form,
  Input,
  Select,
  Tabs,
  message,
  Drawer,
  Collapse,
  Divider,
  Switch,
  InputNumber,
  ColorPicker,
  Card,
  Tag,
  Slider,
  Modal,
  List,
  Empty,
  Tooltip,
  Popconfirm,
  Row,
  Col,
} from 'antd'
import {
  SaveOutlined,
  PlusOutlined,
  DeleteOutlined,
  ArrowLeftOutlined,
  EyeOutlined,
  SettingOutlined,
  FullscreenOutlined,
  ReloadOutlined,
  BgColorsOutlined,
  FontColorsOutlined,
  PlayCircleOutlined,
  AppstoreOutlined,
  BarChartOutlined,
  LineChartOutlined,
  PieChartOutlined,
  FundOutlined,
  DashboardOutlined,
  TableOutlined,
  FileTextOutlined,
  PictureOutlined,
  EnvironmentOutlined,
  ClockCircleOutlined,
  OrderedListOutlined,
  CopyOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  LayoutOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { DndProvider, useDrag, useDrop } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'
import { DashboardInfo, DashboardComponent, dashboardApi, screenApi, CarouselConfig, CarouselPage } from '@/api/dashboard'
import { useAppStore } from '@/store/appStore'
import BaseChart, { ChartType, ChartDataConfig, ChartStyleConfig } from '@/components/chart/BaseChart'
import IndicatorCard from '@/components/chart/IndicatorCard'
import DataSourcePanel, { DataField } from '@/components/chart/DataSourcePanel'
import { v4 as uuidv4 } from 'uuid'
import dayjs from 'dayjs'

const { Header, Sider, Content } = Layout
const { TabPane } = Tabs
const { Panel } = Collapse
const { Option } = Select
const { TextArea } = Input

const dashboardThemes = [
  { name: '科技蓝', value: 'tech-blue', bgColor: '#0a0e27', textColor: '#00e5ff' },
  { name: '深邃黑', value: 'dark-black', bgColor: '#0d1117', textColor: '#58a6ff' },
  { name: '酷炫紫', value: 'cool-purple', bgColor: '#1a0a2e', textColor: '#a855f7' },
  { name: '军绿', value: 'military-green', bgColor: '#0f1f0f', textColor: '#22c55e' },
  { name: '暖色', value: 'warm-orange', bgColor: '#1a0f0a', textColor: '#f97316' },
]

const componentCategories = [
  { key: 'chart', name: '图表组件', icon: '📊' },
  { key: 'indicator', name: '指标卡片', icon: '📈' },
  { key: 'text', name: '文本媒体', icon: '📝' },
  { key: 'other', name: '其他组件', icon: '🔧' },
]

const chartComponents: { type: ChartType; name: string; icon: string }[] = [
  { type: 'line', name: '折线图', icon: '📈' },
  { type: 'bar', name: '柱状图', icon: '📊' },
  { type: 'pie', name: '饼图', icon: '🥧' },
  { type: 'doughnut', name: '环形图', icon: '🍩' },
  { type: 'area', name: '面积图', icon: '📉' },
  { type: 'barStack', name: '堆叠图', icon: '📚' },
  { type: 'lineBar', name: '组合图', icon: '📊📈' },
  { type: 'radar', name: '雷达图', icon: '🕸️' },
  { type: 'gauge', name: '仪表盘', icon: '⏱️' },
  { type: 'funnel', name: '漏斗图', icon: '🔻' },
  { type: 'heatmap', name: '热力图', icon: '🔥' },
  { type: 'scatter', name: '散点图', icon: '🔵' },
]

const ComponentPaletteItem: React.FC<{
  type: string
  name: string
  icon: string
  componentType: string
}> = ({ type, name, icon }) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'DASHBOARD_COMPONENT',
    item: {
      componentType: type,
      componentName: name,
      icon,
    },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  }))

  return (
    <div
      ref={drag}
      style={{
        padding: '10px 12px',
        background: 'rgba(0, 229, 255, 0.05)',
        border: '1px solid rgba(0, 229, 255, 0.2)',
        borderRadius: 4,
        marginBottom: 8,
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        opacity: isDragging ? 0.5 : 1,
        cursor: 'grab',
        fontSize: 13,
        color: '#ccc',
        transition: 'all 0.2s',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.background = 'rgba(0, 229, 255, 0.15)'
        e.currentTarget.style.borderColor = 'rgba(0, 229, 255, 0.5)'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = 'rgba(0, 229, 255, 0.05)'
        e.currentTarget.style.borderColor = 'rgba(0, 229, 255, 0.2)'
      }}
    >
      <span style={{ fontSize: 18 }}>{icon}</span>
      <span>{name}</span>
    </div>
  )
}

interface ResizableComponentProps {
  component: DashboardComponent
  isSelected: boolean
  onSelect: () => void
  onDelete: () => void
  onMove: (x: number, y: number) => void
  onResize: (width: number, height: number) => void
  dataFields: DataField[]
  previewData: any[]
  scale: number
}

const ResizableComponent: React.FC<ResizableComponentProps> = ({
  component,
  isSelected,
  onSelect,
  onDelete,
  onMove,
  onResize,
  dataFields,
  previewData,
  scale,
}) => {
  const [isDragging, setIsDragging] = useState(false)
  const [isResizing, setIsResizing] = useState(false)
  const [dragStart, setDragStart] = useState({ x: 0, y: 0, startX: 0, startY: 0 })
  const [resizeStart, setResizeStart] = useState({ startX: 0, startY: 0, startWidth: 0, startHeight: 0 })
  const componentRef = useRef<HTMLDivElement>(null)

  const props = component.propsConfig ? JSON.parse(component.propsConfig) : {}
  const dataSourceConfig = component.dataSourceConfig ? JSON.parse(component.dataSourceConfig) : {}

  const handleMouseDown = (e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest('.resize-handle')) return
    e.stopPropagation()
    setIsDragging(true)
    setDragStart({
      x: e.clientX,
      y: e.clientY,
      startX: component.positionX,
      startY: component.positionY,
    })
    onSelect()
  }

  const handleResizeMouseDown = (e: React.MouseEvent) => {
    e.stopPropagation()
    e.preventDefault()
    setIsResizing(true)
    setResizeStart({
      startX: e.clientX,
      startY: e.clientY,
      startWidth: component.width,
      startHeight: component.height,
    })
  }

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (isDragging) {
        const dx = (e.clientX - dragStart.x) / scale
        const dy = (e.clientY - dragStart.y) / scale
        onMove(
          Math.max(0, dragStart.startX + dx),
          Math.max(0, dragStart.startY + dy)
        )
      }
      if (isResizing) {
        const dx = (e.clientX - resizeStart.startX) / scale
        const dy = (e.clientY - resizeStart.startY) / scale
        onResize(
          Math.max(100, resizeStart.startWidth + dx),
          Math.max(80, resizeStart.startHeight + dy)
        )
      }
    }

    const handleMouseUp = () => {
      setIsDragging(false)
      setIsResizing(false)
    }

    if (isDragging || isResizing) {
      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)
      return () => {
        document.removeEventListener('mousemove', handleMouseMove)
        document.removeEventListener('mouseup', handleMouseUp)
      }
    }
  }, [isDragging, isResizing, dragStart, resizeStart, scale, onMove, onResize])

  const renderComponent = () => {
    const isChartType = chartComponents.some(c => c.type === component.componentType)

    if (isChartType || component.componentType === 'chart') {
      const chartType = (component.componentType === 'chart' ? 'bar' : component.componentType) as ChartType
      const dataConfig: ChartDataConfig = {
        xField: props.xField || dataFields.find(f => f.isDimension)?.fieldName,
        yField: props.yFields || dataFields.filter(f => f.isMeasure).map(f => f.fieldName),
        seriesField: props.seriesField,
        valueField: props.valueField || dataFields.find(f => f.isMeasure)?.fieldName,
        categoryField: props.categoryField || dataFields.find(f => f.isDimension)?.fieldName,
      }
      const styleConfig: ChartStyleConfig = {
        theme: 'dark',
        title: props.title ? { text: props.title, textStyle: { color: '#00e5ff', fontSize: 14 } } : undefined,
        legend: { show: props.showLegend !== false, position: 'top', textStyle: { color: '#ccc' } },
        tooltip: { show: true, trigger: chartType === 'pie' || chartType === 'doughnut' ? 'item' : 'axis' },
        animation: props.animation !== false,
      }
      return (
        <BaseChart
          type={chartType}
          data={previewData}
          dataConfig={dataConfig}
          styleConfig={styleConfig}
          height="100%"
        />
      )
    }

    if (component.componentType === 'indicator') {
      return (
        <IndicatorCard
          title={props.title || '指标名称'}
          value={props.value || 12345}
          unit={props.unit}
          trend={props.trend}
          trendValue={props.trendValue}
          icon={<DashboardOutlined />}
          color={props.color || '#00e5ff'}
          theme="dark"
          style={{ height: '100%' }}
        />
      )
    }

    if (component.componentType === 'text') {
      return (
        <div style={{
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: props.textAlign === 'center' ? 'center' : props.textAlign === 'right' ? 'flex-end' : 'flex-start',
          fontSize: props.fontSize || 16,
          fontWeight: props.fontWeight || 'normal',
          color: props.color || '#fff',
          padding: 16,
        }}>
          {props.content || '文本内容'}
        </div>
      )
    }

    if (component.componentType === 'table') {
      return (
        <div style={{ 
          padding: 12, 
          height: '100%', 
          background: 'rgba(0, 229, 255, 0.05)',
          borderRadius: 4,
          border: '1px solid rgba(0, 229, 255, 0.2)',
        }}>
          <div style={{ color: '#00e5ff', marginBottom: 8, fontSize: 14, fontWeight: 500 }}>
            {props.title || '数据表格'}
          </div>
          <div style={{ color: '#999', fontSize: 12 }}>表格组件内容...</div>
        </div>
      )
    }

    if (component.componentType === 'image') {
      return (
        <div style={{
          height: '100%',
          background: props.imageUrl ? `url(${props.imageUrl}) center/cover` : 'rgba(0, 229, 255, 0.1)',
          borderRadius: 4,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#666',
        }}>
          {!props.imageUrl && <PictureOutlined style={{ fontSize: 32, opacity: 0.5 }} />}
        </div>
      )
    }

    if (component.componentType === 'map') {
      return (
        <div style={{
          height: '100%',
          background: 'linear-gradient(135deg, rgba(0, 229, 255, 0.1) 0%, rgba(0, 100, 255, 0.1) 100%)',
          borderRadius: 4,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#00e5ff',
        }}>
          <div style={{ textAlign: 'center' }}>
            <EnvironmentOutlined style={{ fontSize: 48, opacity: 0.6 }} />
            <div style={{ marginTop: 8 }}>地图组件</div>
          </div>
        </div>
      )
    }

    return (
      <div style={{ padding: 16, color: '#999', textAlign: 'center' }}>
        {component.componentName}
      </div>
    )
  }

  return (
    <div
      ref={componentRef}
      style={{
        position: 'absolute',
        left: component.positionX,
        top: component.positionY,
        width: component.width,
        height: component.height,
        background: 'rgba(13, 17, 23, 0.8)',
        border: isSelected ? '2px solid #00e5ff' : '1px solid rgba(0, 229, 255, 0.2)',
        borderRadius: 4,
        boxShadow: isSelected ? '0 0 20px rgba(0, 229, 255, 0.3)' : 'none',
        cursor: isDragging ? 'grabbing' : 'grab',
        overflow: 'hidden',
        zIndex: isSelected ? 100 : 1,
      }}
      onMouseDown={handleMouseDown}
    >
      {isSelected && (
        <>
          <div style={{
            position: 'absolute',
            top: -24,
            left: 0,
            right: 0,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '0 4px',
            zIndex: 10,
          }}>
            <Tag color="cyan" style={{ margin: 0 }}>
              {component.componentName}
            </Tag>
            <Tooltip title="删除">
              <Button
                type="primary"
                danger
                size="small"
                icon={<DeleteOutlined />}
                onClick={(e) => { e.stopPropagation(); onDelete() }}
              />
            </Tooltip>
          </div>
          <div
            className="resize-handle"
            style={{
              position: 'absolute',
              right: -4,
              bottom: -4,
              width: 12,
              height: 12,
              background: '#00e5ff',
              borderRadius: '50%',
              cursor: 'nwse-resize',
              zIndex: 20,
              border: '2px solid #0a0e27',
            }}
            onMouseDown={handleResizeMouseDown}
          />
        </>
      )}
      {renderComponent()}
    </div>
  )
}

const DashboardDesigner: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [dashboard, setDashboard] = useState<DashboardInfo | null>(null)
  const [components, setComponents] = useState<DashboardComponent[]>([])
  const [selectedComponentId, setSelectedComponentId] = useState<string | null>(null)
  const [form] = Form.useForm()
  const [propsForm] = Form.useForm()
  const [styleForm] = Form.useForm()
  const [activeTab, setActiveTab] = useState('props')
  const [dataSourceDrawerVisible, setDataSourceDrawerVisible] = useState(false)
  const [dataFields, setDataFields] = useState<DataField[]>([])
  const [previewData, setPreviewData] = useState<any[]>([])
  const [canvasScale, setCanvasScale] = useState(0.5)
  const [backgroundColor, setBackgroundColor] = useState('#0a0e27')
  const [backgroundImage, setBackgroundImage] = useState('')
  const [theme, setTheme] = useState('tech-blue')
  const [autoRefresh, setAutoRefresh] = useState(false)
  const [refreshInterval, setRefreshInterval] = useState(30)
  const [carouselConfig, setCarouselConfig] = useState<CarouselConfig | null>(null)
  const [carouselDrawerVisible, setCarouselDrawerVisible] = useState(false)
  const [expandedCategory, setExpandedCategory] = useState<string[]>(['chart', 'indicator'])

  const canvasRef = useRef<HTMLDivElement>(null)

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

  useEffect(() => {
    setPreviewData(mockData)
    setDataFields([
      { fieldName: 'category', fieldLabel: '品类', fieldType: 'VARCHAR', isDimension: true, isMeasure: false, aggregation: 'none' },
      { fieldName: 'month', fieldLabel: '月份', fieldType: 'VARCHAR', isDimension: true, isMeasure: false, aggregation: 'none' },
      { fieldName: 'sales', fieldLabel: '销售额', fieldType: 'DECIMAL', isDimension: false, isMeasure: true, aggregation: 'sum' },
      { fieldName: 'orders', fieldLabel: '订单数', fieldType: 'INT', isDimension: false, isMeasure: true, aggregation: 'sum' },
      { fieldName: 'profit', fieldLabel: '利润', fieldType: 'DECIMAL', isDimension: false, isMeasure: true, aggregation: 'sum' },
      { fieldName: 'target', fieldLabel: '目标', fieldType: 'DECIMAL', isDimension: false, isMeasure: true, aggregation: 'sum' },
    ])
  }, [])

  useEffect(() => {
    if (id) {
      loadDashboard()
    } else {
      setDashboard({
        appId: currentApp?.id || 1,
        dashboardName: '新建大屏',
        dashboardCode: '',
        description: '',
        status: 0,
        width: 1920,
        height: 1080,
        backgroundColor: '#0a0e27',
        components: [],
        autoRefresh: false,
        refreshInterval: 60,
      })
      form.setFieldsValue({
        dashboardName: '新建大屏',
        dashboardCode: '',
        width: 1920,
        height: 1080,
      })
    }
  }, [id, currentApp])

  const loadDashboard = useCallback(async () => {
    try {
      const mockDashboard: DashboardInfo = {
        id: Number(id),
        appId: currentApp?.id || 1,
        dashboardName: '运营数据大屏',
        dashboardCode: 'operation_dashboard',
        description: '运营核心指标综合展示',
        status: 0,
        version: '1.0.0',
        width: 1920,
        height: 1080,
        backgroundColor: '#0a0e27',
        gridSize: 10,
        autoRefresh: true,
        refreshInterval: 30,
        components: [
          {
            id: uuidv4(),
            componentType: 'text',
            componentName: '大标题',
            positionX: 0,
            positionY: 10,
            width: 1920,
            height: 60,
            propsConfig: JSON.stringify({
              content: '运营数据大屏',
              fontSize: 36,
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
            positionX: 50,
            positionY: 100,
            width: 280,
            height: 120,
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
            positionX: 380,
            positionY: 100,
            width: 280,
            height: 120,
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
            positionX: 710,
            positionY: 100,
            width: 280,
            height: 120,
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
            positionX: 1040,
            positionY: 100,
            width: 280,
            height: 120,
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
            positionX: 1370,
            positionY: 100,
            width: 280,
            height: 120,
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
            positionX: 50,
            positionY: 260,
            width: 600,
            height: 350,
            propsConfig: JSON.stringify({
              title: '各品类销售对比',
              xField: 'category',
              yFields: ['sales', 'target'],
              seriesField: '',
              height: 300,
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
            positionX: 700,
            positionY: 260,
            width: 600,
            height: 350,
            propsConfig: JSON.stringify({
              title: '月度销售趋势',
              xField: 'month',
              yFields: ['sales', 'profit'],
              seriesField: 'category',
              height: 300,
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
            positionX: 1350,
            positionY: 260,
            width: 350,
            height: 350,
            propsConfig: JSON.stringify({
              title: '品类销售占比',
              categoryField: 'category',
              valueField: 'sales',
              height: 300,
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
            positionX: 50,
            positionY: 650,
            width: 900,
            height: 350,
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
            positionX: 1000,
            positionY: 650,
            width: 300,
            height: 350,
            propsConfig: JSON.stringify({
              title: '目标完成率',
              valueField: 'sales',
            }),
            dataSourceConfig: JSON.stringify({}),
            styleConfig: JSON.stringify({}),
            sortOrder: 10,
          },
          {
            id: uuidv4(),
            componentType: 'radar',
            componentName: '能力雷达图',
            positionX: 1350,
            positionY: 650,
            width: 350,
            height: 350,
            propsConfig: JSON.stringify({
              title: '综合能力评估',
              categoryField: 'category',
              valueField: 'sales',
              seriesField: '',
              height: 300,
              showLegend: false,
            }),
            dataSourceConfig: JSON.stringify({}),
            styleConfig: JSON.stringify({}),
            sortOrder: 11,
          },
        ],
      }
      setDashboard(mockDashboard)
      setComponents(mockDashboard.components || [])
      setBackgroundColor(mockDashboard.backgroundColor || '#0a0e27')
      setAutoRefresh(mockDashboard.autoRefresh || false)
      setRefreshInterval(mockDashboard.refreshInterval || 60)
      form.setFieldsValue({
        dashboardName: mockDashboard.dashboardName,
        dashboardCode: mockDashboard.dashboardCode,
        width: mockDashboard.width,
        height: mockDashboard.height,
        description: mockDashboard.description,
      })
    } catch (e) {
      console.error(e)
    }
  }, [id, currentApp])

  const [{ isOver: isCanvasOver }, dropCanvas] = useDrop(() => ({
    accept: 'DASHBOARD_COMPONENT',
    drop: (item: any, monitor) => {
      const offset = monitor.getSourceClientOffset()
      const canvasRect = canvasRef.current?.getBoundingClientRect()
      if (offset && canvasRect) {
        const x = (offset.x - canvasRect.left) / canvasScale
        const y = (offset.y - canvasRect.top) / canvasScale
        handleAddComponent(item, x, y)
      }
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  }))

  const handleAddComponent = (item: any, x: number, y: number) => {
    const defaultWidth = item.componentType === 'indicator' ? 250 : 400
    const defaultHeight = item.componentType === 'indicator' ? 100 : 300

    const newComponent: DashboardComponent = {
      id: uuidv4(),
      componentType: item.componentType,
      componentName: item.componentName,
      positionX: Math.max(0, x - defaultWidth / 2),
      positionY: Math.max(0, y - defaultHeight / 2),
      width: defaultWidth,
      height: defaultHeight,
      propsConfig: JSON.stringify(getDefaultProps(item.componentType)),
      dataSourceConfig: JSON.stringify({}),
      styleConfig: JSON.stringify({}),
      zIndex: components.length + 1,
      sortOrder: components.length,
    }

    setComponents([...components, newComponent])
    setSelectedComponentId(newComponent.id)
  }

  const getDefaultProps = (componentType: string) => {
    const dimensionField = dataFields.find(f => f.isDimension)?.fieldName || 'category'
    const measureFields = dataFields.filter(f => f.isMeasure).map(f => f.fieldName)
    const firstMeasure = measureFields[0] || 'sales'

    switch (componentType) {
      case 'indicator':
        return {
          title: '指标名称',
          value: 0,
          unit: '',
          trend: 'none',
          trendValue: 0,
          color: '#00e5ff',
        }
      case 'text':
        return {
          content: '文本内容',
          fontSize: 16,
          fontWeight: 'normal',
          color: '#fff',
          textAlign: 'left',
        }
      case 'table':
        return {
          title: '数据表格',
          showHeader: true,
          stripe: true,
        }
      case 'image':
        return {
          imageUrl: '',
          fitMode: 'cover',
        }
      case 'map':
        return {
          mapType: 'china',
          showLabel: true,
        }
      default:
        return {
          title: componentType + '图表',
          xField: dimensionField,
          yFields: [firstMeasure],
          seriesField: '',
          valueField: firstMeasure,
          categoryField: dimensionField,
          showLegend: true,
          animation: true,
        }
    }
  }

  const handleComponentMove = (id: string, x: number, y: number) => {
    setComponents(components.map(c => {
      if (c.id === id) {
        return { ...c, positionX: Math.round(x), positionY: Math.round(y) }
      }
      return c
    }))
  }

  const handleComponentResize = (id: string, width: number, height: number) => {
    setComponents(components.map(c => {
      if (c.id === id) {
        return { ...c, width: Math.round(width), height: Math.round(height) }
      }
      return c
    }))
  }

  const handleComponentSelect = (id: string) => {
    setSelectedComponentId(id)
    const comp = components.find(c => c.id === id)
    if (comp) {
      const props = comp.propsConfig ? JSON.parse(comp.propsConfig) : {}
      const style = comp.styleConfig ? JSON.parse(comp.styleConfig) : {}
      propsForm.setFieldsValue(props)
      styleForm.setFieldsValue(style)
    }
  }

  const handleComponentDelete = (id: string) => {
    setComponents(components.filter(c => c.id !== id))
    if (selectedComponentId === id) {
      setSelectedComponentId(null)
    }
  }

  const handlePropsChange = (changedValues: any) => {
    if (!selectedComponentId) return
    setComponents(components.map(c => {
      if (c.id === selectedComponentId) {
        const currentProps = c.propsConfig ? JSON.parse(c.propsConfig) : {}
        return {
          ...c,
          propsConfig: JSON.stringify({ ...currentProps, ...changedValues }),
        }
      }
      return c
    }))
  }

  const handleSave = async () => {
    try {
      if (!dashboard) return
      const values = await form.validateFields()
      const savedDashboard = {
        ...dashboard,
        ...values,
        components,
        backgroundColor,
        autoRefresh,
        refreshInterval,
      }
      setDashboard(savedDashboard)
      message.success('保存成功')
    } catch (e) {
      console.error(e)
    }
  }

  const handleDisplay = () => {
    if (id) {
      navigate(`/screen/display/${id}`)
    }
  }

  const handleThemeChange = (themeValue: string) => {
    setTheme(themeValue)
    const themeConfig = dashboardThemes.find(t => t.value === themeValue)
    if (themeConfig) {
      setBackgroundColor(themeConfig.bgColor)
    }
  }

  const handleFieldsChange = (fields: DataField[]) => {
    setDataFields(fields)
  }

  const handleCopyComponent = () => {
    if (!selectedComponentId) return
    const comp = components.find(c => c.id === selectedComponentId)
    if (comp) {
      const newComp: DashboardComponent = {
        ...comp,
        id: uuidv4(),
        positionX: comp.positionX + 20,
        positionY: comp.positionY + 20,
        componentName: comp.componentName + ' - 副本',
      }
      setComponents([...components, newComp])
      setSelectedComponentId(newComp.id)
    }
  }

  const handleBringForward = () => {
    if (!selectedComponentId) return
    const comp = components.find(c => c.id === selectedComponentId)
    if (comp) {
      setComponents(components.map(c => {
        if (c.id === selectedComponentId) {
          return { ...c, zIndex: (c.zIndex || 0) + 1 }
        }
        return c
      }))
    }
  }

  const handleSendBackward = () => {
    if (!selectedComponentId) return
    const comp = components.find(c => c.id === selectedComponentId)
    if (comp) {
      setComponents(components.map(c => {
        if (c.id === selectedComponentId) {
          return { ...c, zIndex: Math.max(0, (c.zIndex || 0) - 1) }
        }
        return c
      }))
    }
  }

  const selectedComponent = components.find(c => c.id === selectedComponentId)

  const renderPropsPanel = () => {
    if (!selectedComponent) {
      return <Empty description="请选择组件" style={{ marginTop: 40 }} />
    }

    const isChart = chartComponents.some(c => c.type === selectedComponent.componentType) || selectedComponent.componentType === 'chart'

    return (
      <Form
        form={propsForm}
        layout="vertical"
        size="small"
        onValuesChange={handlePropsChange}
      >
        <Form.Item name="title" label="标题">
          <Input />
        </Form.Item>

        {isChart && (
          <>
            {['line', 'bar', 'area', 'barStack', 'lineBar'].includes(selectedComponent.componentType) && (
              <>
                <Form.Item name="xField" label="X轴字段">
                  <Select>
                    {dataFields.filter(f => f.isDimension).map(f => (
                      <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                    ))}
                  </Select>
                </Form.Item>
                <Form.Item name="yFields" label="Y轴字段">
                  <Select mode="multiple">
                    {dataFields.filter(f => f.isMeasure).map(f => (
                      <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                    ))}
                  </Select>
                </Form.Item>
                <Form.Item name="seriesField" label="系列字段">
                  <Select allowClear>
                    {dataFields.filter(f => f.isDimension).map(f => (
                      <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                    ))}
                  </Select>
                </Form.Item>
              </>
            )}
            {['pie', 'doughnut', 'funnel'].includes(selectedComponent.componentType) && (
              <>
                <Form.Item name="categoryField" label="分类字段">
                  <Select>
                    {dataFields.filter(f => f.isDimension).map(f => (
                      <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                    ))}
                  </Select>
                </Form.Item>
                <Form.Item name="valueField" label="数值字段">
                  <Select>
                    {dataFields.filter(f => f.isMeasure).map(f => (
                      <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                    ))}
                  </Select>
                </Form.Item>
              </>
            )}
            <Form.Item name="showLegend" label="显示图例" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="animation" label="启用动画" valuePropName="checked">
              <Switch />
            </Form.Item>
          </>
        )}

        {selectedComponent.componentType === 'indicator' && (
          <>
            <Form.Item name="value" label="显示值">
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="unit" label="单位">
              <Input placeholder="例如：元、个、%" />
            </Form.Item>
            <Form.Item name="trend" label="趋势">
              <Select>
                <Option value="up">上升</Option>
                <Option value="down">下降</Option>
                <Option value="none">无</Option>
              </Select>
            </Form.Item>
            <Form.Item name="trendValue" label="趋势值">
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="color" label="主题色">
              <Input type="color" style={{ height: 32 }} />
            </Form.Item>
          </>
        )}

        {selectedComponent.componentType === 'text' && (
          <>
            <Form.Item name="content" label="文本内容">
              <TextArea rows={3} />
            </Form.Item>
            <Form.Item name="fontSize" label="字号">
              <InputNumber style={{ width: '100%' }} min={12} max={100} />
            </Form.Item>
            <Form.Item name="fontWeight" label="字重">
              <Select>
                <Option value="normal">正常</Option>
                <Option value="bold">加粗</Option>
              </Select>
            </Form.Item>
            <Form.Item name="textAlign" label="对齐方式">
              <Select>
                <Option value="left">左对齐</Option>
                <Option value="center">居中</Option>
                <Option value="right">右对齐</Option>
              </Select>
            </Form.Item>
            <Form.Item name="color" label="文字颜色">
              <Input type="color" style={{ height: 32 }} />
            </Form.Item>
          </>
        )}

        <Divider style={{ margin: '12px 0' }} />
        <Form.Item label="位置和大小">
          <Row gutter={8}>
            <Col span={12}>
              <Form.Item label="X坐标" style={{ marginBottom: 8 }}>
                <InputNumber style={{ width: '100%' }} value={selectedComponent.positionX}
                  onChange={(val) => handleComponentMove(selectedComponent.id, val || 0, selectedComponent.positionY)} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="Y坐标" style={{ marginBottom: 8 }}>
                <InputNumber style={{ width: '100%' }} value={selectedComponent.positionY}
                  onChange={(val) => handleComponentMove(selectedComponent.id, selectedComponent.positionX, val || 0)} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={8}>
            <Col span={12}>
              <Form.Item label="宽度" style={{ marginBottom: 8 }}>
                <InputNumber style={{ width: '100%' }} value={selectedComponent.width}
                  onChange={(val) => handleComponentResize(selectedComponent.id, val || 100, selectedComponent.height)} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="高度" style={{ marginBottom: 8 }}>
                <InputNumber style={{ width: '100%' }} value={selectedComponent.height}
                  onChange={(val) => handleComponentResize(selectedComponent.id, selectedComponent.width, val || 80)} />
              </Form.Item>
            </Col>
          </Row>
        </Form.Item>

        <Divider style={{ margin: '12px 0' }} />
        <Space wrap>
          <Button size="small" icon={<CopyOutlined />} onClick={handleCopyComponent}>
            复制
          </Button>
          <Button size="small" icon={<ArrowUpOutlined />} onClick={handleBringForward}>
            上移一层
          </Button>
          <Button size="small" icon={<ArrowDownOutlined />} onClick={handleSendBackward}>
            下移一层
          </Button>
          <Popconfirm title="确定删除?" onConfirm={() => handleComponentDelete(selectedComponentId!)}>
            <Button size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      </Form>
    )
  }

  return (
    <DndProvider backend={HTML5Backend}>
      <Layout style={{ height: '100vh', background: '#0a0e27' }}>
        <Header style={{
          background: 'linear-gradient(90deg, #0d1117 0%, #161b2d 50%, #0d1117 100%)',
          borderBottom: '1px solid rgba(0, 229, 255, 0.2)',
          padding: '0 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          height: 56,
        }}>
          <Space>
            <Button 
              icon={<ArrowLeftOutlined />} 
              onClick={() => navigate('/screen')}
              style={{ background: 'transparent', color: '#00e5ff', borderColor: 'rgba(0, 229, 255, 0.3)' }}
            >
              返回
            </Button>
            <div style={{ borderLeft: '1px solid rgba(0, 229, 255, 0.2)', height: 24, margin: '0 8px' }} />
            <h3 style={{ margin: 0, color: '#00e5ff' }}>{dashboard?.dashboardName || '大屏设计器'}</h3>
            <Tag color="cyan">
              {dashboard?.width} × {dashboard?.height}
            </Tag>
            {autoRefresh && (
              <Tag color="green">
                <ReloadOutlined spin /> {refreshInterval}s 刷新
              </Tag>
            )}
          </Space>
          <Space>
            <Button 
              icon={<EyeOutlined />} 
              onClick={handleDisplay}
              style={{ background: 'transparent', color: '#00e5ff', borderColor: 'rgba(0, 229, 255, 0.3)' }}
            >
              预览
            </Button>
            <Button 
              type="primary" 
              icon={<SaveOutlined />} 
              onClick={handleSave}
              style={{ background: '#00e5ff', borderColor: '#00e5ff' }}
            >
              保存
            </Button>
          </Space>
        </Header>

        <Layout>
          <Sider 
            width={240} 
            style={{ 
              background: '#0d1117', 
              borderRight: '1px solid rgba(0, 229, 255, 0.2)',
              overflow: 'auto',
            }}
            theme="dark"
          >
            <div style={{ padding: 12 }}>
              <div style={{ 
                fontWeight: 500, 
                marginBottom: 12, 
                display: 'flex', 
                alignItems: 'center', 
                gap: 8,
                color: '#00e5ff',
              }}>
                <AppstoreOutlined />
                组件库
              </div>
              <Collapse
                activeKey={expandedCategory}
                onChange={(keys) => setExpandedCategory(keys as string[])}
                ghost
                size="small"
                defaultActiveKey={['chart', 'indicator']}
              >
                <Panel header="图表组件" key="chart">
                  {chartComponents.map(chart => (
                    <ComponentPaletteItem
                      key={chart.type}
                      type={chart.type}
                      name={chart.name}
                      icon={chart.icon}
                      componentType={chart.type}
                    />
                  ))}
                </Panel>
                <Panel header="指标卡片" key="indicator">
                  <ComponentPaletteItem type="indicator" name="数字指标" icon="📊" componentType="indicator" />
                </Panel>
                <Panel header="文本媒体" key="text">
                  <ComponentPaletteItem type="text" name="文本" icon="📝" componentType="text" />
                  <ComponentPaletteItem type="image" name="图片" icon="🖼️" componentType="image" />
                </Panel>
                <Panel header="其他组件" key="other">
                  <ComponentPaletteItem type="table" name="数据表格" icon="📋" componentType="table" />
                  <ComponentPaletteItem type="map" name="地图" icon="🗺️" componentType="map" />
                </Panel>
              </Collapse>
            </div>
          </Sider>

          <Content
            style={{
              padding: 20,
              overflow: 'auto',
              background: '#161b2d',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
            onClick={() => setSelectedComponentId(null)}
          >
            <div style={{ position: 'relative' }}>
              <div style={{
                position: 'absolute',
                top: -40,
                left: 0,
                right: 0,
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                color: '#888',
                fontSize: 12,
              }}>
                <span>缩放: {Math.round(canvasScale * 100)}%</span>
                <Space>
                  <Button size="small" onClick={() => setCanvasScale(s => Math.max(0.1, s - 0.1))}>-</Button>
                  <Slider 
                    style={{ width: 120 }} 
                    min={0.1} 
                    max={1} 
                    step={0.05} 
                    value={canvasScale}
                    onChange={setCanvasScale}
                  />
                  <Button size="small" onClick={() => setCanvasScale(s => Math.min(1, s + 0.1))}>+</Button>
                  <Button size="small" onClick={() => setCanvasScale(0.5)}>适应</Button>
                </Space>
              </div>
              <div
                ref={(node) => { dropCanvas(node); canvasRef.current = node }}
                style={{
                  width: dashboard?.width || 1920,
                  height: dashboard?.height || 1080,
                  background: `linear-gradient(135deg, ${backgroundColor} 0%, #1a1f3a 50%, ${backgroundColor} 100%)`,
                  backgroundImage: backgroundImage ? `url(${backgroundImage})` : undefined,
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                  position: 'relative',
                  transform: `scale(${canvasScale})`,
                  transformOrigin: 'top left',
                  boxShadow: '0 0 40px rgba(0, 229, 255, 0.2)',
                  border: isCanvasOver ? '2px dashed #00e5ff' : '1px solid rgba(0, 229, 255, 0.3)',
                  borderRadius: 4,
                }}
                onClick={(e) => e.stopPropagation()}
              >
                {components.map(comp => (
                  <ResizableComponent
                    key={comp.id}
                    component={comp}
                    isSelected={selectedComponentId === comp.id}
                    onSelect={() => handleComponentSelect(comp.id)}
                    onDelete={() => handleComponentDelete(comp.id)}
                    onMove={(x, y) => handleComponentMove(comp.id, x, y)}
                    onResize={(w, h) => handleComponentResize(comp.id, w, h)}
                    dataFields={dataFields}
                    previewData={previewData}
                    scale={canvasScale}
                  />
                ))}

                {components.length === 0 && (
                  <div style={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    textAlign: 'center',
                    color: 'rgba(0, 229, 255, 0.5)',
                  }}>
                    <LayoutOutlined style={{ fontSize: 64, marginBottom: 16 }} />
                    <div style={{ fontSize: 20 }}>拖拽左侧组件到此处</div>
                    <div style={{ fontSize: 14, marginTop: 8 }}>开始设计您的大屏</div>
                  </div>
                )}
              </div>
            </div>
          </Content>

          <Sider 
            width={300} 
            style={{ 
              background: '#0d1117', 
              borderLeft: '1px solid rgba(0, 229, 255, 0.2)',
              overflow: 'hidden',
            }}
            theme="dark"
          >
            <Tabs 
              activeKey={activeTab} 
              onChange={setActiveTab} 
              size="small"
              style={{ color: '#ccc' }}
            >
              <TabPane tab={<span><SettingOutlined />属性</span>} key="props">
                <div style={{ padding: 12, height: 'calc(100vh - 110px)', overflow: 'auto' }}>
                  {renderPropsPanel()}
                </div>
              </TabPane>
              <TabPane tab={<span><BgColorsOutlined />样式</span>} key="style">
                <div style={{ padding: 12, height: 'calc(100vh - 110px)', overflow: 'auto' }}>
                  <div style={{ marginBottom: 16 }}>
                    <div style={{ color: '#00e5ff', marginBottom: 8, fontWeight: 500 }}>大屏主题</div>
                    <Row gutter={[8, 8]}>
                      {dashboardThemes.map(t => (
                        <Col span={12} key={t.value}>
                          <div
                            onClick={() => handleThemeChange(t.value)}
                            style={{
                              padding: 8,
                              border: `2px solid ${theme === t.value ? '#00e5ff' : 'transparent'}`,
                              borderRadius: 4,
                              cursor: 'pointer',
                              background: t.bgColor,
                              textAlign: 'center',
                              color: t.textColor,
                              fontSize: 12,
                            }}
                          >
                            {t.name}
                          </div>
                        </Col>
                      ))}
                    </Row>
                  </div>
                  <Divider style={{ borderColor: 'rgba(0, 229, 255, 0.1)', margin: '12px 0' }} />
                  <div style={{ marginBottom: 12 }}>
                    <div style={{ color: '#ccc', marginBottom: 8 }}>背景颜色</div>
                    <ColorPicker value={backgroundColor} onChange={(color) => setBackgroundColor(color.toHexString())} showText />
                  </div>
                  <div style={{ marginBottom: 12 }}>
                    <div style={{ color: '#ccc', marginBottom: 8 }}>背景图片</div>
                    <Input placeholder="输入图片URL" value={backgroundImage} onChange={(e) => setBackgroundImage(e.target.value)} />
                  </div>
                </div>
              </TabPane>
              <TabPane tab={<span><DatabaseOutlined />数据</span>} key="data">
                <div style={{ padding: 12, height: 'calc(100vh - 110px)', overflow: 'auto' }}>
                  <Button
                    type="primary"
                    ghost
                    icon={<DatabaseOutlined />}
                    style={{ width: '100%', marginBottom: 12, borderColor: 'rgba(0, 229, 255, 0.3)', color: '#00e5ff' }}
                    onClick={() => setDataSourceDrawerVisible(true)}
                  >
                    配置数据源
                  </Button>
                  <div style={{ fontSize: 12, color: '#888', marginBottom: 12 }}>
                    已配置 {dataFields.length} 个字段
                  </div>
                  <List
                    size="small"
                    dataSource={dataFields}
                    renderItem={(field) => (
                      <List.Item style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                        <Space>
                          <Tag color={field.isDimension ? 'cyan' : 'green'}>
                            {field.isDimension ? '维度' : '度量'}
                          </Tag>
                          <span style={{ color: '#ccc' }}>{field.fieldLabel}</span>
                        </Space>
                      </List.Item>
                    )}
                  />
                </div>
              </TabPane>
              <TabPane tab={<span><ClockCircleOutlined />刷新</span>} key="refresh">
                <div style={{ padding: 12, height: 'calc(100vh - 110px)', overflow: 'auto' }}>
                  <Form layout="vertical" size="small">
                    <Form.Item label="自动刷新" valuePropName="checked">
                      <Switch checked={autoRefresh} onChange={setAutoRefresh} />
                    </Form.Item>
                    {autoRefresh && (
                      <Form.Item label="刷新间隔(秒)">
                        <InputNumber 
                          style={{ width: '100%' }} 
                          min={1} 
                          max={3600}
                          value={refreshInterval}
                          onChange={(val) => setRefreshInterval(val || 60)}
                        />
                      </Form.Item>
                    )}
                  </Form>
                  <Divider style={{ borderColor: 'rgba(0, 229, 255, 0.1)', margin: '16px 0' }} />
                  <div style={{ marginBottom: 12, color: '#00e5ff', fontWeight: 500 }}>大屏基础信息</div>
                  <Form form={form} layout="vertical" size="small">
                    <Form.Item name="dashboardName" label="大屏名称">
                      <Input style={{ background: 'rgba(0,0,0,0.2)', borderColor: 'rgba(0, 229, 255, 0.2)', color: '#fff' }} />
                    </Form.Item>
                    <Form.Item name="dashboardCode" label="大屏编码">
                      <Input style={{ background: 'rgba(0,0,0,0.2)', borderColor: 'rgba(0, 229, 255, 0.2)', color: '#fff' }} />
                    </Form.Item>
                    <Row gutter={8}>
                      <Col span={12}>
                        <Form.Item name="width" label="宽度">
                          <InputNumber style={{ width: '100%' }} />
                        </Form.Item>
                      </Col>
                      <Col span={12}>
                        <Form.Item name="height" label="高度">
                          <InputNumber style={{ width: '100%' }} />
                        </Form.Item>
                      </Col>
                    </Row>
                  </Form>
                </div>
              </TabPane>
              <TabPane tab={<span><PlayCircleOutlined />轮播</span>} key="carousel">
                <div style={{ padding: 12, height: 'calc(100vh - 110px)', overflow: 'auto' }}>
                  <Button
                    type="primary"
                    ghost
                    icon={<PlayCircleOutlined />}
                    style={{ width: '100%', marginBottom: 12, borderColor: 'rgba(0, 229, 255, 0.3)', color: '#00e5ff' }}
                    onClick={() => setCarouselDrawerVisible(true)}
                  >
                    配置轮播页面
                  </Button>
                  {carouselConfig && carouselConfig.enabled ? (
                    <div>
                      <div style={{ color: '#52c41a', marginBottom: 8 }}>
                        已启用轮播 · {carouselConfig.pages.length} 页
                      </div>
                      <List
                        size="small"
                        dataSource={carouselConfig.pages}
                        renderItem={(page, idx) => (
                          <List.Item>
                            <span style={{ color: '#ccc' }}>第 {idx + 1} 页: {page.pageName}</span>
                          </List.Item>
                        )}
                      />
                    </div>
                  ) : (
                    <Empty description="未配置轮播" style={{ marginTop: 20 }} />
                  )}
                </div>
              </TabPane>
            </Tabs>
          </Sider>
        </Layout>

        <Drawer
          title="数据源配置"
          placement="right"
          width={400}
          open={dataSourceDrawerVisible}
          onClose={() => setDataSourceDrawerVisible(false)}
        >
          <DataSourcePanel
            appId={currentApp?.id}
            dataFields={dataFields}
            onFieldsChange={handleFieldsChange}
          />
        </Drawer>

        <Drawer
          title="轮播配置"
          placement="right"
          width={400}
          open={carouselDrawerVisible}
          onClose={() => setCarouselDrawerVisible(false)}
        >
          <Form layout="vertical" size="small">
            <Form.Item name="enabled" label="启用轮播" valuePropName="checked">
              <Switch defaultChecked={carouselConfig?.enabled || false} />
            </Form.Item>
            <Form.Item name="interval" label="切换间隔(秒)">
              <InputNumber style={{ width: '100%' }} min={1} max={300} defaultValue={10} />
            </Form.Item>
            <Form.Item name="transitionEffect" label="切换效果">
              <Select defaultValue="fade">
                <Option value="fade">淡入淡出</Option>
                <Option value="slide">滑动</Option>
                <Option value="zoom">缩放</Option>
              </Select>
            </Form.Item>
            <Form.Item name="showIndicator" label="显示指示器" valuePropName="checked">
              <Switch defaultChecked />
            </Form.Item>
          </Form>
          <Divider style={{ margin: '16px 0' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <span style={{ fontWeight: 500 }}>轮播页面</span>
            <Button size="small" type="primary" icon={<PlusOutlined />}>
              添加页面
            </Button>
          </div>
          <List
            size="small"
            dataSource={[
              { id: '1', pageName: '总览页面' },
              { id: '2', pageName: '销售页面' },
              { id: '3', pageName: '生产页面' },
            ]}
            renderItem={(item, idx) => (
              <List.Item
                actions={[
                  <Button type="link" size="small" icon={<DeleteOutlined />} danger />,
                ]}
              >
                <span>第 {idx + 1} 页: {item.pageName}</span>
              </List.Item>
            )}
          />
        </Drawer>
      </Layout>
    </DndProvider>
  )
}

export default DashboardDesigner
