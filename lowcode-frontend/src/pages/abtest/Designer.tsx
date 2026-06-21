import React, { useState, useEffect, useCallback } from 'react'
import {
  Card,
  Button,
  Space,
  Form,
  Input,
  Select,
  Table,
  message,
  Modal,
  Row,
  Col,
  InputNumber,
  Tag,
  Divider,
  List,
  Popconfirm,
  Tabs,
  Switch,
  DatePicker,
  Tooltip,
} from 'antd'
import {
  SaveOutlined,
  ArrowLeftOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  ExperimentOutlined,
  ControlOutlined,
  ThunderboltOutlined,
  SettingOutlined,
  PercentageOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { ABTestInfo, ABTestVariant, ABTestMetric, abtestApi, mockABTests } from '@/api/abtest'
import { useAppStore } from '@/store/appStore'
import { v4 as uuidv4 } from 'uuid'
import dayjs from 'dayjs'

const { TabPane } = Tabs
const { Option } = Select
const { TextArea } = Input
const { RangePicker } = DatePicker

const ABTestDesigner: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [form] = Form.useForm()
  const [variantForm] = Form.useForm()
  const [metricForm] = Form.useForm()
  const [abTest, setAbTest] = useState<ABTestInfo | null>(null)
  const [variants, setVariants] = useState<ABTestVariant[]>([])
  const [metrics, setMetrics] = useState<ABTestMetric[]>([])
  const [variantModalVisible, setVariantModalVisible] = useState(false)
  const [metricModalVisible, setMetricModalVisible] = useState(false)
  const [editingVariant, setEditingVariant] = useState<ABTestVariant | null>(null)
  const [editingMetric, setEditingMetric] = useState<ABTestMetric | null>(null)
  const [saveLoading, setSaveLoading] = useState(false)
  const [loadLoading, setLoadLoading] = useState(false)
  const [activeTab, setActiveTab] = useState('basic')

  const loadTest = useCallback(async () => {
    if (!id) return
    setLoadLoading(true)
    try {
      const res: any = await abtestApi.get(Number(id))
      if (res.code === 0 || res.code === 200) {
        const testData = res.data
        setAbTest(testData)
        setVariants(testData.variants || [])
        form.setFieldsValue({
          testName: testData.testName,
          testCode: testData.testCode,
          description: testData.description,
          resourceType: testData.resourceType,
          resourceId: testData.resourceId,
          trafficAllocationType: testData.trafficAllocationType,
          sampleSize: testData.sampleSize,
          confidenceLevel: testData.confidenceLevel,
          startTime: testData.startTime ? dayjs(testData.startTime) : undefined,
          endTime: testData.endTime ? dayjs(testData.endTime) : undefined,
        })
      } else {
        const mockTest = mockABTests.find(t => t.id === Number(id))
        if (mockTest) {
          setAbTest(mockTest)
          setVariants(mockTest.variants || [])
          form.setFieldsValue({
            testName: mockTest.testName,
            testCode: mockTest.testCode,
            description: mockTest.description,
            resourceType: mockTest.resourceType,
            resourceId: mockTest.resourceId,
            trafficAllocationType: mockTest.trafficAllocationType,
            sampleSize: mockTest.sampleSize,
            confidenceLevel: mockTest.confidenceLevel,
            startTime: mockTest.startTime ? dayjs(mockTest.startTime) : undefined,
            endTime: mockTest.endTime ? dayjs(mockTest.endTime) : undefined,
          })
        }
      }
    } catch (e: any) {
      console.error(e)
      const mockTest = mockABTests.find(t => t.id === Number(id))
      if (mockTest) {
        setAbTest(mockTest)
        setVariants(mockTest.variants || [])
        form.setFieldsValue({
          testName: mockTest.testName,
          testCode: mockTest.testCode,
          description: mockTest.description,
          resourceType: mockTest.resourceType,
          resourceId: mockTest.resourceId,
          trafficAllocationType: mockTest.trafficAllocationType,
          sampleSize: mockTest.sampleSize,
          confidenceLevel: mockTest.confidenceLevel,
        })
      }
    } finally {
      setLoadLoading(false)
    }
  }, [id, form])

  useEffect(() => {
    if (id) {
      loadTest()
    } else {
      const defaultVariants: ABTestVariant[] = [
        {
          id: -1,
          variantName: '对照组',
          variantType: 'control',
          trafficWeight: 50,
          description: '原始版本',
        },
        {
          id: -2,
          variantName: '实验组1',
          variantType: 'experimental',
          trafficWeight: 50,
          description: '测试版本',
        },
      ]
      setVariants(defaultVariants)
      setMetrics([
        {
          id: -1,
          metricName: '主转化率',
          metricType: 'conversion',
          metricKey: 'main_conversion',
        },
      ])
      form.setFieldsValue({
        testName: '',
        testCode: '',
        description: '',
        resourceType: 'page',
        resourceId: undefined,
        trafficAllocationType: 'percentage',
        sampleSize: 10000,
        confidenceLevel: 95,
      })
    }
  }, [id, currentApp])

  const handleSave = async () => {
    setSaveLoading(true)
    try {
      const values = await form.validateFields()

      if (variants.length < 2) {
        message.error('至少需要两个变体（对照组和实验组）')
        setSaveLoading(false)
        return
      }

      const hasControl = variants.some(v => v.variantType === 'control')
      if (!hasControl) {
        message.error('必须有一个对照组')
        setSaveLoading(false)
        return
      }

      const totalWeight = variants.reduce((sum, v) => sum + v.trafficWeight, 0)
      if (totalWeight !== 100) {
        message.error('所有变体的流量权重之和必须等于100%')
        setSaveLoading(false)
        return
      }

      const testData: ABTestInfo = {
        ...abTest,
        ...values,
        appId: currentApp?.id || 1,
        status: 0,
        startTime: values.startTime?.toISOString(),
        endTime: values.endTime?.toISOString(),
        variants,
      }

      let res: any
      if (id) {
        res = await abtestApi.update(testData)
      } else {
        res = await abtestApi.save(testData)
      }

      if (res.code === 0 || res.code === 200) {
        setAbTest(res.data)
        message.success('保存成功')
        if (!id && res.data?.id) {
          navigate(`/abtest/designer/${res.data.id}`)
        }
      } else {
        message.error(res.message || '保存失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('保存失败: ' + (e.message || ''))
    } finally {
      setSaveLoading(false)
    }
  }

  const handleAddVariant = () => {
    setEditingVariant(null)
    variantForm.resetFields()
    variantForm.setFieldsValue({
      variantName: '',
      variantType: 'experimental',
      trafficWeight: 0,
      description: '',
    })
    setVariantModalVisible(true)
  }

  const handleEditVariant = (variant: ABTestVariant) => {
    setEditingVariant(variant)
    variantForm.setFieldsValue(variant)
    setVariantModalVisible(true)
  }

  const handleVariantSubmit = async () => {
    try {
      const values = await variantForm.validateFields()

      if (editingVariant) {
        setVariants(prev => prev.map(v =>
          v.id === editingVariant.id ? { ...v, ...values } : v
        ))
      } else {
        const newVariant: ABTestVariant = {
          ...values,
          id: Date.now() * -1,
        }
        setVariants(prev => [...prev, newVariant])
      }

      setVariantModalVisible(false)
      message.success(editingVariant ? '更新成功' : '添加成功')
    } catch (e: any) {
      console.error(e)
    }
  }

  const handleDeleteVariant = (variantId: number) => {
    const variant = variants.find(v => v.id === variantId)
    if (variant?.variantType === 'control') {
      message.error('不能删除对照组')
      return
    }
    setVariants(prev => prev.filter(v => v.id !== variantId))
    message.success('删除成功')
  }

  const handleAddMetric = () => {
    setEditingMetric(null)
    metricForm.resetFields()
    metricForm.setFieldsValue({
      metricName: '',
      metricType: 'conversion',
      metricKey: '',
    })
    setMetricModalVisible(true)
  }

  const handleEditMetric = (metric: ABTestMetric) => {
    setEditingMetric(metric)
    metricForm.setFieldsValue(metric)
    setMetricModalVisible(true)
  }

  const handleMetricSubmit = async () => {
    try {
      const values = await metricForm.validateFields()

      if (editingMetric) {
        setMetrics(prev => prev.map(m =>
          m.id === editingMetric.id ? { ...m, ...values } : m
        ))
      } else {
        const newMetric: ABTestMetric = {
          ...values,
          id: Date.now() * -1,
        }
        setMetrics(prev => [...prev, newMetric])
      }

      setMetricModalVisible(false)
      message.success(editingMetric ? '更新成功' : '添加成功')
    } catch (e: any) {
      console.error(e)
    }
  }

  const handleDeleteMetric = (metricId: number) => {
    setMetrics(prev => prev.filter(m => m.id !== metricId))
    message.success('删除成功')
  }

  const variantColumns = [
    {
      title: '变体名称',
      dataIndex: 'variantName',
      key: 'variantName',
      width: 200,
      render: (text: string, record: ABTestVariant) => (
        <Space>
          {record.variantType === 'control' ? (
            <Tag color="blue" icon={<ControlOutlined />}>对照组</Tag>
          ) : (
            <Tag color="green" icon={<ThunderboltOutlined />}>实验组</Tag>
          )}
          <span>{text}</span>
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'variantType',
      key: 'variantType',
      width: 100,
      render: (type: string) => (
        <Tag color={type === 'control' ? 'blue' : 'green'}>
          {type === 'control' ? '对照组' : '实验组'}
        </Tag>
      ),
    },
    {
      title: '流量权重',
      dataIndex: 'trafficWeight',
      key: 'trafficWeight',
      width: 150,
      render: (weight: number) => (
        <Space>
          <PercentageOutlined style={{ color: '#1677ff' }} />
          <span>{weight}%</span>
        </Space>
      ),
    },
    {
      title: '版本',
      dataIndex: 'version',
      key: 'version',
      width: 100,
      render: (version?: string) => version || '-',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: ABTestVariant) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditVariant(record)}
          >
            编辑
          </Button>
          <Popconfirm title="确定删除该变体?" onConfirm={() => handleDeleteVariant(record.id!)}>
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
              disabled={record.variantType === 'control'}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const metricColumns = [
    {
      title: '指标名称',
      dataIndex: 'metricName',
      key: 'metricName',
      width: 200,
    },
    {
      title: '指标类型',
      dataIndex: 'metricType',
      key: 'metricType',
      width: 120,
      render: (type: string) => {
        const typeMap: Record<string, { text: string; color: string }> = {
          conversion: { text: '转化', color: 'green' },
          revenue: { text: '收入', color: 'gold' },
          engagement: { text: '参与度', color: 'blue' },
        }
        const info = typeMap[type] || typeMap.conversion
        return <Tag color={info.color}>{info.text}</Tag>
      },
    },
    {
      title: '指标Key',
      dataIndex: 'metricKey',
      key: 'metricKey',
      width: 200,
      render: (key: string) => (
        <code style={{ background: '#f5f5f5', padding: '2px 6px', borderRadius: 4 }}>{key}</code>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: ABTestMetric) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditMetric(record)}
          >
            编辑
          </Button>
          <Popconfirm title="确定删除该指标?" onConfirm={() => handleDeleteMetric(record.id!)}>
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const totalTraffic = variants.reduce((sum, v) => sum + v.trafficWeight, 0)

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
              {id ? '编辑 A/B 测试' : '新建 A/B 测试'}
            </h3>
            {abTest?.status !== undefined && (
              <Tag color={abTest.status === 1 ? 'green' : 'orange'}>
                {abTest.status === 0 ? '草稿' : abTest.status === 1 ? '运行中' : abTest.status === 2 ? '已暂停' : '已结束'}
              </Tag>
            )}
          </Space>
          <Space>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              onClick={handleSave}
              loading={saveLoading}
            >
              保存
            </Button>
          </Space>
        </div>
      </Card>

      <Card loading={loadLoading}>
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="基础信息" key="basic">
            <Form form={form} layout="vertical" style={{ maxWidth: 800, margin: '0 auto' }}>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="testName"
                    label="测试名称"
                    rules={[{ required: true, message: '请输入测试名称' }]}
                  >
                    <Input placeholder="请输入测试名称" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    name="testCode"
                    label="测试编码"
                    rules={[{ required: true, message: '请输入测试编码' }]}
                  >
                    <Input placeholder="请输入测试编码（英文）" />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item name="description" label="测试描述">
                <TextArea rows={3} placeholder="请输入测试描述" />
              </Form.Item>

              <Divider orientation="left">资源配置</Divider>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="resourceType"
                    label="资源类型"
                    rules={[{ required: true, message: '请选择资源类型' }]}
                  >
                    <Select placeholder="请选择资源类型">
                      <Option value="page">页面</Option>
                      <Option value="component">组件</Option>
                      <Option value="logic">逻辑流</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    name="resourceId"
                    label="关联资源"
                    rules={[{ required: true, message: '请选择关联资源' }]}
                  >
                    <Select placeholder="请选择关联资源">
                      <Option value={1}>首页</Option>
                      <Option value={2}>登录页</Option>
                      <Option value={3}>商品详情页</Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>

              <Divider orientation="left">流量配置</Divider>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="trafficAllocationType"
                    label="流量分配方式"
                    rules={[{ required: true, message: '请选择流量分配方式' }]}
                  >
                    <Select placeholder="请选择流量分配方式">
                      <Option value="percentage">按比例分配</Option>
                      <Option value="custom">自定义分配</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    name="sampleSize"
                    label="样本量"
                    tooltip="预计参与测试的用户数量"
                  >
                    <InputNumber style={{ width: '100%' }} min={100} step={100} />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="confidenceLevel"
                    label="置信度 (%)"
                    tooltip="统计显著性水平，通常为 95% 或 90%"
                    rules={[{ required: true, message: '请设置置信度' }]}
                  >
                    <Select>
                      <Option value={90}>90%</Option>
                      <Option value={95}>95%</Option>
                      <Option value={99}>99%</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="timeRange" label="测试时间范围">
                    <RangePicker
                      showTime
                      style={{ width: '100%' }}
                      placeholder={['开始时间', '结束时间']}
                    />
                  </Form.Item>
                </Col>
              </Row>
            </Form>
          </TabPane>

          <TabPane tab="变体管理" key="variants">
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Space>
                <span style={{ fontWeight: 500 }}>测试变体</span>
                <Tag color={totalTraffic === 100 ? 'green' : 'orange'}>
                  总流量: {totalTraffic}%
                </Tag>
              </Space>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleAddVariant}>
                添加变体
              </Button>
            </div>

            <Table
              columns={variantColumns}
              dataSource={variants}
              rowKey="id"
              pagination={false}
              locale={{ emptyText: '暂无变体，请添加' }}
            />

            <div style={{ marginTop: 16, padding: 12, background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 4 }}>
              <div style={{ color: '#389e0d', fontWeight: 500, marginBottom: 4 }}>
                <SettingOutlined style={{ marginRight: 4 }} />
                变体说明
              </div>
              <ul style={{ margin: 0, paddingLeft: 20, color: '#52c41a', fontSize: 13 }}>
                <li>必须有且仅有一个对照组（Control）</li>
                <li>所有变体的流量权重之和必须等于 100%</li>
                <li>对照组为原始版本，实验组为测试版本</li>
              </ul>
            </div>
          </TabPane>

          <TabPane tab="指标配置" key="metrics">
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 500 }}>转化指标</span>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleAddMetric}>
                添加指标
              </Button>
            </div>

            <Table
              columns={metricColumns}
              dataSource={metrics}
              rowKey="id"
              pagination={false}
              locale={{ emptyText: '暂无指标，请添加' }}
            />
          </TabPane>
        </Tabs>
      </Card>

      <Modal
        title={editingVariant ? '编辑变体' : '添加变体'}
        open={variantModalVisible}
        onOk={handleVariantSubmit}
        onCancel={() => setVariantModalVisible(false)}
        width={500}
        destroyOnClose
      >
        <Form form={variantForm} layout="vertical">
          <Form.Item
            name="variantName"
            label="变体名称"
            rules={[{ required: true, message: '请输入变体名称' }]}
          >
            <Input placeholder="请输入变体名称" />
          </Form.Item>

          <Form.Item
            name="variantType"
            label="变体类型"
            rules={[{ required: true, message: '请选择变体类型' }]}
          >
            <Select>
              <Option value="control">对照组（原始版本）</Option>
              <Option value="experimental">实验组（测试版本）</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="trafficWeight"
            label="流量权重 (%)"
            rules={[{ required: true, message: '请设置流量权重' }]}
          >
            <InputNumber style={{ width: '100%' }} min={0} max={100} step={5} />
          </Form.Item>

          <Form.Item name="description" label="描述">
            <TextArea rows={3} placeholder="请输入变体描述" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingMetric ? '编辑指标' : '添加指标'}
        open={metricModalVisible}
        onOk={handleMetricSubmit}
        onCancel={() => setMetricModalVisible(false)}
        width={500}
        destroyOnClose
      >
        <Form form={metricForm} layout="vertical">
          <Form.Item
            name="metricName"
            label="指标名称"
            rules={[{ required: true, message: '请输入指标名称' }]}
          >
            <Input placeholder="请输入指标名称" />
          </Form.Item>

          <Form.Item
            name="metricType"
            label="指标类型"
            rules={[{ required: true, message: '请选择指标类型' }]}
          >
            <Select>
              <Option value="conversion">转化</Option>
              <Option value="revenue">收入</Option>
              <Option value="engagement">参与度</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="metricKey"
            label="指标 Key"
            rules={[{ required: true, message: '请输入指标 Key' }]}
            tooltip="用于代码中埋点标识的唯一键"
          >
            <Input placeholder="例如: button_click, purchase" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ABTestDesigner
