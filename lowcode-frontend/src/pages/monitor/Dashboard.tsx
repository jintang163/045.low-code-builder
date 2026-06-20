import React, { useEffect, useState, useCallback } from 'react'
import { Row, Col, Card, Statistic, Table, Tag, Badge, Alert, Button, Space, Spin, Tooltip, Modal, Form, Input, Select, InputNumber, Switch, message } from 'antd'
import {
  EyeOutlined,
  TeamOutlined,
  ApiOutlined,
  WarningOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  ReloadOutlined,
  BellOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'
import { monitorApi, MonitorMetrics, AlertRule } from '@/api/monitor'

const { Option } = Select

const MonitorDashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<MonitorMetrics | null>(null)
  const [loading, setLoading] = useState(false)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [alertModalVisible, setAlertModalVisible] = useState(false)
  const [editingRule, setEditingRule] = useState<AlertRule | null>(null)
  const [alertRules, setAlertRules] = useState<AlertRule[]>([])
  const [form] = Form.useForm()

  const fetchData = useCallback(async () => {
    try {
      const [metricsRes, rulesRes] = await Promise.all([
        monitorApi.getMetrics(),
        monitorApi.getAlertRules(),
      ])
      setMetrics(metricsRes.data)
      setAlertRules(rulesRes.data)
    } catch (e) {
      console.error('获取监控数据失败', e)
    }
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  useEffect(() => {
    if (!autoRefresh) return
    const timer = setInterval(() => {
      setLoading(true)
      fetchData().finally(() => setLoading(false))
    }, 5000)
    return () => clearInterval(timer)
  }, [autoRefresh, fetchData])

  useEffect(() => {
    const sendPageVisit = async () => {
      try {
        await monitorApi.addPageVisitLog({
          sessionId: localStorage.getItem('sessionId') || Math.random().toString(36).slice(2),
          userId: localStorage.getItem('userId') || '',
          username: localStorage.getItem('username') || '',
          pagePath: window.location.pathname,
          pageTitle: document.title,
          referrer: document.referrer,
          userAgent: navigator.userAgent,
          clientIp: '',
          visitTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
        })
      } catch (e) {}
    }
    sendPageVisit()
  }, [])

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'healthy':
        return 'green'
      case 'warning':
        return 'orange'
      case 'error':
        return 'red'
      default:
        return 'default'
    }
  }

  const getStatusText = (status: string) => {
    switch (status) {
      case 'healthy':
        return '正常'
      case 'warning':
        return '告警'
      case 'error':
        return '异常'
      default:
        return '未知'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'healthy':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />
      case 'warning':
        return <WarningOutlined style={{ color: '#faad14' }} />
      case 'error':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
      default:
        return <QuestionCircleOutlined style={{ color: '#8c8c8c' }} />
    }
  }

  const trendOption = metrics ? {
    title: { text: '页面访问趋势(PV)', left: 'center', textStyle: { fontSize: 14, color: '#00e5ff' } },
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: metrics.pageVisitTrend.map((t: any) => t.date.slice(5)),
      axisLine: { lineStyle: { color: '#00e5ff33' } },
      axisLabel: { color: '#00e5ff' },
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#00e5ff33' } },
      axisLabel: { color: '#00e5ff' },
      splitLine: { lineStyle: { color: '#00e5ff22' } },
    },
    series: [
      {
        name: '访问量',
        type: 'line',
        smooth: true,
        data: metrics.pageVisitTrend.map((t: any) => t.count),
        lineStyle: { color: '#00e5ff', width: 2 },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#00e5ff66' },
              { offset: 1, color: '#00e5ff00' },
            ],
          },
        },
        itemStyle: { color: '#00e5ff' },
      },
    ],
  } : {}

  const errorTrendOption = metrics ? {
    title: { text: '错误率趋势', left: 'center', textStyle: { fontSize: 14, color: '#ff6b6b' } },
    tooltip: { trigger: 'axis' },
    legend: { data: ['请求量', '错误数'], bottom: 0, textStyle: { color: '#fff' } },
    grid: { left: 40, right: 20, top: 40, bottom: 40 },
    xAxis: {
      type: 'category',
      data: metrics.errorTrend.map((t: any) => t.date.slice(5)),
      axisLine: { lineStyle: { color: '#ff6b6b33' } },
      axisLabel: { color: '#ff6b6b' },
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#ff6b6b33' } },
      axisLabel: { color: '#ff6b6b' },
      splitLine: { lineStyle: { color: '#ff6b6b22' } },
    },
    series: [
      {
        name: '请求量',
        type: 'bar',
        data: metrics.errorTrend.map((t: any) => t.total),
        itemStyle: { color: '#1890ff66' },
      },
      {
        name: '错误数',
        type: 'bar',
        data: metrics.errorTrend.map((t: any) => t.errors),
        itemStyle: { color: '#ff6b6b' },
      },
    ],
  } : {}

  const apiTop10Option = metrics ? {
    title: { text: 'API响应时间Top10', left: 'center', textStyle: { fontSize: 14, color: '#ffa940' } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 140, right: 40, top: 40, bottom: 20 },
    xAxis: {
      type: 'value',
      name: '平均耗时(ms)',
      axisLine: { lineStyle: { color: '#ffa94033' } },
      axisLabel: { color: '#ffa940' },
      splitLine: { lineStyle: { color: '#ffa94022' } },
    },
    yAxis: {
      type: 'category',
      data: metrics.apiTop10.map((a: any) => a.url.length > 30 ? a.url.slice(0, 30) + '...' : a.url).reverse(),
      axisLine: { lineStyle: { color: '#ffa94033' } },
      axisLabel: { color: '#ffa940', fontSize: 11 },
    },
    series: [
      {
        type: 'bar',
        data: metrics.apiTop10.map((a: any) => a.avgTime).reverse(),
        itemStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#ffa940' },
              { offset: 1, color: '#ff7a45' },
            ],
          },
        },
        label: { show: true, position: 'right', color: '#fff', formatter: '{c}ms' },
      },
    ],
  } : {}

  const apiColumns = [
    { title: '接口URL', dataIndex: 'url', key: 'url', ellipsis: true, width: 280 },
    { title: '调用次数', dataIndex: 'count', key: 'count', width: 100 },
    {
      title: '平均耗时',
      dataIndex: 'avgTime',
      key: 'avgTime',
      width: 100,
      render: (v: number) => (
        <Tag color={v > 1000 ? 'red' : v > 500 ? 'orange' : 'green'}>{v}ms</Tag>
      ),
    },
    {
      title: '错误数',
      dataIndex: 'errorCount',
      key: 'errorCount',
      width: 80,
      render: (v: number) => <Badge count={v} showZero />,
    },
  ]

  const slowSqlColumns = [
    {
      title: 'SQL语句',
      dataIndex: 'sql',
      key: 'sql',
      ellipsis: true,
      render: (v: string) => (
        <Tooltip title={v}>
          <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{v}</span>
        </Tooltip>
      ),
    },
    {
      title: '执行时间',
      dataIndex: 'executeTime',
      key: 'executeTime',
      width: 100,
      render: (v: number) => <Tag color="red">{v}ms</Tag>,
    },
    { title: '数据源', dataIndex: 'dataSource', key: 'dataSource', width: 100 },
    {
      title: '发生时间',
      dataIndex: 'happenTime',
      key: 'happenTime',
      width: 160,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm:ss'),
    },
  ]

  const handleClearAlert = async (alertId: string) => {
    try {
      await monitorApi.clearAlert(alertId)
      message.success('告警已清除')
      fetchData()
    } catch (e) {
      message.error('清除告警失败')
    }
  }

  const openAddRuleModal = () => {
    setEditingRule(null)
    form.resetFields()
    form.setFieldsValue({
      enabled: true,
      operator: '>',
      duration: 5,
      notifyType: 'dingtalk',
      notifyTargets: [],
      threshold: 5,
      ruleType: 'error_rate',
      metricName: 'error_rate',
    })
    setAlertModalVisible(true)
  }

  const openEditRuleModal = (rule: AlertRule) => {
    setEditingRule(rule)
    form.setFieldsValue(rule)
    setAlertModalVisible(true)
  }

  const handleSaveRule = async () => {
    try {
      const values = await form.validateFields()
      if (editingRule) {
        await monitorApi.updateAlertRule({ ...editingRule, ...values })
        message.success('规则更新成功')
      } else {
        await monitorApi.addAlertRule(values)
        message.success('规则创建成功')
      }
      setAlertModalVisible(false)
      fetchData()
    } catch (e) {
      console.error(e)
    }
  }

  const handleDeleteRule = async (id: number) => {
    try {
      await monitorApi.deleteAlertRule(id)
      message.success('规则删除成功')
      fetchData()
    } catch (e) {
      message.error('删除失败')
    }
  }

  if (!metrics) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
        <Spin size="large" tip="加载监控数据中..." />
      </div>
    )
  }

  const statCards = [
    { title: '今日PV', value: metrics.pvTotal, icon: <EyeOutlined />, color: '#00e5ff', bgColor: 'rgba(0, 229, 255, 0.1)' },
    { title: '今日UV', value: metrics.uvTotal, icon: <TeamOutlined />, color: '#52c41a', bgColor: 'rgba(82, 196, 26, 0.1)' },
    { title: '请求总量', value: metrics.requestTotal, icon: <ApiOutlined />, color: '#1890ff', bgColor: 'rgba(24, 144, 255, 0.1)' },
    { title: '错误数', value: metrics.errorTotal, icon: <WarningOutlined />, color: '#ff4d4f', bgColor: 'rgba(255, 77, 79, 0.1)' },
    { title: '错误率', value: `${metrics.errorRate.toFixed(2)}%`, icon: <ThunderboltOutlined />, color: '#ffa940', bgColor: 'rgba(255, 169, 64, 0.1)' },
    { title: 'QPS', value: metrics.qps, icon: <DashboardOutlined />, color: '#722ed1', bgColor: 'rgba(114, 46, 209, 0.1)' },
    { title: '平均响应', value: `${metrics.avgResponseTime.toFixed(0)}ms`, icon: <ClockCircleOutlined />, color: '#13c2c2', bgColor: 'rgba(19, 194, 194, 0.1)' },
    { title: '活跃告警', value: metrics.activeAlerts.length, icon: <BellOutlined />, color: '#eb2f96', bgColor: 'rgba(235, 47, 150, 0.1)' },
  ]

  return (
    <div
      style={{
        padding: 16,
        background: 'linear-gradient(135deg, #0a0e27 0%, #1a1f3a 50%, #0d1117 100%)',
        minHeight: 'calc(100vh - 96px)',
        color: '#fff',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ color: '#00e5ff', margin: 0, fontSize: 24, fontWeight: 'bold' }}>
          <DashboardOutlined /> 实时监控大屏
        </h2>
        <Space>
          <span style={{ color: '#aaa' }}>
            最后更新: {dayjs().format('YYYY-MM-DD HH:mm:ss')}
          </span>
          <Switch
            checked={autoRefresh}
            onChange={setAutoRefresh}
            checkedChildren="自动刷新"
            unCheckedChildren="已暂停"
          />
          <Button icon={<ReloadOutlined spin={loading} />} onClick={fetchData}>
            刷新
          </Button>
        </Space>
      </div>

      <Row gutter={[12, 12]} style={{ marginBottom: 12 }}>
        {statCards.map((card, i) => (
          <Col span={3} key={i}>
            <Card
              style={{
                background: card.bgColor,
                border: `1px solid ${card.color}33`,
                borderRadius: 8,
              }}
              styles={{ body: { padding: 16 } }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Statistic
                  title={<span style={{ color: '#ccc', fontSize: 12 }}>{card.title}</span>}
                  value={card.value as any}
                  valueStyle={{ color: card.color, fontSize: 24, fontWeight: 'bold' }}
                />
                <div
                  style={{
                    fontSize: 32,
                    color: card.color,
                    opacity: 0.6,
                  }}
                >
                  {card.icon}
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[12, 12]} style={{ marginBottom: 12 }}>
        <Col span={8}>
          <Card
            style={{ background: '#0d111788', border: '1px solid #00e5ff33', borderRadius: 8 }}
            styles={{ body: { padding: 12 } }}
          >
            <ReactECharts option={trendOption} style={{ height: 240 }} theme="dark" />
          </Card>
        </Col>
        <Col span={8}>
          <Card
            style={{ background: '#0d111788', border: '1px solid #ff6b6b33', borderRadius: 8 }}
            styles={{ body: { padding: 12 } }}
          >
            <ReactECharts option={errorTrendOption} style={{ height: 240 }} theme="dark" />
          </Card>
        </Col>
        <Col span={8}>
          <Card
            style={{ background: '#0d111788', border: '1px solid #ffa94033', borderRadius: 8 }}
            styles={{ body: { padding: 12 } }}
          >
            <ReactECharts option={apiTop10Option} style={{ height: 240 }} theme="dark" />
          </Card>
        </Col>
      </Row>

      <Row gutter={[12, 12]} style={{ marginBottom: 12 }}>
        <Col span={14}>
          <Card
            title={<span style={{ color: '#ffa940' }}>API响应排行</span>}
            style={{ background: '#0d111788', border: '1px solid #ffa94033', borderRadius: 8 }}
            styles={{ body: { padding: 8 } }}
            size="small"
          >
            <Table
              columns={apiColumns}
              dataSource={metrics.apiTop10}
              pagination={false}
              size="small"
              rowKey="url"
              style={{ color: '#fff' }}
            />
          </Card>
        </Col>
        <Col span={10}>
          <Card
            title={<span style={{ color: '#ff4d4f' }}>慢SQL分析</span>}
            style={{ background: '#0d111788', border: '1px solid #ff4d4f33', borderRadius: 8 }}
            styles={{ body: { padding: 8 } }}
            size="small"
          >
            <Table
              columns={slowSqlColumns}
              dataSource={metrics.slowSqlList}
              pagination={false}
              size="small"
              rowKey={(record: any) => record.sql + record.happenTime}
              style={{ color: '#fff' }}
              locale={{ emptyText: '暂无慢SQL记录' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[12, 12]}>
        <Col span={8}>
          <Card
            title={<span style={{ color: '#52c41a' }}>服务状态</span>}
            style={{ background: '#0d111788', border: '1px solid #52c41a33', borderRadius: 8 }}
            styles={{ body: { padding: 12 } }}
            size="small"
          >
            {metrics.serviceStatus.map((svc: any, i: number) => (
              <div
                key={i}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '8px 0',
                  borderBottom: i < metrics.serviceStatus.length - 1 ? '1px solid #ffffff11' : 'none',
                }}
              >
                <Space>
                  {getStatusIcon(svc.status)}
                  <span style={{ color: '#fff', fontFamily: 'monospace' }}>{svc.name}</span>
                </Space>
                <Space>
                  <Tag color={getStatusColor(svc.status)}>
                    {getStatusText(svc.status)}
                  </Tag>
                  <span style={{ color: '#888', fontSize: 12 }}>
                    {svc.totalRequests} req | {svc.errorCount} err
                  </span>
                </Space>
              </div>
            ))}
          </Card>
        </Col>
        <Col span={8}>
          <Card
            title={
              <Space>
                <span style={{ color: '#ff4d4f' }}>实时告警</span>
                <Badge count={metrics.activeAlerts.length} size="small" />
              </Space>
            }
            style={{ background: '#0d111788', border: '1px solid #ff4d4f33', borderRadius: 8 }}
            styles={{ body: { padding: 8 } }}
            size="small"
          >
            {metrics.activeAlerts.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '24px 0', color: '#888' }}>
                <CheckCircleOutlined style={{ fontSize: 32, color: '#52c41a' }} />
                <div style={{ marginTop: 8 }}>暂无告警</div>
              </div>
            ) : (
              <Space direction="vertical" style={{ width: '100%' }}>
                {metrics.activeAlerts.map((alert: any, i: number) => (
                  <Alert
                    key={i}
                    type="warning"
                    showIcon
                    message={
                      <Space>
                        <span style={{ fontWeight: 'bold' }}>{alert.ruleName}</span>
                        <span style={{ color: '#888', fontSize: 12 }}>
                          {dayjs(alert.triggerTime).format('HH:mm:ss')}
                        </span>
                      </Space>
                    }
                    description={alert.message}
                    action={
                      <Button size="small" type="link" onClick={() => handleClearAlert(alert.id)}>
                        清除
                      </Button>
                    }
                  />
                ))}
              </Space>
            )}
          </Card>
        </Col>
        <Col span={8}>
          <Card
            title={
              <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                <span style={{ color: '#722ed1' }}>告警规则</span>
                <Button
                  type="primary"
                  size="small"
                  icon={<PlusOutlined />}
                  onClick={openAddRuleModal}
                >
                  新增
                </Button>
              </Space>
            }
            style={{ background: '#0d111788', border: '1px solid #722ed133', borderRadius: 8 }}
            styles={{ body: { padding: 8 } }}
            size="small"
          >
            {alertRules.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '24px 0', color: '#888' }}>
                暂无告警规则
              </div>
            ) : (
              alertRules.map((rule: AlertRule, i: number) => (
                <div
                  key={rule.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '8px 0',
                    borderBottom: i < alertRules.length - 1 ? '1px solid #ffffff11' : 'none',
                  }}
                >
                  <Space direction="vertical" size={0}>
                    <Space>
                      {rule.enabled ? (
                        <Tag color="green">启用</Tag>
                      ) : (
                        <Tag color="default">禁用</Tag>
                      )}
                      <span style={{ color: '#fff' }}>{rule.ruleName}</span>
                    </Space>
                    <span style={{ color: '#888', fontSize: 12 }}>
                      {rule.ruleType === 'error_rate' ? '错误率' : '慢接口'} {rule.operator} {rule.threshold}
                      {rule.ruleType === 'error_rate' ? '%' : 'ms'}
                    </span>
                  </Space>
                  <Space>
                    <Button
                      size="small"
                      icon={<EditOutlined />}
                      onClick={() => openEditRuleModal(rule)}
                    />
                    <Button
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => handleDeleteRule(rule.id!)}
                    />
                  </Space>
                </div>
              ))
            )}
          </Card>
        </Col>
      </Row>

      <Modal
        title={editingRule ? '编辑告警规则' : '新增告警规则'}
        open={alertModalVisible}
        onOk={handleSaveRule}
        onCancel={() => setAlertModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="ruleName" label="规则名称" rules={[{ required: true }]}>
                <Input placeholder="例如：错误率超过5%" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="enabled" label="是否启用" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="ruleType" label="规则类型" rules={[{ required: true }]}>
                <Select>
                  <Option value="error_rate">错误率告警</Option>
                  <Option value="slow_api">慢接口告警</Option>
                  <Option value="slow_sql">慢SQL告警</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="operator" label="比较符" rules={[{ required: true }]}>
                <Select>
                  <Option value=">">{'>'}</Option>
                  <Option value=">=">{'≥'}</Option>
                  <Option value="<">{'<'}</Option>
                  <Option value="<=">{'≤'}</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={10}>
              <Form.Item name="threshold" label="阈值" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="duration" label="持续时间(分钟)">
                <InputNumber style={{ width: '100%' }} min={1} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="notifyType" label="通知方式">
                <Select>
                  <Option value="dingtalk">钉钉通知</Option>
                  <Option value="email">邮件</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="webhookUrl" label="Webhook地址">
            <Input placeholder="钉钉机器人Webhook URL" />
          </Form.Item>
          <Form.Item name="notifyTargets" label="通知对象(手机号)">
            <Select mode="tags" placeholder="输入手机号后按回车" />
          </Form.Item>
          <Form.Item name="description" label="规则描述">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default MonitorDashboard
