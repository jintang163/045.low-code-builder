import React, { useState } from 'react'
import { Modal, Form, Input, Checkbox, Alert, Descriptions, Tag, message } from 'antd'
import { WarningOutlined, RollbackOutlined, SaveOutlined } from '@ant-design/icons'
import { versionApi, VersionSnapshot } from '@/api/version'
import './RollbackConfirmModal.less'

const { TextArea } = Input

interface RollbackConfirmModalProps {
  open: boolean
  snapshot: VersionSnapshot | null
  onCancel: () => void
  onConfirm: () => void
}

const RollbackConfirmModal: React.FC<RollbackConfirmModalProps> = ({
  open,
  snapshot,
  onCancel,
  onConfirm,
}) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [createBackup, setCreateBackup] = useState(true)

  const handleOk = async () => {
    if (!snapshot?.id) return

    try {
      const values = await form.validateFields()
      setLoading(true)

      await versionApi.rollbackToSnapshot(snapshot.id!, {
        snapshotId: snapshot.id!,
        rollbackReason: values.rollbackReason,
        createNewSnapshot: createBackup,
      })

      message.success('回滚成功')
      onConfirm()
    } catch (e) {
      console.error('回滚失败:', e)
      message.error('回滚失败')
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
    form.resetFields()
    setCreateBackup(true)
    onCancel()
  }

  const formatDateTime = (dateStr?: string) => {
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
      open={open}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      okText="确认回滚"
      okButtonProps={{ danger: true }}
      width={520}
      destroyOnClose
      className="rollback-confirm-modal"
    >
      <Alert
        type="warning"
        message="回滚操作不可逆"
        description="回滚将恢复到所选版本的配置。建议在回滚前创建当前状态的备份快照。"
        showIcon
        style={{ marginBottom: 16 }}
      />

      {snapshot && (
        <div className="snapshot-info">
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="目标版本">
              <strong>v{snapshot.version}</strong>
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {formatDateTime(snapshot.createdTime)}
            </Descriptions.Item>
            <Descriptions.Item label="创建人">
              {snapshot.createdBy || '系统'}
            </Descriptions.Item>
            <Descriptions.Item label="资源名称">
              {snapshot.resourceName}
            </Descriptions.Item>
            <Descriptions.Item label="快照类型">
              {snapshot.snapshotType === 1 ? '自动快照' : '手动快照'}
            </Descriptions.Item>
            {snapshot.description && (
              <Descriptions.Item label="版本描述">
                {snapshot.description}
              </Descriptions.Item>
            )}
            {snapshot.gitCommitId && (
              <Descriptions.Item label="Git 提交">
                <span style={{ fontFamily: 'monospace' }}>
                  {snapshot.gitCommitId.slice(0, 8)}
                </span>
                {snapshot.gitBranch && <Tag color="blue">{snapshot.gitBranch}</Tag>}
              </Descriptions.Item>
            )}
          </Descriptions>
        </div>
      )}

      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="rollbackReason"
          label="回滚原因"
          rules={[{ required: true, message: '请输入回滚原因' }]}
        >
          <TextArea
            rows={3}
            placeholder="请详细说明回滚的原因，如：功能异常、用户反馈问题等..."
          />
        </Form.Item>

        <Form.Item>
          <Checkbox
            checked={createBackup}
            onChange={(e) => setCreateBackup(e.target.checked)}
          >
            <SaveOutlined /> 回滚前创建快照备份
          </Checkbox>
          <div className="backup-hint">
            启用后将在回滚前自动创建当前版本的快照备份，便于后续可通过版本历史中恢复。
          </div>
        </Form.Item>
      </Form>
    </Modal>
  )
}

export default RollbackConfirmModal
