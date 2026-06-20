import React, { useState, useEffect } from 'react'
import {
  Timeline,
  Card,
  Button,
  Tag,
  Space,
  Checkbox,
  Modal,
  Descriptions,
  Empty,
  Spin,
  message,
  Input,
} from 'antd'
import {
  HistoryOutlined,
  RollbackOutlined,
  DiffOutlined,
  EyeOutlined,
  CameraOutlined,
  ClockCircleOutlined,
  UserOutlined,
  FieldNumberOutlined,
  OrderedListOutlined,
} from '@ant-design/icons'
import {
  DataModelVersion,
  dataModelVersionApi,
  DataModel,
} from '@/api/dataModel'
import VersionCompareModal from './VersionCompareModal'
import RollbackConfirmModal from './RollbackConfirmModal'
import './VersionHistoryPanel.less'

const { TextArea } = Input

interface VersionHistoryPanelProps {
  modelId: number
  onClose?: () => void
  onRollbackSuccess?: (model: DataModel) => void
}

const VersionHistoryPanel: React.FC<VersionHistoryPanelProps> = ({
  modelId,
  onClose,
  onRollbackSuccess,
}) => {
  const [versions, setVersions] = useState<DataModelVersion[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [selectedVersions, setSelectedVersions] = useState<DataModelVersion[]>([])
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentDetail, setCurrentDetail] = useState<DataModelVersion | null>(null)
  const [compareModalVisible, setCompareModalVisible] = useState(false)
  const [rollbackModalVisible, setRollbackModalVisible] = useState(false)
  const [rollbackVersion, setRollbackVersion] = useState<DataModelVersion | null>(null)
  const [snapshotModalVisible, setSnapshotModalVisible] = useState(false)
  const [snapshotDescription, setSnapshotDescription] = useState('')
  const [snapshotLoading, setSnapshotLoading] = useState(false)

  const loadVersions = async () => {
    if (!modelId) return
    setLoading(true)
    try {
      const res = await dataModelVersionApi.getVersions(modelId)
      setVersions(res.data || [])
    } catch (e) {
      console.error('加载版本列表失败:', e)
      message.error('加载版本列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadVersions()
  }, [modelId])

  const handleCheckboxChange = (version: DataModelVersion, checked: boolean) => {
    if (checked) {
      if (selectedIds.length >= 2) {
        message.warning('最多只能选择两个版本进行对比')
        return
      }
      setSelectedIds([...selectedIds, version.id])
      setSelectedVersions([...selectedVersions, version])
    } else {
      setSelectedIds(selectedIds.filter((id) => id !== version.id))
      setSelectedVersions(selectedVersions.filter((v) => v.id !== version.id))
    }
  }

  const handleCompare = () => {
    if (selectedIds.length !== 2) {
      message.warning('请选择两个版本进行对比')
      return
    }
    setCompareModalVisible(true)
  }

  const handleViewDetail = async (version: DataModelVersion) => {
    try {
      setLoading(true)
      const res = await dataModelVersionApi.getVersion(version.id)
      setCurrentDetail(res.data || null)
      setDetailModalVisible(true)
    } catch (e) {
      console.error('获取版本详情失败:', e)
      message.error('获取版本详情失败')
    } finally {
      setLoading(false)
    }
  }

  const handleRollback = (version: DataModelVersion) => {
    setRollbackVersion(version)
    setRollbackModalVisible(true)
  }

  const handleRollbackSuccess = (model: DataModel) => {
    setRollbackModalVisible(false)
    setRollbackVersion(null)
    loadVersions()
    message.success('回滚成功')
    onRollbackSuccess?.(model)
  }

  const handleCreateSnapshot = async () => {
    if (!modelId) return
    setSnapshotLoading(true)
    try {
      await dataModelVersionApi.createSnapshot(modelId, snapshotDescription)
      message.success('快照创建成功')
      setSnapshotModalVisible(false)
      setSnapshotDescription('')
      loadVersions()
    } catch (e) {
      console.error('创建快照失败:', e)
      message.error('创建快照失败')
    } finally {
      setSnapshotLoading(false)
    }
  }

  const getChangeTypeTag = (changeType: number) => {
    switch (changeType) {
      case 1:
        return <Tag color="green">新增</Tag>
      case 2:
        return <Tag color="gold">修改</Tag>
      case 3:
        return <Tag color="red">删除</Tag>
      default:
        return <Tag>未知</Tag>
    }
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
      second: '2-digit',
    })
  }

  return (
    <div className="dm-version-history-panel">
      <div className="panel-header">
        <div className="header-title">
          <HistoryOutlined />
          <span>版本历史</span>
        </div>
        <Space>
          <Button type="primary" icon={<CameraOutlined />} onClick={() => setSnapshotModalVisible(true)}>
            创建快照
          </Button>
          <Button icon={<DiffOutlined />} onClick={handleCompare} disabled={selectedIds.length !== 2}>
            对比选中 ({selectedIds.length}/2)
          </Button>
          <Button icon={<HistoryOutlined />} onClick={loadVersions}>
            刷新
          </Button>
        </Space>
      </div>

      <div className="panel-content">
        <Spin spinning={loading}>
          {versions.length > 0 ? (
            <Timeline
              mode="left"
              items={versions.map((version) => ({
                color: version.changeType === 1 ? 'green' : version.changeType === 2 ? 'blue' : 'red',
                label: (
                  <div className="timeline-label">
                    <div className="label-time">
                      <ClockCircleOutlined /> {formatDateTime(version.createdTime)}
                    </div>
                  </div>
                ),
                children: (
                  <Card
                    size="small"
                    className="version-card"
                    title={
                      <Space>
                        <span className="version-number">v{version.version}</span>
                        <span className="version-name">{version.versionName}</span>
                        {getChangeTypeTag(version.changeType)}
                      </Space>
                    }
                    extra={
                      <Checkbox
                        checked={selectedIds.includes(version.id)}
                        onChange={(e) => handleCheckboxChange(version, e.target.checked)}
                        onClick={(e) => e.stopPropagation()}
                      />
                    }
                  >
                    <div className="version-info">
                      <Descriptions column={2} size="small">
                        <Descriptions.Item label="操作人">
                          <UserOutlined /> {version.operator}
                        </Descriptions.Item>
                        <Descriptions.Item label="表名">
                          {version.tableName}
                        </Descriptions.Item>
                        <Descriptions.Item label="字段数">
                          <FieldNumberOutlined /> {version.fieldCount}
                        </Descriptions.Item>
                        <Descriptions.Item label="索引数">
                          <OrderedListOutlined /> {version.indexCount}
                        </Descriptions.Item>
                      </Descriptions>
                      {version.changeDescription && (
                        <div className="change-description">{version.changeDescription}</div>
                      )}
                      <div className="version-actions">
                        <Space>
                          <Button size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(version)}>
                            详情
                          </Button>
                          <Button size="small" icon={<DiffOutlined />} onClick={() => {
                            setSelectedIds([version.id])
                            setSelectedVersions([version])
                            message.info('请再选择一个版本进行对比')
                          }}>
                            对比
                          </Button>
                          <Button
                            size="small"
                            type="primary"
                            danger
                            icon={<RollbackOutlined />}
                            onClick={() => handleRollback(version)}
                          >
                            回滚
                          </Button>
                        </Space>
                      </div>
                    </div>
                  </Card>
                ),
              }))}
            />
          ) : (
            <Empty description="暂无版本记录" />
          )}
        </Spin>
      </div>

      <Modal
        title="版本详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
        ]}
        width={700}
      >
        {currentDetail && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="版本号">v{currentDetail.version}</Descriptions.Item>
            <Descriptions.Item label="版本名称">{currentDetail.versionName}</Descriptions.Item>
            <Descriptions.Item label="变更类型">
              {getChangeTypeTag(currentDetail.changeType)}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatDateTime(currentDetail.createdTime)}</Descriptions.Item>
            <Descriptions.Item label="操作人">{currentDetail.operator}</Descriptions.Item>
            <Descriptions.Item label="模型名称">{currentDetail.modelName}</Descriptions.Item>
            <Descriptions.Item label="表名">{currentDetail.tableName}</Descriptions.Item>
            <Descriptions.Item label="字段数">{currentDetail.fieldCount}</Descriptions.Item>
            <Descriptions.Item label="索引数">{currentDetail.indexCount}</Descriptions.Item>
            {currentDetail.changeDescription && (
              <Descriptions.Item label="变更描述">{currentDetail.changeDescription}</Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>

      <Modal
        title="创建快照"
        open={snapshotModalVisible}
        onCancel={() => {
          setSnapshotModalVisible(false)
          setSnapshotDescription('')
        }}
        onOk={handleCreateSnapshot}
        confirmLoading={snapshotLoading}
        okText="创建"
        width={500}
      >
        <div className="snapshot-form">
          <p style={{ marginBottom: 12, color: '#666' }}>
            创建当前数据模型的快照版本，用于后续版本对比和回滚。
          </p>
          <div className="form-item">
            <label>快照描述（可选）</label>
            <TextArea
              rows={3}
              placeholder="请输入快照描述，便于后续识别..."
              value={snapshotDescription}
              onChange={(e) => setSnapshotDescription(e.target.value)}
              maxLength={200}
              showCount
            />
          </div>
        </div>
      </Modal>

      <VersionCompareModal
        visible={compareModalVisible}
        onCancel={() => {
          setCompareModalVisible(false)
        }}
        modelId={modelId}
        sourceVersion={selectedVersions[0]?.version || ''}
        targetVersion={selectedVersions[1]?.version || ''}
      />

      <RollbackConfirmModal
        visible={rollbackModalVisible}
        onCancel={() => {
          setRollbackModalVisible(false)
          setRollbackVersion(null)
        }}
        onConfirm={handleRollbackSuccess}
        modelId={modelId}
        targetVersion={rollbackVersion}
      />
    </div>
  )
}

export default VersionHistoryPanel
