import React, { useState, useEffect, useCallback } from 'react'
import {
  Layout, Button, Space, Form, Input, Select, Table, Card, Tabs, message,
  Row, Col, Tag, Tree, Spin, Empty, Modal, Divider, Popconfirm, Badge, Tooltip,
} from 'antd'
import {
  ArrowLeftOutlined, PlusOutlined, DeleteOutlined, SaveOutlined,
  PlayCircleOutlined, DatabaseOutlined, ApiOutlined, SwapOutlined,
  ReloadOutlined, LinkOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import {
  dataSourceApi, virtualViewApi, DataSource, TableColumnInfo,
  VirtualView, VirtualViewJoin,
} from '@/api/dataModel'
import { useAppStore } from '@/store/appStore'

const { Sider, Content } = Layout
const { TabPane } = Tabs
const { Option } = Select

interface FieldMapping {
  sourceField: string
  sourceType: string
  targetComponent: string
  targetProperty: string
}

const DataSourceDesigner: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { currentApp } = useAppStore()

  const [dataSource, setDataSource] = useState<DataSource | null>(null)
  const [tables, setTables] = useState<string[]>([])
  const [selectedTable, setSelectedTable] = useState<string | null>(null)
  const [columns, setColumns] = useState<TableColumnInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [tablesLoading, setTablesLoading] = useState(false)
  const [columnsLoading, setColumnsLoading] = useState(false)
  const [fieldMappings, setFieldMappings] = useState<FieldMapping[]>([])
  const [restApiResult, setRestApiResult] = useState<any[]>([])

  const [virtualViews, setVirtualViews] = useState<VirtualView[]>([])
  const [viewModalVisible, setViewModalVisible] = useState(false)
  const [editingView, setEditingView] = useState<VirtualView | null>(null)
  const [viewForm] = Form.useForm()

  const [joinModalVisible, setJoinModalVisible] = useState(false)
  const [joins, setJoins] = useState<VirtualViewJoin[]>([])
  const [joinForm] = Form.useForm()
  const [dataSources, setDataSources] = useState<DataSource[]>([])
  const [viewResult, setViewResult] = useState<any[]>([])
  const [viewResultVisible, setViewResultVisible] = useState(false)

  const fetchDataSources = useCallback(async () => {
    if (!currentApp?.id) return
    try {
      const res: any = await dataSourceApi.list(currentApp.id)
      if (res.code === 0 || res.code === 200) {
        setDataSources((res.data || []).filter((ds: DataSource) => ds.dbType !== 'rest_api'))
      }
    } catch (e: any) {
      message.error('获取数据源列表失败')
    }
  }, [currentApp])

  useEffect(() => {
    fetchDataSources()
  }, [fetchDataSources])

  const fetchDataSource = useCallback(async () => {
    if (!id) return
    try {
      setLoading(true)
      const res: any = await dataSourceApi.get(Number(id))
      if (res.code === 0 || res.code === 200) {
        setDataSource(res.data)
      }
    } catch (e: any) {
      message.error('获取数据源详情失败')
    } finally {
      setLoading(false)
    }
  }, [id])

  const fetchTables = useCallback(async () => {
    if (!id) return
    try {
      setTablesLoading(true)
      const res: any = await dataSourceApi.getTables(Number(id))
      if (res.code === 0 || res.code === 200) {
        setTables(res.data || [])
      }
    } catch (e: any) {
      message.error('获取表列表失败')
    } finally {
      setTablesLoading(false)
    }
  }, [id])

  const fetchColumns = useCallback(async (tableName: string) => {
    if (!id) return
    try {
      setColumnsLoading(true)
      setSelectedTable(tableName)
      const res: any = await dataSourceApi.getTableColumns(Number(id), tableName)
      if (res.code === 0 || res.code === 200) {
        setColumns(res.data || [])
      }
    } catch (e: any) {
      message.error('获取字段信息失败')
    } finally {
      setColumnsLoading(false)
    }
  }, [id])

  const fetchVirtualViews = useCallback(async () => {
    if (!currentApp?.id) return
    try {
      const res: any = await virtualViewApi.list(currentApp.id)
      if (res.code === 0 || res.code === 200) {
        setVirtualViews(res.data || [])
      }
    } catch (e: any) {
      message.error('获取虚拟视图失败')
    }
  }, [currentApp])

  useEffect(() => {
    if (id) {
      fetchDataSource()
      fetchTables()
    } else {
      fetchVirtualViews()
    }
  }, [id, fetchDataSource, fetchTables, fetchVirtualViews])

  const handleCallRestApi = async () => {
    if (!id) return
    try {
      setLoading(true)
      const res: any = await dataSourceApi.callRestApi(Number(id))
      if (res.code === 0 || res.code === 200) {
        setRestApiResult(res.data || [])
        message.success('API调用成功')
      }
    } catch (e: any) {
      message.error('API调用失败: ' + e.message)
    } finally {
      setLoading(false)
    }
  }

  const handleAddFieldMapping = (column: TableColumnInfo) => {
    setFieldMappings(prev => [...prev, {
      sourceField: column.columnName,
      sourceType: column.typeName,
      targetComponent: '',
      targetProperty: '',
    }])
    message.info(`已添加字段映射: ${column.columnName}`)
  }

  const handleRemoveFieldMapping = (index: number) => {
    setFieldMappings(prev => prev.filter((_, i) => i !== index))
  }

  const handleSaveVirtualView = async () => {
    try {
      const values = await viewForm.validateFields()
      const viewData: VirtualView = {
        ...values,
        appId: currentApp?.id || 0,
        joinConfig: JSON.stringify(joins),
      }
      if (editingView?.id) {
        const res: any = await virtualViewApi.update({ ...viewData, id: editingView.id })
        if (res.code === 0 || res.code === 200) {
          message.success('更新成功')
          setViewModalVisible(false)
          fetchVirtualViews()
        }
      } else {
        const res: any = await virtualViewApi.save(viewData)
        if (res.code === 0 || res.code === 200) {
          message.success('创建成功')
          setViewModalVisible(false)
          fetchVirtualViews()
        }
      }
    } catch (e: any) {
      if (e.message) message.error(e.message)
    }
  }

  const handleDeleteView = async (viewId: number) => {
    try {
      const res: any = await virtualViewApi.delete(viewId)
      if (res.code === 0 || res.code === 200) {
        message.success('删除成功')
        fetchVirtualViews()
      }
    } catch (e: any) {
      message.error('删除失败')
    }
  }

  const handleQueryView = async (viewId: number) => {
    try {
      setLoading(true)
      setViewResultVisible(true)
      const res: any = await virtualViewApi.query(viewId)
      if (res.code === 0 || res.code === 200) {
        setViewResult(res.data || [])
      }
    } catch (e: any) {
      message.error('查询失败: ' + e.message)
    } finally {
      setLoading(false)
    }
  }

  const handleAddJoin = async () => {
    try {
      const values = await joinForm.validateFields()
      setJoins(prev => [...prev, values as VirtualViewJoin])
      joinForm.resetFields()
      message.success('已添加关联关系')
    } catch (e: any) {
      // validation error
    }
  }

  const handleRemoveJoin = (index: number) => {
    setJoins(prev => prev.filter((_, i) => i !== index))
  }

  const tableTreeData = tables.map(t => ({
    key: t,
    title: t,
    icon: <DatabaseOutlined />,
    isLeaf: true,
  }))

  const columnColumns = [
    { title: '字段名', dataIndex: 'columnName', key: 'columnName' },
    { title: '类型', dataIndex: 'typeName', key: 'typeName', width: 100 },
    { title: '长度', dataIndex: 'columnSize', key: 'columnSize', width: 80 },
    {
      title: '可空', dataIndex: 'nullable', key: 'nullable', width: 60,
      render: (v: boolean) => v ? <Tag color="blue">YES</Tag> : <Tag color="red">NO</Tag>,
    },
    { title: '备注', dataIndex: 'remarks', key: 'remarks', ellipsis: true },
    { title: '默认值', dataIndex: 'defaultValue', key: 'defaultValue', width: 100, ellipsis: true },
    {
      title: '操作', key: 'action', width: 80,
      render: (_: any, record: TableColumnInfo) => (
        <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => handleAddFieldMapping(record)}>
          映射
        </Button>
      ),
    },
  ]

  const mappingColumns = [
    { title: '源字段', dataIndex: 'sourceField', key: 'sourceField' },
    { title: '源类型', dataIndex: 'sourceType', key: 'sourceType', width: 100 },
    {
      title: '目标组件', dataIndex: 'targetComponent', key: 'targetComponent',
      render: (v: string, _: any, idx: number) => (
        <Input
          value={v}
          placeholder="如: Input, Select"
          onChange={e => {
            const newMappings = [...fieldMappings]
            newMappings[idx].targetComponent = e.target.value
            setFieldMappings(newMappings)
          }}
        />
      ),
    },
    {
      title: '目标属性', dataIndex: 'targetProperty', key: 'targetProperty',
      render: (v: string, _: any, idx: number) => (
        <Input
          value={v}
          placeholder="如: value, options"
          onChange={e => {
            const newMappings = [...fieldMappings]
            newMappings[idx].targetProperty = e.target.value
            setFieldMappings(newMappings)
          }}
        />
      ),
    },
    {
      title: '操作', key: 'action', width: 60,
      render: (_: any, __: any, idx: number) => (
        <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => handleRemoveFieldMapping(idx)} />
      ),
    },
  ]

  const joinColumns = [
    { title: '左数据源', dataIndex: 'leftDataSourceId', key: 'leftDs', render: (v: number) => dataSources.find(d => d.id === v)?.sourceName || v },
    { title: '左表', dataIndex: 'leftTable', key: 'leftTable' },
    { title: '左字段', dataIndex: 'leftColumn', key: 'leftCol' },
    { title: '关联类型', dataIndex: 'joinType', key: 'joinType', render: (v: string) => <Tag color="blue">{v}</Tag> },
    { title: '右数据源', dataIndex: 'rightDataSourceId', key: 'rightDs', render: (v: number) => dataSources.find(d => d.id === v)?.sourceName || v },
    { title: '右表', dataIndex: 'rightTable', key: 'rightTable' },
    { title: '右字段', dataIndex: 'rightColumn', key: 'rightCol' },
    {
      title: '操作', key: 'action', width: 60,
      render: (_: any, __: any, idx: number) => (
        <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => handleRemoveJoin(idx)} />
      ),
    },
  ]

  const viewColumns = [
    { title: '视图名称', dataIndex: 'viewName', key: 'viewName' },
    { title: '视图编码', dataIndex: 'viewCode', key: 'viewCode' },
    {
      title: '状态', dataIndex: 'status', key: 'status',
      render: (s: number) => <Badge status={s === 1 ? 'success' : 'default'} text={s === 1 ? '启用' : '禁用'} />,
    },
    {
      title: '操作', key: 'action',
      render: (_: any, record: VirtualView) => (
        <Space>
          <Button type="link" size="small" icon={<PlayCircleOutlined />} onClick={() => handleQueryView(record.id!)}>查询</Button>
          <Button type="link" size="small" icon={<SwapOutlined />} onClick={() => {
            setEditingView(record)
            viewForm.setFieldsValue(record)
            try { setJoins(record.joinConfig ? JSON.parse(record.joinConfig) : []) } catch { setJoins([]) }
            setViewModalVisible(true)
          }}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => handleDeleteView(record.id!)}>
            <Button type="link" danger size="small" icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  if (id) {
    return (
      <Layout style={{ height: '100vh', background: '#fff' }}>
        <div style={{ padding: '8px 16px', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/dataSource')}>返回</Button>
            <span style={{ fontSize: 16, fontWeight: 'bold' }}>{dataSource?.sourceName || '数据源设计器'}</span>
            <Tag color={dataSource?.dbType === 'rest_api' ? 'blue' : 'green'}>
              {dataSource?.sourceType === 'REST_API' ? 'REST API' : dataSource?.dbType || ''}
            </Tag>
          </Space>
          <Space>
            {dataSource?.dbType !== 'rest_api' && (
              <Button icon={<ReloadOutlined />} onClick={fetchTables}>刷新表</Button>
            )}
            {dataSource?.dbType === 'rest_api' && (
              <Button type="primary" icon={<PlayCircleOutlined />} onClick={handleCallRestApi} loading={loading}>调用API</Button>
            )}
          </Space>
        </div>
        <Layout>
          {dataSource?.dbType !== 'rest_api' ? (
            <>
              <Sider width={260} style={{ background: '#fafafa', borderRight: '1px solid #f0f0f0', overflow: 'auto' }}>
                <div style={{ padding: 12 }}>
                  <div style={{ fontWeight: 'bold', marginBottom: 8 }}>
                    <DatabaseOutlined /> 表列表
                  </div>
                  {tablesLoading ? <Spin /> : (
                    <Tree
                      treeData={tableTreeData}
                      onSelect={(keys) => {
                        if (keys.length > 0) fetchColumns(keys[0] as string)
                      }}
                      defaultExpandAll
                    />
                  )}
                </div>
              </Sider>
              <Content style={{ padding: 16, overflow: 'auto' }}>
                <Tabs defaultActiveKey="fields">
                  <TabPane tab="表字段" key="fields">
                    {selectedTable ? (
                      <Card title={`表: ${selectedTable}`} size="small">
                        <Table
                          columns={columnColumns}
                          dataSource={columns}
                          rowKey="columnName"
                          loading={columnsLoading}
                          size="small"
                          pagination={false}
                        />
                      </Card>
                    ) : (
                      <Empty description="请从左侧选择一个表" />
                    )}
                  </TabPane>
                  <TabPane tab={`字段映射 (${fieldMappings.length})`} key="mapping">
                    <Card size="small">
                      <Table
                        columns={mappingColumns}
                        dataSource={fieldMappings}
                        rowKey="sourceField"
                        size="small"
                        pagination={false}
                      />
                      {fieldMappings.length > 0 && (
                        <div style={{ marginTop: 16, textAlign: 'right' }}>
                          <Button type="primary" icon={<SaveOutlined />}>保存映射配置</Button>
                        </div>
                      )}
                    </Card>
                  </TabPane>
                </Tabs>
              </Content>
            </>
          ) : (
            <Content style={{ padding: 16, overflow: 'auto' }}>
              <Card title="REST API 调用结果" size="small">
                {restApiResult.length > 0 ? (
                  <Table
                    columns={Object.keys(restApiResult[0] || {}).map(key => ({
                      title: key,
                      dataIndex: key,
                      key,
                      ellipsis: true,
                    }))}
                    dataSource={restApiResult.map((row, idx) => ({ ...row, _key: idx }))}
                    rowKey="_key"
                    size="small"
                    scroll={{ x: true }}
                    pagination={{ pageSize: 10 }}
                  />
                ) : (
                  <Empty description="点击上方「调用API」按钮获取数据" />
                )}
              </Card>
            </Content>
          )}
        </Layout>
      </Layout>
    )
  }

  return (
    <Layout style={{ height: '100vh', background: '#fff' }}>
      <div style={{ padding: '8px 16px', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/dataSource')}>返回</Button>
          <span style={{ fontSize: 16, fontWeight: 'bold' }}>虚拟视图管理</span>
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => {
          setEditingView(null)
          viewForm.resetFields()
          setJoins([])
          setViewModalVisible(true)
        }}>新建虚拟视图</Button>
      </div>
      <Content style={{ padding: 16, overflow: 'auto' }}>
        <Card title="虚拟视图列表" extra={<Tag>跨数据源关联查询</Tag>}>
          <Table
            columns={viewColumns}
            dataSource={virtualViews}
            rowKey="id"
            size="small"
          />
        </Card>
      </Content>

      <Modal
        title={editingView ? '编辑虚拟视图' : '新建虚拟视图'}
        open={viewModalVisible}
        onOk={handleSaveVirtualView}
        onCancel={() => setViewModalVisible(false)}
        width={800}
        destroyOnClose
      >
        <Form form={viewForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="viewName" label="视图名称" rules={[{ required: true, message: '请输入' }]}>
                <Input placeholder="请输入视图名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="viewCode" label="视图编码" rules={[{ required: true, message: '请输入' }]}>
                <Input placeholder="请输入视图编码" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="viewSql" label="视图SQL(可选)">
            <Input.TextArea rows={3} placeholder="SELECT * FROM table" />
          </Form.Item>
        </Form>

        <Divider>跨数据源关联配置</Divider>

        <Table
          columns={joinColumns}
          dataSource={joins}
          rowKey={(_, idx) => String(idx)}
          size="small"
          pagination={false}
        />

        <div style={{ marginTop: 12, padding: 12, background: '#fafafa', borderRadius: 4 }}>
          <div style={{ fontWeight: 'bold', marginBottom: 8 }}>添加关联关系</div>
          <Form form={joinForm} layout="inline" size="small">
            <Form.Item name="leftDataSourceId" rules={[{ required: true }]}>
              <Select style={{ width: 120 }} placeholder="左数据源">
                {dataSources.map(ds => <Option key={ds.id} value={ds.id}>{ds.sourceName}</Option>)}
              </Select>
            </Form.Item>
            <Form.Item name="leftTable" rules={[{ required: true }]}>
              <Input style={{ width: 80 }} placeholder="左表" />
            </Form.Item>
            <Form.Item name="leftAlias">
              <Input style={{ width: 60 }} placeholder="左别名" />
            </Form.Item>
            <Form.Item name="leftColumn" rules={[{ required: true }]}>
              <Input style={{ width: 80 }} placeholder="左字段" />
            </Form.Item>
            <Form.Item name="joinType" rules={[{ required: true }]}>
              <Select style={{ width: 80 }} placeholder="关联类型">
                <Option value="INNER">INNER</Option>
                <Option value="LEFT">LEFT</Option>
              </Select>
            </Form.Item>
            <Form.Item name="rightDataSourceId" rules={[{ required: true }]}>
              <Select style={{ width: 120 }} placeholder="右数据源">
                {dataSources.map(ds => <Option key={ds.id} value={ds.id}>{ds.sourceName}</Option>)}
              </Select>
            </Form.Item>
            <Form.Item name="rightTable" rules={[{ required: true }]}>
              <Input style={{ width: 80 }} placeholder="右表" />
            </Form.Item>
            <Form.Item name="rightAlias">
              <Input style={{ width: 60 }} placeholder="右别名" />
            </Form.Item>
            <Form.Item name="rightColumn" rules={[{ required: true }]}>
              <Input style={{ width: 80 }} placeholder="右字段" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleAddJoin}>添加</Button>
            </Form.Item>
          </Form>
        </div>
      </Modal>

      <Modal
        title="查询结果"
        open={viewResultVisible}
        onCancel={() => setViewResultVisible(false)}
        width={900}
        footer={null}
      >
        {viewResult.length > 0 ? (
          <Table
            columns={Object.keys(viewResult[0] || {}).map(key => ({
              title: key,
              dataIndex: key,
              key,
              ellipsis: true,
            }))}
            dataSource={viewResult.map((row, idx) => ({ ...row, _key: idx }))}
            rowKey="_key"
            size="small"
            scroll={{ x: true }}
            pagination={{ pageSize: 10 }}
          />
        ) : (
          <Empty description="暂无数据" />
        )}
      </Modal>
    </Layout>
  )
}

export default DataSourceDesigner
