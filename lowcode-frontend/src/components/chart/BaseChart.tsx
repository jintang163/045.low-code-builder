import React, { useEffect, useRef, useMemo, useCallback } from 'react'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'
import { LinkageFilter } from '../../hooks/useChartLinkage'

export type ChartType = 
  | 'line' 
  | 'bar' 
  | 'pie' 
  | 'doughnut' 
  | 'scatter' 
  | 'radar' 
  | 'gauge' 
  | 'funnel' 
  | 'sankey' 
  | 'treemap' 
  | 'heatmap' 
  | 'pictorialBar'
  | 'area'
  | 'barStack'
  | 'lineBar'

export interface ChartField {
  fieldName: string
  fieldLabel: string
  fieldType: 'dimension' | 'measure'
  aggregation?: 'sum' | 'avg' | 'count' | 'max' | 'min' | 'none'
  format?: string
}

export interface ChartDataConfig {
  xField?: string
  yField?: string | string[]
  seriesField?: string
  valueField?: string
  angleField?: string
  radiusField?: string
  categoryField?: string
}

export interface ChartStyleConfig {
  theme?: 'light' | 'dark' | 'custom'
  backgroundColor?: string
  title?: {
    text: string
    subtext?: string
    left?: 'left' | 'center' | 'right' | number
    textStyle?: Record<string, any>
  }
  legend?: {
    show?: boolean
    position?: 'top' | 'bottom' | 'left' | 'right'
    orient?: 'horizontal' | 'vertical'
  }
  tooltip?: {
    show?: boolean
    trigger?: 'item' | 'axis'
    formatter?: string
  }
  grid?: {
    left?: number | string
    right?: number | string
    top?: number | string
    bottom?: number | string
  }
  colorList?: string[]
  animation?: boolean
  animationDuration?: number
}

export interface ChartProps {
  type: ChartType
  data: Record<string, any>[]
  dataConfig: ChartDataConfig
  styleConfig?: ChartStyleConfig
  width?: number | string
  height?: number | string
  loading?: boolean
  onClick?: (params: any) => void
  onHover?: (params: any) => void
  onSelect?: (params: any, selected: boolean) => void
  option?: any
  notMerge?: boolean
  lazyUpdate?: boolean
  componentId?: string
  linkageFilters?: LinkageFilter[]
  highlightData?: Record<string, any>[]
  enableLinkage?: boolean
  onChartClick?: (data: Record<string, any>) => void
  onChartHover?: (data: Record<string, any>) => void
  onChartSelect?: (data: Record<string, any>, selected: boolean) => void
}

const defaultColors = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#48b5d5'
]

const darkColors = [
  '#00e5ff', '#52c41a', '#ffa940', '#ff6b6b', '#1890ff',
  '#722ed1', '#13c2c2', '#eb2f96', '#faad14', '#a0d911'
]

