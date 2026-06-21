import React, { useState, useEffect } from 'react'
import { Steps, Button, Space } from 'antd'
import type { StepsProps } from 'antd/es/steps'

export interface StepsFieldProps extends StepsProps {
  current?: number
  onChange?: (current: number) => void
  items?: Array<{
    title: string
    description?: string
    icon?: React.ReactNode
    status?: 'wait' | 'process' | 'finish' | 'error'
  }>
  direction?: 'horizontal' | 'vertical'
  status?: 'wait' | 'process' | 'finish' | 'error'
  showNavigation?: boolean
  style?: React.CSSProperties
  className?: string
}

const StepsField: React.FC<StepsFieldProps> = ({
  current = 0,
  onChange,
  items = [],
  direction = 'horizontal',
  status = 'process',
  showNavigation = false,
  style,
  className,
  ...restProps
}) => {
  const [innerCurrent, setInnerCurrent] = useState<number>(current)

  useEffect(() => {
    setInnerCurrent(current)
  }, [current])

  const handlePrev = () => {
    const newCurrent = Math.max(0, innerCurrent - 1)
    setInnerCurrent(newCurrent)
    onChange?.(newCurrent)
  }

  const handleNext = () => {
    const newCurrent = Math.min(items.length - 1, innerCurrent + 1)
    setInnerCurrent(newCurrent)
    onChange?.(newCurrent)
  }

  const handleStepClick = (index: number) => {
    setInnerCurrent(index)
    onChange?.(index)
  }

  const displayItems = items.length > 0
    ? items.map((item, index) => ({
        ...item,
        status: index < innerCurrent ? 'finish' : index === innerCurrent ? status : 'wait',
      }))
    : [
        { title: '步骤一', description: '第一步描述', status: 'finish' as const },
        { title: '步骤二', description: '第二步描述', status: 'process' as const },
        { title: '步骤三', description: '第三步描述', status: 'wait' as const },
      ]

  return (
    <div className={className} style={{ width: '100%', ...style }}>
      <Steps
        current={innerCurrent}
        direction={direction}
        status={status}
        items={displayItems}
        onChange={showNavigation ? handleStepClick : undefined}
        {...restProps}
      />
      {showNavigation && (
        <div style={{ marginTop: 24, textAlign: 'center' }}>
          <Space>
            <Button
              onClick={handlePrev}
              disabled={innerCurrent === 0}
            >
              上一步
            </Button>
            <Button
              type="primary"
              onClick={handleNext}
              disabled={innerCurrent === displayItems.length - 1}
            >
              下一步
            </Button>
          </Space>
        </div>
      )}
    </div>
  )
}

export default StepsField
