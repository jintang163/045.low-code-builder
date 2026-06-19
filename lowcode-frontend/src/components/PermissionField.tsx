import React from 'react'
import { Form, Input, InputNumber, Select, Switch, DatePicker } from 'antd'
import { usePermission } from '@/hooks/usePermission'

interface PermissionFieldProps {
  fieldId: number
  label: string
  name: string
  type?: 'input' | 'number' | 'select' | 'switch' | 'date' | 'textarea'
  placeholder?: string
  required?: boolean
  options?: { label: string; value: any }[]
  rules?: any[]
}

export const PermissionField: React.FC<PermissionFieldProps> = ({
  fieldId,
  label,
  name,
  type = 'input',
  placeholder,
  required = false,
  options = [],
  rules = [],
}) => {
  const { canViewField, canEditField, loading } = usePermission()

  const canView = canViewField(fieldId)
  const canEdit = canEditField(fieldId)

  if (!canView) {
    return null
  }

  const renderField = () => {
    const isDisabled = !canEdit || loading

    switch (type) {
      case 'number':
        return <InputNumber placeholder={placeholder} disabled={isDisabled} style={{ width: '100%' }} />
      case 'select':
        return (
          <Select placeholder={placeholder} disabled={isDisabled} options={options} />
        )
      case 'switch':
        return <Switch disabled={isDisabled} />
      case 'date':
        return <DatePicker placeholder={placeholder} disabled={isDisabled} style={{ width: '100%' }} />
      case 'textarea':
        return <Input.TextArea placeholder={placeholder} disabled={isDisabled} rows={3} />
      default:
        return <Input placeholder={placeholder} disabled={isDisabled} />
    }
  }

  const fieldRules = [...rules]
  if (required && canEdit) {
    fieldRules.unshift({ required: true, message: `请输入${label}` })
  }

  return (
    <Form.Item label={label} name={name} rules={fieldRules}>
      {renderField()}
    </Form.Item>
  )
}

export default PermissionField
