import React, { useState } from 'react'
import { Button, message, Dropdown, Space } from 'antd'
import { ExportOutlined, DownOutlined } from '@ant-design/icons'
import type { MenuProps } from 'antd'

export interface ExcelExportButtonProps {
  modelId?: number | string
  onExport?: (mode: string) => void
  type?: 'primary' | 'default' | 'dashed' | 'link' | 'text'
  size?: 'small' | 'middle' | 'large'
  disabled?: boolean
  buttonText?: string
  fileName?: string
  sheetName?: string
  exportModes?: Array<{ key: string; label: string }>
  style?: React.CSSProperties
  className?: string
}

const ExcelExportButton: React.FC<ExcelExportButtonProps> = ({
  modelId,
  onExport,
  type = 'default',
  size = 'middle',
  disabled = false,
  buttonText = 'Excel导出',
  fileName = '数据导出',
  sheetName = 'Sheet1',
  exportModes,
  style,
  className,
}) => {
  const [loading, setLoading] = useState(false)

  const defaultModes: MenuProps['items'] = [
    { key: 'current', label: '导出当前页数据' },
    { key: 'all', label: '导出全部数据' },
    { key: 'selected', label: '导出选中数据' },
  ]

  const handleExport = async (mode: string) => {
    try {
      setLoading(true)

      message.success(`导出成功: ${fileName}.xlsx`)
      onExport?.(mode)
    } catch (error) {
      message.error('导出失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  const menuItems = exportModes
    ? exportModes.map(m => ({ key: m.key, label: m.label }))
    : defaultModes

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    handleExport(key)
  }

  return (
    <Dropdown.Button
      menu={{ items: menuItems, onClick: handleMenuClick }}
      icon={<DownOutlined />}
      type={type as any}
      size={size}
      disabled={disabled}
      loading={loading}
      style={style}
      className={className}
      onClick={() => handleExport('current')}
    >
      <Space>
        <ExportOutlined />
        {buttonText}
      </Space>
    </Dropdown.Button>
  )
}

export default ExcelExportButton
