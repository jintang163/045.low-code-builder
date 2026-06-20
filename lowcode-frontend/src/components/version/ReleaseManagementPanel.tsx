import React, { useState, useEffect } from 'react'
import {
  Table,
  Card,
  Button,
  Tag,
  Space,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  DatePicker,
  Empty,
  Spin,
  message,
  Row,
  Col,
  Divider,
  Alert,
  Radio,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  RocketOutlined,
  RollbackOutlined,
  StopOutlined,
  CloseOutlined,
  PlusOutlined,
  CloudServerOutlined,
  UserOutlined,
  ClockCircleOutlined,
  TagOutlined,
  PercentageOutlined,
} from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import { versionApi, ReleaseRecord, VersionSnapshot, ReleaseRecordDTO } from '@/api/version'
import './ReleaseManagementPanel.less'

const { TextArea } = Input
const { Option } = Select
const { RangePicker } = DatePicker

interface ReleaseManagementPanelProps {
  resourceId: number
  resourceType: string
  appId: number
  onClose?: () => void
  onPublishSuccess?: () => void
}

const ReleaseStatusMap: Record<number, { label: string; color: string }> = {
  0: { label: '待发布', color: 'default' },
  1: { label: '已发布', color: 'success' },
  2: { label: '发布中', color: 'processing' },
  3: { label: '发布失败', color: 'error' },
  4: { label: '已回滚', color: 'warning' },
  5: { label: '灰度发布中', color: 'purple' },
}

const ReleaseTypeMap: Record<number, { label: string; color: string }> = {
  1: { label: '正式发布', color: 'green' },
  2: { label: '灰度发布', color: 'purple' },
}

