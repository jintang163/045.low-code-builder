import React, { useState, useEffect } from 'react'
import {
  Timeline,
  Card,
  Button,
  Tag,
  Input,
  DatePicker,
  Space,
  Checkbox,
  Modal,
  Descriptions,
  Empty,
  Spin,
  message,
} from 'antd'
import {
  HistoryOutlined,
  RollbackOutlined,
  DiffOutlined,
  EyeOutlined,
  SearchOutlined,
  GitBranchOutlined,
  ClockCircleOutlined,
  UserOutlined,
} from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import { versionApi, VersionSnapshot } from '@/api/version'
import RollbackConfirmModal from './RollbackConfirmModal'
import VersionDiffModal from './VersionDiffModal'
import './VersionHistoryPanel.less'

const { RangePicker } = DatePicker

interface VersionHistoryPanelProps {
  resourceId: number
  resourceType: string
  appId: number
  onClose?: () => void
  onRollbackSuccess?: () => void
}

const VersionHistoryPanel: React.FC<VersionHistoryPanelProps> = ({
  resourceId,
  resourceType,
  appId,
  onClose,
  onRollbackSuccess,
}) => {
  const [snapshots, setSnapshots] = useState<VersionSnapshot[]>([])
  const [loading, setLoading] = useState(false)
  const [searchVersion, setSearchVersion] = useState('')
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentDetail, setCurrentDetail] = useState<VersionSnapshot | null>(null)
  const [rollbackModalVisible, setRollbackModalVisible] = useState(false)
  const [rollbackSnapshot, setRollbackSnapshot] = useState<VersionSnapshot | null>(null)
  const [diffModalVisible, setDiffModalVisible] = useState(false)

  const loadSnapshots = async () => {
    setLoading(true)
    try {
      const res = await versionApi.getSnapshotList({
        resourceId,
        resourceType,
        appId,
      })
      setSnapshots(res.data || [])
    } catch (e) {
      console.error('加载版本快照失败:', e)
      message.error('加载版本快照失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSnapshots()
  }, [resourceId, resourceType, appId])

  const filteredSnapshots = snapshots.filter((snapshot) => {
    let matchVersion = true
    let matchDate = true

    if (searchVersion) {
      matchVersion = snapshot.version.toLowerCase().includes(searchVersion.toLowerCase())
    }

    if (dateRange && dateRange[0] && dateRange[1]) {
      const snapshotDate = new Date(snapshot.createdTime || '')
      const startDate = dateRange[0].toDate()
      const endDate = dateRange[1].toDate()
      endDate.setHours(23, 59, 59, 999)
      matchDate = snapshotDate >= startDate && snapshotDate <= endDate
    }

    return matchVersion && matchDate
  })

  const handleCheckboxChange = (id: number, checked: boolean) => {
    if (checked) {
      if (selectedIds.length >= 2) {
        message.warning('最多只能选择两个版本进行对比')
        return
      }
      setSelectedIds([...selectedIds, id])
    } else {
      setSelectedIds(selectedIds.filter((sid) => sid !== id))
    }
  }

  const handleCompare = () => {
    if (selectedIds.length !== 2) {
      message.warning('请选择两个版本进行对比')
      return
    }
    setDiffModalVisible(true)
  }

  const handleViewDetail = async (snapshot: VersionSnapshot) => {
    if (!snapshot.id) return
    try {
      setLoading(true)
      const res = await versionApi.getSnapshotDetail(snapshot.id)
      setCurrentDetail(res.data || null)
      setDetailModalVisible(true)
    } catch (e) {
      console.error('获取快照详情失败:', e)
      message.error('获取快照详情失败')
    } finally {
      setLoading(false)
    }
  }

  const handleRollback = (snapshot: VersionSnapshot) => {
    setRollbackSnapshot(snapshot)
    setRollbackModalVisible(true)
  }

  const handleRollbackSuccess = () => {
    setRollbackModalVisible(false)
    setRollbackSnapshot(null)
    loadSnapshots()
    message.success('回滚成功')
    onRollbackSuccess?.()
  }

  const getSnapshotTypeTag = (snapshotType: number) => {
    if (snapshotType === 1) {
      return <Tag color="blue">自动快照</Tag>
    }
    return <Tag color="green">手动快照</Tag>
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
      second: '2-digit',
    })
  }

  return (
    <div className="version-history-panel">
      <div className="panel-header">
        <Space className="search-bar">
          <Input
            placeholder="按版本号搜索"
            prefix={<SearchOutlined />}
            value={searchVersion}
            onChange={(e) => setSearchVersion(e.target.value)}
            style={{ width: 200 }}
            allowClear
          />
          <RangePicker
            placeholder={['开始时间', '结束时间']}
            value={dateRange}
            onChange={(dates) => setDateRange(dates as [Dayjs, Dayjs] | null)}
            allowClear
          />
          <Button type="primary" icon={<DiffOutlined />} onClick={handleCompare} disabled={selectedIds.length !== 2}>
            对比选中 ({selectedIds.length}/2)
          </Button>
          <Button icon={<HistoryOutlined />} onClick={loadSnapshots}>
            刷新
          </Button>
        </Space>
      </div>

      <div className="panel-content">
        <Spin spinning={loading}>
          {filteredSnapshots.length > 0 ? (
            <Timeline
              mode="left"
              items={filteredSnapshots.map((snapshot) => ({
                color: snapshot.isPublished === 1 ? 'gold' : 'blue',
                label: (
                  <div className="timeline-label">
                    <div className="label-time">
                      <ClockCircleOutlined /> {formatDateTime(snapshot.createdTime)}
                    </div>
                  </div>
                ),
                children: (
                  <Card
                    size="small"
                    className={`snapshot-card ${snapshot.isPublished === 1 ? 'published' : ''}`}
                    title={
                      <Space>
                        <span className="version-number">v{snapshot.version}</span>
                        {getSnapshotTypeTag(snapshot.snapshotType)}
                        {snapshot.isPublished === 1 && <Tag color="gold">已发布</Tag>}
                        {snapshot.tag && <Tag color="purple">{snapshot.tag}</Tag>}
                      </Space>
                    }
                    extra={
                      <Checkbox
                        checked={selectedIds.includes(snapshot.id!)}
                        onChange={(e) => handleCheckboxChange(snapshot.id!, e.target.checked)}
                        onClick={(e) => e.stopPropagation()}
                      />
                    }
                  >
                    <div className="snapshot-info">
                      <Descriptions column={2} size="small">
                        <Descriptions.Item label="创建人">
                          <UserOutlined /> {snapshot.createdBy || '系统'}
                        </Descriptions.Item>
                        {snapshot.gitCommitId && (
                          <Descriptions.Item label="Git提交">
                            <Space>
                              <GitBranchOutlined />
                              <span className="git-commit">{snapshot.gitCommitId.slice(0, 7)}</span>
                              {snapshot.gitBranch && <Tag>{snapshot.gitBranch}</Tag>}
                            </Space>
                          </Descriptions.Item>
                        )}
                      </Descriptions>
                      {snapshot.description && (
                        <div className="snapshot-description">{snapshot.description}</div>
                      )}
                      {snapshot.gitCommitMessage && (
                        <div className="git-message">
                          <Tag color="cyan">Git Message</Tag>
                          <span className="message-text">{snapshot.gitCommitMessage}</span>
                        </div>
                      )}
                      <div className="snapshot-actions">
                        <Space>
                          <Button size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(snapshot)}>
                            详情
                          </Button>
                          <Button size="small" icon={<DiffOutlined />} onClick={() => {
                            setSelectedIds([snapshot.id!])
                            message.info('请再选择一个版本进行对比')
                          }}>
                            对比
                          </Button>
                          <Button
                            size="small"
                            type="primary"
                            danger
                            icon={<RollbackOutlined />}
                            onClick={() => handleRollback(snapshot)}
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
            <Empty description="暂无版本快照" />
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
            <Descriptions.Item label="快照类型">
              {currentDetail.snapshotType === 1 ? '自动快照' : '手动快照'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatDateTime(currentDetail.createdTime)}</Descriptions.Item>
            <Descriptions.Item label="创建人">{currentDetail.createdBy || '系统'}</Descriptions.Item>
            <Descriptions.Item label="资源名称">{currentDetail.resourceName}</Descriptions.Item>
            <Descriptions.Item label="是否已发布">
              {currentDetail.isPublished === 1 ? '是' : '否'}
            </Descriptions.Item>
            {currentDetail.publishedVersion && (
              <Descriptions.Item label="发布版本">{currentDetail.publishedVersion}</Descriptions.Item>
            )}
            {currentDetail.tag && <Descriptions.Item label="标签">{currentDetail.tag}</Descriptions.Item>}
            {currentDetail.description && (
              <Descriptions.Item label="描述">{currentDetail.description}</Descriptions.Item>
            )}
            {currentDetail.gitCommitId && (
              <Descriptions.Item label="Git提交ID">{currentDetail.gitCommitId}</Descriptions.Item>
            )}
            {currentDetail.gitCommitMessage && (
              <Descriptions.Item label="Git提交信息">{currentDetail.gitCommitMessage}</Descriptions.Item>
            )}
            {currentDetail.gitBranch && (
              <Descriptions.Item label="Git分支">{currentDetail.gitBranch}</Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>

      <RollbackConfirmModal
        open={rollbackModalVisible}
        snapshot={rollbackSnapshot}
        onCancel={() => {
          setRollbackModalVisible(false)
          setRollbackSnapshot(null)
        }}
        onConfirm={handleRollbackSuccess}
      />

      <VersionDiffModal
        open={diffModalVisible}
        oldSnapshotId={selectedIds[0]}
        newSnapshotId={selectedIds[1]}
        resourceType={resourceType}
        onClose={() => {
          setDiffModalVisible(false)
          setSelectedIds([])
        }}
      />
    </div>
  )
}

export default VersionHistoryPanel