const BaseChart: React.FC<ChartProps> = ({
  type,
  data,
  dataConfig,
  styleConfig = {},
  width = '100%',
  height = 300,
  loading = false,
  onClick,
  onHover,
  onSelect,
  option: customOption,
  notMerge = false,
  lazyUpdate = false,
  componentId,
  linkageFilters = [],
  highlightData = [],
  enableLinkage = false,
  onChartClick,
  onChartHover,
  onChartSelect,
}) => {
  const chartRef = useRef<any>(null)
  const selectedItems = useRef<Set<string>>(new Set())

  const getColors = useCallback(() => {
    if (styleConfig.colorList && styleConfig.colorList.length > 0) {
      return styleConfig.colorList
    }
    return styleConfig.theme === 'dark' ? darkColors : defaultColors
  }, [styleConfig.theme, styleConfig.colorList])

  const filterDataByLinkage = useCallback((
    sourceData: Record<string, any>[],
    filters: LinkageFilter[]
  ): Record<string, any>[] => {
    if (filters.length === 0) return sourceData
    return sourceData.filter(item => {
      return filters.every(filter => {
        const value = item[filter.field]
        const op = filter.operator || 'eq'
        switch (op) {
          case 'eq':
            return value === filter.value
          case 'ne':
            return value !== filter.value
          case 'in':
            return Array.isArray(filter.value) && filter.value.includes(value)
          case 'notIn':
            return Array.isArray(filter.value) && !filter.value.includes(value)
          case 'gt':
            return value > filter.value
          case 'lt':
            return value < filter.value
          case 'gte':
            return value >= filter.value
          case 'lte':
            return value <= filter.value
          default:
            return value === filter.value
        }
      })
    })
  }, [])

  const getHighlightKey = useCallback((item: Record<string, any>): string => {
    const keys = Object.keys(item).sort()
    return keys.map(k => `${k}:${item[k]}`).join('|')
  }, [])

  const isHighlighted = useCallback((
    item: Record<string, any>,
    highlightList: Record<string, any>[]
  ): boolean => {
    if (highlightList.length === 0) return false
    const itemKey = getHighlightKey(item)
    return highlightList.some(h => getHighlightKey(h) === itemKey)
  }, [getHighlightKey])

  const processedData = useMemo(() => {
    return filterDataByLinkage(data, linkageFilters)
  }, [data, linkageFilters, filterDataByLinkage])

  const buildOption = useCallback(() => {
    const colors = getColors()
    const chartData = processedData
    const baseOption: any = {
      color: colors,
      backgroundColor: styleConfig.backgroundColor || 'transparent',
      animation: styleConfig.animation !== false,
      animationDuration: styleConfig.animationDuration || 1000,
    }

    if (styleConfig.title) {
      baseOption.title = {
        text: styleConfig.title.text,
        subtext: styleConfig.title.subtext,
        left: styleConfig.title.left || 'center',
        textStyle: {
          color: styleConfig.theme === 'dark' ? '#fff' : '#333',
          fontSize: 16,
          fontWeight: 'bold',
          ...styleConfig.title.textStyle,
        },
        subtextStyle: {
          color: styleConfig.theme === 'dark' ? '#aaa' : '#999',
        },
      }
    }

    if (styleConfig.legend) {
      baseOption.legend = {
        show: styleConfig.legend.show !== false,
        type: 'scroll',
        top: styleConfig.legend.position === 'top' ? 0 : undefined,
        bottom: styleConfig.legend.position === 'bottom' ? 0 : undefined,
        left: styleConfig.legend.position === 'left' ? 0 : undefined,
        right: styleConfig.legend.position === 'right' ? 0 : undefined,
        orient: styleConfig.legend.orient || 'horizontal',
        textStyle: {
          color: styleConfig.theme === 'dark' ? '#ccc' : '#666',
        },
      }
    }

    if (styleConfig.tooltip) {
      baseOption.tooltip = {
        show: styleConfig.tooltip.show !== false,
        trigger: styleConfig.tooltip.trigger || 'axis',
        ...(styleConfig.tooltip.formatter ? { formatter: styleConfig.tooltip.formatter } : {}),
      }
    }

    if (styleConfig.grid) {
      baseOption.grid = {
        left: styleConfig.grid.left || 50,
        right: styleConfig.grid.right || 20,
        top: styleConfig.grid.top || 40,
        bottom: styleConfig.grid.bottom || 30,
      }
    }

    const textColor = styleConfig.theme === 'dark' ? '#ccc' : '#666'
    const axisLineColor = styleConfig.theme === 'dark' ? '#ffffff33' : '#e8e8e8'
    const splitLineColor = styleConfig.theme === 'dark' ? '#ffffff11' : '#f0f0f0'
    const highlightOpacity = 1
    const dimOpacity = 0.3

    let typeSpecificOption: any = {}

    switch (type) {
      case 'line':
      case 'area': {
        const xField = dataConfig.xField || 'name'
        const yFields = Array.isArray(dataConfig.yField) ? dataConfig.yField : (dataConfig.yField ? [dataConfig.yField] : ['value'])
        const seriesField = dataConfig.seriesField
        const hasHighlight = highlightData.length > 0

        if (seriesField) {
          const categories = [...new Set(chartData.map(d => d[xField]))]
          const seriesNames = [...new Set(chartData.map(d => d[seriesField]))]
          const series = seriesNames.map((name, idx) => {
            const seriesData = categories.map(cat => {
              const item = chartData.find(d => d[xField] === cat && d[seriesField] === name)
              return item ? item[yFields[0]] : null
            })
            return {
              name,
              type: 'line',
              smooth: true,
              data: seriesData,
              areaStyle: type === 'area' ? {
                color: {
                  type: 'linear',
                  x: 0, y: 0, x2: 0, y2: 1,
                  colorStops: [
                    { offset: 0, color: colors[idx % colors.length] + '88' },
                    { offset: 1, color: colors[idx % colors.length] + '00' },
                  ],
                },
                opacity: hasHighlight ? dimOpacity : undefined,
              } : undefined,
              itemStyle: { color: colors[idx % colors.length], opacity: hasHighlight ? dimOpacity : undefined },
              lineStyle: { width: 2, opacity: hasHighlight ? dimOpacity : undefined },
              emphasis: {
                itemStyle: { opacity: highlightOpacity },
                lineStyle: { opacity: highlightOpacity },
              },
            }
          })

          typeSpecificOption = {
            xAxis: {
              type: 'category',
              data: categories,
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
            },
            yAxis: {
              type: 'value',
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
            },
            series,
          }
        } else {
          const xData = chartData.map(d => d[xField])
          const series = yFields.map((field, idx) => ({
            name: field,
            type: 'line',
            smooth: true,
            data: chartData.map(d => d[field]),
            areaStyle: type === 'area' ? {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: colors[idx % colors.length] + '88' },
                  { offset: 1, color: colors[idx % colors.length] + '00' },
                ],
              },
              opacity: hasHighlight ? dimOpacity : undefined,
            } : undefined,
            itemStyle: { color: colors[idx % colors.length], opacity: hasHighlight ? dimOpacity : undefined },
            lineStyle: { width: 2, opacity: hasHighlight ? dimOpacity : undefined },
            emphasis: {
              itemStyle: { opacity: highlightOpacity },
              lineStyle: { opacity: highlightOpacity },
            },
          }))

          typeSpecificOption = {
            xAxis: {
              type: 'category',
              data: xData,
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
            },
            yAxis: {
              type: 'value',
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
            },
            series,
          }
        }
        break
      }

      case 'bar':
      case 'barStack': {
        const xField = dataConfig.xField || 'name'
        const yFields = Array.isArray(dataConfig.yField) ? dataConfig.yField : (dataConfig.yField ? [dataConfig.yField] : ['value'])
        const seriesField = dataConfig.seriesField
        const hasHighlight = highlightData.length > 0

        if (seriesField) {
          const categories = [...new Set(chartData.map(d => d[xField]))]
          const seriesNames = [...new Set(chartData.map(d => d[seriesField]))]
          const series = seriesNames.map((name, idx) => ({
            name,
            type: 'bar',
            stack: type === 'barStack' ? 'total' : undefined,
            data: categories.map(cat => {
              const item = chartData.find(d => d[xField] === cat && d[seriesField] === name)
              return item ? item[yFields[0]] : 0
            }),
            itemStyle: { 
              color: colors[idx % colors.length], 
              borderRadius: [4, 4, 0, 0],
              opacity: hasHighlight ? dimOpacity : undefined,
            },
            emphasis: {
              itemStyle: { opacity: highlightOpacity },
            },
            barMaxWidth: 30,
          }))

          typeSpecificOption = {
            xAxis: {
              type: 'category',
              data: categories,
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
            },
            yAxis: {
              type: 'value',
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
            },
            series,
          }
        } else {
          const series = yFields.map((field, idx) => ({
            name: field,
            type: 'bar',
            stack: type === 'barStack' ? 'total' : undefined,
            data: chartData.map(d => d[field]),
            itemStyle: { 
              color: colors[idx % colors.length], 
              borderRadius: [4, 4, 0, 0],
              opacity: hasHighlight ? dimOpacity : undefined,
            },
            emphasis: {
              itemStyle: { opacity: highlightOpacity },
            },
            barMaxWidth: 30,
          }))

          typeSpecificOption = {
            xAxis: {
              type: 'category',
              data: chartData.map(d => d[xField]),
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
            },
            yAxis: {
              type: 'value',
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
            },
            series,
          }
        }
        break
      }

      case 'lineBar': {
        const xField = dataConfig.xField || 'name'
        const yFields = Array.isArray(dataConfig.yField) ? dataConfig.yField : ['value1', 'value2']
        const hasHighlight = highlightData.length > 0
        
        typeSpecificOption = {
          xAxis: {
            type: 'category',
            data: chartData.map(d => d[xField]),
            axisLine: { lineStyle: { color: axisLineColor } },
            axisLabel: { color: textColor },
          },
          yAxis: [
            {
              type: 'value',
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
            },
            {
              type: 'value',
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
              splitLine: { show: false },
            },
          ],
          series: [
            {
              name: yFields[0] || '柱状图',
              type: 'bar',
              data: chartData.map(d => d[yFields[0]]),
              itemStyle: { 
                color: colors[0], 
                borderRadius: [4, 4, 0, 0],
                opacity: hasHighlight ? dimOpacity : undefined,
              },
              emphasis: {
                itemStyle: { opacity: highlightOpacity },
              },
              barMaxWidth: 30,
              yAxisIndex: 0,
            },
            {
              name: yFields[1] || '折线图',
              type: 'line',
              smooth: true,
              data: chartData.map(d => d[yFields[1]]),
              itemStyle: { color: colors[1], opacity: hasHighlight ? dimOpacity : undefined },
              lineStyle: { width: 2, opacity: hasHighlight ? dimOpacity : undefined },
              emphasis: {
                itemStyle: { opacity: highlightOpacity },
                lineStyle: { opacity: highlightOpacity },
              },
              yAxisIndex: 1,
            },
          ],
        }
        break
      }

      case 'pie':
      case 'doughnut': {
        const categoryField = dataConfig.categoryField || 'name'
        const valueField = dataConfig.valueField || 'value'
        const hasHighlight = highlightData.length > 0

        const pieData = chartData.map(d => {
          const highlighted = isHighlighted(d, highlightData)
          return {
            name: d[categoryField],
            value: d[valueField],
            itemStyle: hasHighlight && !highlighted ? {
              opacity: dimOpacity,
            } : undefined,
            emphasis: hasHighlight ? {
              itemStyle: {
                opacity: highlightOpacity,
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)',
              },
            } : undefined,
          }
        })

        typeSpecificOption = {
          tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)',
          },
          series: [
            {
              type: 'pie',
              radius: type === 'doughnut' ? ['40%', '70%'] : '70%',
              center: ['50%', '50%'],
              avoidLabelOverlap: true,
              itemStyle: {
                borderRadius: 6,
                borderColor: styleConfig.theme === 'dark' ? '#0d1117' : '#fff',
                borderWidth: 2,
              },
              label: {
                show: true,
                formatter: '{b}: {d}%',
                color: textColor,
                opacity: hasHighlight ? dimOpacity : undefined,
              },
              labelLine: {
                show: true,
                lineStyle: { color: axisLineColor, opacity: hasHighlight ? dimOpacity : undefined },
              },
              data: pieData,
              emphasis: {
                itemStyle: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)',
                },
                scale: true,
                scaleSize: 10,
                label: { opacity: highlightOpacity },
                labelLine: { opacity: highlightOpacity },
              },
            },
          ],
        }
        break
      }

      case 'scatter': {
        const xField = dataConfig.xField || 'x'
        const yField = dataConfig.yField || 'y'
        const seriesField = dataConfig.seriesField
        const hasHighlight = highlightData.length > 0

        if (seriesField) {
          const seriesNames = [...new Set(chartData.map(d => d[seriesField]))]
          const series = seriesNames.map((name, idx) => ({
            name,
            type: 'scatter',
            data: chartData.filter(d => d[seriesField] === name).map(d => {
              const highlighted = isHighlighted(d, highlightData)
              return {
                value: [d[xField], d[yField]],
                itemStyle: hasHighlight && !highlighted ? { opacity: dimOpacity } : undefined,
              }
            }),
            itemStyle: { color: colors[idx % colors.length] },
            symbolSize: 10,
            emphasis: {
              itemStyle: { opacity: highlightOpacity },
            },
          }))

          typeSpecificOption = {
            xAxis: {
              type: 'value',
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
            },
            yAxis: {
              type: 'value',
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
            },
            series,
          }
        } else {
          const scatterData = chartData.map(d => {
            const highlighted = isHighlighted(d, highlightData)
            return {
              value: [d[xField], d[yField]],
              itemStyle: hasHighlight && !highlighted ? { opacity: dimOpacity } : undefined,
            }
          })
          typeSpecificOption = {
            xAxis: {
              type: 'value',
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
            },
            yAxis: {
              type: 'value',
              axisLine: { lineStyle: { color: axisLineColor } },
              axisLabel: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
            },
            series: [{
              type: 'scatter',
              data: scatterData,
              itemStyle: { color: colors[0] },
              symbolSize: 10,
              emphasis: {
                itemStyle: { opacity: highlightOpacity },
              },
            }],
          }
        }
        break
      }

      case 'radar': {
        const categoryField = dataConfig.categoryField || 'name'
        const valueField = dataConfig.valueField || 'value'
        const seriesField = dataConfig.seriesField
        const hasHighlight = highlightData.length > 0

        const indicators = [...new Set(chartData.map(d => d[categoryField]))].map(name => {
          const item = chartData.find(d => d[categoryField] === name)
          return { name, max: (item?.max) || 100 }
        })

        if (seriesField) {
          const seriesNames = [...new Set(chartData.map(d => d[seriesField]))]
          const series = seriesNames.map((name, idx) => ({
            name,
            type: 'radar',
            data: [{
              value: indicators.map(ind => {
                const item = chartData.find(d => d[categoryField] === ind.name && d[seriesField] === name)
                return item ? item[valueField] : 0
              }),
              name,
            }],
            itemStyle: { color: colors[idx % colors.length], opacity: hasHighlight ? dimOpacity : undefined },
            areaStyle: { opacity: hasHighlight ? dimOpacity * 0.3 : 0.3 },
            emphasis: {
              itemStyle: { opacity: highlightOpacity },
              areaStyle: { opacity: highlightOpacity * 0.3 },
            },
          }))

          typeSpecificOption = {
            radar: {
              indicator: indicators,
              axisName: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
              splitArea: {
                areaStyle: {
                  color: styleConfig.theme === 'dark' 
                    ? ['rgba(255,255,255,0.02)', 'rgba(255,255,255,0.05)']
                    : ['#fff', '#fafafa'],
                },
              },
            },
            series,
          }
        } else {
          typeSpecificOption = {
            radar: {
              indicator: indicators,
              axisName: { color: textColor },
              splitLine: { lineStyle: { color: splitLineColor } },
              splitArea: {
                areaStyle: {
                  color: styleConfig.theme === 'dark' 
                    ? ['rgba(255,255,255,0.02)', 'rgba(255,255,255,0.05)']
                    : ['#fff', '#fafafa'],
                },
              },
            },
            series: [{
              type: 'radar',
              data: [{
                value: chartData.map(d => d[valueField]),
                name: '数据',
              }],
              itemStyle: { color: colors[0], opacity: hasHighlight ? dimOpacity : undefined },
              areaStyle: { opacity: hasHighlight ? dimOpacity * 0.3 : 0.3 },
              emphasis: {
                itemStyle: { opacity: highlightOpacity },
                areaStyle: { opacity: highlightOpacity * 0.3 },
              },
            }],
          }
        }
        break
      }

      case 'gauge': {
        const valueField = dataConfig.valueField || 'value'
        const value = chartData.length > 0 ? chartData[0][valueField] : 0

        typeSpecificOption = {
          series: [{
            type: 'gauge',
            progress: { show: true, width: 12 },
            axisLine: {
              lineStyle: {
                width: 12,
                color: [
                  [0.3, '#52c41a'],
                  [0.7, '#faad14'],
                  [1, '#ff4d4f'],
                ],
              },
            },
            axisTick: { show: false },
            splitLine: { length: 12, lineStyle: { color: axisLineColor } },
            axisLabel: { color: textColor, distance: 20, fontSize: 10 },
            pointer: { itemStyle: { color: colors[0] } },
            detail: {
              valueAnimation: true,
              formatter: '{value}',
              color: styleConfig.theme === 'dark' ? '#fff' : '#333',
              fontSize: 24,
              fontWeight: 'bold',
              offsetCenter: [0, '60%'],
            },
            data: [{ value }],
          }],
        }
        break
      }

      case 'funnel': {
        const categoryField = dataConfig.categoryField || 'name'
        const valueField = dataConfig.valueField || 'value'
        const hasHighlight = highlightData.length > 0

        typeSpecificOption = {
          tooltip: {
            trigger: 'item',
            formatter: '{b}: {c}',
          },
          series: [{
            type: 'funnel',
            left: '10%',
            width: '80%',
            label: {
              show: true,
              position: 'inside',
              formatter: '{b}: {c}',
              color: '#fff',
              opacity: hasHighlight ? dimOpacity : undefined,
            },
            labelLine: { show: false },
            itemStyle: { borderColor: '#fff', borderWidth: 2 },
            emphasis: {
              label: { fontSize: 16, opacity: highlightOpacity },
              itemStyle: { opacity: highlightOpacity },
            },
            data: chartData.map((d, idx) => {
              const highlighted = isHighlighted(d, highlightData)
              return {
                name: d[categoryField],
                value: d[valueField],
                itemStyle: { 
                  color: colors[idx % colors.length],
                  opacity: hasHighlight && !highlighted ? dimOpacity : undefined,
                },
              }
            }),
          }],
        }
        break
      }

      case 'heatmap': {
        const xField = dataConfig.xField || 'x'
        const yField = dataConfig.yField || 'y'
        const valueField = dataConfig.valueField || 'value'
        const hasHighlight = highlightData.length > 0

        const xCategories = [...new Set(chartData.map(d => d[xField]))]
        const yCategories = [...new Set(chartData.map(d => d[yField]))]

        const heatmapData = chartData.map(d => {
          const highlighted = isHighlighted(d, highlightData)
          return {
            value: [d[xField], d[yField], d[valueField]],
            itemStyle: hasHighlight && !highlighted ? { opacity: dimOpacity } : undefined,
          }
        })

        typeSpecificOption = {
          tooltip: {
            position: 'top',
            formatter: (params: any) => `${params.name}: ${params.value[2]}`,
          },
          grid: {
            height: '60%',
            top: '10%',
          },
          xAxis: {
            type: 'category',
            data: xCategories,
            splitArea: { show: true },
            axisLine: { lineStyle: { color: axisLineColor } },
            axisLabel: { color: textColor },
          },
          yAxis: {
            type: 'category',
            data: yCategories,
            splitArea: { show: true },
            axisLine: { lineStyle: { color: axisLineColor } },
            axisLabel: { color: textColor },
          },
          visualMap: {
            min: Math.min(...chartData.map(d => d[valueField])),
            max: Math.max(...chartData.map(d => d[valueField])),
            calculable: true,
            orient: 'horizontal',
            left: 'center',
            bottom: '5%',
            textStyle: { color: textColor },
          },
          series: [{
            type: 'heatmap',
            data: heatmapData,
            label: { show: false },
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowColor: 'rgba(0, 0, 0, 0.5)',
                opacity: highlightOpacity,
              },
            },
          }],
        }
        break
      }

      default:
        typeSpecificOption = {
          series: [{ type: 'bar', data: [] }],
        }
    }

    const finalOption = { ...baseOption, ...typeSpecificOption }

    if (customOption) {
      return deepMerge(finalOption, customOption)
    }

    return finalOption
  }, [
    type, 
    chartData, 
    dataConfig, 
    styleConfig, 
    customOption, 
    getColors,
    highlightData,
    isHighlighted,
  ])

  const option = useMemo(() => buildOption(), [buildOption])

  const extractChartData = useCallback((params: any): Record<string, any> => {
    const result: Record<string, any> = {}
    if (params.data) {
      if (typeof params.data === 'object' && !Array.isArray(params.data)) {
        if (params.data.value !== undefined) {
          const dataIndex = params.dataIndex
          if (dataIndex !== undefined && chartData[dataIndex]) {
            return { ...chartData[dataIndex] }
          }
        }
        return { ...params.data }
      }
    }
    if (params.dataIndex !== undefined && chartData[params.dataIndex]) {
      return { ...chartData[params.dataIndex] }
    }
    if (params.name) {
      result.name = params.name
    }
    if (params.value !== undefined) {
      result.value = params.value
    }
    if (params.seriesName) {
      result.seriesName = params.seriesName
    }
    return result
  }, [chartData])

  const handleChartClick = useCallback((params: any) => {
    if (onClick) {
      onClick(params)
    }
    if (onChartClick && componentId) {
      const chartDataItem = extractChartData(params)
      onChartClick(chartDataItem)
    }
  }, [onClick, onChartClick, componentId, extractChartData])

  const handleChartHover = useCallback((params: any) => {
    if (onHover) {
      onHover(params)
    }
    if (onChartHover && componentId) {
      const chartDataItem = extractChartData(params)
      onChartHover(chartDataItem)
    }
  }, [onHover, onChartHover, componentId, extractChartData])

  const handleChartSelect = useCallback((params: any) => {
    if (onSelect || onChartSelect) {
      const chartDataItem = extractChartData(params)
      const key = getHighlightKey(chartDataItem)
      const isSelected = selectedItems.current.has(key)
      const newSelected = !isSelected
      
      if (newSelected) {
        selectedItems.current.add(key)
      } else {
        selectedItems.current.delete(key)
      }
      
      if (onSelect) {
        onSelect(params, newSelected)
      }
      if (onChartSelect && componentId) {
        onChartSelect(chartDataItem, newSelected)
      }
    }
  }, [onSelect, onChartSelect, componentId, extractChartData, getHighlightKey])

  const onChartReady = useCallback((echartsInstance: any) => {
    if (onClick || onChartClick) {
      echartsInstance.on('click', handleChartClick)
    }
    if (onHover || onChartHover) {
      echartsInstance.on('mouseover', handleChartHover)
    }
    if (onSelect || onChartSelect) {
      echartsInstance.on('click', handleChartSelect)
    }
  }, [onClick, onHover, onSelect, onChartClick, onChartHover, onChartSelect, handleChartClick, handleChartHover, handleChartSelect])

  return (
    <div style={{ position: 'relative', width, height }}>
      <ReactECharts
        ref={chartRef}
        option={option}
        notMerge={notMerge}
        lazyUpdate={lazyUpdate}
        style={{ width: '100%', height: '100%' }}
        onChartReady={onChartReady}
      />
      {loading && (
        <div style={{
          position: 'absolute',
          top: 0, left: 0, right: 0, bottom: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: styleConfig.theme === 'dark' ? 'rgba(13,17,23,0.8)' : 'rgba(255,255,255,0.8)',
        }}>
          <div style={{ color: styleConfig.theme === 'dark' ? '#00e5ff' : '#1677ff' }}>加载中...</div>
        </div>
      )}
    </div>
  )
}

function deepMerge(target: any, source: any): any {
  const result = { ...target }
  for (const key in source) {
    if (source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])) {
      result[key] = deepMerge(result[key] || {}, source[key])
    } else {
      result[key] = source[key]
    }
  }
  return result
}

export default BaseChart
