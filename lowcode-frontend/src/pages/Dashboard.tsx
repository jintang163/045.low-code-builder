import React, { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, Table, Tag, Space, Button } from 'antd'
import {
  DatabaseOutlined,
  TableOutlined,
  FormOutlined,
  BranchesOutlined,
  ArrowUpOutlined,
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import { useNavigate } from 'react-router-dom'
import { useAppStore } from '@/store/appStore'

const Dashboard: React.FC = () => {
  const navigate = useNavigate()
  const { setCurrentApp } = useAppStore()
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setCurrentApp({ id: 1, name: '示例应用' })
  }, [])

  const stats = [
    { title: '数据源', value: 5, icon: <DatabaseOutlined style={{ color: '#1677ff' }} />, color: '#e6f7ff' },
    { title: '数据模型', value: 12, icon: <TableOutlined style={{ color: '#52c41a' }} />, color: '#f6ffed' },
    { title: '页面', value: 24, icon: <FormOutlined style={{ color: '#fa8c16' }} />, color: '#fff7e6' },
    { title: '业务逻辑', value: 8, icon: <BranchesOutlined style={{ color: '#722ed1' }} />, color: '#f9f0ff' },
  ]

  const lineOption = {
    title: { text: '应用使用趋势', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { data: ['页面访问', 'API调用'], bottom: 0 },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    },
    yAxis: { type: 'value' },
    series: [
      { name: '页面访问', type: 'line', smooth: true, data: [120, 132, 101, 134, 90, 230, 210] },
      { name: 'API调用', type: 'line', smooth: true, data: [220, 182, 191, 234, 290, 330, 310] },
    ],
  }

  const pieOption = {
    title: { text: '组件使用占比', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        label: { show: false },
        data: [
          { value: 1048, name: '表单组件' },
          { value: 735, name: '表格组件' },
          { value: 580, name: '按钮组件' },
          { value: 484, name: '图表组件' },
          { value: 300, name: '布局容器' },
        ],
      },
    ],
  }

  const recentModels = [
    { key: 1, name: '用户表', code: 'sys_user', fields: 8, status: '已发布', updateTime: '2024-01-15 14:30' },
    { key: 2, name: '订单表', code: 'biz_order', fields: 12, status: '已发布', updateTime: '2024-01-15 12:20' },
    { key: 3, name: '产品表', code: 'biz_product', fields: 15, status: '草稿', updateTime: '2024-01-14 18:45' },
    { key: 4, name: '角色表', code: 'sys_role', fields: 6, status: '已发布', updateTime: '2024-01-14 10:15' },
    { key: 5, name: '菜单表', code: 'sys_menu', fields: 10, status: '已发布', updateTime: '2024-01-13 16:30' },
  ]

  const modelColumns = [
    { title: '模型名称', dataIndex: 'name', key: 'name' },
    { title: '模型编码', dataIndex: 'code', key: 'code' },
    { title: '字段数', dataIndex: 'fields', key: 'fields' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === '已发布' ? 'green' : 'orange'}>{status}</Tag>
      ),
    },
    { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime' },
  ]

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        {stats.map((stat, index) => (
          <Col span={6} key={index}>
            <Card>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Statistic title={stat.title} value={stat.value} />
                <div
                  style={{
                    fontSize: 48,
                    width: 80,
                    height: 80,
                    borderRadius: '50%',
                    background: stat.color,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {stat.icon}
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={12}>
          <Card>
            <ReactECharts option={lineOption} style={{ height: 300 }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            <ReactECharts option={pieOption} style={{ height: 300 }} />
          </Card>
        </Col>
      </Row>

      <Card
        title="最近数据模型"
        extra={
          <Button type="link" onClick={() => navigate('/dataModel')}>
            查看全部 <ArrowUpOutlined rotate={-45} />
          </Button>
        }
      >
        <Table
          columns={modelColumns}
          dataSource={recentModels}
          pagination={false}
          size="middle"
        />
      </Card>
    </div>
  )
}

export default Dashboard
