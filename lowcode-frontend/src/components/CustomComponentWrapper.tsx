import React from 'react'

export const CustomComponentPreview: React.FC = () => {
  return <div>自定义组件预览</div>
}

export const useCustomComponentSchema = () => {
  return { schema: {} }
}

const CustomComponentWrapper: React.FC = () => {
  return <div>自定义组件</div>
}

export default CustomComponentWrapper
