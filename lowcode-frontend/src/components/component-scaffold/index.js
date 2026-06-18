import React, { useState } from 'react'
import { Button, Badge, Space, Typography } from 'antd'

const { Text } = Typography

const CustomComponent = (props) => {
  const {
    title = '自定义组件',
    count: initialCount = 0,
    disabled = false,
    type = 'primary',
    visible = true,
    style = {},
    onClick,
    onChange,
    ...restProps
  } = props

  const [count, setCount] = useState(initialCount)

  if (!visible) {
    return null
  }

  const handleClick = () => {
    if (disabled) return

    const newCount = count + 1
    setCount(newCount)

    if (onClick && typeof onClick === 'function') {
      onClick({
        timestamp: Date.now(),
        value: newCount,
      })
    }

    if (onChange && typeof onChange === 'function') {
      onChange({
        oldValue: count,
        newValue: newCount,
      })
    }
  }

  const baseStyle = {
    padding: 16,
    borderRadius: 8,
    background: '#f5f5f5',
    border: '1px solid #e8e8e8',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 12,
    ...style,
  }

  return (
    <div style={baseStyle} {...restProps}>
      <Space direction="vertical" align="center" style={{ width: '100%' }}>
        <Text strong style={{ fontSize: 16 }}>{title}</Text>

        <Badge count={count} color="#52c41a">
          <Button
            type={type}
            disabled={disabled}
            onClick={handleClick}
            size="large"
          >
            点击计数
          </Button>
        </Badge>

        <Text type="secondary" style={{ fontSize: 12 }}>
          当前计数: {count}
        </Text>

        {disabled && (
          <Text type="warning" style={{ fontSize: 12 }}>
            组件已禁用
          </Text>
        )}
      </Space>
    </div>
  )
}

export default CustomComponent
