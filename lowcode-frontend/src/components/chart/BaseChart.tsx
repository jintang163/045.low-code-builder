import React, { useEffect, useRef, useMemo, useCallback } from 'react'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'

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
  option?: any
  notMerge?: boolean
  lazyUpdate?: boolean
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
  option: customOption,
  notMerge = false,
  lazyUpdate = false,
}) => {
  const chartRef = useRef<any>(null)

  const getColors = useCallback(() => {
    if (styleConfig.colorList && styleConfig.colorList.length > 0) {
      return styleConfig.colorList
    }
    return styleConfig.theme === 'dark' ? darkColors : defaultColors
  }, [styleConfig.theme, styleConfig.colorList])

  const buildOption = useCallback(() => {
    const colors = getColors()
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

    let typeSpecificOption: any = {}

    switch (type) {
      case 'line':
      case 'area': {
        const xField = dataConfig.xField || 'name'
        const yFields = Array.isArray(dataConfig.yField) ? dataConfig.yField : (dataConfig.yField ? [dataConfig.yField] : ['value'])
        const seriesField = dataConfig.seriesField

        if (seriesField) {
          const categories = [...new Set(data.map(d => d[xField]))]
          const seriesNames = [...new Set(data.map(d => d[seriesField]))]
          const series = seriesNames.map((name, idx) => {
            const seriesData = categories.map(cat => {
              const item = data.find(d => d[xField] === cat && d[seriesField] === name)
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
              } : undefined,
              itemStyle: { color: colors[idx % colors.length] },
              lineStyle: { width: 2 },
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
          const xData = data.map(d => d[xField])
          const series = yFields.map((field, idx) => ({
            name: field,
            type: 'line',
            smooth: true,
            data: data.map(d => d[field]),
            areaStyle: type === 'area' ? {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: colors[idx % colors.length] + '88' },
                  { offset: 1, color: colors[idx % colors.length] + '00' },
                ],
              },
            } : undefined,
            itemStyle: { color: colors[idx % colors.length] },
            lineStyle: { width: 2 },
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

        if (seriesField) {
          const categories = [...new Set(data.map(d => d[xField]))]
          const seriesNames = [...new Set(data.map(d => d[seriesField]))]
          const series = seriesNames.map((name, idx) => ({
            name,
            type: 'bar',
            stack: type === 'barStack' ? 'total' : undefined,
            data: categories.map(cat => {
              const item = data.find(d => d[xField] === cat && d[seriesField] === name)
              return item ? item[yFields[0]] : 0
            }),
            itemStyle: { color: colors[idx % colors.length], borderRadius: [4, 4, 0, 0] },
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
            data: data.map(d => d[field]),
            itemStyle: { color: colors[idx % colors.length], borderRadius: [4, 4, 0, 0] },
            barMaxWidth: 30,
          }))

          typeSpecificOption = {
            xAxis: {
              type: 'category',
              data: data.map(d => d[xField]),
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
        
        typeSpecificOption = {
          xAxis: {
            type: 'category',
            data: data.map(d => d[xField]),
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
              data: data.map(d => d[yFields[0]]),
              itemStyle: { color: colors[0], borderRadius: [4, 4, 0, 0] },
              barMaxWidth: 30,
              yAxisIndex: 0,
            },
            {
              name: yFields[1] || '折线图',
              type: 'line',
              smooth: true,
              data: data.map(d => d[yFields[1]]),
              itemStyle: { color: colors[1] },
              lineStyle: { width: 2 },
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

        const pieData = data.map(d => ({
          name: d[categoryField],
          value: d[valueField],
        }))

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
              },
              labelLine: {
                show: true,
                lineStyle: { color: axisLineColor },
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

        if (seriesField) {
          const seriesNames = [...new Set(data.map(d => d[seriesField]))]
          const series = seriesNames.map((name, idx) => ({
            name,
            type: 'scatter',
            data: data.filter(d => d[seriesField] === name).map(d => [d[xField], d[yField]]),
            itemStyle: { color: colors[idx % colors.length] },
            symbolSize: 10,
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
              data: data.map(d => [d[xField], d[yField]]),
              itemStyle: { color: colors[0] },
              symbolSize: 10,
            }],
          }
        }
        break
      }

      case 'radar': {
        const categoryField = dataConfig.categoryField || 'name'
        const valueField = dataConfig.valueField || 'value'
        const seriesField = dataConfig.seriesField

        const indicators = [...new Set(data.map(d => d[categoryField]))].map(name => {
          const item = data.find(d => d[categoryField] === name)
          return { name, max: (item?.max) || 100 }
        })

        if (seriesField) {
          const seriesNames = [...new Set(data.map(d => d[seriesField]))]
          const series = seriesNames.map((name, idx) => ({
            name,
            type: 'radar',
            data: [{
              value: indicators.map(ind => {
                const item = data.find(d => d[categoryField] === ind.name && d[seriesField] === name)
                return item ? item[valueField] : 0
              }),
              name,
            }],
            itemStyle: { color: colors[idx % colors.length] },
            areaStyle: { opacity: 0.3 },
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
                value: data.map(d => d[valueField]),
                name: '数据',
              }],
              itemStyle: { color: colors[0] },
              areaStyle: { opacity: 0.3 },
            }],
          }
        }
        break
      }

      case 'gauge': {
        const valueField = dataConfig.valueField || 'value'
        const value = data.length > 0 ? data[0][valueField] : 0

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
            },
            labelLine: { show: false },
            itemStyle: { borderColor: '#fff', borderWidth: 2 },
            emphasis: {
              label: { fontSize: 16 },
            },
            data: data.map((d, idx) => ({
              name: d[categoryField],
              value: d[valueField],
              itemStyle: { color: colors[idx % colors.length] },
            })),
          }],
        }
        break
      }

      case 'heatmap': {
        const xField = dataConfig.xField || 'x'
        const yField = dataConfig.yField || 'y'
        const valueField = dataConfig.valueField || 'value'

        const xCategories = [...new Set(data.map(d => d[xField]))]
        const yCategories = [...new Set(data.map(d => d[yField]))]

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
            min: Math.min(...data.map(d => d[valueField])),
            max: Math.max(...data.map(d => d[valueField])),
            calculable: true,
            orient: 'horizontal',
            left: 'center',
            bottom: '5%',
            textStyle: { color: textColor },
          },
          series: [{
            type: 'heatmap',
            data: data.map(d => [d[xField], d[yField], d[valueField]]),
            label: { show: false },
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowColor: 'rgba(0, 0, 0, 0.5)',
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
  }, [type, data, dataConfig, styleConfig, customOption, getColors])

  const option = useMemo(() => buildOption(), [buildOption])

  const onChartReady = useCallback((echarts: any) => {
    if (onClick) {
      echarts.on('click', onClick)
    }
    if (onHover) {
      echarts.on('mouseover', onHover)
    }
  }, [onClick, onHover])

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
