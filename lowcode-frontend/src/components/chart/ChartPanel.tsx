import React from 'react'
import { Card, Space, Tag } from 'antd'
import BaseChart, { ChartType, ChartDataConfig, ChartStyleConfig } from './BaseChart'

export interface ChartPanelProps {
  title?: string
  subTitle?: string
  type: ChartType
  data: Record<string, any>[]
  dataConfig: ChartDataConfig
  styleConfig?: ChartStyleConfig
  width?: number | string
  height?: number | string
  loading?: boolean
  extra?: React.ReactNode
  tags?: { label: string; color: string }[]
  onClick?: (params: any) => void
  onHover?: (params: any) => void
  theme?: 'light' | 'dark'
  bordered?: boolean
  showHeader?: boolean
}

const ChartPanel: React.FC<ChartPanelProps> = ({
  title,
  subTitle,
  type,
  data,
  dataConfig,
  styleConfig = {},
  width = '100%',
  height = 300,
  loading = false,
  extra,
  tags,
  onClick,
  onHover,
  theme = 'light',
  bordered = true,
  showHeader = true,
}) => {
  const cardStyle: React.CSSProperties = {
    background: theme === 'dark' ? '#0d111788' : '#fff',
    border: bordered ? `1px solid ${theme === 'dark' ? '#00e5ff33' : '#e8e8e8'}` : 'none',
    borderRadius: 8,
  }

  const titleStyle: React.CSSProperties = {
    color: theme === 'dark' ? '#fff' : '#333',
    fontSize: 14,
    fontWeight: 500,
  }

  const subTitleStyle: React.CSSProperties = {
    color: theme === 'dark' ? '#888' : '#999',
    fontSize: 12,
    marginTop: 4,
  }

  return (
    <Card
      style={cardStyle}
      styles={{ body: { padding: 12 } }}
      size="small"
      title={
        showHeader && (
          <div>
            <div style={titleStyle}>{title || '图表'}</div>
            {subTitle && <div style={subTitleStyle}>{subTitle}</div>}
            {tags && tags.length > 0 && (
              <Space size={4} style={{ marginTop: 8 }}>
                {tags.map((tag, idx) => (
                  <Tag key={idx} color={tag.color} style={{ margin: 0 }}>
                    {tag.label}
                  </Tag>
                ))}
              </Space>
            )}
          </div>
        )
      }
      extra={extra}
    >
      <BaseChart
        type={type}
        data={data}
        dataConfig={dataConfig}
        styleConfig={{ ...styleConfig, theme }}
        width={width}
        height={height}
        loading={loading}
        onClick={onClick}
        onHover={onHover}
      />
    </Card>
  )
}

export default ChartPanel
