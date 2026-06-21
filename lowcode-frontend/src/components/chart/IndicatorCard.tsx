import React from 'react'
import { Card } from 'antd'
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  MinusOutlined,
} from '@ant-design/icons'

export interface IndicatorCardProps {
  title: string
  value: string | number
  unit?: string
  trend?: 'up' | 'down' | 'none'
  trendValue?: string | number
  trendUnit?: string
  icon?: React.ReactNode
  color?: string
  bgColor?: string
  borderColor?: string
  prefix?: React.ReactNode
  suffix?: React.ReactNode
  onClick?: () => void
  style?: React.CSSProperties
  size?: 'small' | 'medium' | 'large'
  theme?: 'light' | 'dark'
}

const IndicatorCard: React.FC<IndicatorCardProps> = ({
  title,
  value,
  unit,
  trend = 'none',
  trendValue,
  trendUnit = '%',
  icon,
  color,
  bgColor,
  borderColor,
  prefix,
  suffix,
  onClick,
  style,
  size = 'medium',
  theme = 'light',
}) => {
  const sizeConfig = {
    small: { titleSize: 12, valueSize: 24, padding: 12 },
    medium: { titleSize: 14, valueSize: 32, padding: 16 },
    large: { titleSize: 16, valueSize: 48, padding: 24 },
  }[size]

  const getTrendIcon = () => {
    switch (trend) {
      case 'up':
        return <ArrowUpOutlined style={{ color: '#52c41a' }} />
      case 'down':
        return <ArrowDownOutlined style={{ color: '#ff4d4f' }} />
      default:
        return <MinusOutlined style={{ color: '#8c8c8c' }} />
    }
  }

  const getTrendColor = () => {
    switch (trend) {
      case 'up':
        return '#52c41a'
      case 'down':
        return '#ff4d4f'
      default:
        return '#8c8c8c'
    }
  }

  const cardStyle: React.CSSProperties = {
    background: bgColor || (theme === 'dark' ? '#0d111788' : '#fff'),
    border: `1px solid ${borderColor || (theme === 'dark' ? '#00e5ff33' : '#e8e8e8')}`,
    borderRadius: 8,
    cursor: onClick ? 'pointer' : 'default',
    ...style,
  }

  const valueColor = color || (theme === 'dark' ? '#00e5ff' : '#1677ff')
  const titleColor = theme === 'dark' ? '#ccc' : '#666'

  return (
    <Card styles={{ body: { padding: sizeConfig.padding } }} style={cardStyle} onClick={onClick}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{ flex: 1 }}>
          <div style={{ 
            fontSize: sizeConfig.titleSize, 
            color: titleColor, 
            marginBottom: 8,
            display: 'flex',
            alignItems: 'center',
            gap: 6,
          }}>
            {prefix}
            {title}
            {suffix}
          </div>
          <div style={{ 
            fontSize: sizeConfig.valueSize, 
            fontWeight: 'bold', 
            color: valueColor,
            lineHeight: 1.2,
            marginBottom: 8,
          }}>
            {value}
            {unit && <span style={{ fontSize: sizeConfig.titleSize, marginLeft: 4, fontWeight: 'normal' }}>{unit}</span>}
          </div>
          {trendValue !== undefined && (
            <div style={{ 
              fontSize: sizeConfig.titleSize, 
              color: getTrendColor(),
              display: 'flex',
              alignItems: 'center',
              gap: 4,
            }}>
              {getTrendIcon()}
              <span>{trendValue}{trendUnit}</span>
              <span style={{ color: titleColor, marginLeft: 4 }}>较上期</span>
            </div>
          )}
        </div>
        {icon && (
          <div style={{ 
            fontSize: sizeConfig.valueSize * 1.2, 
            opacity: 0.6,
            color: valueColor,
          }}>
            {icon}
          </div>
        )}
      </div>
    </Card>
  )
}

export default IndicatorCard
