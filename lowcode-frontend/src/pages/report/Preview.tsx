import React, { useState, useEffect, useCallback } from 'react'
import { Card, Button, Space, Tag, message, Spin, Empty } from 'antd'
import { ArrowLeftOutlined, ReloadOutlined, DownloadOutlined, PrinterOutlined } from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { ReportInfo, ReportComponent, reportApi, ChartLinkageConfig } from '@/api/report'
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
  const [dataLoading, setDataLoading] = useState(false)
  const [reportData, setReportData] = useState<any[]>([])
  const [linkageConfigs, setLinkageConfigs] = useState<ChartLinkageConfig[]>([])
  const [activeFilters, setActiveFilters] = useState<Record<string, any>>({})

  useEffect(() => {
    loadReport()
  }, [id])

  const loadReport = async () => {
    setLoading(true)
    try {
      const res: any = await reportApi.get(Number(id))
      if (res.code === 0 || res.code === 200) {
        const reportData = res.data
        setReport(reportData)
        setComponents(reportData.components || mockComponents)
        if (reportData.linkageConfigs) {
          try {
            const configs = typeof reportData.linkageConfigs === 'string'
              ? JSON.parse(reportData.linkageConfigs)
              : reportData.linkageConfigs
            setLinkageConfigs(configs)
          } catch (e) {
            console.error('解析联动配置失败:', e)
          }
        }
        loadReportData()
      } else {
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
        setReportData(mockData)
      }
    } catch (e: any) {
      console.error(e)
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
      setReportData(mockData)
    } finally {
      setLoading(false)
    }
  }

  const loadReportData = async () => {
    if (!id) return
    setDataLoading(true)
    try {
      const res: any = await reportApi.queryData(Number(id), activeFilters)
      if (res.code === 0 || res.code === 200) {
        const result = res.data
        setReportData(result.rows || [])
      }
    } catch (e) {
      console.error('加载报表数据失败:', e)
    } finally {
      setDataLoading(false)
    }
  }

  const handleRefresh = () => {
    loadReportData()
    message.success('数据已刷新')
  }

  const handleExport = () => {
    message.success('导出功能开发中')
  }

  const handlePrint = () => {
    window.print()
  }

  const handleChartClick = useCallback((componentId: string, params: any) => {
    const linkages = linkageConfigs.filter(c => c.sourceComponentId === componentId)
    if (linkages.length === 0) return

    const newFilters = { ...activeFilters }
    linkages.forEach(linkage => {
      linkage.mappingFields?.forEach(mapping => {
        const value = params?.data?.[mapping.sourceField]
        if (value !== undefined) {
          newFilters[mapping.targetField] = value
        }
      })
    })
    setActiveFilters(newFilters)
  }, [linkageConfigs, activeFilters])

  const getFilteredData = (componentId: string): any[] => {
    const isTarget = linkageConfigs.some(c => c.targetComponentId === componentId)
    if (!isTarget || Object.keys(activeFilters).length === 0) {
      return reportData
    }

    return reportData.filter(row => {
      return Object.entries(activeFilters).every(([key, value]) => {
        if (value === undefined || value === null) return true
        return row[key] === value
      })
    })
  }

  const renderComponent = (comp: ReportComponent) => {
    const props = comp.propsConfig ? JSON.parse(comp.propsConfig) : {}
    const styleConfig = comp.styleConfig ? JSON.parse(comp.styleConfig) : {}
    const componentData = getFilteredData(comp.id)

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
          data={componentData}
          dataConfig={dataConfig}
          styleConfig={styleCfg}
          height={styleConfig.height || 300}
          onClick={(params: any) => handleChartClick(comp.id, params)}
        />
      )
    }

    if (comp.componentType === 'indicator') {
      const totalSales = componentData.reduce((sum: number, row: any) => sum + (row.sales || 0), 0)
      return (
        <Card style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 14, color: '#666', marginBottom: 8 }}>{props.title || '指标'}</div>
          <div style={{ fontSize: 32, fontWeight: 'bold', color: '#1677ff' }}>
            {props.value?.toLocaleString() || totalSales.toLocaleString()}
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
      return <CrossTable data={componentData} config={config} title={props.title} />
    }

    if (comp.componentType === 'groupSummary') {
      const config = {
        groupFields: props.groupFields || [],
        summaryFields: props.summaryFields || [],
        showGroupTotal: props.showGroupTotal,
        showGrandTotal: props.showGrandTotal,
      }
      return <GroupSummary data={componentData} config={config} title={props.title} />
    }

    return (
      <Card>
        <div style={{ textAlign: 'center', color: '#999', padding: '20px 0' }}>
          {comp.componentName}
        </div>
      </Card>
    )
  }

  const hasActiveFilters = Object.keys(activeFilters).length > 0

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
            {hasActiveFilters && (
              <Tag color="orange" closable onClose={() => setActiveFilters({})}>
                已筛选
              </Tag>
            )}
          </Space>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={handleRefresh} loading={dataLoading}>
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
        {dataLoading && (
          <div style={{ textAlign: 'center', padding: 20 }}>
            <Spin tip="加载数据中..." />
          </div>
        )}
        {!loading && components.length === 0 ? (
          <Empty description="暂无报表组件" />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {components.map(comp => (
              <Card 
                key={comp.id} 
                type="inner" 
                title={comp.componentName}
                size="small"
              >
                {renderComponent(comp)}
              </Card>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}

export default ReportPreview
