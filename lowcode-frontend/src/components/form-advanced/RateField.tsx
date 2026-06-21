import React, { useState, useEffect } from 'react'
import { Rate, Tooltip } from 'antd'
import type { RateProps } from 'antd/es/rate'

export interface RateFieldProps extends RateProps {
  value?: number
  onChange?: (value: number) => void
  allowHalf?: boolean
  allowClear?: boolean
  count?: number
  disabled?: boolean
  character?: React.ReactNode
  style?: React.CSSProperties
  className?: string
}

const RateField: React.FC<RateFieldProps> = ({
  value,
  onChange,
  allowHalf = false,
  allowClear = true,
  count = 5,
  disabled = false,
  character = '★',
  style,
  className,
  ...restProps
}) => {
  const [innerValue, setInnerValue] = useState<number>(value || 0)

  useEffect(() => {
    if (value !== undefined) {
      setInnerValue(value)
    }
  }, [value])

  const handleChange = (val: number) => {
    setInnerValue(val)
    onChange?.(val)
  }

  return (
    <div className={className} style={style}>
      <Tooltip title={innerValue ? `${innerValue} 分` : '请评分'}>
        <Rate
          value={innerValue}
          onChange={handleChange}
          allowHalf={allowHalf}
          allowClear={allowClear}
          count={count}
          disabled={disabled}
          character={character}
          {...restProps}
        />
      </Tooltip>
    </div>
  )
}

export default RateField
