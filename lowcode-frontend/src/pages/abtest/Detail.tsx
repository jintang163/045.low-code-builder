import React, { useState, useEffect, useCallback } from 'react'
import {
  Card,
  Button,
  Space,
  Tag,
  Table,
  message,
  Row,
  Col,
  Divider,
  Modal,
  Select,
  Tooltip,
  Badge,
  Descriptions,
  Progress,
  Alert,
  Statistic,
  Empty,
} from 'antd'
import {
  ArrowLeftOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  StopOutlined,
  TrophyOutlined,
  ReloadOutlined,
  ExperimentOutlined,
  ControlOutlined,
  ThunderboltOutlined,
  RiseOutlined,
  EyeOutlined,
  UserOutlined,
  ClickOutlined,
  PercentageOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import {
  ABTestInfo,
  ABTestVariantStats,
  ABTestStats,
  abtestApi,
  mockABTests,
  mockStats,
} from '@/api/abtest'
import { useAppStore } from '@/store/appStore'
import dayjs from 'dayjs'
import BaseChart, { ChartDataConfig, ChartStyleConfig } from '@/components/chart/BaseChart'
import IndicatorCard from '@/components/chart/IndicatorCard'

const { Option } = Select

const ABTestDetail: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [abTest, setAbTest] = useState<ABTestInfo | null>(null)
  const [stats, setStats] = useState<ABTestStats | null>(null)
  const [loading, setLoading] = useState(false)
  const [statsLoading, setStatsLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [stopModalVisible, setStopModalVisible] = useState(false)
  const [winnerVariantId, setWinnerVariantId] = useState<number | null>(null)

  const statusMap: Record<number, { text: string; color: string; status: any }> = {
    0: { text: '草稿', color: 'default', status: 'default' },
    1: { text: '运行中', color: 'green', status: 'processing' },
    2: { text: '已暂停', color: 'orange', status: 'warning' },
    3: { text: '已结束', color: 'blue', status: 'success' },
  }

  const resourceTypeMap: Record<string, { text: string; icon: any; color: string }> = {
    page: { text: '页面', icon: <EyeOutlined />, color: 'blue' },
    component: { text: '组件', icon: <ThunderboltOutlined />, color: 'purple' },
    logic: { text: '逻辑', icon: <ExperimentOutlined />, color: 'green' },
  }

  const loadTest = useCallback(async () => {
    if (!id) return
    setLoading(true)
    try {
      const res: any = await abtestApi.get(Number(id))
      if (res.code === 0 || res.code === 200) {
        setAbTest(res.data)
      } else {
        const mockTest = mockABTests.find(t => t.id === Number(id))
        if (mockTest) {
          setAbTest(mockTest)
        }
      }
    } catch (e: any) {
      console.error(e)
      const mockTest = mockABTests.find(t => t.id === Number(id))
      if (mockTest) {
        setAbTest(mockTest)
      }
    } finally {
      setLoading(false)
    }
  }, [id])

  const loadStats = useCallback(async () => {
    if (!id) return
    setStatsLoading(true)
    try {
      const res: any = await abtestApi.stats(Number(id))
      if (res.code === 0 || res.code === 200) {
        setStats(res.data)
      } else {
          setStats(mockStats)
      }
    } catch (e: any) {
      console.error(e)
      setStats(mockStats)
    } finally {
      setStatsLoading(false)
    }
  }, [id])

  useEffect(() => {
    loadTest()
    loadStats()
  }, [id])

  const handleStart = async () => {
    if (!id) return
    setActionLoading(true)
    try {
      const res: any = await abtestApi.start(Number(id))
      if (res.code === 0 || res.code === 200) {
        message.success('测试已启动')
        setAbTest(prev => prev ? { ...prev, status: 1 } : null)
        loadStats()
      } else {
        message.error(res.message || '启动失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('启动失败: ' + (e.message || ''))
    } finally {
      setActionLoading(false)
    }
  }

  const handlePause = async () => {
    if (!id) return
    setActionLoading(true)
    try {
      const res: any = await abtestApi.pause(Number(id))
      if (res.code === 0 || res.code === 200) {
        message.success('测试已暂停')
        setAbTest(prev => prev ? { ...prev, status: 2 } : null)
      } else {
        message.error(res.message || '暂停失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('暂停失败: ' + (e.message || ''))
    } finally {
      setActionLoading(false)
    }
  }

  const handleStop = () => {
    setStopModalVisible(true)
  }

  const handleStopConfirm = async () => {
    if (!id) return
    setActionLoading(true)
    try {
      const res: any = await abtestApi.stop(Number(id), winnerVariantId || undefined)
      if (res.code === 0 || res.code === 200) {
        message.success('测试已结束')
        setAbTest(prev => prev ? {
          ...prev,
          status: 3,
          winnerVariantId: winnerVariantId || undefined,
        } : null)
        setStopModalVisible(false)
        loadStats()
      } else {
        message.error(res.message || '结束失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('结束失败: ' + (e.message || ''))
    } finally {
      setActionLoading(false)
    }
  }

  const handlePromote = async (variantId: number) => {
    if (!id) return
    try {
      const res: any = await abtestApi.promoteWinner(Number(id), variantId)
      if (res.code === 0 || res.code === 200) {
        message.success('优胜版本已推广')
      } else {
        message.error(res.message || '推广失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('推广失败: ' + (e.message || ''))
    }
  }

  const handleRefresh = () => {
    loadStats()
    message.success('数据已刷新')
  }

  const getWinnerVariant = (): ABTestVariantStats | null => {
    if (!stats?.winnerVariantId) return null
    return stats.variants.find(v => v.variantId === stats.winnerVariantId) || null
  }

  const chartData = stats?.variants.map(v => ({
    variantName: v.variantName,
    conversionRate: v.conversionRate,
    pageViews: v.pageViews,
    uniqueVisitors: v.uniqueVisitors,
    conversions: v.conversions,
  })) || []

  const chartDataConfig: ChartDataConfig = {
    xField: 'variantName',
    yField: ['conversionRate'],
  }

  const chartStyleConfig: ChartStyleConfig = {
    title: {
      text: '转化率对比',
      left: 'center',
    },
    legend: {
      show: true,
      position: 'top',
    },
    tooltip: {
      show: true,
      trigger: 'axis',
    },
  }

  const columns = [
    {
      title: '变体名称',
      dataIndex: 'variantName',
      key: 'variantName',
      width: 200,
      render: (text: string, record: ABTestVariantStats) => (
        <Space>
          {record.variantType === 'control' ? (
            <Tag color="blue" icon={<ControlOutlined />}>对照组</Tag>
          ) : (
            <Tag color="green" icon={<ThunderboltOutlined />}>实验组</Tag>
          )}
          <span style={{ fontWeight: 500 }}>{text}</span>
          {stats?.winnerVariantId === record.variantId && (
            <TrophyOutlined style={{ color: '#faad14' }} />
          )}
        </Space>
      ),
    },
    {
      title: '浏览量',
      dataIndex: 'pageViews',
      key: 'pageViews',
      width: 120,
      render: (val: number) => val?.toLocaleString() || '-',
    },
    {
      title: '独立访客',
      dataIndex: 'uniqueVisitors',
      key: 'uniqueVisitors',
      width: 120,
      render: (val: number) => val?.toLocaleString() || '-',
    },
    {
      title: '转化数',
      dataIndex: 'conversions',
      key: 'conversions',
      width: 120,
      render: (val: number) => val?.toLocaleString() || '-',
    },
    {
      title: '转化率',
      dataIndex: 'conversionRate',
      key: 'conversionRate',
      width: 150,
      render: (rate: number, record: ABTestVariantStats) => (
        <Space direction="vertical" size={4} style={{ width: '100%' }}>
          <span style={{ fontWeight: 500 }}>{rate?.toFixed(2)}%</span>
          <Progress
            percent={rate || 0}
            size="small"
            strokeColor={record.variantType === 'control' ? '#1677ff' : '#52c41a'}
            showInfo={false}
          />
        </Space>
      ),
    },
    {
      title: '置信区间',
      dataIndex: 'confidenceInterval',
      key: 'confidenceInterval',
      width: 120,
      render: (val: number, record: ABTestVariantStats) => {
        const rate = record.conversionRate || 0
        const ci = val || 0
        return (
          <span>
            {(rate - ci).toFixed(2)}% ~ {(rate + ci).toFixed(2)}%
          </span>
        )
      },
    },
    {
      title: '统计显著性',
      dataIndex: 'statisticalSignificance',
      key: 'statisticalSignificance',
      width: 120,
      render: (val: number) => {
        if (val === undefined || val === null || val === 0) {
          return <Tag color="default">-</Tag>
        }
        const percent = (val * 100).toFixed(1)
        const color = val >= 0.95 ? 'green' : val >= 0.9 ? 'blue' : 'orange'
        return <Tag color={color}>{percent}%</Tag>
      },
    },
    {
      title: '相对提升',
      dataIndex: 'improvementVsControl',
      key: 'improvementVsControl',
      width: 120,
      render: (val: number) => {
        if (val === undefined || val === null) return '-'
        const color = val > 0 ? 'green' : val < 0 ? 'red' : 'default'
        const icon = val > 0 ? '↑' : val < 0 ? '↓' : '-'
        return (
          <span style={{ color: color === 'green' ? '#52c41a' : color === 'red' ? '#ff4d4f' : '#999' }}>
            {icon} {Math.abs(val).toFixed(2)}%
          </span>
        )
      },
    },
  ]

  const winner = getWinnerVariant()

  return (
    <div style={{ background: '#f5f5f5', minHeight: '100vh', padding: 16 }}>
      <Card style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/abtest')}>
              返回列表
            </Button>
            <div style={{ borderLeft: '1px solid #e8e8e8', height: 24, margin: '0 8px' }} />
            <h3 style={{ margin: 0 }}>
              <ExperimentOutlined style={{ marginRight: 8, color: '#1677ff' }} />
              {abTest?.testName || 'A/B 测试详情'}
            </h3>
            {abTest?.status !== undefined && (
              <Badge
              status={statusMap[abTest.status]?.status || 'default'}
              text={statusMap[abTest.status]?.text || '未知'}
            />
            )}
          </Space>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={handleRefresh}
              loading={statsLoading}
            >
              刷新数据
            </Button>
            {abTest?.status === 0 && (
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={handleStart}
                loading={actionLoading}
              >
                开始测试
              </Button>
            )}
            {abTest?.status === 1 && (
              <>
                <Button
                  icon={<PauseCircleOutlined />}
                  onClick={handlePause}
                  loading={actionLoading}
                >
                  暂停测试
                </Button>
                <Button
                  danger
                  icon={<StopOutlined />}
                  onClick={handleStop}
                  loading={actionLoading}
                >
                  结束测试
                </Button>
              </>
            )}
            {abTest?.status === 2 && (
              <>
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={handleStart}
                  loading={actionLoading}
                >
                  继续测试
                </Button>
                <Button
                  danger
                  icon={<StopOutlined />}
                  onClick={handleStop}
                  loading={actionLoading}
                >
                  结束测试
                </Button>
              </>
            )}
            {abTest?.status === 3 && winner && (
              <Button
                type="primary"
                icon={<TrophyOutlined />}
                onClick={() => handlePromote(winner.variantId)}
              >
                推广优胜版本
              </Button>
            )}
          </Space>
        </div>
      </Card>

      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card loading={loading} title="基本信息">
            {abTest && (
            <Descriptions column={3} size="small">
              <Descriptions.Item label="测试名称">{abTest.testName}</Descriptions.Item>
              <Descriptions.Item label="测试编码">
                <Tag color="default" style={{ fontFamily: 'monospace' }}>
                  {abTest.testCode}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="资源类型">
                <Tag
                  color={resourceTypeMap[abTest.resourceType]?.color || 'default'}
                  icon={resourceTypeMap[abTest.resourceType]?.icon}
                >
                  {resourceTypeMap[abTest.resourceType]?.text || '未知'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="流量分配方式">
                {abTest.trafficAllocationType === 'percentage' ? '按比例分配' : '自定义分配'}
              </Descriptions.Item>
              <Descriptions.Item label="置信度">{abTest.confidenceLevel}%</Descriptions.Item>
              <Descriptions.Item label="样本量">
                {abTest.sampleSize?.toLocaleString() || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="开始时间">
                {abTest.startTime ? dayjs(abTest.startTime).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="结束时间">
                {abTest.endTime ? dayjs(abTest.endTime).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="变体数量">
                {abTest.variants?.length || 0} 个
              </Descriptions.Item>
              <Descriptions.Item label="描述" span={3}>
                {abTest.description || '-'}
              </Descriptions.Item>
            </Descriptions>
          )}
          </Card>
        </Col>

        <Col span={24}>
          <Row gutter={16}>
            <Col span={6}>
              <IndicatorCard
                title="总浏览量"
                value={stats?.totalPageViews?.toLocaleString() || 0}
                icon={<EyeOutlined />}
                color="#1677ff"
              />
            </Col>
            <Col span={6}>
              <IndicatorCard
                title="独立访客"
                value={stats?.totalUniqueVisitors?.toLocaleString() || 0}
                icon={<UserOutlined />}
                color="#52c41a"
              />
            </Col>
            <Col span={6}>
              <IndicatorCard
                title="总转化数"
                value={stats?.totalConversions?.toLocaleString() || 0}
                icon={<ClickOutlined />}
                color="#722ed1"
              />
            </Col>
            <Col span={6}>
              <IndicatorCard
                title="总体转化率"
                value={stats?.overallConversionRate?.toFixed(2) || '0.00'}
                unit="%"
                icon={<PercentageOutlined />}
                color="#fa8c16"
              />
            </Col>
          </Row>
        </Col>

        {winner && stats?.isStatisticallySignificant && (
          <Col span={24}>
            <Alert
              message={
                <Space>
                  <TrophyOutlined style={{ fontSize: 20, color: '#faad14' }} />
                  <span style={{ fontWeight: 500 }}>
                    统计显著！优胜版本为：{winner.variantName}
                  </span>
                  <Tag color="green">
                    转化率提升 {winner.improvementVsControl?.toFixed(2) || 0}%
                  </Tag>
                  <Tag color="blue">
                    置信度 {stats.confidenceLevel}%
                  </Tag>
                </Space>
              }
              type="success"
              showIcon
              action={
                <Button size="small" type="primary" onClick={() => handlePromote(winner.variantId)}>
                  推广此版本
                </Button>
              }
            />
          </Col>
        )}

        <Col span={24}>
          <Card title="转化率对比" loading={statsLoading}>
            {chartData.length > 0 ? (
              <BaseChart
                type="bar"
                data={chartData}
                dataConfig={chartDataConfig}
                styleConfig={chartStyleConfig}
                height={350}
              />
            ) : (
              <Empty description="暂无数据" style={{ padding: '40px 0' }} />
            )}
          </Card>
        </Col>

        <Col span={24}>
          <Card title="详细数据" loading={statsLoading}>
            <Table
              columns={columns}
              dataSource={stats?.variants || []}
              rowKey="variantId"
              pagination={false}
            />
          </Card>
        </Col>

        {abTest?.conclusion && (
          <Col span={24}>
            <Card title="测试结论">
              <div style={{ padding: '12px 0', color: '#666', lineHeight: 1.8 }}>
              {abTest.conclusion}
              </div>
            </Card>
          </Col>
        )}
      </Row>

      <Modal
        title="结束测试"
        open={stopModalVisible}
        onOk={handleStopConfirm}
        onCancel={() => setStopModalVisible(false)}
        confirmLoading={actionLoading}
        okText="确认结束"
        okButtonProps={{ danger: true }}
      >
        <div style={{ marginBottom: 16 }}>
          <p>确定要结束此测试吗？结束后测试将无法继续运行。</p>
        </div>
        <div style={{ marginBottom: 8, fontWeight: 500 }}>选择优胜版本（可选）：</div>
        <Select
          placeholder="选择优胜变体"
          style={{ width: '100%' }}
          value={winnerVariantId}
          onChange={setWinnerVariantId}
          allowClear
        >
          {abTest?.variants?.map(v => (
            <Option key={v.id} value={v.id}>
              {v.variantName} ({v.variantType === 'control' ? '对照组' : '实验组'}
            </Option>
          ))}
        </Select>
        <div style={{ marginTop: 8, fontSize: 12, color: '#999' }}>
          如果不选择，系统将根据统计数据自动判定
        </div>
      </Modal>
    </div>
  )
}

export default ABTestDetail
