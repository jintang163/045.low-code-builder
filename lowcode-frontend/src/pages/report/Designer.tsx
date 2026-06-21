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
  List,
  Empty,
  Tooltip,
  Popconfirm,
} from 'antd'
import {
  SaveOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  DeleteOutlined,
  ArrowLeftOutlined,
  EyeOutlined,
  SettingOutlined,
  BarChartOutlined,
  TableOutlined,
  PieChartOutlined,
  LineChartOutlined,
  RadarChartOutlined,
  FundOutlined,
  FileTextOutlined,
  DatabaseOutlined,
  LinkOutlined,
  OrderedListOutlined,
  AppstoreOutlined,
  UndoOutlined,
  RedoOutlined,
  FontColorsOutlined,
  BgColorsOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { DndProvider, useDrag, useDrop } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'
import { ReportInfo, ReportComponent, reportApi, DataSourceConfig, CrosstabConfig, GroupSummaryConfig, ChartLinkageConfig } from '@/api/report'
import { useAppStore } from '@/store/appStore'
import BaseChart, { ChartType, ChartDataConfig, ChartStyleConfig } from '@/components/chart/BaseChart'
import CrossTable from '@/components/chart/CrossTable'
import GroupSummary from '@/components/chart/GroupSummary'
import DataSourcePanel, { DataField } from '@/components/chart/DataSourcePanel'
import { v4 as uuidv4 } from 'uuid'

const { Header, Sider, Content } = Layout
const { TabPane } = Tabs
const { Panel } = Collapse
const { Option } = Select
const { TextArea } = Input

const componentCategories = [
  { key: 'table', name: '表格组件', icon: '📊' },
  { key: 'chart', name: '图表组件', icon: '📈' },
  { key: 'text', name: '文本组件', icon: '📝' },
]

const chartTypes: { type: ChartType; name: string; icon: string }[] = [
  { type: 'line', name: '折线图', icon: '📈' },
  { type: 'bar', name: '柱状图', icon: '📊' },
  { type: 'pie', name: '饼图', icon: '🥧' },
  { type: 'doughnut', name: '环形图', icon: '🍩' },
  { type: 'area', name: '面积图', icon: '📉' },
  { type: 'barStack', name: '堆叠图', icon: '📚' },
  { type: 'lineBar', name: '组合图', icon: '📊📈' },
  { type: 'scatter', name: '散点图', icon: '🔵' },
  { type: 'radar', name: '雷达图', icon: '🕸️' },
  { type: 'gauge', name: '仪表盘', icon: '⏱️' },
  { type: 'funnel', name: '漏斗图', icon: '🔻' },
  { type: 'heatmap', name: '热力图', icon: '🔥' },
]

const ComponentItem: React.FC<{
  type: string
  name: string
  icon: string
  componentType: string
}> = ({ type, name, icon, componentType }) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'REPORT_COMPONENT',
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
        padding: '8px 12px',
        background: '#fff',
        border: '1px solid #e8e8e8',
        borderRadius: 4,
        marginBottom: 8,
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        opacity: isDragging ? 0.5 : 1,
        cursor: 'grab',
        fontSize: 13,
      }}
    >
      <span style={{ fontSize: 16 }}>{icon}</span>
      <span>{name}</span>
    </div>
  )
}

interface CanvasComponentProps {
  component: ReportComponent
  isSelected: boolean
  index: number
  onSelect: () => void
  onDelete: () => void
  onDrop: (item: any, index: number) => void
  dataFields: DataField[]
  previewData: any[]
  linkageConfigs: ChartLinkageConfig[]
}

