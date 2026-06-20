import React, { useState, useEffect } from 'react'
import { Modal, Tabs, Statistic, Row, Col, List, Tag, Empty, Spin, message, Space, Button } from 'antd'
import {
  PlusOutlined,
  MinusOutlined,
  EditOutlined,
  FieldNumberOutlined,
  OrderedListOutlined,
  SettingOutlined,
  RollbackOutlined,
  CheckCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import {
  VersionCompareResult,
  FieldChange,
  IndexChange,
  ModelPropertyChange,
  dataModelVersionApi,
  DataModelVersion,
} from '@/api/dataModel'
import './VersionCompareModal.less'

const { TabPane } = Tabs

interface VersionCompareModalProps {
  visible: boolean
  onCancel: () => void
  modelId: number
  sourceVersion: string
  targetVersion: string
  onRollback?: (version: DataModelVersion) => void
}

const VersionCompareModal: React.FC<VersionCompareModalProps> = ({
  visible,
  onCancel,
  modelId,
  sourceVersion,
  targetVersion,
  onRollback,
}) => {
  const [loading, setLoading] = useState(false)
  const [compareResult, setCompareResult] = useState<VersionCompareResult | null>(null)

  const loadCompareResult = async () => {
    if (!modelId || !sourceVersion || !targetVersion) return
    setLoading(true)
    try {
      const res = await dataModelVersionApi.compareVersions(modelId, sourceVersion, targetVersion)
      setCompareResult(res.data || null)
    } catch (e) {
      console.error('加载版本对比结果失败:', e)
      message.error('加载版本对比结果失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (visible && modelId && sourceVersion && targetVersion) {
      loadCompareResult()
    }
  }, [visible, modelId, sourceVersion, targetVersion])

  const getChangeTypeTag = (changeType: string) => {
    switch (changeType) {
      case 'ADD':
        return (
          <Tag color="green" icon={<PlusOutlined />}>
            新增
          </Tag>
        )
      case 'DELETE':
        return (
          <Tag color="red" icon={<MinusOutlined />}>
            删除
          </Tag>
        )
      case 'MODIFY':
        return (
          <Tag color="gold" icon={<EditOutlined />}>
            修改
          </Tag>
        )
      default:
        return <Tag>{changeType}</Tag>
    }
  }

  const getChangeItemClass = (changeType: string) => {
    switch (changeType) {
      case 'ADD':
        return 'change-item-added'
      case 'DELETE':
        return 'change-item-deleted'
      case 'MODIFY':
        return 'change-item-modified'
      default:
        return ''
    }
  }

  const renderFieldChangeList = (changes: FieldChange[]) => {
    if (!changes || changes.length === 0) {
      return <Empty description="暂无字段变更" image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    return (
      <List
        dataSource={changes}
        renderItem={(item) => (
          <List.Item className={`change-item ${getChangeItemClass(item.changeType)}`}>
            <List.Item.Meta
              avatar={getChangeTypeTag(item.changeType)}
              title={<span className="change-title">{item.fieldName}</span>}
              description={
                <div className="change-content">
                  {item.changeType === 'ADD' && (
                    <div className="change-value new-value">
                      <span className="change-label">新值：</span>
                      <code>{JSON.stringify(item.newValue)}</code>
                    </div>
                  )}
                  {item.changeType === 'DELETE' && (
                    <div className="change-value old-value">
                      <span className="change-label">旧值：</span>
                      <code>{JSON.stringify(item.oldValue)}</code>
                    </div>
                  )}
                  {item.changeType === 'MODIFY' && (
                    <Space direction="vertical" style={{ width: '100%' }}>
                      <div className="change-value old-value">
                        <span className="change-label">旧值：</span>
                        <code>{JSON.stringify(item.oldValue)}</code>
                      </div>
                      <div className="change-value new-value">
                        <span className="change-label">新值：</span>
                        <code>{JSON.stringify(item.newValue)}</code>
                      </div>
                      {item.changedProperties && item.changedProperties.length > 0 && (
                        <div className="changed-properties">
                          <span className="properties-label">变更属性：</span>
                          {item.changedProperties.map((prop, idx) => (
                            <Tag key={idx} color="blue" size="small">
                              {prop}
                            </Tag>
                          ))}
                        </div>
                      )}
                    </Space>
                  )}
                </div>
              }
            />
          </List.Item>
        )}
      />
    )
  }

  const renderIndexChangeList = (changes: IndexChange[]) => {
    if (!changes || changes.length === 0) {
      return <Empty description="暂无索引变更" image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    return (
      <List
        dataSource={changes}
        renderItem={(item) => (
          <List.Item className={`change-item ${getChangeItemClass(item.changeType)}`}>
            <List.Item.Meta
              avatar={getChangeTypeTag(item.changeType)}
              title={<span className="change-title">{item.indexName}</span>}
              description={
                <div className="change-content">
                  {item.changeType === 'ADD' && (
                    <div className="change-value new-value">
                      <span className="change-label">新值：</span>
                      <code>{JSON.stringify(item.newValue)}</code>
                    </div>
                  )}
                  {item.changeType === 'DELETE' && (
                    <div className="change-value old-value">
                      <span className="change-label">旧值：</span>
                      <code>{JSON.stringify(item.oldValue)}</code>
                    </div>
                  )}
                  {item.changeType === 'MODIFY' && (
                    <Space direction="vertical" style={{ width: '100%' }}>
                      <div className="change-value old-value">
                        <span className="change-label">旧值：</span>
                        <code>{JSON.stringify(item.oldValue)}</code>
                      </div>
                      <div className="change-value new-value">
                        <span className="change-label">新值：</span>
                        <code>{JSON.stringify(item.newValue)}</code>
                      </div>
                    </Space>
                  )}
                </div>
              }
            />
          </List.Item>
        )}
      />
    )
  }

  const renderPropertyChangeList = (changes: ModelPropertyChange[]) => {
    if (!changes || changes.length === 0) {
      return <Empty description="暂无模型属性变更" image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    return (
      <List
        dataSource={changes}
        renderItem={(item) => (
          <List.Item className={`change-item ${getChangeItemClass('MODIFY')}`}>
            <List.Item.Meta
              avatar={getChangeTypeTag('MODIFY')}
              title={<span className="change-title">{item.propertyName}</span>}
              description={
                <div className="change-content">
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div className="change-value old-value">
                      <span className="change-label">旧值：</span>
                      <code>{JSON.stringify(item.oldValue)}</code>
                    </div>
                    <div className="change-value new-value">
                      <span className="change-label">新值：</span>
                      <code>{JSON.stringify(item.newValue)}</code>
                    </div>
                  </Space>
                </div>
              }
            />
          </List.Item>
        )}
      />
    )
  }

  const handleRollback = () => {
    if (compareResult && onRollback) {
      onRollback({
        id: 0,
        modelId,
        version: targetVersion,
        versionName: '',
        changeType: 2,
        changeDescription: '',
        operator: '',
        fieldCount: 0,
        indexCount: 0,
        tableName: '',
        modelName: '',
        createdTime: '',
      })
    }
  }

  return (
    <Modal
      title={
        <Space>
          <span>版本对比</span>
          {compareResult && (
            <Tag color="blue">
              v{compareResult.sourceVersion} → v{compareResult.targetVersion}
            </Tag>
          )}
        </Space>
      }
      open={visible}
      onCancel={onCancel}
      width={1000}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          关闭
        </Button>,
        compareResult && onRollback && (
          <Button
            key="rollback"
            type="primary"
            danger
            icon={<RollbackOutlined />}
            onClick={handleRollback}
          >
            回滚到此版本
          </Button>
        ),
      ]}
      className="dm-version-compare-modal"
      destroyOnClose
    >
      <Spin spinning={loading}>
        {compareResult ? (
          <div className="compare-container">
            <div className="compare-stats">
              <Row gutter={16}>
                <Col span={6}>
                  <Statistic
                    title="新增"
                    value={compareResult.addCount}
                    valueStyle={{ color: '#52c41a' }}
                    prefix={<PlusOutlined />}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title="修改"
                    value={compareResult.modifyCount}
                    valueStyle={{ color: '#faad14' }}
                    prefix={<EditOutlined />}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title="删除"
                    value={compareResult.deleteCount}
                    valueStyle={{ color: '#ff4d4f' }}
                    prefix={<MinusOutlined />}
                  />
                </Col>
                <Col span={6}>
                  <div className="compatibility-box">
                    <div className="compatibility-label">兼容性</div>
                    {compareResult.isCompatible ? (
                      <div className="compatible">
                        <CheckCircleOutlined />
                        <span>兼容</span>
                      </div>
                    ) : (
                      <div className="incompatible">
                        <WarningOutlined />
                        <span>有风险</span>
                      </div>
                    )}
                  </div>
                </Col>
              </Row>
              {compareResult.compatibilityWarnings && compareResult.compatibilityWarnings.length > 0 && (
                <div className="warnings-box">
                  <WarningOutlined style={{ color: '#faad14', marginRight: 8 }} />
                  <span>兼容性警告：</span>
                  <ul>
                    {compareResult.compatibilityWarnings.map((warning, idx) => (
                      <li key={idx}>{warning}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            <Tabs defaultActiveKey="field">
              <TabPane
                tab={
                  <span>
                    <FieldNumberOutlined /> 字段变更
                    {compareResult.fieldChanges && compareResult.fieldChanges.length > 0 && (
                      <Tag color="red" style={{ marginLeft: 8 }}>
                        {compareResult.fieldChanges.length}
                      </Tag>
                    )}
                  </span>
                }
                key="field"
              >
                {renderFieldChangeList(compareResult.fieldChanges || [])}
              </TabPane>

              <TabPane
                tab={
                  <span>
                    <OrderedListOutlined /> 索引变更
                    {compareResult.indexChanges && compareResult.indexChanges.length > 0 && (
                      <Tag color="red" style={{ marginLeft: 8 }}>
                        {compareResult.indexChanges.length}
                      </Tag>
                    )}
                  </span>
                }
                key="index"
              >
                {renderIndexChangeList(compareResult.indexChanges || [])}
              </TabPane>

              <TabPane
                tab={
                  <span>
                    <SettingOutlined /> 模型属性
                    {compareResult.propertyChanges && compareResult.propertyChanges.length > 0 && (
                      <Tag color="red" style={{ marginLeft: 8 }}>
                        {compareResult.propertyChanges.length}
                      </Tag>
                    )}
                  </span>
                }
                key="property"
              >
                {renderPropertyChangeList(compareResult.propertyChanges || [])}
              </TabPane>
            </Tabs>
          </div>
        ) : (
          <Empty description="暂无对比数据" />
        )}
      </Spin>
    </Modal>
  )
}

export default VersionCompareModal
