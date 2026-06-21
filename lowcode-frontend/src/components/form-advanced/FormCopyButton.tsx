import React, { useState } from 'react'
import { Button, Modal, Form, Input, Select, message } from 'antd'
import { CopyOutlined } from '@ant-design/icons'

export interface FormCopyButtonProps {
  sourcePageId?: number | string
  onSuccess?: (newPage: any) => void
  onError?: (error: any) => void
  type?: 'primary' | 'default' | 'dashed' | 'link' | 'text'
  size?: 'small' | 'middle' | 'large'
  disabled?: boolean
  buttonText?: string
  style?: React.CSSProperties
  className?: string
}

const FormCopyButton: React.FC<FormCopyButtonProps> = ({
  sourcePageId,
  onSuccess,
  onError,
  type = 'default',
  size = 'middle',
  disabled = false,
  buttonText = '复制表单',
  style,
  className,
}) => {
  const [modalVisible, setModalVisible] = useState(false)
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()

  const handleClick = () => {
    setModalVisible(true)
    form.resetFields()
  }

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      message.success('表单复制成功（演示）')
      onSuccess?.(values)
      setModalVisible(false)
    } catch (error) {
      onError?.(error)
      message.error('复制失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <Button
        type={type}
        size={size}
        icon={<CopyOutlined />}
        onClick={handleClick}
        disabled={disabled}
        style={style}
        className={className}
      >
        {buttonText}
      </Button>

      <Modal
        title="复制表单"
        open={modalVisible}
        onOk={handleOk}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        okText="确定复制"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="新表单名称"
            name="newPageName"
            rules={[{ required: true, message: '请输入新表单名称' }]}
          >
            <Input placeholder="请输入新表单名称" />
          </Form.Item>

          <Form.Item
            label="新表单编码"
            name="newPageCode"
            rules={[{ required: true, message: '请输入新表单编码' }]}
          >
            <Input placeholder="请输入新表单编码" />
          </Form.Item>

          <Form.Item label="复制模式" name="copyMode" initialValue="full">
            <Select>
              <Select.Option value="full">全量复制（字段+数据+样式）</Select.Option>
              <Select.Option value="structure">仅字段结构</Select.Option>
              <Select.Option value="withData">字段+数据</Select.Option>
            </Select>
          </Form.Item>

          {sourcePageId && (
            <Form.Item label="源表单ID" initialValue={sourcePageId}>
              <Input disabled />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </>
  )
}

export default FormCopyButton
