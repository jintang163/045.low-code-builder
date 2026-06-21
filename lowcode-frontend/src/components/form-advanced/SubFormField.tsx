import React, { useState, useEffect, useCallback } from 'react'
import { Table, Button, Modal, Form, Input, Space, Popconfirm, Tag, Empty } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons'
import type { ColumnType } from 'antd/es/table'

export interface SubFormFieldProps {
  value?: any[]
  onChange?: (value: any[]) => void
  columns?: Array<{
    key: string
    title: string
    dataIndex: string
    type?: 'input' | 'number' | 'select' | 'date'
    options?: Array<{ label: string; value: any }>
    width?: number | string
  }>
  title?: string
  showAddButton?: boolean
  showDeleteButton?: boolean
  showEditButton?: boolean
  minRows?: number
  maxRows?: number
  relationModelId?: string | number
  foreignKeyField?: string
  disabled?: boolean
  style?: React.CSSProperties
  className?: string
  onAdd?: () => void
  onDelete?: (index: number, record: any) => void
  onEdit?: (index: number, record: any) => void
}

const SubFormField: React.FC<SubFormFieldProps> = ({
  value = [],
  onChange,
  columns = [],
  title = '子表单',
  showAddButton = true,
  showDeleteButton = true,
  showEditButton = true,
  minRows = 0,
  maxRows = 10,
  relationModelId,
  foreignKeyField,
  disabled = false,
  style,
  className,
  onAdd,
  onDelete,
  onEdit,
}) => {
  const [dataSource, setDataSource] = useState<any[]>([])
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRecord, setEditingRecord] = useState<any>(null)
  const [editingIndex, setEditingIndex] = useState<number>(-1)
  const [viewingRecord, setViewingRecord] = useState<any>(null)
  const [viewModalVisible, setViewModalVisible] = useState(false)
  const [form] = Form.useForm()

  useEffect(() => {
    if (value) {
      setDataSource(value)
    } else {
      setDataSource([])
    }
  }, [value])

  const defaultColumns: ColumnType<any>[] = columns.length > 0
    ? columns.map(col => ({
        title: col.title,
        dataIndex: col.dataIndex,
        key: col.key || col.dataIndex,
        width: col.width,
        ellipsis: true,
      }))
    : [
        { title: '字段1', dataIndex: 'field1', key: 'field1', ellipsis: true },
        { title: '字段2', dataIndex: 'field2', key: 'field2', ellipsis: true },
      ]

  const operationColumn: ColumnType<any> = {
    title: '操作',
    key: 'operation',
    width: 150,
    fixed: 'right',
    render: (_, record, index) => (
      <Space size="small">
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => handleView(record)}
        >
          查看
        </Button>
        {showEditButton && !disabled && (
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record, index)}
          >
            编辑
          </Button>
        )}
        {showDeleteButton && !disabled && (
          <Popconfirm
            title="确定删除这条记录吗？"
            onConfirm={() => handleDelete(index)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger size="small" icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        )}
      </Space>
    ),
  }

  const tableColumns = [...defaultColumns]
  if (showEditButton || showDeleteButton) {
    if (!disabled) {
      tableColumns.push(operationColumn)
    }
  }

  const handleAdd = () => {
    if (maxRows > 0 && dataSource.length >= maxRows) {
      return
    }
    setEditingRecord(null)
    setEditingIndex(-1)
    form.resetFields()
    setModalVisible(true)
    onAdd?.()
  }

  const handleEdit = (record: any, index: number) => {
    setEditingRecord({ ...record })
    setEditingIndex(index)
    form.setFieldsValue(record)
    setModalVisible(true)
    onEdit?.(index, record)
  }

  const handleView = (record: any) => {
    setViewingRecord(record)
    setViewModalVisible(true)
  }

  const handleDelete = (index: number) => {
    if (minRows > 0 && dataSource.length <= minRows) {
      return
    }
    const newData = [...dataSource]
    const deletedRecord = newData.splice(index, 1)[0]
    setDataSource(newData)
    onChange?.(newData)
    onDelete?.(index, deletedRecord)
  }

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields()
      const newData = [...dataSource]

      if (editingIndex >= 0) {
        newData[editingIndex] = { ...newData[editingIndex], ...values }
      } else {
        newData.push(values)
      }

      setDataSource(newData)
      onChange?.(newData)
      setModalVisible(false)
      form.resetFields()
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  const handleModalCancel = () => {
    setModalVisible(false)
    form.resetFields()
  }

  const canAdd = maxRows <= 0 || dataSource.length < maxRows

  return (
    <div className={className} style={style}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 12,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontWeight: 500, fontSize: 14 }}>{title}</span>
          <Tag color="blue">{dataSource.length} 条记录</Tag>
          {maxRows > 0 && (
            <Tag color="default">最多 {maxRows} 条</Tag>
          )}
        </div>
        {showAddButton && !disabled && (
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={handleAdd}
            disabled={!canAdd}
          >
            添加
          </Button>
        )}
      </div>

      <div
        style={{
          border: '1px solid #f0f0f0',
          borderRadius: 4,
          overflow: 'hidden',
        }}
      >
        {dataSource.length > 0 ? (
          <Table
            size="small"
            dataSource={dataSource}
            columns={tableColumns}
            pagination={false}
            rowKey={(record, index) => `row_${index}`}
            scroll={{ x: 'max-content' }}
          />
        ) : (
          <Empty
            description={disabled ? '暂无数据' : '暂无数据，点击"添加"按钮添加'}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            style={{ padding: '32px 0' }}
          />
        )}
      </div>

      <Modal
        title={editingIndex >= 0 ? '编辑记录' : '添加记录'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={handleModalCancel}
        okText="确定"
        cancelText="取消"
        width={600}
      >
        <Form form={form} layout="vertical">
          {columns.length > 0 ? (
            columns.map(col => (
              <Form.Item
                key={col.key || col.dataIndex}
                label={col.title}
                name={col.dataIndex}
                rules={[{ required: false, message: `请输入${col.title}` }]}
              >
                <Input placeholder={`请输入${col.title}`} />
              </Form.Item>
            ))
          ) : (
            <>
              <Form.Item label="字段1" name="field1">
                <Input placeholder="请输入字段1" />
              </Form.Item>
              <Form.Item label="字段2" name="field2">
                <Input placeholder="请输入字段2" />
              </Form.Item>
            </>
          )}
        </Form>
      </Modal>

      <Modal
        title="查看详情"
        open={viewModalVisible}
        onCancel={() => setViewModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setViewModalVisible(false)}>
            关闭
          </Button>,
        ]}
      >
        {viewingRecord && (
          <div>
            {Object.entries(viewingRecord).map(([key, val]) => (
              <div key={key} style={{ marginBottom: 12 }}>
                <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>{key}</div>
                <div style={{ fontSize: 14 }}>{val as React.ReactNode}</div>
              </div>
            ))}
          </div>
        )}
      </Modal>
    </div>
  )
}

export default SubFormField
