import React, { useMemo } from 'react'
import { Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'

export interface GroupSummaryConfig {
  groupFields: string[]
  summaryFields: { 
    field: string
    aggregation: 'sum' | 'avg' | 'count' | 'max' | 'min' | 'none'
    label?: string
    format?: string
  }[]
  showGrandTotal?: boolean
  showGroupTotal?: boolean
  grandTotalLabel?: string
  groupTotalLabel?: string
  collapsible?: boolean
}

export interface GroupSummaryProps {
  data: Record<string, any>[]
  config: GroupSummaryConfig
  style?: React.CSSProperties
  size?: 'small' | 'middle' | 'large'
  bordered?: boolean
  pagination?: boolean | any
  loading?: boolean
  theme?: 'light' | 'dark'
  onRowClick?: (record: any) => void
}

const GroupSummary: React.FC<GroupSummaryProps> = ({
  data,
  config,
  style,
  size = 'middle',
  bordered = true,
  pagination = false,
  loading = false,
  theme = 'light',
  onRowClick,
}) => {
  const { columns, dataSource } = useMemo(() => {
    const { 
      groupFields, 
      summaryFields, 
      showGrandTotal = false, 
      showGroupTotal = false,
      grandTotalLabel = '总计',
      groupTotalLabel = '小计',
    } = config

    if (groupFields.length === 0 || summaryFields.length === 0) {
      return { columns: [], dataSource: [] }
    }

    const columns: ColumnsType<any> = []

    groupFields.forEach((field, idx) => {
      columns.push({
        title: field,
        dataIndex: `group_${idx}`,
        key: `group_${idx}`,
        width: 140,
      })
    })

    summaryFields.forEach(sf => {
      columns.push({
        title: sf.label || sf.field,
        dataIndex: `summary_${sf.field}`,
        key: `summary_${sf.field}`,
        width: 120,
        align: 'right',
        render: (value: any) => formatValue(value, sf.aggregation, sf.format),
      })
    })

    const dataSource: any[] = []

    const buildGroupTree = (data: any[], level: number): any[] => {
      if (level >= groupFields.length) {
        return []
      }

      const field = groupFields[level]
      const groups: Record<string, any[]> = {}

      data.forEach(item => {
        const key = item[field] ?? '(空)'
        if (!groups[key]) {
          groups[key] = []
        }
        groups[key].push(item)
      })

      const result: any[] = []

      Object.entries(groups).forEach(([label, items]) => {
        const groupRow: any = {}
        groupRow[`group_${level}`] = label
        groupRow._level = level
        groupRow._isGroupRow = true

        summaryFields.forEach(sf => {
          const vals = items.map(d => d[sf.field]).filter(v => v !== null && v !== undefined && v !== '')
          groupRow[`summary_${sf.field}`] = aggregate(vals, sf.aggregation)
        })

        result.push(groupRow)

        if (level < groupFields.length - 1) {
          const children = buildGroupTree(items, level + 1)
          result.push(...children)
        }

        if (showGroupTotal && level < groupFields.length - 1) {
          const subtotalRow: any = {}
          subtotalRow[`group_${level}`] = `${label} ${groupTotalLabel}`
          subtotalRow._isSubtotal = true
          subtotalRow._level = level

          summaryFields.forEach(sf => {
            const vals = items.map(d => d[sf.field]).filter(v => v !== null && v !== undefined && v !== '')
            subtotalRow[`summary_${sf.field}`] = aggregate(vals, sf.aggregation)
          })

          result.push(subtotalRow)
        }
      })

      return result
    }

    const groupData = buildGroupTree(data, 0)
    dataSource.push(...groupData)

    if (showGrandTotal) {
      const totalRow: any = {}
      totalRow[`group_0`] = grandTotalLabel
      totalRow._isGrandTotal = true

      summaryFields.forEach(sf => {
        const vals = data.map(d => d[sf.field]).filter(v => v !== null && v !== undefined && v !== '')
        totalRow[`summary_${sf.field}`] = aggregate(vals, sf.aggregation)
      })

      dataSource.push(totalRow)
    }

    return { columns, dataSource }
  }, [data, config])

  const rowClassName = (record: any) => {
    if (record._isGrandTotal) return 'group-summary-grand-total'
    if (record._isSubtotal) return 'group-summary-subtotal'
    if (record._isGroupRow) return `group-summary-level-${record._level}`
    return ''
  }

  return (
    <div style={style}>
      <Table
        columns={columns}
        dataSource={dataSource}
        size={size}
        bordered={bordered}
        pagination={pagination}
        loading={loading}
        rowKey={(record, index) => `row_${index}`}
        rowClassName={rowClassName}
        scroll={{ x: 'max-content' }}
        onRow={(record) => ({
          onClick: () => onRowClick?.(record),
          style: { cursor: onRowClick ? 'pointer' : 'default' },
        })}
      />
      <style>{`
        .group-summary-grand-total td {
          background: ${theme === 'dark' ? '#1a1f3a' : '#f0f5ff'} !important;
          font-weight: bold;
          color: ${theme === 'dark' ? '#00e5ff' : '#1677ff'} !important;
        }
        .group-summary-subtotal td {
          background: ${theme === 'dark' ? '#161b2d' : '#fafafa'} !important;
          font-weight: 600;
        }
        .group-summary-level-0 td {
          background: ${theme === 'dark' ? '#0d1117' : '#f9f9f9'} !important;
          font-weight: 600;
        }
        .group-summary-level-1 td {
          padding-left: 32px !important;
        }
        .group-summary-level-2 td {
          padding-left: 48px !important;
        }
        .ant-table-thead > tr > th {
          background: ${theme === 'dark' ? '#161b2d' : '#fafafa'} !important;
          color: ${theme === 'dark' ? '#ccc' : '#333'} !important;
          border-color: ${theme === 'dark' ? '#ffffff11' : '#f0f0f0'} !important;
        }
        .ant-table-tbody > tr > td {
          color: ${theme === 'dark' ? '#ccc' : '#333'} !important;
          border-color: ${theme === 'dark' ? '#ffffff11' : '#f0f0f0'} !important;
        }
        .ant-table-container {
          background: ${theme === 'dark' ? '#0d1117' : '#fff'} !important;
        }
      `}</style>
    </div>
  )
}

function aggregate(values: any[], aggregation: string): number | null {
  if (values.length === 0) return null

  const nums = values.map(v => Number(v)).filter(v => !isNaN(v))
  
  if (nums.length === 0) return null

  switch (aggregation) {
    case 'sum':
      return nums.reduce((a, b) => a + b, 0)
    case 'avg':
      return nums.reduce((a, b) => a + b, 0) / nums.length
    case 'count':
      return nums.length
    case 'max':
      return Math.max(...nums)
    case 'min':
      return Math.min(...nums)
    case 'none':
    default:
      return nums[0]
  }
}

function formatValue(value: any, aggregation: string, format?: string): React.ReactNode {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  
  if (typeof value === 'number') {
    if (format === 'percent') {
      return `${(value * 100).toFixed(2)}%`
    }
    if (format === 'currency') {
      return `¥${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
    }
    if (aggregation === 'count') {
      return value.toLocaleString()
    }
    if (Math.abs(value) >= 1000) {
      return value.toLocaleString(undefined, { maximumFractionDigits: 2 })
    }
    return value.toFixed(2)
  }
  
  return value
}

export default GroupSummary