const CanvasComponent: React.FC<CanvasComponentProps> = ({
  component,
  isSelected,
  index,
  onSelect,
  onDelete,
  onDrop,
  dataFields,
  previewData,
  linkageConfigs,
}) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'REPORT_COMPONENT_ITEM',
    item: () => ({
      componentId: component.id,
      index,
    }),
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  }))

  const [{ isOver }, drop] = useDrop(() => ({
    accept: ['REPORT_COMPONENT', 'REPORT_COMPONENT_ITEM'],
    drop: (item: any) => onDrop(item, index),
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  }))

  const props = component.propsConfig ? JSON.parse(component.propsConfig) : {}
  const dataSourceConfig = component.dataSourceConfig ? JSON.parse(component.dataSourceConfig) : {}
  const style = component.styleConfig ? JSON.parse(component.styleConfig) : {}

  const renderComponent = () => {
    switch (component.componentType) {
      case 'crosstab': {
        const crosstabConfig: CrosstabConfig = {
          rowFields: props.rowFields || [],
          columnFields: props.columnFields || [],
          valueFields: props.valueFields || [],
          showRowTotal: props.showRowTotal,
          showColumnTotal: props.showColumnTotal,
          showRowSubtotal: props.showRowSubtotal,
          showColumnSubtotal: props.showColumnSubtotal,
        }
        return (
          <CrossTable
            data={previewData}
            config={crosstabConfig}
            style={{ width: '100%' }}
          />
        )
      }
      case 'groupSummary': {
        const config: GroupSummaryConfig = {
          groupFields: props.groupFields || [],
          summaryFields: props.summaryFields || [],
          showGrandTotal: props.showGrandTotal,
          showGroupTotal: props.showGroupTotal,
        }
        return (
          <GroupSummary
            data={previewData}
            config={config}
            style={{ width: '100%' }}
          />
        )
      }
      case 'chart':
      case 'line':
      case 'bar':
      case 'pie':
      case 'doughnut':
      case 'area':
      case 'barStack':
      case 'lineBar':
      case 'scatter':
      case 'radar':
      case 'gauge':
      case 'funnel':
      case 'heatmap': {
        const chartType = (component.componentType === 'chart' ? 'bar' : component.componentType) as ChartType
        const dataConfig: ChartDataConfig = {
          xField: props.xField || dataFields.find(f => f.isDimension)?.fieldName,
          yField: props.yFields || dataFields.filter(f => f.isMeasure).map(f => f.fieldName),
          seriesField: props.seriesField,
          valueField: props.valueField || dataFields.find(f => f.isMeasure)?.fieldName,
          categoryField: props.categoryField || dataFields.find(f => f.isDimension)?.fieldName,
        }
        const styleConfig: ChartStyleConfig = {
          theme: 'light',
          title: props.title ? { text: props.title } : undefined,
          legend: props.showLegend !== false ? { show: true, position: 'top' } : { show: false },
          tooltip: { show: true, trigger: chartType === 'pie' || chartType === 'doughnut' ? 'item' : 'axis' },
          animation: props.animation !== false,
        }
        return (
          <BaseChart
            type={chartType}
            data={previewData}
            dataConfig={dataConfig}
            styleConfig={styleConfig}
            height={props.height || 280}
            onClick={(params: any) => {
              console.log('Chart clicked:', params)
            }}
          />
        )
      }
      case 'text': {
        return (
          <div style={{ 
            padding: 16, 
            fontSize: props.fontSize || 14,
            fontWeight: props.fontWeight || 'normal',
            color: props.color || '#333',
            textAlign: props.textAlign || 'left',
          }}>
            {props.content || '文本内容'}
          </div>
        )
      }
      case 'table': {
        return (
          <div style={{ padding: 16, background: '#fafafa', borderRadius: 4 }}>
            <TableOutlined style={{ fontSize: 24, color: '#999', display: 'block', textAlign: 'center' }} />
            <div style={{ color: '#999', textAlign: 'center', marginTop: 8 }}>数据表格</div>
          </div>
        )
      }
      default:
        return (
          <div style={{ padding: 16, color: '#999', textAlign: 'center' }}>
            {component.componentName}
          </div>
        )
    }
  }

  return (
    <div
      ref={(node) => { drag(node); drop(node) }}
      style={{
        position: 'relative',
        margin: '8px 0',
        padding: 8,
        border: isSelected ? '2px solid #1677ff' : '1px solid #e8e8e8',
        borderRadius: 4,
        background: isSelected ? '#e6f7ff' : '#fff',
        opacity: isDragging ? 0.5 : 1,
        cursor: 'move',
      }}
      onClick={(e) => { e.stopPropagation(); onSelect() }}
    >
      {isSelected && (
        <>
          <div style={{ 
            position: 'absolute', 
            top: -10, 
            right: -8, 
            zIndex: 10,
            display: 'flex',
            gap: 4,
          }}>
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
          <div style={{
            position: 'absolute',
            top: -10,
            left: 8,
            zIndex: 10,
          }}>
            <Tag color="blue" style={{ margin: 0 }}>
              {component.componentName}
            </Tag>
          </div>
        </>
      )}
      <div style={{ minHeight: 60 }}>
        {renderComponent()}
      </div>
      {isOver && (
        <div style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(22, 119, 255, 0.1)',
          border: '2px dashed #1677ff',
          borderRadius: 4,
          pointerEvents: 'none',
        }} />
      )}
    </div>
  )
}