const ReleaseManagementPanel: React.FC<ReleaseManagementPanelProps> = ({
  resourceId,
  resourceType,
  appId,
  onClose,
  onPublishSuccess,
}) => {
  const [releases, setReleases] = useState<ReleaseRecord[]>([])
  const [snapshots, setSnapshots] = useState<VersionSnapshot[]>([])
  const [loading, setLoading] = useState(false)
  const [createModalVisible, setCreateModalVisible] = useState(false)
  const [form] = Form.useForm<ReleaseRecordDTO>()
  const [rollbackModalVisible, setRollbackModalVisible] = useState(false)
  const [currentRelease, setCurrentRelease] = useState<ReleaseRecord | null>(null)
  const [rollbackReason, setRollbackReason] = useState('')

  const loadReleases = async () => {
    setLoading(true)
    try {
      const res = await versionApi.getReleaseList({
        resourceId,
        resourceType,
        appId,
      })
      setReleases(res.data || [])
    } catch (e) {
      console.error('加载发布记录失败:', e)
      message.error('加载发布记录失败')
    } finally {
      setLoading(false)
    }
  }

  const loadSnapshots = async () => {
    try {
      const res = await versionApi.getSnapshotList({
        resourceId,
        resourceType,
        appId,
      })
      setSnapshots(res.data || [])
    } catch (e) {
      console.error('加载快照列表失败:', e)
    }
  }

  useEffect(() => {
    loadReleases()
    loadSnapshots()
  }, [resourceId, resourceType, appId])

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

  const handleCreateRelease = async () => {
    try {
      const values = await form.validateFields()
      const data: ReleaseRecordDTO = {
        ...values,
        appId,
        resourceType,
        resourceId,
      }

      if (values.releaseType === 2) {
        data.grayPercent = values.grayPercent
        data.grayUserGroup = values.grayUserGroup
        data.grayUserIds = values.grayUserIds
      }

      const res = await versionApi.createRelease(data)
      setCreateModalVisible(false)
      form.resetFields()
      loadReleases()
      message.success('发布记录创建成功')
      onPublishSuccess?.()

      if (values.releaseType === 1 && res.data?.id) {
        Modal.confirm({
          title: '立即发布',
          content: '发布记录已创建，是否立即发布？',
          onOk: async () => {
            try {
              await versionApi.publishRelease(res.data!.id!)
              message.success('发布成功')
              loadReleases()
            } catch (e) {
              console.error('发布失败:', e)
              message.error('发布失败')
            }
          },
        })
      }
    } catch (e) {
      console.error('创建发布记录失败:', e)
    }
  }

  const handlePublish = async (record: ReleaseRecord) => {
    if (!record.id) return
    Modal.confirm({
      title: '确认发布',
      content: `确定要发布版本 ${record.releaseTitle} 吗？`,
      onOk: async () => {
        try {
          await versionApi.publishRelease(record.id!)
          message.success('发布成功')
          loadReleases()
          onPublishSuccess?.()
        } catch (e) {
          console.error('发布失败:', e)
          message.error('发布失败')
        }
      },
    })
  }

  const handleRollback = async (record: ReleaseRecord) => {
    setCurrentRelease(record)
    setRollbackReason('')
    setRollbackModalVisible(true)
  }

  const confirmRollback = async () => {
    if (!currentRelease?.id) return
    try {
      await versionApi.rollbackRelease(currentRelease!.id!, { reason: rollbackReason || '未填写' })
      setRollbackModalVisible(false)
      setCurrentRelease(null)
      message.success('回滚成功')
      loadReleases()
    } catch (e) {
      console.error('回滚失败:', e)
      message.error('回滚失败')
    }
  }

  const handleStopGray = async (record: ReleaseRecord) => {
    if (!record.id) return
    Modal.confirm({
      title: '停止灰度',
      content: '确定要停止当前灰度发布吗？',
      onOk: async () => {
        try {
          await versionApi.stopGrayRelease(record.id!)
          message.success('已停止灰度发布')
          loadReleases()
        } catch (e) {
          console.error('停止灰度失败:', e)
          message.error('停止灰度失败')
        }
      },
    })
  }

  const handleCancelGray = async (record: ReleaseRecord) => {
    if (!record.id) return
    Modal.confirm({
      title: '取消灰度',
      content: '确定要取消当前灰度发布吗？这将回滚到上一个版本。',
      onOk: async () => {
        try {
          await versionApi.cancelGrayRelease(record.id!)
          message.success('已取消灰度发布')
          loadReleases()
        } catch (e) {
          console.error('取消灰度失败:', e)
          message.error('取消灰度失败')
        }
      },
    })
  }

  const renderStatusTag = (status: number) => {
    const statusInfo = ReleaseStatusMap[status] || { label: '未知', color: 'default' }
    return <Tag color={statusInfo.color}>{statusInfo.label}</Tag>
  }

  const renderGrayInfo = (record: ReleaseRecord) => {
    if (record.releaseType !== 2) return null
    return (
      <Space size="small" direction="vertical" style={{ width: '100%' }}>
        {record.grayPercent && (
          <span>
            <TagOutlined /> 灰度比例: {record.grayPercent}%
          </span>
        )}
        {record.grayUserGroup && (
          <span>
            <UserOutlined /> 用户组: {record.grayUserGroup}
          </span>
        )}
        {record.grayUserIds && (
          <span>
            <UserOutlined /> 指定用户: {record.grayUserIds}
          </span>
        )}
      </Space>
    )
  }

  const columns: ColumnsType<ReleaseRecord> = [
    {
      title: '发布标题',
      dataIndex: 'releaseTitle',
      key: 'releaseTitle',
      render: (text, record) => (
        <div>
          <div style={{ fontWeight: 500 }}>{text}</div>
          <div style={{ fontSize: 12, color: '#999' }}>
            <Tag color="blue">v{record.version}</Tag>
          </div>
        </div>
      ),
    },
    {
      title: '状态',
      dataIndex: 'releaseStatus',
      key: 'releaseStatus',
      width: 120,
      render: (status) => renderStatusTag(status),
    },
    {
      title: '发布类型',
      dataIndex: 'releaseType',
      key: 'releaseType',
      width: 120,
      render: (type) => {
        const typeInfo = ReleaseTypeMap[type] || { label: '未知', color: 'default' }
        return <Tag color={typeInfo.color}>{typeInfo.label}</Tag>
      },
    },
    {
      title: '灰度信息',
      dataIndex: 'grayInfo',
      key: 'grayInfo',
      width: 200,
      render: (_, record) => renderGrayInfo(record),
    },
    {
      title: '发布时间',
      dataIndex: 'releaseTime',
      key: 'releaseTime',
      width: 180,
      render: (time) => (
        <div>
          <ClockCircleOutlined style={{ marginRight: 4 }} />
          {formatDateTime(time)}
        </div>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 280,
      render: (_, record) => (
        <Space>
          {record.releaseStatus === 0 && (
            <Button
              size="small"
              type="primary"
              icon={<RocketOutlined />}
              onClick={() => handlePublish(record)}
            >
              发布
            </Button>
          )}
          {(record.releaseStatus === 1 && (
            <Button
              size="small"
              danger
              icon={<RollbackOutlined />}
              onClick={() => handleRollback(record)}
            >
              回滚
            </Button>
          )}
          {record.releaseStatus === 5 && (
            <>
              <Button
                size="small"
                icon={<StopOutlined />}
                onClick={() => handleStopGray(record)}
              >
                停止灰度
              </Button>
              <Button
                size="small"
                danger
                icon={<CloseOutlined />}
                onClick={() => handleCancelGray(record)}
              >
                取消灰度
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div className="release-management-panel">
      <div className="panel-header">
        <Space>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.resetFields()
              setCreateModalVisible(true)
            }}
          >
            创建发布
          </Button>
          <Button icon={<CloudServerOutlined />} onClick={loadReleases}>
            刷新
          </Button>
        </Space>
      </div>

      <div className="panel-content">
        <Spin spinning={loading}>
          {releases.length > 0 ? (
            <Table
              columns={columns}
              dataSource={releases}
              rowKey="id"
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条记录`,
              }}
              expandable={{
                expandedRowRender: (record) => (
                  <div style={{ padding: '12px 24px' }}>
                    <Row gutter={16}>
                      <Col span={12}>
                        <div style={{ marginBottom: 8 }}>
                          <strong>发布说明：</strong>
                          {record.releaseNote || '暂无说明'}
                        </div>
                        {record.gitCommitId && (
                          <div style={{ marginBottom: 8 }}>
                            <strong>Git提交：</strong>
                            <Tag color="cyan">{record.gitCommitId.slice(0, 7)}</Tag>
                            {record.gitBranch && <Tag>{record.gitBranch}</Tag>}
                          </div>
                        )}
                        {record.gitCommitMessage && (
                          <div style={{ marginBottom: 8 }}>
                            <strong>Git信息：</strong>
                            {record.gitCommitMessage}
                          </div>
                        )}
                      </Col>
                      <Col span={12}>
                        {record.rollbackTime && (
                          <div style={{ marginBottom: 8 }}>
                            <strong>回滚时间：</strong>
                            {formatDateTime(record.rollbackTime)}
                          </div>
                        )}
                        {record.rollbackReason && (
                          <div style={{ marginBottom: 8 }}>
                            <strong>回滚原因：</strong>
                            {record.rollbackReason}
                          </div>
                        )}
                        <div style={{ marginBottom: 8 }}>
                          <strong>创建人：</strong>
                          {record.createdBy || '系统'}
                        </div>
                        <div>
                          <strong>创建时间：</strong>
                          {formatDateTime(record.createdTime)}
                        </div>
                      </Col>
                    </Row>
                  </div>
                ),
              }}
            />
          ) : (
            <Empty description="暂无发布记录" />
          )}
        </Spin>
      </div>

      <Modal
        title="创建发布"
        open={createModalVisible}
        onCancel={() => setCreateModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setCreateModalVisible(false)}>
            取消
          </Button>,
          <Button key="submit" type="primary" onClick={handleCreateRelease}>
            创建
          </Button>,
        ]}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Alert
            type="info"
            message="发布配置"
            description="选择要发布的版本快照，填写发布信息。"
            showIcon
            style={{ marginBottom: 16 }}
          />

          <Form.Item
            name="snapshotId"
            label="选择版本快照"
            rules={[{ required: true, message: '请选择版本快照' }]}
          >
            <Select placeholder="请选择要发布的版本快照">
              {snapshots.map((snapshot) => (
                <Option key={snapshot.id} value={snapshot.id}>
                  <Space>
                    <span>v{snapshot.version}</span>
                    <Tag color={snapshot.snapshotType === 1 ? 'blue' : 'green'}>
                      {snapshot.snapshotType === 1 ? '自动' : '手动'}
                    </Tag>
                    {snapshot.description && <span style={{ color: '#999' }}>{snapshot.description}</span>}
                  </Space>
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="version"
                label="版本号"
                rules={[{ required: true, message: '请输入版本号' }]}
              >
                <Input placeholder="例如：1.0.0" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="releaseTitle"
                label="发布标题"
                rules={[{ required: true, message: '请输入发布标题' }]}
              >
                <Input placeholder="例如：2024年第一季度功能更新" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="releaseNote" label="发布说明">
            <TextArea rows={3} placeholder="请输入本次发布的功能更新说明..." />
          </Form.Item>

          <Form.Item
            name="releaseType"
            label="发布类型"
            rules={[{ required: true, message: '请选择发布类型' }]}
          >
            <Radio.Group>
              <Radio value={1}>
              <Tag color="green">正式发布</Tag>
            </Radio>
              <Radio value={2}>
              <Tag color="purple">灰度发布</Tag>
            </Radio.Group>
          </Form.Item>

          <Form.Item noStyle shouldUpdate={(prev, curr) => prev.releaseType !== curr.releaseType}>
            {({ getFieldValue }) => {
              const releaseType = getFieldValue('releaseType')
              if (releaseType === 2) {
                return (
                  <div className="gray-config">
                    <Divider orientation="left">灰度配置</Divider>
                    <Alert
                      type="warning"
                      message="灰度发布配置"
                      description="配置灰度发布的规则，只有符合条件的用户才能看到新版本。"
                      showIcon
                      style={{ marginBottom: 16 }}
                    />
                    <Row gutter={16}>
                      <Col span={12}>
                        <Form.Item
                          name="grayPercent"
                          label={
                            <span>
                              <PercentageOutlined /> 灰度百分比
                            </span>
                          }
                          rules={[{ required: true, message: '请输入灰度百分比' }]}
                        >
                          <InputNumber
                          min={0}
                          max={100}
                          style={{ width: '100%' }}
                          placeholder="0-100"
                        />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item name="grayType" label="灰度类型">
                        <Select placeholder="选择灰度类型">
                          <Option value={1}>按百分比</Option>
                          <Option value={2}>按用户组</Option>
                          <Option value={3}>指定用户</Option>
                        </Select>
                      </Form.Item>
                    </Col>
                  </Row>
                    <Form.Item name="grayUserGroup" label="用户组">
                      <Input placeholder="多个用户组用逗号分隔" />
                    </Form.Item>
                    <Form.Item name="grayUserIds" label="指定用户ID">
                      <Input placeholder="多个用户ID用逗号分隔" />
                    </Form.Item>
                  </div>
                )
              }
              return null
            }}
          </Form.Item>

          <Form.Item name="targetEnvironment" label="目标环境">
            <Select placeholder="选择发布环境">
              <Option value="production">生产环境</Option>
              <Option value="staging">预发布环境</Option>
              <Option value="test">测试环境</Option>
            </Select>
          </Form.Item>

          <Form.Item name="scheduledTime" label="定时发布">
            <DatePicker showTime style={{ width: '100%' }} placeholder="选择发布时间" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="回滚确认"
        open={rollbackModalVisible}
        onCancel={() => {
          setRollbackModalVisible(false)
          setCurrentRelease(null)
        }}
        onOk={confirmRollback}
        okText="确认回滚"
        okButtonProps={{ danger: true }}
      >
        <Alert
          type="warning"
          message="回滚操作"
          description="回滚将恢复到上一个版本，请谨慎操作。"
          showIcon
          style={{ marginBottom: 16 }}
        />
        <div style={{ marginBottom: 16 }}>
          <p>
            <strong>当前版本：</strong>v{currentRelease?.version} - {currentRelease?.releaseTitle}
          </p>
        </div>
        <Form.Item label="回滚原因" required>
          <TextArea
            rows={3}
            value={rollbackReason}
            onChange={(e) => setRollbackReason(e.target.value)}
            placeholder="请输入回滚原因..."
          />
        </Form.Item>
      </Modal>
    </div>
  )
}

export default ReleaseManagementPanel
