import React, { useState, useEffect } from 'react'
import { Badge, Tooltip, Drawer, List, Tag, Button, Space, Typography } from 'antd'
import {
  WifiOutlined,
  WifiOffOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'
import { useNetworkStatus } from '@/hooks/useNetworkStatus'
import { getPendingCount, getPendingChanges, getLastSync, PendingChange } from '@/utils/offline/indexedDB'

const { Text, Title } = Typography

interface OfflineStatusProps {
  showSyncButton?: boolean
  onSync?: () => Promise<void>
}

const OfflineStatus: React.FC<OfflineStatusProps> = ({ showSyncButton = true, onSync }) => {
  const { isOnline, networkStatus } = useNetworkStatus()
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [pendingCount, setPendingCount] = useState(0)
  const [pendingChanges, setPendingChanges] = useState<PendingChange[]>([])
  const [lastSyncTime, setLastSyncTime] = useState<string | null>(null)
  const [syncing, setSyncing] = useState(false)

  const loadPendingCount = async () => {
    try {
      const count = await getPendingCount()
      setPendingCount(count)
    } catch (e) {
      console.error('获取待同步数量失败:', e)
    }
  }

  const loadPendingChanges = async () => {
    try {
      const changes = await getPendingChanges()
      setPendingChanges(changes)
    } catch (e) {
      console.error('获取待同步变更失败:', e)
    }
  }

  const loadLastSync = async () => {
    try {
      const lastSync = await getLastSync()
      if (lastSync) {
        setLastSyncTime(lastSync.endTime || lastSync.startTime)
      }
    } catch (e) {
      console.error('获取最后同步时间失败:', e)
    }
  }

  useEffect(() => {
    loadPendingCount()
    loadLastSync()

    const interval = setInterval(() => {
      loadPendingCount()
      loadLastSync()
    }, 5000)

    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    if (drawerVisible) {
      loadPendingChanges()
    }
  }, [drawerVisible])

  const handleSync = async () => {
    if (!isOnline || !onSync) return

    setSyncing(true)
    try {
      await onSync()
      await loadPendingCount()
      await loadPendingChanges()
      await loadLastSync()
    } catch (e) {
      console.error('同步失败:', e)
    } finally {
      setSyncing(false)
    }
  }

  const getStatusColor = () => {
    if (networkStatus === 'checking') return 'gold'
    if (isOnline) return 'success'
    return 'error'
  }

  const getStatusIcon = () => {
    if (networkStatus === 'checking') {
      return <SyncOutlined spin style={{ color: '#faad14' }} />
    }
    if (isOnline) {
      return <WifiOutlined style={{ color: '#52c41a' }} />
    }
    return <WifiOffOutlined style={{ color: '#ff4d4f' }} />
  }

  const getStatusText = () => {
    if (networkStatus === 'checking') return '检测中'
    if (isOnline) return '在线'
    return '离线'
  }

  const getStatusTagColor = (status: string) => {
    switch (status) {
      case 'pending':
        return 'orange'
      case 'syncing':
        return 'blue'
      case 'synced':
        return 'green'
      case 'failed':
        return 'red'
      default:
        return 'default'
    }
  }

  const getStatusTagText = (status: string) => {
    switch (status) {
      case 'pending':
        return '待同步'
      case 'syncing':
        return '同步中'
      case 'synced':
        return '已同步'
      case 'failed':
        return '失败'
      default:
        return status
    }
  }

  const getResourceTypeText = (type: string) => {
    switch (type) {
      case 'page':
        return '页面'
      case 'dataModel':
        return '数据模型'
      case 'businessLogic':
        return '业务逻辑'
      default:
        return type
    }
  }

  const getActionText = (action: string) => {
    switch (action) {
      case 'create':
        return '创建'
      case 'update':
        return '更新'
      case 'delete':
        return '删除'
      default:
        return action
    }
  }

  return (
    <>
      <Tooltip title={`${getStatusText()}${pendingCount > 0 ? ` · ${pendingCount} 项待同步` : ''}`}>
        <Badge
          count={pendingCount}
          color={getStatusColor()}
          offset={[-2, 2]}
          style={{ cursor: 'pointer' }}
        >
          <span
            onClick={() => setDrawerVisible(true)}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
              cursor: 'pointer',
              padding: '4px 8px',
              borderRadius: 4,
            }}
          >
            {getStatusIcon()}
          </span>
        </Badge>
      </Tooltip>

      <Drawer
        title="同步状态"
        placement="right"
        width={400}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        extra={
          showSyncButton && onSync && (
            <Button
              type="primary"
              icon={<SyncOutlined spin={syncing} />}
              onClick={handleSync}
              disabled={!isOnline || syncing}
            >
              {syncing ? '同步中...' : '立即同步'}
            </Button>
          )
        }
      >
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <div
            style={{
              padding: 16,
              background: isOnline ? '#f6ffed' : '#fff2f0',
              borderRadius: 8,
              border: `1px solid ${isOnline ? '#b7eb8f' : '#ffccc7'}`,
            }}
          >
            <Space>
              {getStatusIcon()}
              <Text strong={true} style={{ fontSize: 16 }}>
                {getStatusText()}
              </Text>
            </Space>
            {!isOnline && (
              <Text type="warning" style={{ display: 'block', marginTop: 8 }}>
                当前处于离线状态，您的修改将保存在本地，网络恢复后自动同步。
              </Text>
            )}
          </div>

          {lastSyncTime && (
            <div>
              <Text type="secondary">
                <ClockCircleOutlined style={{ marginRight: 4 }} />
                最后同步: {new Date(lastSyncTime).toLocaleString('zh-CN')}
              </Text>
            </div>
          )}

          <div>
            <Title level={5} style={{ marginBottom: 12 }}>
              <Space>
                <SyncOutlined />
                待同步 ({pendingCount})
              </Space>
            </Title>
            {pendingChanges.length === 0 ? (
              <div
                style={{
                  textAlign: 'center',
                  padding: 32,
                  color: '#999',
                }}
              >
                <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a', marginBottom: 12 }} />
                <div>暂无待同步项</div>
              </div>
            ) : (
              <List
                dataSource={pendingChanges}
                renderItem={(item) => (
                  <List.Item key={item.id}>
                    <List.Item.Meta
                      avatar={
                        <div
                          style={{
                            width: 40,
                            height: 40,
                            borderRadius: 8,
                            background: '#f0f5ff',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: 18,
                          }}
                        >
                          {item.action === 'create' && '➕'}
                          {item.action === 'update' && '✏️'}
                          {item.action === 'delete' && '🗑️'}
                        </div>
                      }
                      title={
                        <Space>
                          <span>{getResourceTypeText(item.resourceType)}</span>
                          <Tag color={getStatusTagColor(item.status)}>
                            {getStatusTagText(item.status)}
                          </Tag>
                          <Tag color="blue">{getActionText(item.action)}</Tag>
                        </Space>
                      }
                      description={
                        <Space direction="vertical" size={0}>
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            ID: {item.resourceId}
                          </Text>
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            {new Date(item.createdAt).toLocaleString('zh-CN')}
                          </Text>
                          {item.retryCount > 0 && (
                            <Text type="warning" style={{ fontSize: 12 }}>
                              已重试 {item.retryCount} 次
                            </Text>
                          )}
                          {item.errorMessage && (
                            <Text type="danger" style={{ fontSize: 12 }}>
                              <ExclamationCircleOutlined /> {item.errorMessage}
                            </Text>
                          )}
                        </Space>
                      }
                    />
                  </List.Item>
                )}
              />
            )}
          </div>

          <div
            style={{
              padding: 12,
              background: '#fafafa',
              borderRadius: 8,
              fontSize: 12,
              color: '#999',
            }}
          >
            <div>💡 提示</div>
            <ul style={{ margin: '8px 0 0 16px', padding: 0 }}>
              <li>离线时修改会自动保存在本地</li>
              <li>网络恢复后会自动同步到服务器</li>
              <li>同步失败的内容会自动重试</li>
            </ul>
          </div>
        </Space>
      </Drawer>
    </>
  )
}

export default OfflineStatus
