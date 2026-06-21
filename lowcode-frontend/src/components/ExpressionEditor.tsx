import React from 'react'

interface ExpressionEditorProps {
  value?: string
  onChange?: (value: string) => void
  fields?: any[]
  functions?: any[]
  categories?: any[]
  onValidate?: (expr: string) => Promise<any>
}

const ExpressionEditor: React.FC<ExpressionEditorProps> = ({ value, onChange }) => {
  return (
    <textarea
      value={value}
      onChange={(e) => onChange?.(e.target.value)}
      style={{ width: '100%', height: 120, fontFamily: 'monospace' }}
    />
  )
}

export default ExpressionEditor
