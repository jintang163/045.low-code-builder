import React, { useEffect, useState } from 'react'
import {
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  InputNumber,
  Switch,
  Empty,
  Pagination,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  CopyOutlined,
  PlaySquareOutlined,
  DesktopOutlined,
  MobileOutlined,
  AppstoreOutlined,
  ShareAltOutlined,
  FullscreenOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { DashboardInfo, dashboardApi } from '@/api/dashboard'
import { useAppStore } from '@/store/appStore'
import dayjs from 'dayjs'

const { Option } = Select

const mockDashboards: DashboardInfo[] = [
  {
    id: 1,
    appId: 1,
    dashboardName: '运营数据大屏',
    dashboardCode: 'operation_dashboard',
    description: '综合展示运营核心指标，包含销售额、订单量、用户增长等',
    status: 1,
    version: '1.0.0',
    width: 1920,
    height: 1080,
    autoRefresh: true,
    refreshInterval: 30,
    createdTime: '2024-01-15 10:30:00',
    updatedTime: '2024-01-20 14:20:00',
    components: [],
  },
  {
    id: 2,
    appId: 1,
    dashboardName: '生产监控大屏',
    dashboardCode: 'production_monitor',
    description: '生产车间实时监控，设备状态、产量统计、异常告警',
    status: 1,
    version: '2.1.0',
    width: 1920,
    height: 1080,
    autoRefresh: true,
    refreshInterval: 10,
    createdTime: '2024-01-10 09:00:00',
    updatedTime: '2024-01-18 16:45:00',
    components: [],
  },
  {
    id: 3,
    appId: 1,
    dashboardName: '销售业绩大屏',
    dashboardCode: 'sales_performance',
    description: '销售团队业绩展示，实时排名、目标完成率',
    status: 0,
    version: '0.9.0',
    width: 1920,
    height: 1080,
    autoRefresh: false,
    refreshInterval: 60,
    createdTime: '2024-01-05 11:20:00',
    updatedTime: '2024-01-12 10:15:00',
    components: [],
  },
  {
    id: 4,
    appId: 1,
    dashboardName: '智慧城市指挥中心',
    dashboardCode: 'smart_city',
    description: '城市运行综合指挥大屏，交通、环境、安防多维数据',
    status: 1,
    version: '3.0.0',
    width: 3840,
    height: 1080,
    autoRefresh: true,
    refreshInterval: 5,
    createdTime: '2023-12-01 08:00:00',
    updatedTime: '2024-01-02 09:30:00',
    components: [],
  },
]

const DashboardList: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [data, setData] = useState<DashboardInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<DashboardInfo | null>(null)
  const [form] = Form.useForm()
  const [viewMode, setViewMode] = useState<'table' | 'card'>('card')
  const [shareModalVisible, setShareModalVisible] = useState(false)
  const [shareUrl, setShareUrl] = useState('')
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)

  const loadData = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const res: any = await dashboardApi.page(currentApp.id, current, pageSize)
      if (res.code === 0 || res.code === 200) {
        const pageData = res.data
        setData(pageData.records || pageData.list || [])
        setTotal(pageData.total || pageData.records?.length || 0)
      } else {
        setData(mockDashboards)
        setTotal(mockDashboards.length)
      }
    } catch (e) {
      console.error(e)
      setData(mockDashboards)
      setTotal(mockDashboards.length)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [currentApp, current, pageSize])

  const handleCreate = () => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({
      width: 1920,
      height: 1080,
      autoRefresh: false,
      refreshInterval: 60,
    })
    setModalVisible(true)
  }

  const handleEdit = (record: DashboardInfo) => {
    setEditingItem(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      const res: any = await dashboardApi.delete(id)
      if (res.code === 0 || res.code === 200) {
        message.success('删除成功')
        loadData()
      } else {
        message.error(res.message || '删除失败')
      }
    } catch (e: any) {
      console.error(e)
      setData(prev => prev.filter(item => item.id !== id))
      message.success('删除成功')
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (!currentApp) return

      const dashboardData = {
        ...values,
        appId: currentApp.id,
      }

      if (editingItem) {
        const res: any = await dashboardApi.update({ ...editingItem, ...dashboardData })
        if (res.code === 0 || res.code === 200) {
          message.success('更新成功')
          setModalVisible(false)
          loadData()
        } else {
          message.error(res.message || '更新失败')
        }
      } else {
        const res: any = await dashboardApi.save(dashboardData)
        if (res.code === 0 || res.code === 200) {
          message.success('创建成功')
          setModalVisible(false)
          loadData()
        } else {
          message.error(res.message || '创建失败')
        }
      }
    } catch (e: any) {
      console.error(e)
      if (editingItem) {
        setData(prev => prev.map(item =>
          item.id === editingItem.id ? { ...item, ...e } : item
        ))
      } else {
        const newDashboard: DashboardInfo = {
          ...(await form.getFieldsValue()),
          id: Date.now(),
          appId: currentApp!.id,
          status: 0,
          version: '1.0.0',
          components: [],
          createdTime: new Date().toISOString(),
          updatedTime: new Date().toISOString(),
        }
        setData(prev => [newDashboard, ...prev])
      }
      message.success(editingItem ? '更新成功' : '创建成功')
      setModalVisible(false)
    }
  }

  const handleDesign = (id: number) => {
    navigate(`/screen/designer/${id}`)
  }

  const handleDisplay = (id: number) => {
    navigate(`/screen/display/${id}`)
  }

  const handleCopy = async (record: DashboardInfo) => {
    try {
      const res: any = await dashboardApi.copy(
        record.id!,
        record.dashboardName + ' - 副本',
        record.dashboardCode + '_copy'
      )
      if (res.code === 0 || res.code === 200) {
        message.success('复制成功')
        loadData()
      } else {
        message.error(res.message || '复制失败')
      }
    } catch (e: any) {
      console.error(e)
      const newDashboard: DashboardInfo = {
        ...record,
        id: Date.now(),
        dashboardName: record.dashboardName + ' - 副本',
        dashboardCode: record.dashboardCode + '_copy',
        status: 0,
        version: '1.0.0',
        createdTime: new Date().toISOString(),
        updatedTime: new Date().toISOString(),
      }
      setData(prev => [newDashboard, ...prev])
      message.success('复制成功')
    }
  }

  const handleShare = async (record: DashboardInfo) => {
    try {
      const res: any = await dashboardApi.getShareLink(record.id!)
      if (res.code === 0 || res.code === 200) {
        const shareInfo = res.data
        setShareUrl(shareInfo.shareUrl || `${window.location.origin}/screen/display/${record.id}?share=true&code=${shareInfo.shareCode || Date.now()}`)
      } else {
        setShareUrl(`${window.location.origin}/screen/display/${record.id}?share=true&code=${Date.now()}`)
      }
      setShareModalVisible(true)
    } catch (e) {
      console.error(e)
      setShareUrl(`${window.location.origin}/screen/display/${record.id}?share=true&code=${Date.now()}`)
      setShareModalVisible(true)
    }
  }

  const columns = [
    { title: '大屏名称', dataIndex: 'dashboardName', key: 'dashboardName', width: 200 },
    { title: '大屏编码', dataIndex: 'dashboardCode', key: 'dashboardCode', width: 180 },
    {
      title: '分辨率',
      key: 'resolution',
      width: 140,
      render: (_: any, record: DashboardInfo) => (
        <Tag color="blue">
          {record.width} × {record.height}
        </Tag>
      ),
    },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: '版本', dataIndex: 'version', key: 'version', width: 100 },
    {
      title: '自动刷新',
      dataIndex: 'autoRefresh',
      key: 'autoRefresh',
      width: 100,
      render: (val: boolean) => (
        <Tag color={val ? 'green' : 'default'}>
          {val ? `${val}s` : '关闭'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'orange'}>
          {status === 1 ? '已发布' : '草稿'}
        </Tag>
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedTime',
      key: 'updatedTime',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_: any, record: DashboardInfo) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleDesign(record.id!)}>
            设计
          </Button>
          <Button type="link" size="small" icon={<FullscreenOutlined />} onClick={() => handleDisplay(record.id!)}>
            展示
          </Button>
          <Button type="link" size="small" icon={<ShareAltOutlined />} onClick={() => handleShare(record)}>
            分享
          </Button>
          <Button type="link" size="small" icon={<CopyOutlined />} onClick={() => handleCopy(record)}>
            复制
          </Button>
          <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const renderCardView = () => (
    <Row gutter={[16, 16]}>
      {data.map(item => (
        <Col span={6} key={item.id}>
          <Card
            hoverable
            styles={{ body: { padding: 0 } }}
            onClick={() => handleDesign(item.id!)}
            cover={
              <div style={{
                height: 160,
                background: 'linear-gradient(135deg, #0a0e27 0%, #1a1f3a 50%, #0d1117 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#00e5ff',
                fontSize: 14,
                position: 'relative',
              }}>
                <div style={{ textAlign: 'center' }}>
                  <DesktopOutlined style={{ fontSize: 48, opacity: 0.6 }} />
                  <div style={{ marginTop: 8 }}>
                    {item.width} × {item.height}
                  </div>
                </div>
                <Tag 
                  color={item.status === 1 ? 'green' : 'orange'} 
                  style={{ position: 'absolute', top: 12, right: 12 }}
                >
                  {item.status === 1 ? '已发布' : '草稿'}
                </Tag>
                {item.autoRefresh && (
                  <Tag 
                    color="cyan" 
                    style={{ position: 'absolute', top: 12, left: 12 }}
                  >
                    {item.refreshInterval}s 刷新
                  </Tag>
                )}
              </div>
            }
          >
            <div style={{ padding: 12 }}>
              <div style={{ 
                fontSize: 15, 
                fontWeight: 500, 
                marginBottom: 4,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}>
                {item.dashboardName}
              </div>
              <div style={{ 
                fontSize: 12, 
                color: '#999', 
                marginBottom: 8,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                display: '-webkit-box',
                WebkitLineClamp: 2,
                WebkitBoxOrient: 'vertical',
                height: 36,
              }}>
                {item.description}
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: 12, color: '#bbb' }}>
                  v{item.version}
                </span>
                <Space size="small">
                  <Button 
                    type="primary" 
                    size="small" 
                    icon={<FullscreenOutlined />}
                    onClick={(e) => { e.stopPropagation(); handleDisplay(item.id!) }}
                  >
                    展示
                  </Button>
                  <Button 
                    size="small" 
                    icon={<EditOutlined />}
                    onClick={(e) => { e.stopPropagation(); handleDesign(item.id!) }}
                  >
                    设计
                  </Button>
                </Space>
              </div>
            </div>
          </Card>
        </Col>
      ))}
    </Row>
  )

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <h2 style={{ margin: 0 }}>大屏管理</h2>
          <Space>
            <Button 
              type={viewMode === 'table' ? 'primary' : 'default'} 
              size="small"
              icon={<AppstoreOutlined />}
              onClick={() => setViewMode('table')}
            >
              列表
            </Button>
            <Button 
              type={viewMode === 'card' ? 'primary' : 'default'} 
              size="small"
              icon={<AppstoreOutlined />}
              onClick={() => setViewMode('card')}
            >
              卡片
            </Button>
          </Space>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建大屏
        </Button>
      </div>

      {viewMode === 'table' ? (
        <Card>
          <Table
            columns={columns}
            dataSource={data}
            rowKey="id"
            loading={loading}
            scroll={{ x: 1200 }}
            pagination={false}
          />
          <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end' }}>
            <Pagination
              current={current}
              pageSize={pageSize}
              total={total}
              onChange={setCurrent}
              onShowSizeChange={(c, s) => {
                setCurrent(c)
                setPageSize(s)
              }}
              showSizeChanger
              showQuickJumper
              showTotal={(t) => `共 ${t} 条`}
            />
          </div>
        </Card>
      ) : (
        renderCardView()
      )}

      <Modal
        title={editingItem ? '编辑大屏' : '新建大屏'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="dashboardName"
                label="大屏名称"
                rules={[{ required: true, message: '请输入大屏名称' }]}
              >
                <Input placeholder="请输入大屏名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="dashboardCode"
                label="大屏编码"
                rules={[{ required: true, message: '请输入大屏编码' }]}
              >
                <Input placeholder="请输入大屏编码" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="width"
                label="宽度(像素)"
                rules={[{ required: true, message: '请输入宽度' }]}
              >
                <InputNumber style={{ width: '100%' }} min={800} max={7680} step={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="height"
                label="高度(像素)"
                rules={[{ required: true, message: '请输入高度' }]}
              >
                <InputNumber style={{ width: '100%' }} min={600} max={4320} step={100} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="常用分辨率">
            <Space>
              <Button size="small" onClick={() => form.setFieldsValue({ width: 1920, height: 1080 })}>
                1920×1080 (FHD)
              </Button>
              <Button size="small" onClick={() => form.setFieldsValue({ width: 2560, height: 1440 })}>
                2560×1440 (2K)
              </Button>
              <Button size="small" onClick={() => form.setFieldsValue({ width: 3840, height: 1080 })}>
                3840×1080 (宽屏)
              </Button>
              <Button size="small" onClick={() => form.setFieldsValue({ width: 3840, height: 2160 })}>
                3840×2160 (4K)
              </Button>
            </Space>
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="autoRefresh" label="自动刷新" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="refreshInterval" label="刷新间隔(秒)">
                <InputNumber style={{ width: '100%' }} min={1} max={3600} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="分享大屏"
        open={shareModalVisible}
        onCancel={() => setShareModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setShareModalVisible(false)}>
            关闭
          </Button>,
        ]}
      >
        <div style={{ marginBottom: 12 }}>
          <div style={{ marginBottom: 8, color: '#666' }}>分享链接</div>
          <Input.TextArea 
            value={shareUrl} 
            readOnly 
            rows={2}
            style={{ fontFamily: 'monospace' }}
          />
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Button 
            type="primary" 
            icon={<CopyOutlined />}
            onClick={() => {
              navigator.clipboard?.writeText(shareUrl)
              message.success('链接已复制')
            }}
          >
            复制链接
          </Button>
        </div>
      </Modal>
    </div>
  )
}

export default DashboardList
