import React, { useState, useEffect } from 'react'
import { Card, Button, Space, Tag, message } from 'antd'
import { ArrowLeftOutlined, ReloadOutlined, DownloadOutlined, PrinterOutlined } from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { ReportInfo, ReportComponent, reportApi } from '@/api/report'
import BaseChart, { ChartType, ChartDataConfig, ChartStyleConfig } from '@/components/chart/BaseChart'
import CrossTable from '@/components/chart/CrossTable'
import GroupSummary from '@/components/chart/GroupSummary'
import { v4 as uuidv4 } from 'uuid'

const mockData = [
  { category: '电子产品', month: '1月', sales: 12000, orders: 150, profit: 3600, target: 10000, region: '华东' },
  { category: '服装', month: '1月', sales: 8000, orders: 200, profit: 2400, target: 7000, region: '华东' },
  { category: '食品', month: '1月', sales: 5000, orders: 300, profit: 1500, target: 4500, region: '华北' },
  { category: '家居', month: '1月', sales: 6500, orders: 120, profit: 1950, target: 6000, region: '华北' },
  { category: '电子产品', month: '2月', sales: 15000, orders: 180, profit: 4500, target: 12000, region: '华东' },
  { category: '服装', month: '2月', sales: 9500, orders: 240, profit: 2850, target: 8000, region: '华南' },
  { category: '食品', month: '2月', sales: 6000, orders: 350, profit: 1800, target: 5000, region: '华南' },
  { category: '家居', month: '2月', sales: 7200, orders: 135, profit: 2160, target: 6500, region: '华东' },
]

const mockComponents: ReportComponent[] = [
  {
    id: uuidv4(),
    componentType: 'text',
    componentName: '报表标题',
    propsConfig: JSON.stringify({
      content: '销售数据综合报表',
      fontSize: 28,
      fontWeight: 'bold',
      textAlign: 'center',
    }),
    dataSourceConfig: JSON.stringify({}),
    styleConfig: JSON.stringify({ padding: '20px 0' }),
    sortOrder: 0,
  },
  {
    id: uuidv4(),
    componentType: 'indicator',
    componentName: '总销售额',
    propsConfig: JSON.stringify({
      title: '总销售额',
      value: 125680,
      unit: '元',
      trend: 'up',
      trendValue: 12.5,
    }),
    dataSourceConfig: JSON.stringify({}),
    styleConfig: JSON.stringify({}),
    sortOrder: 1,
  },
  {
    id: uuidv4(),
    componentType: 'bar',
    componentName: '销售柱状图',
    propsConfig: JSON.stringify({
      title: '各品类销售对比',
      xField: 'category',
      yFields: ['sales', 'target'],
      showLegend: true,
    }),
    dataSourceConfig: JSON.stringify({}),
    styleConfig: JSON.stringify({ height: 350 }),
    sortOrder: 2,
  },
  {
    id: uuidv4(),
    componentType: 'line',
    componentName: '销售趋势图',
    propsConfig: JSON.stringify({
      title: '月度销售趋势',
      xField: 'month',
      yFields: ['sales', 'profit'],
      seriesField: 'category',
      showLegend: true,
    }),
    dataSourceConfig: JSON.stringify({}),
    styleConfig: JSON.stringify({ height: 350 }),
    sortOrder: 3,
  },
  {
    id: uuidv4(),
    componentType: 'crosstab',
    componentName: '交叉表',
    propsConfig: JSON.stringify({
      title: '品类×月份 销售交叉表',
      rowFields: ['category'],
      colFields: ['month'],
      valueFields: [{ field: 'sales', aggregate: 'sum', label: '销售额' }],
      showRowTotal: true,
      showColTotal: true,
    }),
    dataSourceConfig: JSON.stringify({}),
    styleConfig: JSON.stringify({}),
    sortOrder: 4,
  },
  {
    id: uuidv4(),
    componentType: 'groupSummary',
    componentName: '分组汇总',
    propsConfig: JSON.stringify({
      title: '按地区分组汇总',
      groupFields: ['region', 'category'],
      summaryFields: [
        { field: 'sales', aggregate: 'sum', label: '销售额合计' },
        { field: 'orders', aggregate: 'sum', label: '订单数合计' },
        { field: 'profit', aggregate: 'sum', label: '利润合计' },
      ],
      showGroupTotal: true,
      showGrandTotal: true,
    }),
    dataSourceConfig: JSON.stringify({}),
    styleConfig: JSON.stringify({}),
    sortOrder: 5,
  },
]

