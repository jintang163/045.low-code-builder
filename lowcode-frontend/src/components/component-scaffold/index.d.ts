import React from 'react'

interface CustomComponentProps {
  title?: string
  count?: number
  disabled?: boolean
  type?: 'primary' | 'default' | 'dashed' | 'link' | 'text'
  visible?: boolean
  style?: React.CSSProperties
  onClick?: (e: any) => void
  onChange?: (e: any) => void
  [key: string]: any
}

declare const CustomComponent: React.FC<CustomComponentProps>

export default CustomComponent
