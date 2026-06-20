import React, { useState, useEffect } from 'react'
import { Modal, Form, Input, Alert, Descriptions, message, Space, Tag } from 'antd'
import {
  WarningOutlined,
  RollbackOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  FieldNumberOutlined,
  OrderedListOutlined,
  RiskOutlined,
} from '@ant-design/icons'
import {
  RollbackCheckResult,
  DataModelVersion,
  dataModelVersionApi,
  DataModel,
} from '@/api/dataModel'
import './RollbackConfirmModal.less'

const { TextArea } = Input

interface RollbackConfirmModalProps {
  visible: boolean
  onCancel: () => void
  onConfirm: (model: DataModel) => void
  modelId: number
  targetVersion: DataModelVersion | null
}

const RollbackConfirmModal: React.FC<RollbackConfirmModalProps> = ({
  visible,
  onCancel,
  onConfirm,
  modelId,
  targetVersion,
}) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [checkLoading, setCheckLoading] = useState(false)
  const [checkResult, setCheckResult] = useState<RollbackCheckResult | null>(null)

  const loadCheckResult = async () => {
    if (!modelId || !targetVersion?.id) return
    setCheckLoading(true)
    try {
      const res = await dataModelVersionApi.checkRollback(modelId, targetVersion.id)
      setCheckResult(res.data || null)
    } catch (e) {
      console.error('加载回滚校验结果失败:', e)
      message.error('加载回滚校验结果失败')
    } finally {
      setCheckLoading(false)
    }
  }

  useEffect(() => {
    if (visible && modelId && targetVersion?.id) {
      loadCheckResult()
      form.resetFields()
    }
  }, [visible, modelId, targetVersion])

  const handleOk = async () => {
    if (!modelId || !targetVersion?.id) return

    try {
      const values = await form.validateFields()
      setLoading(true)

      const res = await dataModelVersionApi.rollback(
        modelId,
        targetVersion.id,
        values.rollbackReason
      )

      message.success('回滚成功')
      onConfirm(res.data)
    } catch (e) {
      console.error('回滚失败:', e)
      message.error('回滚失败')
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
    form.resetFields()
    setCheckResult(null)
    onCancel()
  }

  const formatDateTime = (dateStr: string) => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <Modal
      title={
        <div className="modal-title">
          <RollbackOutlined style={{ color: '#faad14', marginRight: 8 }} />
          版本回滚确认
        </div>
      }
      open={visible}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      okText="确认回滚"
      okButtonProps={{ danger: true }}
      width={600}
      destroyOnClose
      className="dm-rollback-confirm-modal"
    >
      <Alert
        type="warning"
        message="回滚操作不可逆"
        description="回滚将恢复数据模型到所选版本。请谨慎操作。"
        showIcon
        style={{ marginBottom: 16 }}
      />

      {targetVersion && (
        <div className="version-info">
          <Descriptions column={1} bordered size="small" title="目标版本信息">
            <Descriptions.Item label="版本号">
              <strong>v{targetVersion.version}</strong>
            </Descriptions.Item>
            <Descriptions.Item label="版本名称">{targetVersion.versionName}</Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {formatDateTime(targetVersion.createdTime)}
            </Descriptions.Item>
            <Descriptions.Item label="操作人">{targetVersion.operator}</Descriptions.Item>
            <Descriptions.Item label="字段数">
              <FieldNumberOutlined /> {targetVersion.fieldCount}
            </Descriptions.Item>
            <Descriptions.Item label="索引数">
              <OrderedListOutlined /> {targetVersion.indexCount}
            </Descriptions.Item>
            {targetVersion.changeDescription && (
              <Descriptions.Item label="变更描述">
                {targetVersion.changeDescription}
              </Descriptions.Item>
            )}
          </Descriptions>
        </div>
      )}

      {checkLoading && (
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          正在校验回滚兼容性...
        </div>
      )}

      {!checkLoading && checkResult && (
        <div className="check-result">
          <div className="result-status">
            {checkResult.canRollback ? (
              <div className="status-success">
                <CheckCircleOutlined />
                <span>可以回滚</span>
              </div>
            ) : (
              <div className="status-error">
                <CloseCircleOutlined />
                <span>无法回滚</span>
              </div>
            )}
          </div>

          <div className="result-stats">
            <Space>
              <Tag icon={<FieldNumberOutlined />}>
                受影响字段：{checkResult.affectedFieldCount} 个
              </Tag>
              <Tag icon={<OrderedListOutlined />}>
                受影响索引：{checkResult.affectedIndexCount} 个
              </Tag>
              <Tag
                icon={<RiskOutlined />}
                color={checkResult.dataLossRisk ? 'red' : 'green'}
              >
                {checkResult.dataLossRisk ? '有数据丢失风险' : '无数据丢失风险'}
              </Tag>
            </Space>
          </div>

          {checkResult.warnings && checkResult.warnings.length > 0 && (
            <div className="warnings-list">
              <div className="list-title">
                <WarningOutlined style={{ color: '#faad14' }} />
                <span>警告</span>
              </div>
              <ul>
                {checkResult.warnings.map((warning, idx) => (
                  <li key={idx} className="warning-item">
                    <ExclamationCircleOutlined />
                    {warning}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {checkResult.errors && checkResult.errors.length > 0 && (
            <div className="errors-list">
              <div className="list-title">
                <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                <span>错误</span>
              </div>
              <ul>
                {checkResult.errors.map((error, idx) => (
                  <li key={idx} className="error-item">
                    <CloseCircleOutlined />
                    {error}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {checkResult.suggestion && (
            <div className="suggestion-box">
              <span className="suggestion-label">建议：</span>
              <span>{checkResult.suggestion}</span>
            </div>
          )}
        </div>
      )}

      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="rollbackReason"
          label="回滚原因（可选）"
        >
          <TextArea
            rows={3}
            placeholder="请详细说明回滚的原因，如：功能异常、数据错误等..."
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default RollbackConfirmModal
