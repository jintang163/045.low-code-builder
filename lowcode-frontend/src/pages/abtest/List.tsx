import React, { useEffect, useState } from 'react'
import {
  Table,
  Button,
  Space,
  Tag,
  Input,
  Select,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  Badge,
  Tooltip,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  SearchOutlined,
  ExperimentOutlined,
  FileTextOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { ABTestInfo, abtestApi, mockABTests } from '@/api/abtest'
import { useAppStore } from '@/store/appStore'
import dayjs from 'dayjs'

const { Option } = Select

const ABTestList: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [data, setData] = useState<ABTestInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<number | null>(null)
  const [deleteLoading, setDeleteLoading] = useState<number | null>(null)
  const [actionLoading, setActionLoading] = useState<number | null>(null)

  const statusMap: Record<number, { text: string; color: string; status: any }> = {
    0: { text: '草稿', color: 'default', status: 'default' },
    1: { text: '运行中', color: 'green', status: 'processing' },
    2: { text: '已暂停', color: 'orange', status: 'warning' },
    3: { text: '已结束', color: 'blue', status: 'success' },
  }

  const resourceTypeMap: Record<string, { text: string; icon: any; color: string }> = {
    page: { text: '页面', icon: <FileTextOutlined />, color: 'blue' },
    component: { text: '组件', icon: <ThunderboltOutlined />, color: 'purple' },
    logic: { text: '逻辑', icon: <ExperimentOutlined />, color: 'green' },
  }

  const loadData = async () => {
    if (!currentApp) return
    setLoading(true)
    try {
      const res: any = await abtestApi.list(currentApp.id!)
      if (res.code === 0 || res.code === 200) {
        setData(res.data || [])
      } else {
        setData(mockABTests)
      }
    } catch (e) {
      console.error(e)
      setData(mockABTests)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [currentApp])

  const handleCreate = () => {
    navigate('/abtest/designer')
  }

  const handleEdit = (record: ABTestInfo) => {
    navigate(`/abtest/designer/${record.id}`)
  }

  const handleDetail = (record: ABTestInfo) => {
    navigate(`/abtest/detail/${record.id}`)
  }

  const handleDelete = async (id: number) => {
    setDeleteLoading(id)
    try {
      const res: any = await abtestApi.delete(id)
      if (res.code === 0 || res.code === 200) {
        message.success('删除成功')
        setData(prev => prev.filter(item => item.id !== id))
      } else {
        message.error(res.message || '删除失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('删除失败: ' + (e.message || ''))
    } finally {
      setDeleteLoading(null)
    }
  }

  const handleStart = async (record: ABTestInfo) => {
    setActionLoading(record.id!)
    try {
      const res: any = await abtestApi.start(record.id!)
      if (res.code === 0 || res.code === 200) {
        message.success('测试已启动')
        setData(prev => prev.map(item =>
          item.id === record.id ? { ...item, status: 1 } : item
        ))
      } else {
        message.error(res.message || '启动失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('启动失败: ' + (e.message || ''))
    } finally {
      setActionLoading(null)
    }
  }

  const handlePause = async (record: ABTestInfo) => {
    setActionLoading(record.id!)
    try {
      const res: any = await abtestApi.pause(record.id!)
      if (res.code === 0 || res.code === 200) {
        message.success('测试已暂停')
        setData(prev => prev.map(item =>
          item.id === record.id ? { ...item, status: 2 } : item
        ))
      } else {
        message.error(res.message || '暂停失败')
      }
    } catch (e: any) {
      console.error(e)
      message.error('暂停失败: ' + (e.message || ''))
    } finally {
      setActionLoading(null)
    }
  }

  const filteredData = data.filter(item => {
    const matchSearch = !searchText ||
      item.testName.toLowerCase().includes(searchText.toLowerCase()) ||
      item.testCode.toLowerCase().includes(searchText.toLowerCase())
    const matchStatus = statusFilter === null || item.status === statusFilter
    return matchSearch && matchStatus
  })

  const columns = [
    {
      title: '测试名称',
      dataIndex: 'testName',
      key: 'testName',
      width: 220,
      render: (text: string, record: ABTestInfo) => (
        <Space>
          <ExperimentOutlined style={{ color: '#1677ff' }} />
          <span style={{ fontWeight: 500 }}>{text}</span>
        </Space>
      ),
    },
    {
      title: '测试编码',
      dataIndex: 'testCode',
      key: 'testCode',
      width: 180,
      render: (text: string) => (
        <Tag color="default" style={{ fontFamily: 'monospace' }}>
          {text}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => {
        const info = statusMap[status] || statusMap[0]
        return (
          <Badge status={info.status} text={info.text} />
        )
      },
    },
    {
      title: '资源类型',
      dataIndex: 'resourceType',
      key: 'resourceType',
      width: 100,
      render: (type: string) => {
        const info = resourceTypeMap[type] || resourceTypeMap.page
        return (
          <Tag color={info.color} icon={info.icon}>
            {info.text}
          </Tag>
        )
      },
    },
    {
      title: '流量分配',
      key: 'traffic',
      width: 120,
      render: (_: any, record: ABTestInfo) => {
        const total = record.variants?.reduce((sum, v) => sum + v.trafficWeight, 0) || 0
        return (
          <span>
            {record.variants?.length || 0} 个变体
            <span style={{ color: '#999', marginLeft: 4 }}>
              ({total}%)
            </span>
          </span>
        )
      },
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 160,
      render: (time: string) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 160,
      render: (time: string) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_: any, record: ABTestInfo) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleDetail(record)}
            >
              详情
            </Button>
          </Tooltip>
          {record.status === 0 && (
            <Tooltip title="编辑">
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleEdit(record)}
              >
                编辑
              </Button>
            </Tooltip>
          )}
          {record.status === 0 && (
            <Tooltip title="开始测试">
              <Button
                type="link"
                size="small"
                icon={<PlayCircleOutlined />}
                loading={actionLoading === record.id}
                onClick={() => handleStart(record)}
              >
                开始
              </Button>
            </Tooltip>
          )}
          {record.status === 1 && (
            <Tooltip title="暂停测试">
              <Button
                type="link"
                size="small"
                icon={<PauseCircleOutlined />}
                loading={actionLoading === record.id}
                onClick={() => handlePause(record)}
              >
                暂停
              </Button>
            </Tooltip>
          )}
          {record.status === 2 && (
            <Tooltip title="继续测试">
              <Button
                type="link"
                size="small"
                icon={<PlayCircleOutlined />}
                loading={actionLoading === record.id}
                onClick={() => handleStart(record)}
              >
                继续
              </Button>
            </Tooltip>
          )}
          {(record.status === 0 || record.status === 2 || record.status === 3) && (
            <Popconfirm title="确定删除该测试?" onConfirm={() => handleDelete(record.id!)}>
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                loading={deleteLoading === record.id}
              >
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  const statusTabs = [
    { key: 'all', label: '全部', value: null },
    { key: 'draft', label: '草稿', value: 0 },
    { key: 'running', label: '运行中', value: 1 },
    { key: 'paused', label: '已暂停', value: 2 },
    { key: 'ended', label: '已结束', value: 3 },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ margin: 0 }}>A/B 测试管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建测试
        </Button>
      </div>

      <Card>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={8}>
            <Input
              placeholder="搜索测试名称/编码"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              allowClear
            />
          </Col>
          <Col span={16}>
            <Space>
              {statusTabs.map(tab => (
                <Tag.CheckableTag
                  key={tab.key}
                  checked={statusFilter === tab.value}
                  onChange={(checked) => {
                    setStatusFilter(checked ? tab.value : null)
                  }}
                  style={{
                    padding: '4px 12px',
                    borderRadius: 4,
                    fontSize: 13,
                  }}
                >
                  {tab.label}
                </Tag.CheckableTag>
              ))}
            </Space>
          </Col>
        </Row>

        <Table
          columns={columns}
          dataSource={filteredData}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1200 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </Card>
    </div>
  )
}

export default ABTestList
