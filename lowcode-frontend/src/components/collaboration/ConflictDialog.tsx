import React, { useState, useEffect } from 'react'
import { Modal, Button, Space, Tag, Descriptions, Typography, Divider } from 'antd'
import { WarningOutlined, CheckOutlined, CloseOutlined, EditOutlined } from '@ant-design/icons'
import { ConflictInfo, CRDTOperation, formatOperationDescription, deepDiff } from '@/utils/collaboration'
import './ConflictDialog.less'

const { Text } = Typography

interface ConflictDialogProps {
  conflict: ConflictInfo | null
  open: boolean
  onClose: () => void
  onResolve: (conflictId: string, resolution: 'A' | 'B' | 'MANUAL') => void
  currentUserId?: string
}

const conflictTypeMap: Record<string, { label: string; color: string }> = {
  PROPERTY_CONFLICT: { label: '属性冲突', color: 'orange' },
  STRUCTURE_CONFLICT: { label: '结构冲突', color: 'red' },
  DELETE_UPDATE_CONFLICT: { label: '删除/更新冲突', color: 'purple' },
}

const operationTypeMap: Record<string, { label: string; color: string }> = {
  INSERT: { label: '添加', color: 'green' },
  UPDATE: { label: '更新', color: 'blue' },
  DELETE: { label: '删除', color: 'red' },
  MOVE: { label: '移动', color: 'purple' },
  PROP_CHANGE: { label: '属性变更', color: 'orange' },
}

const OperationCard: React.FC<{
  operation: CRDTOperation
  title: string
  side: 'left' | 'right'
  isMyOperation?: boolean
}> = ({ operation, title, side, isMyOperation }) => {
  const opTypeInfo = operationTypeMap[operation.type] || { label: operation.type, color: 'default' }

  const renderDiff = () => {
    if (!operation.oldData && !operation.data) {
      return <Text type="secondary">无详细数据</Text>
    }

    if (operation.type === 'INSERT' && operation.data) {
      return (
        <div className="diff-section">
          <Text type="success">新增内容：</Text>
          <pre className="diff-content">{JSON.stringify(operation.data, null, 2)}</pre>
        </div>
      )
    }

    if (operation.type === 'DELETE' && operation.oldData) {
      return (
        <div className="diff-section">
          <Text type="danger">删除内容：</Text>
          <pre className="diff-content">{JSON.stringify(operation.oldData, null, 2)}</pre>
        </div>
      )
    }

    if (operation.oldData && operation.data) {
      const diffs = deepDiff(operation.oldData, operation.data)
      if (diffs.length === 0) {
        return <Text type="secondary">无变更</Text>
      }
      return (
        <div className="diff-section">
          <Text type="secondary">变更详情：</Text>
          <div className="diff-list">
            {diffs.slice(0, 5).map((diff, index) => (
              <div key={index} className={`diff-item diff-${diff.type}`}>
                <span className="diff-path">{diff.path}</span>
                <span className="diff-type">
                  {diff.type === 'added' && '+ '}
                  {diff.type === 'removed' && '- '}
                  {diff.type === 'updated' && '~ '}
                </span>
                <span className="diff-value">
                  {diff.type === 'added' && String(diff.newValue)}
                  {diff.type === 'removed' && String(diff.oldValue)}
                  {diff.type === 'updated' && `${String(diff.oldValue)} → ${String(diff.newValue)}`}
                </span>
              </div>
            ))}
            {diffs.length > 5 && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                ...还有 {diffs.length - 5} 处变更
              </Text>
            )}
          </div>
        </div>
      )
    }

    return <Text type="secondary">无详细数据</Text>
  }

  return (
    <div className={`operation-card operation-card-${side}`}>
      <div className="operation-card-header">
        <Space>
          <span className="operation-card-title">{title}</span>
          {isMyOperation && <Tag color="blue">我的修改</Tag>}
        </Space>
        <Tag color={opTypeInfo.color}>{opTypeInfo.label}</Tag>
      </div>
      <Descriptions column={1} size="small" className="operation-info">
        <Descriptions.Item label="操作人">{operation.username}</Descriptions.Item>
        <Descriptions.Item label="目标类型">{operation.targetType}</Descriptions.Item>
        <Descriptions.Item label="目标ID">{operation.targetId}</Descriptions.Item>
        <Descriptions.Item label="描述">{formatOperationDescription(operation)}</Descriptions.Item>
      </Descriptions>
      <Divider style={{ margin: '12px 0' }} />
      {renderDiff()}
    </div>
  )
}

export const ConflictDialog: React.FC<ConflictDialogProps> = ({
  conflict,
  open,
  onClose,
  onResolve,
  currentUserId,
}) => {
  const [countdown, setCountdown] = useState(0)

  useEffect(() => {
    if (open && conflict) {
      setCountdown(30)
    }
  }, [open, conflict])

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => {
        setCountdown((prev) => prev - 1)
      }, 1000)
      return () => clearTimeout(timer)
    }
  }, [countdown])

  if (!conflict) return null

  const conflictTypeInfo = conflictTypeMap[conflict.conflictType] || {
    label: conflict.conflictType,
    color: 'default',
  }

  const isOperationAMine = conflict.operationA.userId === currentUserId
  const isOperationBMine = conflict.operationB.userId === currentUserId

  const handleResolveA = () => {
    onResolve(conflict.conflictId, 'A')
    onClose()
  }

  const handleResolveB = () => {
    onResolve(conflict.conflictId, 'B')
    onClose()
  }

  const handleManual = () => {
    onResolve(conflict.conflictId, 'MANUAL')
    onClose()
  }

  return (
    <Modal
      title={
        <Space>
          <WarningOutlined style={{ color: '#faad14' }} />
          <span>编辑冲突</span>
          <Tag color={conflictTypeInfo.color}>{conflictTypeInfo.label}</Tag>
        </Space>
      }
      open={open}
      onCancel={onClose}
      width={800}
      closable={false}
      maskClosable={false}
      footer={
        <div className="conflict-dialog-footer">
          {countdown > 0 && (
            <Text type="secondary" className="countdown-text">
              建议在 {countdown} 秒内做出选择
            </Text>
          )}
          <Space>
            <Button icon={<EditOutlined />} onClick={handleManual}>
              手动合并
            </Button>
            <Button icon={<CheckOutlined />} type="primary" onClick={isOperationAMine ? handleResolveA : handleResolveB}>
              保留我的修改
            </Button>
            <Button icon={<CloseOutlined />} danger onClick={isOperationAMine ? handleResolveB : handleResolveA}>
              保留对方修改
            </Button>
          </Space>
        </div>
      }
      className="conflict-dialog"
    >
      <div className="conflict-description">
        <Text type="secondary">冲突描述：</Text>
        <Text>{conflict.description}</Text>
      </div>
      <div className="conflict-operations">
        <OperationCard
          operation={conflict.operationA}
          title="操作 A"
          side="left"
          isMyOperation={isOperationAMine}
        />
        <div className="conflict-vs">
          <span>VS</span>
        </div>
        <OperationCard
          operation={conflict.operationB}
          title="操作 B"
          side="right"
          isMyOperation={isOperationBMine}
        />
      </div>
    </Modal>
  )
}

export default ConflictDialog