const ReportDesigner: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [report, setReport] = useState<ReportInfo | null>(null)
  const [components, setComponents] = useState<ReportComponent[]>([])
  const [selectedComponentId, setSelectedComponentId] = useState<string | null>(null)
  const [form] = Form.useForm()
  const [propsForm] = Form.useForm()
  const [styleForm] = Form.useForm()
  const [dataSourceDrawerVisible, setDataSourceDrawerVisible] = useState(false)
  const [activeTab, setActiveTab] = useState('props')
  const [previewVisible, setPreviewVisible] = useState(false)
  const [dataFields, setDataFields] = useState<DataField[]>([])
  const [dataSourceConfig, setDataSourceConfig] = useState<DataSourceConfig | null>(null)
  const [previewData, setPreviewData] = useState<any[]>([])
  const [linkageConfigs, setLinkageConfigs] = useState<ChartLinkageConfig[]>([])
  const [linkageDrawerVisible, setLinkageDrawerVisible] = useState(false)
  const [expandedCategory, setExpandedCategory] = useState<string[]>(['table', 'chart'])

  const canvasRef = useRef<HTMLDivElement>(null)
  const [saveLoading, setSaveLoading] = useState(false)
  const [loadLoading, setLoadLoading] = useState(false)
  const [previewDataLoading, setPreviewDataLoading] = useState(false)

  const mockData = [
    { category: '电子产品', region: '华东', month: '1月', sales: 12000, orders: 150, profit: 3600 },
    { category: '电子产品', region: '华东', month: '2月', sales: 15000, orders: 180, profit: 4500 },
    { category: '电子产品', region: '华南', month: '1月', sales: 10000, orders: 120, profit: 3000 },
    { category: '电子产品', region: '华南', month: '2月', sales: 13000, orders: 160, profit: 3900 },
    { category: '服装', region: '华东', month: '1月', sales: 8000, orders: 200, profit: 2400 },
    { category: '服装', region: '华东', month: '2月', sales: 9500, orders: 240, profit: 2850 },
    { category: '服装', region: '华南', month: '1月', sales: 7000, orders: 180, profit: 2100 },
    { category: '服装', region: '华南', month: '2月', sales: 8500, orders: 210, profit: 2550 },
    { category: '食品', region: '华东', month: '1月', sales: 5000, orders: 300, profit: 1500 },
    { category: '食品', region: '华东', month: '2月', sales: 6000, orders: 350, profit: 1800 },
    { category: '食品', region: '华南', month: '1月', sales: 4500, orders: 280, profit: 1350 },
    { category: '食品', region: '华南', month: '2月', sales: 5500, orders: 320, profit: 1650 },
  ]

  useEffect(() => {
    setPreviewData(mockData)
    setDataFields([
      { fieldName: 'category', fieldLabel: '品类', fieldType: 'VARCHAR', isDimension: true, isMeasure: false, aggregation: 'none' },
      { fieldName: 'region', fieldLabel: '区域', fieldType: 'VARCHAR', isDimension: true, isMeasure: false, aggregation: 'none' },
      { fieldName: 'month', fieldLabel: '月份', fieldType: 'VARCHAR', isDimension: true, isMeasure: false, aggregation: 'none' },
      { fieldName: 'sales', fieldLabel: '销售额', fieldType: 'DECIMAL', isDimension: false, isMeasure: true, aggregation: 'sum' },
      { fieldName: 'orders', fieldLabel: '订单数', fieldType: 'INT', isDimension: false, isMeasure: true, aggregation: 'sum' },
      { fieldName: 'profit', fieldLabel: '利润', fieldType: 'DECIMAL', isDimension: false, isMeasure: true, aggregation: 'sum' },
    ])
  }, [])

  const loadPreviewData = useCallback(async () => {
    if (!report?.id) return
    setPreviewDataLoading(true)
    try {
      const res: any = await reportApi.queryData(report.id!)
      if (res.code === 0 || res.code === 200) {
        const result = res.data
        if (result.rows) {
          setPreviewData(result.rows)
        }
        if (result.columns && result.columns.length > 0) {
          const fields = result.columns.map((col: any) => ({
            fieldName: col.fieldName || col.name,
            fieldLabel: col.fieldLabel || col.label || col.name,
            fieldType: col.fieldType || 'VARCHAR',
            isDimension: col.isDimension !== false && !isNumberType(col.fieldType),
            isMeasure: col.isMeasure === true || isNumberType(col.fieldType),
            aggregation: isNumberType(col.fieldType) ? 'sum' : 'none',
          }))
          setDataFields(fields)
        }
      }
    } catch (e) {
      console.error('加载预览数据失败:', e)
    } finally {
      setPreviewDataLoading(false)
    }
  }, [report?.id])

  const isNumberType = (typeName: string): boolean => {
    if (!typeName) return false
    const numTypes = ['INT', 'BIGINT', 'DECIMAL', 'DOUBLE', 'FLOAT', 'NUMBER', 'NUMERIC', 'TINYINT', 'SMALLINT']
    return numTypes.some(t => typeName.toUpperCase().includes(t))
  }

  useEffect(() => {
    if (id) {
      loadReport()
    } else {
      setReport({
        appId: currentApp?.id || 1,
        reportName: '新建报表',
        reportCode: '',
        reportType: 'comprehensive',
        description: '',
        status: 0,
        components: [],
      })
      form.setFieldsValue({
        reportName: '新建报表',
        reportCode: '',
        reportType: 'comprehensive',
      })
    }
  }, [id, currentApp])

  const loadReport = useCallback(async () => {
    if (!id) return
    setLoadLoading(true)
    try {
      const res: any = await reportApi.get(Number(id))
      if (res.code === 0 || res.code === 200) {
        const reportData = res.data
        setReport(reportData)
        setComponents(reportData.components || [])
        form.setFieldsValue({
          reportName: reportData.reportName,
          reportCode: reportData.reportCode,
          reportType: reportData.reportType,
          description: reportData.description,
        })
        if (reportData.dataSourceConfig) {
          try {
            const dsConfig = typeof reportData.dataSourceConfig === 'string'
              ? JSON.parse(reportData.dataSourceConfig)
              : reportData.dataSourceConfig
            setDataSourceConfig(dsConfig)
            if (dsConfig.fields) {
              setDataFields(dsConfig.fields)
            }
          } catch (e) {
            console.error('解析数据源配置失败:', e)
          }
        }
        loadPreviewData()
      } else {
        message.error(res.message || '加载报表失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('加载报表失败: ' + (e.message || ''))
    } finally {
      setLoadLoading(false)
    }
  }, [id, currentApp, form, loadPreviewData])

  const handleDrop = useCallback((item: any, dropIndex?: number) => {
    if (item.componentType && !item.isExisting) {
      const newComponent: ReportComponent = {
        id: uuidv4(),
        componentType: item.componentType,
        componentName: item.componentName,
        positionX: 0,
        positionY: 0,
        width: 800,
        height: 300,
        propsConfig: JSON.stringify(getDefaultProps(item.componentType)),
        dataSourceConfig: JSON.stringify({}),
        styleConfig: JSON.stringify({}),
        sortOrder: components.length,
      }

      if (dropIndex !== undefined) {
        const newComponents = [...components]
        newComponents.splice(dropIndex, 0, newComponent)
        setComponents(newComponents)
      } else {
        setComponents([...components, newComponent])
      }

      setSelectedComponentId(newComponent.id)
    } else if (item.componentId && item.index !== undefined) {
      const dragIndex = item.index
      if (dropIndex !== undefined && dragIndex !== dropIndex) {
        const newComponents = [...components]
        const [removed] = newComponents.splice(dragIndex, 1)
        const insertIndex = dropIndex > dragIndex ? dropIndex - 1 : dropIndex
        newComponents.splice(insertIndex, 0, removed)
        setComponents(newComponents)
      }
    }
  }, [components])

  const getDefaultProps = (componentType: string) => {
    const dimensionField = dataFields.find(f => f.isDimension)?.fieldName || 'category'
    const measureFields = dataFields.filter(f => f.isMeasure).map(f => f.fieldName)
    const firstMeasure = measureFields[0] || 'sales'

    switch (componentType) {
      case 'crosstab':
        return {
          rowFields: [dimensionField],
          columnFields: [],
          valueFields: [{ field: firstMeasure, aggregation: 'sum', label: firstMeasure }],
          showRowTotal: true,
          showColumnTotal: true,
          showRowSubtotal: false,
          showColumnSubtotal: false,
        }
      case 'groupSummary':
        return {
          groupFields: [dimensionField],
          summaryFields: [{ field: firstMeasure, aggregation: 'sum', label: firstMeasure }],
          showGrandTotal: true,
          showGroupTotal: true,
        }
      case 'line':
      case 'bar':
      case 'area':
      case 'barStack':
        return {
          title: componentType + '图表',
          xField: dimensionField,
          yFields: [firstMeasure],
          seriesField: '',
          height: 280,
          showLegend: true,
          animation: true,
        }
      case 'pie':
      case 'doughnut':
      case 'funnel':
        return {
          title: componentType + '图表',
          categoryField: dimensionField,
          valueField: firstMeasure,
          height: 280,
          showLegend: true,
          animation: true,
        }
      case 'radar':
        return {
          title: '雷达图',
          categoryField: dimensionField,
          valueField: firstMeasure,
          seriesField: '',
          height: 280,
          showLegend: true,
          animation: true,
        }
      case 'gauge':
        return {
          title: '仪表盘',
          valueField: firstMeasure,
          height: 280,
          animation: true,
        }
      case 'scatter':
        return {
          title: '散点图',
          xField: measureFields[0] || 'sales',
          yField: measureFields[1] || 'orders',
          seriesField: '',
          height: 280,
          showLegend: true,
          animation: true,
        }
      case 'heatmap':
        return {
          title: '热力图',
          xField: 'category',
          yField: 'month',
          valueField: firstMeasure,
          height: 280,
          animation: true,
        }
      case 'lineBar':
        return {
          title: '组合图',
          xField: dimensionField,
          yFields: [measureFields[0] || 'sales', measureFields[1] || 'orders'],
          height: 280,
          showLegend: true,
          animation: true,
        }
      case 'text':
        return {
          content: '文本内容',
          fontSize: 14,
          fontWeight: 'normal',
          color: '#333',
          textAlign: 'left',
        }
      default:
        return {}
    }
  }

  const [{ isOver: isCanvasOver }, dropCanvas] = useDrop(() => ({
    accept: ['REPORT_COMPONENT', 'REPORT_COMPONENT_ITEM'],
    drop: (item: any) => handleDrop(item),
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  }))

  const selectedComponent = components.find(c => c.id === selectedComponentId)

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

  const handleStyleChange = (changedValues: any) => {
    if (!selectedComponentId) return
    setComponents(components.map(c => {
      if (c.id === selectedComponentId) {
        const currentStyle = c.styleConfig ? JSON.parse(c.styleConfig) : {}
        return {
          ...c,
          styleConfig: JSON.stringify({ ...currentStyle, ...changedValues }),
        }
      }
      return c
    }))
  }

  const handleSave = async () => {
    if (!report) return
    setSaveLoading(true)
    try {
      const values = await form.validateFields()
      const savedReport = {
        ...report,
        ...values,
        components,
        dataSourceConfig: JSON.stringify(dataSourceConfig || {
          sourceType: 'model',
          fields: dataFields,
        }),
      }

      let res: any
      if (report.id) {
        res = await reportApi.update(savedReport)
      } else {
        res = await reportApi.save(savedReport)
      }

      if (res.code === 0 || res.code === 200) {
        setReport(res.data)
        message.success('保存成功')
        if (!report.id && res.data?.id) {
          navigate(`/report/designer/${res.data.id}`)
        }
      } else {
        message.error(res.message || '保存失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('保存失败: ' + (e.message || ''))
    } finally {
      setSaveLoading(false)
    }
  }

  const handlePreview = () => {
    setPreviewVisible(true)
  }

  const handleFieldsChange = (fields: DataField[]) => {
    setDataFields(fields)
  }

  const handleDataSourceConfigChange = (config: any) => {
    setDataSourceConfig(config)
    if (config.fields) {
      setDataFields(config.fields)
    }
  }

  const renderPropsPanel = () => {
    if (!selectedComponent) {
      return <Empty description="请选择组件" style={{ marginTop: 40 }} />
    }

    const isChart = ['line', 'bar', 'pie', 'doughnut', 'area', 'barStack', 'lineBar', 'scatter', 'radar', 'gauge', 'funnel', 'heatmap', 'chart'].includes(selectedComponent.componentType)
    const isCrosstab = selectedComponent.componentType === 'crosstab'
    const isGroupSummary = selectedComponent.componentType === 'groupSummary'
    const isText = selectedComponent.componentType === 'text'

    return (
      <Form
        form={propsForm}
        layout="vertical"
        size="small"
        onValuesChange={handlePropsChange}
      >
        {isText && (
          <>
            <Form.Item name="content" label="文本内容">
              <TextArea rows={3} />
            </Form.Item>
            <Form.Item name="fontSize" label="字号">
              <InputNumber style={{ width: '100%' }} min={12} max={72} />
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
          </>
        )}

        {isChart && (
          <>
            <Form.Item name="title" label="图表标题">
              <Input />
            </Form.Item>
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
            {selectedComponent.componentType === 'radar' && (
              <>
                <Form.Item name="categoryField" label="指标字段">
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
            {selectedComponent.componentType === 'scatter' && (
              <>
                <Form.Item name="xField" label="X轴字段">
                  <Select>
                    {dataFields.filter(f => f.isMeasure).map(f => (
                      <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                    ))}
                  </Select>
                </Form.Item>
                <Form.Item name="yField" label="Y轴字段">
                  <Select>
                    {dataFields.filter(f => f.isMeasure).map(f => (
                      <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                    ))}
                  </Select>
                </Form.Item>
              </>
            )}
            {selectedComponent.componentType === 'gauge' && (
              <Form.Item name="valueField" label="数值字段">
                <Select>
                  {dataFields.filter(f => f.isMeasure).map(f => (
                    <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                  ))}
                </Select>
              </Form.Item>
            )}
            {selectedComponent.componentType === 'heatmap' && (
              <>
                <Form.Item name="xField" label="X轴字段">
                  <Select>
                    {dataFields.map(f => (
                      <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                    ))}
                  </Select>
                </Form.Item>
                <Form.Item name="yField" label="Y轴字段">
                  <Select>
                    {dataFields.map(f => (
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
            <Form.Item name="height" label="图表高度">
              <InputNumber style={{ width: '100%' }} min={200} max={800} />
            </Form.Item>
            <Form.Item name="showLegend" label="显示图例" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="animation" label="启用动画" valuePropName="checked">
              <Switch />
            </Form.Item>
          </>
        )}

        {isCrosstab && (
          <>
            <Form.Item label="行维度">
              <Select mode="multiple" value={propsForm.getFieldValue('rowFields')}
                onChange={(val) => propsForm.setFieldsValue({ rowFields: val })}>
                {dataFields.filter(f => f.isDimension).map(f => (
                  <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item label="列维度">
              <Select mode="multiple" value={propsForm.getFieldValue('columnFields')}
                onChange={(val) => propsForm.setFieldsValue({ columnFields: val })}>
                {dataFields.filter(f => f.isDimension).map(f => (
                  <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                ))}
              </Select>
            </Form.Item>
            <Divider orientation="left" style={{ margin: '12px 0' }}>值字段</Divider>
            <List
              size="small"
              dataSource={propsForm.getFieldValue('valueFields') || []}
              renderItem={(item: any, idx) => (
                <List.Item
                  actions={[
                    <Popconfirm title="确定删除?" onConfirm={() => {
                      const fields = propsForm.getFieldValue('valueFields') || []
                      fields.splice(idx, 1)
                      propsForm.setFieldsValue({ valueFields: fields })
                    }}>
                      <Button type="link" size="small" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                  ]}
                >
                  <Space>
                    <Select
                      size="small"
                      value={item.field}
                      style={{ width: 120 }}
                      onChange={(val) => {
                        const fields = propsForm.getFieldValue('valueFields') || []
                        fields[idx].field = val
                        propsForm.setFieldsValue({ valueFields: fields })
                      }}
                    >
                      {dataFields.filter(f => f.isMeasure).map(f => (
                        <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                      ))}
                    </Select>
                    <Select
                      size="small"
                      value={item.aggregation}
                      style={{ width: 80 }}
                      onChange={(val) => {
                        const fields = propsForm.getFieldValue('valueFields') || []
                        fields[idx].aggregation = val
                        propsForm.setFieldsValue({ valueFields: fields })
                      }}
                    >
                      <Option value="sum">求和</Option>
                      <Option value="avg">平均</Option>
                      <Option value="count">计数</Option>
                      <Option value="max">最大</Option>
                      <Option value="min">最小</Option>
                    </Select>
                  </Space>
                </List.Item>
              )}
            />
            <Button
              type="dashed"
              size="small"
              icon={<PlusOutlined />}
              style={{ width: '100%', marginTop: 8 }}
              onClick={() => {
                const fields = propsForm.getFieldValue('valueFields') || []
                const firstMeasure = dataFields.find(f => f.isMeasure)?.fieldName || ''
                fields.push({ field: firstMeasure, aggregation: 'sum', label: firstMeasure })
                propsForm.setFieldsValue({ valueFields: fields })
              }}
            >
              添加值字段
            </Button>
            <Divider orientation="left" style={{ margin: '16px 0' }}>显示设置</Divider>
            <Form.Item name="showRowTotal" label="显示行合计" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="showColumnTotal" label="显示列合计" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="showRowSubtotal" label="显示行小计" valuePropName="checked">
              <Switch />
            </Form.Item>
          </>
        )}

        {isGroupSummary && (
          <>
            <Form.Item label="分组字段">
              <Select mode="multiple" value={propsForm.getFieldValue('groupFields')}
                onChange={(val) => propsForm.setFieldsValue({ groupFields: val })}>
                {dataFields.filter(f => f.isDimension).map(f => (
                  <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                ))}
              </Select>
            </Form.Item>
            <Divider orientation="left" style={{ margin: '12px 0' }}>汇总字段</Divider>
            <List
              size="small"
              dataSource={propsForm.getFieldValue('summaryFields') || []}
              renderItem={(item: any, idx) => (
                <List.Item
                  actions={[
                    <Popconfirm title="确定删除?" onConfirm={() => {
                      const fields = propsForm.getFieldValue('summaryFields') || []
                      fields.splice(idx, 1)
                      propsForm.setFieldsValue({ summaryFields: fields })
                    }}>
                      <Button type="link" size="small" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                  ]}
                >
                  <Space>
                    <Select
                      size="small"
                      value={item.field}
                      style={{ width: 120 }}
                      onChange={(val) => {
                        const fields = propsForm.getFieldValue('summaryFields') || []
                        fields[idx].field = val
                        propsForm.setFieldsValue({ summaryFields: fields })
                      }}
                    >
                      {dataFields.filter(f => f.isMeasure).map(f => (
                        <Option key={f.fieldName} value={f.fieldName}>{f.fieldLabel}</Option>
                      ))}
                    </Select>
                    <Select
                      size="small"
                      value={item.aggregation}
                      style={{ width: 80 }}
                      onChange={(val) => {
                        const fields = propsForm.getFieldValue('summaryFields') || []
                        fields[idx].aggregation = val
                        propsForm.setFieldsValue({ summaryFields: fields })
                      }}
                    >
                      <Option value="sum">求和</Option>
                      <Option value="avg">平均</Option>
                      <Option value="count">计数</Option>
                      <Option value="max">最大</Option>
                      <Option value="min">最小</Option>
                    </Select>
                  </Space>
                </List.Item>
              )}
            />
            <Button
              type="dashed"
              size="small"
              icon={<PlusOutlined />}
              style={{ width: '100%', marginTop: 8 }}
              onClick={() => {
                const fields = propsForm.getFieldValue('summaryFields') || []
                const firstMeasure = dataFields.find(f => f.isMeasure)?.fieldName || ''
                fields.push({ field: firstMeasure, aggregation: 'sum', label: firstMeasure })
                propsForm.setFieldsValue({ summaryFields: fields })
              }}
            >
              添加汇总字段
            </Button>
            <Divider orientation="left" style={{ margin: '16px 0' }}>显示设置</Divider>
            <Form.Item name="showGrandTotal" label="显示总计" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="showGroupTotal" label="显示分组小计" valuePropName="checked">
              <Switch />
            </Form.Item>
          </>
        )}
      </Form>
    )
  }

  const renderStylePanel = () => {
    if (!selectedComponent) {
      return <Empty description="请选择组件" style={{ marginTop: 40 }} />
    }

    return (
      <Form
        form={styleForm}
        layout="vertical"
        size="small"
        onValuesChange={handleStyleChange}
      >
        <Form.Item label="宽度">
          <InputNumber style={{ width: '100%' }} min={100} max={2000} />
        </Form.Item>
        <Form.Item label="高度">
          <InputNumber style={{ width: '100%' }} min={50} max={2000} />
        </Form.Item>
        <Form.Item label="外边距">
          <InputNumber style={{ width: '100%' }} min={0} max={100} />
        </Form.Item>
        <Form.Item label="内边距">
          <InputNumber style={{ width: '100%' }} min={0} max={100} />
        </Form.Item>
        <Form.Item label="背景色">
          <ColorPicker showText />
        </Form.Item>
        <Form.Item label="边框">
          <Input placeholder="例如: 1px solid #e8e8e8" />
        </Form.Item>
        <Form.Item label="圆角">
          <InputNumber style={{ width: '100%' }} min={0} max={50} />
        </Form.Item>
        <Form.Item label="阴影">
          <Input placeholder="例如: 0 2px 8px rgba(0,0,0,0.1)" />
        </Form.Item>
      </Form>
    )
  }

  const renderLinkagePanel = () => {
    return (
      <div>
        <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontWeight: 500 }}>图表联动配置</span>
          <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => {
            const newLinkage: ChartLinkageConfig = {
              sourceComponentId: '',
              targetComponentId: '',
              triggerType: 'click',
              mappingFields: [],
            }
            setLinkageConfigs([...linkageConfigs, newLinkage])
          }}>
            添加联动
          </Button>
        </div>
        {linkageConfigs.length === 0 ? (
          <Empty description="暂无联动配置" style={{ marginTop: 20 }} />
        ) : (
          <List
            size="small"
            dataSource={linkageConfigs}
            renderItem={(item, idx) => (
              <List.Item
                style={{ padding: '12px 0', borderBottom: '1px solid #f0f0f0' }}
                actions={[
                  <Popconfirm title="确定删除?" onConfirm={() => {
                    setLinkageConfigs(linkageConfigs.filter((_, i) => i !== idx))
                  }}>
                    <Button type="link" size="small" danger icon={<DeleteOutlined />} />
                  </Popconfirm>
                ]}
              >
                <div style={{ width: '100%' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                    <Select
                      size="small"
                      style={{ flex: 1 }}
                      placeholder="源组件"
                      value={item.sourceComponentId}
                      onChange={(val) => {
                        const newConfigs = [...linkageConfigs]
                        newConfigs[idx].sourceComponentId = val
                        setLinkageConfigs(newConfigs)
                      }}
                    >
                      {components.filter(c => ['line', 'bar', 'pie', 'doughnut', 'scatter'].includes(c.componentType)).map(c => (
                        <Option key={c.id} value={c.id}>{c.componentName}</Option>
                      ))}
                    </Select>
                    <LinkOutlined style={{ color: '#1677ff' }} />
                    <Select
                      size="small"
                      style={{ flex: 1 }}
                      placeholder="目标组件"
                      value={item.targetComponentId}
                      onChange={(val) => {
                        const newConfigs = [...linkageConfigs]
                        newConfigs[idx].targetComponentId = val
                        setLinkageConfigs(newConfigs)
                      }}
                    >
                      {components.filter(c => c.id !== item.sourceComponentId).map(c => (
                        <Option key={c.id} value={c.id}>{c.componentName}</Option>
                      ))}
                    </Select>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 12, color: '#999' }}>触发方式:</span>
                    <Select
                      size="small"
                      style={{ width: 80 }}
                      value={item.triggerType}
                      onChange={(val) => {
                        const newConfigs = [...linkageConfigs]
                        newConfigs[idx].triggerType = val as any
                        setLinkageConfigs(newConfigs)
                      }}
                    >
                      <Option value="click">点击</Option>
                      <Option value="hover">悬停</Option>
                      <Option value="select">选中</Option>
                    </Select>
                  </div>
                </div>
              </List.Item>
            )}
          />
        )}
      </div>
    )
  }

  return (
    <DndProvider backend={HTML5Backend}>
      <Layout style={{ height: '100vh', background: '#f5f5f5' }}>
        <Header style={{ 
          background: '#fff', 
          borderBottom: '1px solid #e8e8e8',
          padding: '0 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          height: 56,
        }}>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/report')}>
              返回
            </Button>
            <div style={{ borderLeft: '1px solid #e8e8e8', height: 24, margin: '0 8px' }} />
            <h3 style={{ margin: 0 }}>{report?.reportName || '报表设计器'}</h3>
            <Tag color={report?.status === 1 ? 'green' : 'orange'}>
              {report?.status === 1 ? '已发布' : '草稿'}
            </Tag>
          </Space>
          <Space>
            <Button icon={<UndoOutlined />} disabled>撤销</Button>
            <Button icon={<RedoOutlined />} disabled>重做</Button>
            <Divider type="vertical" style={{ margin: '0 4px' }} />
            <Button icon={<EyeOutlined />} onClick={handlePreview}>预览</Button>
            <Button type="primary" icon={<SaveOutlined />} onClick={handleSave} loading={saveLoading}>保存</Button>
          </Space>
        </Header>

        <Layout>
          <Sider width={220} style={{ background: '#fff', borderRight: '1px solid #e8e8e8' }}>
            <div style={{ padding: 12 }}>
              <div style={{ fontWeight: 500, marginBottom: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
                <AppstoreOutlined />
                组件库
              </div>
              <Collapse
                activeKey={expandedCategory}
                onChange={(keys) => setExpandedCategory(keys as string[])}
                ghost
                size="small"
              >
                <Panel header="表格组件" key="table">
                  <ComponentItem type="crosstab" name="交叉表" icon="📊" componentType="crosstab" />
                  <ComponentItem type="groupSummary" name="分组汇总" icon="📋" componentType="groupSummary" />
                  <ComponentItem type="table" name="数据表格" icon="📑" componentType="table" />
                </Panel>
                <Panel header="图表组件" key="chart">
                  {chartTypes.map(chart => (
                    <ComponentItem
                      key={chart.type}
                      type={chart.type}
                      name={chart.name}
                      icon={chart.icon}
                      componentType={chart.type}
                    />
                  ))}
                </Panel>
                <Panel header="文本组件" key="text">
                  <ComponentItem type="text" name="文本" icon="📝" componentType="text" />
                </Panel>
              </Collapse>
            </div>
          </Sider>

          <Content
            ref={(node) => { dropCanvas(node); canvasRef.current = node }}
            style={{
              padding: 16,
              overflow: 'auto',
              background: '#f0f2f5',
            }}
            onClick={() => setSelectedComponentId(null)}
          >
            <div style={{
              maxWidth: 900,
              margin: '0 auto',
              minHeight: 'calc(100vh - 88px)',
              background: '#fff',
              padding: 24,
              borderRadius: 4,
              boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
              border: isCanvasOver ? '2px dashed #1677ff' : 'none',
            }}>
              {components.length === 0 ? (
                <div style={{
                  height: 400,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#999',
                  border: '2px dashed #d9d9d9',
                  borderRadius: 8,
                }}>
                  <AppstoreOutlined style={{ fontSize: 48, marginBottom: 16, color: '#d9d9d9' }} />
                  <div style={{ fontSize: 16 }}>拖拽左侧组件到此处</div>
                  <div style={{ fontSize: 12, marginTop: 8 }}>开始设计您的报表</div>
                </div>
              ) : (
                components.map((comp, index) => (
                  <CanvasComponent
                    key={comp.id}
                    component={comp}
                    isSelected={selectedComponentId === comp.id}
                    index={index}
                    onSelect={() => handleComponentSelect(comp.id)}
                    onDelete={() => handleComponentDelete(comp.id)}
                    onDrop={(item) => handleDrop(item, index)}
                    dataFields={dataFields}
                    previewData={previewData}
                    linkageConfigs={linkageConfigs}
                  />
                ))
              )}
            </div>
          </Content>

          <Sider width={300} style={{ background: '#fff', borderLeft: '1px solid #e8e8e8' }}>
            <Tabs activeKey={activeTab} onChange={setActiveTab} size="small">
              <TabPane tab={<span><SettingOutlined />属性</span>} key="props">
                <div style={{ padding: 12, height: 'calc(100vh - 110px)', overflow: 'auto' }}>
                  {renderPropsPanel()}
                </div>
              </TabPane>
              <TabPane tab={<span><BgColorsOutlined />样式</span>} key="style">
                <div style={{ padding: 12, height: 'calc(100vh - 110px)', overflow: 'auto' }}>
                  {renderStylePanel()}
                </div>
              </TabPane>
              <TabPane tab={<span><DatabaseOutlined />数据</span>} key="data">
                <div style={{ padding: 12, height: 'calc(100vh - 110px)', overflow: 'auto' }}>
                  <Button
                    type="dashed"
                    icon={<DatabaseOutlined />}
                    style={{ width: '100%', marginBottom: 12 }}
                    onClick={() => setDataSourceDrawerVisible(true)}
                  >
                    配置数据源
                  </Button>
                  <Button
                    type="primary"
                    ghost
                    icon={<ReloadOutlined />}
                    style={{ width: '100%', marginBottom: 12 }}
                    onClick={loadPreviewData}
                    loading={previewDataLoading}
                  >
                    刷新数据
                  </Button>
                  <div style={{ fontSize: 12, color: '#999', marginBottom: 12 }}>
                    已配置 {dataFields.length} 个字段
                  </div>
                  <List
                    size="small"
                    dataSource={dataFields}
                    locale={{ emptyText: '暂无字段，请先配置数据源' }}
                    renderItem={(field) => (
                      <List.Item>
                        <Space>
                          <Tag color={field.isDimension ? 'blue' : 'green'}>
                            {field.isDimension ? '维度' : '度量'}
                          </Tag>
                          <span>{field.fieldLabel}</span>
                        </Space>
                      </List.Item>
                    )}
                  />
                  {previewData.length > 0 && (
                    <div style={{ marginTop: 12 }}>
                      <div style={{ fontSize: 12, color: '#999', marginBottom: 8 }}>
                        数据预览 (共 {previewData.length} 条)
                      </div>
                      <div 
                        style={{ 
                          maxHeight: 200, 
                          overflow: 'auto',
                          fontSize: 11,
                          background: '#fafafa',
                          padding: 8,
                          borderRadius: 4,
                          fontFamily: 'monospace',
                        }}
                      >
                        {previewData.slice(0, 10).map((row, idx) => (
                          <div key={idx} style={{ padding: '2px 0', borderBottom: idx < 9 ? '1px solid #f0f0f0' : 'none' }}>
                            {JSON.stringify(row)}
                          </div>
                        ))}
                        {previewData.length > 10 && (
                          <div style={{ textAlign: 'center', padding: 4, color: '#999' }}>
                            仅显示前 10 条
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              </TabPane>
              <TabPane tab={<span><LinkOutlined />联动</span>} key="linkage">
                <div style={{ padding: 12, height: 'calc(100vh - 110px)', overflow: 'auto' }}>
                  {renderLinkagePanel()}
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
            value={dataSourceConfig}
            onChange={handleDataSourceConfigChange}
            dataFields={dataFields}
            onFieldsChange={handleFieldsChange}
          />
        </Drawer>

        <Modal
          title="报表预览"
          open={previewVisible}
          onCancel={() => setPreviewVisible(false)}
          footer={null}
          width={900}
          style={{ top: 20 }}
        >
          <div style={{ maxHeight: '75vh', overflow: 'auto', padding: 16, background: '#fff' }}>
            {components.map(comp => {
              const props = comp.propsConfig ? JSON.parse(comp.propsConfig) : {}
              if (['line', 'bar', 'pie', 'doughnut', 'area', 'barStack', 'lineBar', 'scatter', 'radar', 'gauge', 'funnel', 'heatmap'].includes(comp.componentType)) {
                const dataConfig: ChartDataConfig = {
                  xField: props.xField,
                  yField: props.yFields,
                  seriesField: props.seriesField,
                  valueField: props.valueField,
                  categoryField: props.categoryField,
                }
                return (
                  <Card key={comp.id} size="small" style={{ marginBottom: 16 }} title={props.title}>
                    <BaseChart
                      type={comp.componentType as ChartType}
                      data={previewData}
                      dataConfig={dataConfig}
                      height={props.height || 280}
                    />
                  </Card>
                )
              }
              if (comp.componentType === 'crosstab') {
                return (
                  <Card key={comp.id} size="small" style={{ marginBottom: 16 }} title="交叉表">
                    <CrossTable
                      data={previewData}
                      config={{
                        rowFields: props.rowFields || [],
                        columnFields: props.columnFields || [],
                        valueFields: props.valueFields || [],
                        showRowTotal: props.showRowTotal,
                        showColumnTotal: props.showColumnTotal,
                      }}
                    />
                  </Card>
                )
              }
              if (comp.componentType === 'groupSummary') {
                return (
                  <Card key={comp.id} size="small" style={{ marginBottom: 16 }} title="分组汇总">
                    <GroupSummary
                      data={previewData}
                      config={{
                        groupFields: props.groupFields || [],
                        summaryFields: props.summaryFields || [],
                        showGrandTotal: props.showGrandTotal,
                        showGroupTotal: props.showGroupTotal,
                      }}
                    />
                  </Card>
                )
              }
              if (comp.componentType === 'text') {
                return (
                  <div key={comp.id} style={{
                    padding: 16,
                    fontSize: props.fontSize || 14,
                    fontWeight: props.fontWeight || 'normal',
                    color: props.color || '#333',
                    textAlign: props.textAlign || 'left',
                  }}>
                    {props.content}
                  </div>
                )
              }
              return null
            })}
          </div>
        </Modal>
      </Layout>
    </DndProvider>
  )
}

export default ReportDesigner
