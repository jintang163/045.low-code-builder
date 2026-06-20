import React, { useState, useEffect } from 'react'
import { Modal, Tabs, Statistic, Row, Col, List, Collapse, Tag, Empty, Spin, message, Space } from 'antd'
import {
  PlusOutlined,
  MinusOutlined,
  EditOutlined,
  FileTextOutlined,
  DatabaseOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { versionApi, VersionDiffVO, DiffItem } from '@/api/version'
import './VersionDiffModal.less'

const { TabPane } = Tabs
const { Panel } = Collapse

interface VersionDiffModalProps {
  open: boolean
  oldSnapshotId: number
  newSnapshotId: number
  resourceType?: string
  onClose: () => void
}

interface DiffStats {
  added: number
  modified: number
  removed: number
}

const VersionDiffModal: React.FC<VersionDiffModalProps> = ({
  open,
  oldSnapshotId,
  newSnapshotId,
  resourceType,
  onClose,
}) => {
  const [loading, setLoading] = useState(false)
  const [diffData, setDiffData] = useState<VersionDiffVO | null>(null)
  const [activeTab, setActiveTab] = useState('page')
  const [expandedKeys, setExpandedKeys] = useState<string[]>(['page', 'dataModel', 'logic'])

  const loadDiffData = async () => {
    if (!oldSnapshotId || !newSnapshotId) return
    setLoading(true)
    try {
      const res = await versionApi.compareVersions({
        oldSnapshotId,
        newSnapshotId,
        resourceType,
      })
      setDiffData(res.data || null)
    } catch (e) {
      console.error('加载版本差异失败:', e)
      message.error('加载版本差异失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (open && oldSnapshotId && newSnapshotId) {
      loadDiffData()
    }
  }, [open, oldSnapshotId, newSnapshotId, resourceType])

  const calculateStats = (diffs: DiffItem[]): DiffStats => {
    return diffs.reduce(
      (acc, item) => {
        if (item.diffType === 'added') acc.added++
        else if (item.diffType === 'modified') acc.modified++
        else if (item.diffType === 'removed') acc.removed++
        return acc
      },
      { added: 0, modified: 0, removed: 0 }
    )
  }

  const getTotalStats = (): DiffStats => {
    if (!diffData) return { added: 0, modified: 0, removed: 0 }
    const pageStats = calculateStats(diffData.pageDiffs || [])
    const dataModelStats = calculateStats(diffData.dataModelDiffs || [])
    const logicStats = calculateStats(diffData.logicDiffs || [])
    return {
      added: pageStats.added + dataModelStats.added + logicStats.added,
      modified: pageStats.modified + dataModelStats.modified + logicStats.modified,
      removed: pageStats.removed + dataModelStats.removed + logicStats.removed,
    }
  }

  const getDiffTypeTag = (diffType: string) => {
    switch (diffType) {
      case 'added':
        return (
          <Tag color="green" icon={<PlusOutlined />}>
            新增
          </Tag>
        )
      case 'removed':
        return (
          <Tag color="red" icon={<MinusOutlined />}>
            删除
          </Tag>
        )
      case 'modified':
        return (
          <Tag color="gold" icon={<EditOutlined />}>
            修改
          </Tag>
        )
      default:
        return <Tag>{diffType}</Tag>
    }
  }

  const getDiffItemClass = (diffType: string) => {
    switch (diffType) {
      case 'added':
        return 'diff-item-added'
      case 'removed':
        return 'diff-item-removed'
      case 'modified':
        return 'diff-item-modified'
      default:
        return ''
    }
  }

  const renderDiffList = (diffs: DiffItem[], type: string) => {
    if (!diffs || diffs.length === 0) {
      return <Empty description={`暂无${type}差异`} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    return (
      <List
        dataSource={diffs}
        renderItem={(item) => (
          <List.Item className={`diff-item ${getDiffItemClass(item.diffType)}`}>
            <List.Item.Meta
              avatar={getDiffTypeTag(item.diffType)}
              title={<span className="diff-field">{item.field}</span>}
              description={
                <div className="diff-content">
                  {item.diffType === 'added' && (
                    <div className="diff-value new-value">
                      <span className="diff-label">新值：</span>
                      <code>{item.newValue}</code>
                    </div>
                  )}
                  {item.diffType === 'removed' && (
                    <div className="diff-value old-value">
                      <span className="diff-label">旧值：</span>
                      <code>{item.oldValue}</code>
                    </div>
                  )}
                  {item.diffType === 'modified' && (
                    <Space direction="vertical" style={{ width: '100%' }}>
                      <div className="diff-value old-value">
                        <span className="diff-label">旧值：</span>
                        <code>{item.oldValue}</code>
                      </div>
                      <div className="diff-value new-value">
                        <span className="diff-label">新值：</span>
                        <code>{item.newValue}</code>
                      </div>
                    </Space>
                  )}
                  {item.path && <div className="diff-path">路径：{item.path}</div>}
                </div>
              }
            />
          </List.Item>
        )}
      />
    )
  }

  const totalStats = getTotalStats()
  const pageStats = diffData ? calculateStats(diffData.pageDiffs || []) : { added: 0, modified: 0, removed: 0 }
  const dataModelStats = diffData ? calculateStats(diffData.dataModelDiffs || []) : { added: 0, modified: 0, removed: 0 }
  const logicStats = diffData ? calculateStats(diffData.logicDiffs || []) : { added: 0, modified: 0, removed: 0 }

  return (
    <Modal
      title={
        <Space>
          <span>版本差异对比</span>
          {diffData && (
            <Tag color="blue">
              v{diffData.oldVersion} → v{diffData.newVersion}
            </Tag>
          )}
        </Space>
      }
      open={open}
      onCancel={onClose}
      width={1000}
      footer={null}
      className="version-diff-modal"
    >
      <Spin spinning={loading}>
        {diffData ? (
          <div className="diff-container">
            <div className="diff-stats">
              <Row gutter={16}>
                <Col span={8}>
                  <Statistic
                    title="新增"
                    value={totalStats.added}
                    valueStyle={{ color: '#52c41a' }}
                    prefix={<PlusOutlined />}
                  />
                </Col>
                <Col span={8}>
                  <Statistic
                    title="修改"
                    value={totalStats.modified}
                    valueStyle={{ color: '#faad14' }}
                    prefix={<EditOutlined />}
                  />
                </Col>
                <Col span={8}>
                  <Statistic
                    title="删除"
                    value={totalStats.removed}
                    valueStyle={{ color: '#ff4d4f' }}
                    prefix={<MinusOutlined />}
                  />
                </Col>
              </Row>
            </div>

            <div className="diff-versions">
              <Row gutter={16}>
                <Col span={12}>
                  <div className="version-box old-version">
                    <div className="version-label">旧版本</div>
                    <div className="version-number">v{diffData.oldVersion}</div>
                    <div className="version-id">快照ID: {diffData.oldSnapshotId}</div>
                  </div>
                </Col>
                <Col span={12}>
                  <div className="version-box new-version">
                    <div className="version-label">新版本</div>
                    <div className="version-number">v{diffData.newVersion}</div>
                    <div className="version-id">快照ID: {diffData.newSnapshotId}</div>
                  </div>
                </Col>
              </Row>
            </div>

            <Tabs activeKey={activeTab} onChange={setActiveTab}>
              <TabPane
                tab={
                  <span>
                    <FileTextOutlined /> 页面配置
                    {pageStats.added + pageStats.modified + pageStats.removed > 0 && (
                      <Tag color="red" style={{ marginLeft: 8 }}>
                        {pageStats.added + pageStats.modified + pageStats.removed}
                      </Tag>
                    )}
                  </span>
                }
                key="page"
              >
                <Collapse
                  activeKey={expandedKeys.includes('page') ? ['page'] : []}
                  onChange={(keys) => {
                    if (keys.includes('page')) {
                      setExpandedKeys([...new Set([...expandedKeys, 'page'])])
                    } else {
                      setExpandedKeys(expandedKeys.filter((k) => k !== 'page'))
                    }
                  }}
                >
                  <Panel
                    header={
                      <Space>
                        <span>页面配置差异</span>
                        {pageStats.added > 0 && <Tag color="green">+{pageStats.added}</Tag>}
                        {pageStats.modified > 0 && <Tag color="gold">~{pageStats.modified}</Tag>}
                        {pageStats.removed > 0 && <Tag color="red">-{pageStats.removed}</Tag>}
                      </Space>
                    }
                    key="page"
                  >
                    {renderDiffList(diffData.pageDiffs || [], '页面配置')}
                  </Panel>
                </Collapse>
              </TabPane>

              <TabPane
                tab={
                  <span>
                    <DatabaseOutlined /> 数据模型
                    {dataModelStats.added + dataModelStats.modified + dataModelStats.removed > 0 && (
                      <Tag color="red" style={{ marginLeft: 8 }}>
                        {dataModelStats.added + dataModelStats.modified + dataModelStats.removed}
                      </Tag>
                    )}
                  </span>
                }
                key="dataModel"
              >
                <Collapse
                  activeKey={expandedKeys.includes('dataModel') ? ['dataModel'] : []}
                  onChange={(keys) => {
                    if (keys.includes('dataModel')) {
                      setExpandedKeys([...new Set([...expandedKeys, 'dataModel'])])
                    } else {
                      setExpandedKeys(expandedKeys.filter((k) => k !== 'dataModel'))
                    }
                  }}
                >
                  <Panel
                    header={
                      <Space>
                        <span>数据模型差异</span>
                        {dataModelStats.added > 0 && <Tag color="green">+{dataModelStats.added}</Tag>}
                        {dataModelStats.modified > 0 && <Tag color="gold">~{dataModelStats.modified}</Tag>}
                        {dataModelStats.removed > 0 && <Tag color="red">-{dataModelStats.removed}</Tag>}
                      </Space>
                    }
                    key="dataModel"
                  >
                    {renderDiffList(diffData.dataModelDiffs || [], '数据模型')}
                  </Panel>
                </Collapse>
              </TabPane>

              <TabPane
                tab={
                  <span>
                    <ThunderboltOutlined /> 业务逻辑
                    {logicStats.added + logicStats.modified + logicStats.removed > 0 && (
                      <Tag color="red" style={{ marginLeft: 8 }}>
                        {logicStats.added + logicStats.modified + logicStats.removed}
                      </Tag>
                    )}
                  </span>
                }
                key="logic"
              >
                <Collapse
                  activeKey={expandedKeys.includes('logic') ? ['logic'] : []}
                  onChange={(keys) => {
                    if (keys.includes('logic')) {
                      setExpandedKeys([...new Set([...expandedKeys, 'logic'])])
                    } else {
                      setExpandedKeys(expandedKeys.filter((k) => k !== 'logic'))
                    }
                  }}
                >
                  <Panel
                    header={
                      <Space>
                        <span>业务逻辑差异</span>
                        {logicStats.added > 0 && <Tag color="green">+{logicStats.added}</Tag>}
                        {logicStats.modified > 0 && <Tag color="gold">~{logicStats.modified}</Tag>}
                        {logicStats.removed > 0 && <Tag color="red">-{logicStats.removed}</Tag>}
                      </Space>
                    }
                    key="logic"
                  >
                    {renderDiffList(diffData.logicDiffs || [], '业务逻辑')}
                  </Panel>
                </Collapse>
              </TabPane>
            </Tabs>
          </div>
        ) : (
          <Empty description="暂无差异数据" />
        )}
      </Spin>
    </Modal>
  )
}

export default VersionDiffModal
