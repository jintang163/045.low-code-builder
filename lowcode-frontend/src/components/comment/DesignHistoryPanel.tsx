import React, { useState, useEffect } from 'react'
import {
  List,
  Avatar,
  Tag,
  Empty,
  Spin,
  Typography,
  Button,
  Space,
  Tooltip,
  Drawer,
  Descriptions,
} from 'antd'
import {
  UserOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SaveOutlined,
  CloudUploadOutlined,
  RollbackOutlined,
  DragOutlined,
  ColumnHeightOutlined,
  HistoryOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { collaborationApi, DesignHistory } from '@/api/collaboration'
import './DesignHistoryPanel.less'

const { Text, Paragraph } = Typography

export interface DesignHistoryPanelProps {
  appId: number
  targetType: string
  targetId: number
  limit?: number
  collapsible?: boolean
}

const OPERATION_TYPE_CONFIG: Record<string, { label: string; icon: React.ReactNode; color: string }> = {
  CREATE: { label: '创建', icon: <PlusOutlined />, color: 'green' },
  UPDATE: { label: '更新', icon: <EditOutlined />, color: 'blue' },
  DELETE: { label: '删除', icon: <DeleteOutlined />, color: 'red' },
  MOVE: { label: '移动', icon: <DragOutlined />, color: 'purple' },
  RESIZE: { label: '调整大小', icon: <ColumnHeightOutlined />, color: 'cyan' },
  PUBLISH: { label: '发布', icon: <CloudUploadOutlined />, color: 'geekblue' },
  ROLLBACK: { label: '回滚', icon: <RollbackOutlined />, color: 'orange' },
  SAVE: { label: '保存', icon: <SaveOutlined />, color: 'default' },
}

const DesignHistoryPanel: React.FC<DesignHistoryPanelProps> = ({
  appId,
  targetType,
  targetId,
  limit = 50,
  collapsible = false,
}) => {
  const [history, setHistory] = useState<DesignHistory[]>([])
  const [loading, setLoading] = useState(false)
  const [expanded, setExpanded] = useState(!collapsible)
  const [selectedItem, setSelectedItem] = useState<DesignHistory | null>(null)
  const [detailVisible, setDetailVisible] = useState(false)

  useEffect(() => {
    if (expanded) {
      loadHistory()
    }
  }, [appId, targetType, targetId, expanded])

  const loadHistory = async () => {
    try {
      setLoading(true)
      const res = await collaborationApi.listHistory(appId, targetType, targetId, limit)
      if (res.code === 0 || res.code === 200) {
        setHistory(res.data || [])
      }
    } catch (e) {
      console.error('加载历史记录失败:', e)
    } finally {
      setLoading(false)
    }
  }

  const viewDetail = (item: DesignHistory) => {
    setSelectedItem(item)
    setDetailVisible(true)
  }

  const getOperationConfig = (type: string) => {
    return OPERATION_TYPE_CONFIG[type] || { label: type, icon: <HistoryOutlined />, color: 'default' }
  }

  if (collapsible && !expanded) {
    return (
      <Button
        type="text"
        icon={<HistoryOutlined />}
        onClick={() => setExpanded(true)}
        className="history-toggle-btn"
      >
        历史记录 ({history.length})
      </Button>
    )
  }

  return (
    <div className="design-history-panel">
      <div className="panel-header">
        <div className="header-title">
          <HistoryOutlined />
          <span>设计历史</span>
          <Tag color="blue">{history.length} 条记录</Tag>
        </div>
        <Space>
          <Button type="text" icon={<HistoryOutlined />} size="small" onClick={loadHistory}>
            刷新
          </Button>
          {collapsible && (
            <Button type="text" size="small" onClick={() => setExpanded(false)}>
              收起
            </Button>
          )}
        </Space>
      </div>

      <div className="panel-content">
        <Spin spinning={loading}>
          {history.length === 0 ? (
            <Empty description="暂无历史记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          ) : (
            <List
              className="history-list"
              itemLayout="horizontal"
              dataSource={history}
              renderItem={(item) => {
                const config = getOperationConfig(item.operationType)
                return (
                  <List.Item className="history-item" key={item.id}>
                    <List.Item.Meta
                      avatar={
                        <div className={`operation-icon operation-${item.operationType?.toLowerCase()}`}>
                          <Tooltip title={config.label}>
                            <Avatar
                              size={36}
                              icon={config.icon}
                              style={{ backgroundColor: 'transparent', color: '#fff' }}
                            />
                          </Tooltip>
                        </div>
                      }
                      title={
                        <div className="item-title">
                          <Tag color={config.color} className="op-tag">
                            {config.label}
                          </Tag>
                          <Text strong>{item.targetName || item.operationDesc || '未命名'}</Text>
                          <Tooltip title="查看详情">
                            <Button
                              type="text"
                              size="small"
                              icon={<EyeOutlined />}
                              onClick={() => viewDetail(item)}
                            />
                          </Tooltip>
                        </div>
                      }
                      description={
                        <div className="item-desc">
                          <div className="user-info">
                            <Avatar size={16} src={item.createdByAvatar} icon={<UserOutlined />} />
                            <span style={{ marginLeft: 4 }}>{item.createdByName || '系统'}</span>
                          </div>
                          <Tooltip title={dayjs(item.createdTime).format('YYYY-MM-DD HH:mm:ss')}>
                            <span className="time">{dayjs(item.createdTime).fromNow()}</span>
                          </Tooltip>
                        </div>
                      }
                    />
                  </List.Item>
                )
              }}
            />
          )}
        </Spin>
      </div>

      <Drawer
        title="历史记录详情"
        placement="right"
        width={600}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
      >
        {selectedItem && (
          <div className="history-detail">
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="操作类型">
                <Tag color={getOperationConfig(selectedItem.operationType).color}>
                  {getOperationConfig(selectedItem.operationType).label}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="操作对象">
                {selectedItem.targetName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="对象类型">
                {selectedItem.targetType}
              </Descriptions.Item>
              <Descriptions.Item label="操作说明">
                {selectedItem.operationDesc || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="操作人">
                <Avatar size={20} src={selectedItem.createdByAvatar} icon={<UserOutlined />} />
                <span style={{ marginLeft: 4 }}>{selectedItem.createdByName || '-'}</span>
              </Descriptions.Item>
              <Descriptions.Item label="操作时间">
                {dayjs(selectedItem.createdTime).format('YYYY-MM-DD HH:mm:ss')}
              </Descriptions.Item>
              <Descriptions.Item label="IP地址">
                {selectedItem.ipAddress || '-'}
              </Descriptions.Item>
            </Descriptions>

            {selectedItem.operationDesc && (
              <div className="detail-section">
                <h4>操作说明</h4>
                <Paragraph>{selectedItem.operationDesc}</Paragraph>
              </div>
            )}

            {selectedItem.beforeSnapshot && (
              <div className="detail-section">
                <h4>操作前快照</h4>
                <pre className="json-preview">{JSON.stringify(JSON.parse(selectedItem.beforeSnapshot), null, 2)}</pre>
              </div>
            )}

            {selectedItem.afterSnapshot && (
              <div className="detail-section">
                <h4>操作后快照</h4>
                <pre className="json-preview">{JSON.stringify(JSON.parse(selectedItem.afterSnapshot), null, 2)}</pre>
              </div>
            )}

            {selectedItem.diffJson && (
              <div className="detail-section">
                <h4>差异对比</h4>
                <pre className="json-preview">{JSON.stringify(JSON.parse(selectedItem.diffJson), null, 2)}</pre>
              </div>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}

export default DesignHistoryPanel
