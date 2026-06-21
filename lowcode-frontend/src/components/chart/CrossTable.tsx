import React, { useMemo } from 'react'
import { Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'

export interface CrosstabColumn {
  key: string
  title: string
  dataIndex: string
  width?: number
  align?: 'left' | 'center' | 'right'
  render?: (value: any, record: any, index: number) => React.ReactNode
  sorter?: boolean
}

export interface CrosstabConfig {
  rowFields: string[]
  columnFields: string[]
  valueFields: { field: string; aggregation: 'sum' | 'avg' | 'count' | 'max' | 'min' | 'none'; label?: string }[]
  showRowTotal?: boolean
  showColumnTotal?: boolean
  showRowSubtotal?: boolean
  showColumnSubtotal?: boolean
  rowTotalLabel?: string
  columnTotalLabel?: string
}

export interface CrossTableProps {
  data: Record<string, any>[]
  config: CrosstabConfig
  style?: React.CSSProperties
  size?: 'small' | 'middle' | 'large'
  bordered?: boolean
  pagination?: boolean | any
  loading?: boolean
  theme?: 'light' | 'dark'
  onRowClick?: (record: any) => void
}

const CrossTable: React.FC<CrossTableProps> = ({
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
    const { rowFields, columnFields, valueFields, showRowTotal, showColumnTotal, rowTotalLabel = '合计', columnTotalLabel = '总计' } = config

    if (rowFields.length === 0 || valueFields.length === 0) {
      return { columns: [], dataSource: [] }
    }

    const columnValues = columnFields.length > 0 
      ? [...new Set(data.map(d => columnFields.map(f => d[f]).join('|')))]
      : ['']

    const rowGroups = groupByRows(data, rowFields)

    const columns: ColumnsType<any> = []
    
    rowFields.forEach((field, idx) => {
      columns.push({
        title: field,
        dataIndex: `row_${idx}`,
        key: `row_${idx}`,
        width: 120,
        fixed: idx === 0 ? 'left' : undefined,
        render: (text: string, record: any, index: number) => {
          if (idx === 0 || record[`row_${idx - 1}_span`]) {
            return text
          }
          return null
        },
      })
    })

    if (columnFields.length > 0) {
      columnValues.forEach(colVal => {
        valueFields.forEach(vf => {
          const title = colVal ? `${colVal} - ${vf.label || vf.field}` : (vf.label || vf.field)
          columns.push({
            title,
            dataIndex: `val_${colVal}_${vf.field}`,
            key: `val_${colVal}_${vf.field}`,
            width: 100,
            align: 'right',
            render: (value: any) => formatValue(value, vf.aggregation),
          })
        })
      })
    } else {
      valueFields.forEach(vf => {
        columns.push({
          title: vf.label || vf.field,
          dataIndex: `val_${vf.field}`,
          key: `val_${vf.field}`,
          width: 100,
          align: 'right',
          render: (value: any) => formatValue(value, vf.aggregation),
        })
      })
    }

    if (showColumnTotal) {
      valueFields.forEach(vf => {
        columns.push({
          title: columnTotalLabel,
          dataIndex: `total_${vf.field}`,
          key: `total_${vf.field}`,
          width: 100,
          align: 'right',
          fixed: 'right',
          render: (value: any) => (
            <strong>{formatValue(value, vf.aggregation)}</strong>
          ),
        })
      })
    }

    const dataSource: any[] = []

    const traverse = (groups: any[], level: number, parentRow: any = {}) => {
      groups.forEach(group => {
        const row: any = { ...parentRow }
        row[`row_${level}`] = group.label
        
        for (let i = 0; i < level; i++) {
          row[`row_${i}_span`] = false
        }
        row[`row_${level}_span`] = true

        if (level < rowFields.length - 1 && group.children) {
          traverse(group.children, level + 1, row)
          
          if (showRowSubtotal) {
            const subtotalRow: any = { ...row, _isSubtotal: true, _level: level }
            subtotalRow[`row_${level}`] = `${group.label} 小计`
            
            if (columnFields.length > 0) {
              columnValues.forEach(colVal => {
                valueFields.forEach(vf => {
                  const vals = group.children
                    .flatMap((c: any) => c.data || [])
                    .filter((d: any) => columnFields.every(f => d[f] === colVal.split('|')[columnFields.indexOf(f)]))
                    .map((d: any) => d[vf.field])
                    .filter(v => v !== null && v !== undefined && v !== '')
                  subtotalRow[`val_${colVal}_${vf.field}`] = aggregate(vals, vf.aggregation)
                })
              })
            } else {
              valueFields.forEach(vf => {
                const vals = group.children
                  .flatMap((c: any) => c.data || [])
                  .map((d: any) => d[vf.field])
                  .filter(v => v !== null && v !== undefined && v !== '')
                subtotalRow[`val_${vf.field}`] = aggregate(vals, vf.aggregation)
              })
            }

            if (showColumnTotal) {
              valueFields.forEach(vf => {
                const vals = group.children
                  .flatMap((c: any) => c.data || [])
                  .map((d: any) => d[vf.field])
                  .filter(v => v !== null && v !== undefined && v !== '')
                subtotalRow[`total_${vf.field}`] = aggregate(vals, vf.aggregation)
              })
            }
            
            dataSource.push(subtotalRow)
          }
        } else {
          const leafRow: any = { ...row, _isLeaf: true, _level: level }
          
          if (columnFields.length > 0) {
            columnValues.forEach(colVal => {
              valueFields.forEach(vf => {
                const filteredData = (group.data || []).filter(d => 
                  columnFields.every(f => {
                    const colValParts = colVal.split('|')
                    return d[f] === colValParts[columnFields.indexOf(f)]
                  })
                )
                const vals = filteredData.map(d => d[vf.field]).filter(v => v !== null && v !== undefined && v !== '')
                leafRow[`val_${colVal}_${vf.field}`] = aggregate(vals, vf.aggregation)
              })
            })
          } else {
            valueFields.forEach(vf => {
              const vals = (group.data || []).map(d => d[vf.field]).filter(v => v !== null && v !== undefined && v !== '')
              leafRow[`val_${vf.field}`] = aggregate(vals, vf.aggregation)
            })
          }

          if (showColumnTotal) {
            valueFields.forEach(vf => {
              const vals = (group.data || []).map(d => d[vf.field]).filter(v => v !== null && v !== undefined && v !== '')
              leafRow[`total_${vf.field}`] = aggregate(vals, vf.aggregation)
            })
          }
          
          dataSource.push(leafRow)
        }
      })
    }

    const rootGroups = buildRowHierarchy(data, rowFields, 0)
    traverse(rootGroups, 0)

    if (showRowTotal) {
      const totalRow: any = { _isTotal: true }
      totalRow[`row_0`] = rowTotalLabel
      
      if (columnFields.length > 0) {
        columnValues.forEach(colVal => {
          valueFields.forEach(vf => {
            const filteredData = data.filter(d => 
              columnFields.every(f => {
                const colValParts = colVal.split('|')
                return d[f] === colValParts[columnFields.indexOf(f)]
              })
            )
            const vals = filteredData.map(d => d[vf.field]).filter(v => v !== null && v !== undefined && v !== '')
            totalRow[`val_${colVal}_${vf.field}`] = aggregate(vals, vf.aggregation)
          })
        })
      } else {
        valueFields.forEach(vf => {
          const vals = data.map(d => d[vf.field]).filter(v => v !== null && v !== undefined && v !== '')
          totalRow[`val_${vf.field}`] = aggregate(vals, vf.aggregation)
        })
      }

      if (showColumnTotal) {
        valueFields.forEach(vf => {
          const vals = data.map(d => d[vf.field]).filter(v => v !== null && v !== undefined && v !== '')
          totalRow[`total_${vf.field}`] = aggregate(vals, vf.aggregation)
        })
      }
      
      dataSource.push(totalRow)
    }

    return { columns, dataSource }
  }, [data, config])

  const tableStyle: React.CSSProperties = {
    ...style,
    background: theme === 'dark' ? '#0d1117' : '#fff',
  }

  const rowClassName = (record: any) => {
    if (record._isTotal) return 'crosstab-total-row'
    if (record._isSubtotal) return 'crosstab-subtotal-row'
    return ''
  }

  return (
    <div style={tableStyle}>
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
        .crosstab-total-row td {
          background: ${theme === 'dark' ? '#1a1f3a' : '#fafafa'} !important;
          font-weight: bold;
        }
        .crosstab-subtotal-row td {
          background: ${theme === 'dark' ? '#0d1117' : '#f5f5f5'} !important;
          font-weight: 600;
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

function buildRowHierarchy(data: any[], fields: string[], level: number): any[] {
  if (level >= fields.length) return []

  const field = fields[level]
  const groups: Record<string, any[]> = {}

  data.forEach(item => {
    const key = item[field] ?? '(空)'
    if (!groups[key]) {
      groups[key] = []
    }
    groups[key].push(item)
  })

  return Object.entries(groups).map(([label, items]) => ({
    label,
    data: items,
    children: level < fields.length - 1 
      ? buildRowHierarchy(items, fields, level + 1) 
      : undefined,
  }))
}

function groupByRows(data: any[], rowFields: string[]): any[] {
  return buildRowHierarchy(data, rowFields, 0)
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

function formatValue(value: any, aggregation: string): React.ReactNode {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  
  if (typeof value === 'number') {
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

export default CrossTable