const ReportPreview: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const [report, setReport] = useState<ReportInfo | null>(null)
  const [components, setComponents] = useState<ReportComponent[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadReport()
  }, [id])

  const loadReport = async () => {
    setLoading(true)
    try {
      const mockReport: ReportInfo = {
        id: Number(id),
        appId: 1,
        reportName: '销售数据报表',
        reportCode: 'sales_report',
        reportType: 'comprehensive',
        description: '销售数据综合报表',
        status: 1,
        version: '1.0.0',
        createdTime: '2024-01-15 10:30:00',
        updatedTime: '2024-01-20 14:20:00',
        components: mockComponents,
      }
      setReport(mockReport)
      setComponents(mockComponents)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = () => {
    message.success('数据已刷新')
  }

  const handleExport = () => {
    message.success('导出功能开发中')
  }

  const handlePrint = () => {
    window.print()
  }

  const renderComponent = (comp: ReportComponent) => {
    const props = comp.propsConfig ? JSON.parse(comp.propsConfig) : {}
    const styleConfig = comp.styleConfig ? JSON.parse(comp.styleConfig) : {}

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
      const styleCfg: ChartStyleConfig = {
        theme: 'light',
        title: props.title ? { text: props.title, left: 'center' } : undefined,
        legend: { show: props.showLegend !== false, position: 'top' },
        tooltip: { show: true, trigger: chartType === 'pie' || chartType === 'doughnut' ? 'item' : 'axis' },
      }
      return (
        <BaseChart
          type={chartType}
          data={mockData}
          dataConfig={dataConfig}
          styleConfig={styleCfg}
          height={styleConfig.height || 300}
        />
      )
    }

    if (comp.componentType === 'indicator') {
      return (
        <Card style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 14, color: '#666', marginBottom: 8 }}>{props.title || '指标'}</div>
          <div style={{ fontSize: 32, fontWeight: 'bold', color: '#1677ff' }}>
            {props.value?.toLocaleString() || 0}
            <span style={{ fontSize: 14, fontWeight: 'normal', marginLeft: 4 }}>{props.unit || ''}</span>
          </div>
          {props.trend && (
            <div style={{
              marginTop: 8,
              color: props.trend === 'up' ? '#52c41a' : '#ff4d4f',
              fontSize: 13,
            }}>
              {props.trend === 'up' ? '↑' : '↓'} {props.trendValue || 0}%
            </div>
          )}
        </Card>
      )
    }

    if (comp.componentType === 'text') {
      return (
        <div
          style={{
            fontSize: props.fontSize || 16,
            fontWeight: props.fontWeight || 'normal',
            textAlign: props.textAlign || 'left',
            color: props.color || '#333',
            padding: styleConfig.padding || 0,
          }}
        >
          {props.content || ''}
        </div>
      )
    }

    if (comp.componentType === 'crosstab') {
      const config = {
        rowFields: props.rowFields || [],
        colFields: props.colFields || [],
        valueFields: props.valueFields || [],
        showRowTotal: props.showRowTotal,
        showColTotal: props.showColTotal,
        showRowSubtotal: props.showRowSubtotal,
        showColSubtotal: props.showColSubtotal,
      }
      return <CrossTable data={mockData} config={config} title={props.title} />
    }

    if (comp.componentType === 'groupSummary') {
      const config = {
        groupFields: props.groupFields || [],
        summaryFields: props.summaryFields || [],
        showGroupTotal: props.showGroupTotal,
        showGrandTotal: props.showGrandTotal,
      }
      return <GroupSummary data={mockData} config={config} title={props.title} />
    }

    return (
      <Card>
        <div style={{ textAlign: 'center', color: '#999', padding: '20px 0' }}>
          {comp.componentName}
        </div>
      </Card>
    )
  }

  return (
    <div style={{ background: '#f5f5f5', minHeight: '100vh', padding: 16 }}>
      <Card style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/report')}>
              返回列表
            </Button>
            <span style={{ fontSize: 18, fontWeight: 500 }}>{report?.reportName}</span>
            <Tag color="blue">v{report?.version}</Tag>
          </Space>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
              刷新
            </Button>
            <Button icon={<DownloadOutlined />} onClick={handleExport}>
              导出
            </Button>
            <Button icon={<PrinterOutlined />} onClick={handlePrint}>
              打印
            </Button>
          </Space>
        </div>
      </Card>

      <Card loading={loading}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {components.map(comp => (
            <Card key={comp.id} type="inner" title={comp.componentName}>
              {renderComponent(comp)}
            </Card>
          ))}
        </div>
      </Card>
    </div>
  )
}

export default ReportPreview
