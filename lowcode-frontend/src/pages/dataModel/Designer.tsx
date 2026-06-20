import React, { useState, useEffect, useCallback } from 'react'
import {
  Layout,
  Button,
  Space,
  Table,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Tabs,
  Modal,
  message,
  Drawer,
  Tag,
  Divider,
} from 'antd'
import {
  SaveOutlined,
  PlayCircleOutlined,
  CodeOutlined,
  PlusOutlined,
  DeleteOutlined,
  ArrowLeftOutlined,
  ImportOutlined,
  SyncOutlined,
  HistoryOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { DndProvider, useDrag, useDrop } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'
import ReactECharts from 'echarts-for-react'
import { DataModel, ModelField, dataModelApi, fieldTypeOptions } from '@/api/dataModel'
import { useAppStore } from '@/store/appStore'
import { VersionHistoryPanel } from '@/components/dataModelVersion'

const { Header, Sider, Content } = Layout
const { Option } = Select

const fieldTypeColors: Record<string, string> = {
  VARCHAR: '#1677ff',
  TEXT: '#13c2c2',
  INT: '#52c41a',
  BIGINT: '#73d13d',
  DECIMAL: '#faad14',
  DOUBLE: '#fa8c16',
  DATE: '#eb2f96',
  DATETIME: '#f759ab',
  TIME: '#ff85c0',
  BOOLEAN: '#a0d911',
  ENUM: '#722ed1',
  RELATION: '#9254de',
  JSON: '#1890ff',
}

const FieldItem: React.FC<{
  field: ModelField
  index: number
  isSelected: boolean
  onSelect: () => void
  onDelete: () => void
}> = ({ field, index, isSelected, onSelect, onDelete }) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'FIELD',
    item: { index, field },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  }))

  const [, drop] = useDrop(() => ({
    accept: 'FIELD',
    drop: (item: { index: number }) => {
      console.log('Dropped', item)
    },
  }))

  return (
    <div
      ref={(node) => drag(drop(node))}
      style={{
        padding: '12px 16px',
        background: isSelected ? '#e6f7ff' : '#fff',
        border: `1px solid ${isSelected ? '#1677ff' : '#e8e8e8'}`,
        borderRadius: 4,
        marginBottom: 8,
        cursor: 'move',
        opacity: isDragging ? 0.5 : 1,
      }}
      onClick={onSelect}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 12, color: '#888' }}>{index + 1}</span>
          <Tag color={fieldTypeColors[field.fieldType] || '#1677ff'} style={{ margin: 0 }}>
            {field.fieldType}
          </Tag>
          <strong>{field.fieldName}</strong>
          <span style={{ color: '#888', fontSize: 12 }}>{field.fieldCode}</span>
          {field.isPrimaryKey === 1 && <Tag color="red">主键</Tag>}
          {field.isAutoIncrement === 1 && <Tag color="orange">自增</Tag>}
        </div>
        <Button
          type="text"
          danger
          size="small"
          icon={<DeleteOutlined />}
          onClick={(e) => { e.stopPropagation(); onDelete() }}
        />
      </div>
    </div>
  )
}

const DataModelDesigner: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [model, setModel] = useState<DataModel | null>(null)
  const [selectedFieldIndex, setSelectedFieldIndex] = useState<number | null>(null)
  const [form] = Form.useForm()
  const [fieldForm] = Form.useForm()
  const [sqlVisible, setSqlVisible] = useState(false)
  const [sqlContent, setSqlContent] = useState('')
  const [activeTab, setActiveTab] = useState('design')
  const [erOption, setErOption] = useState<any>({})
  const [importVisible, setImportVisible] = useState(false)
  const [versionDrawerVisible, setVersionDrawerVisible] = useState(false)
  const [dataSources] = useState([
    { id: 1, name: '主数据库' },
    { id: 2, name: '业务数据库' },
  ])
  const [tables, setTables] = useState<{ name: string; comment: string }[]>([])

  const loadModel = useCallback(async () => {
    if (!id || id === 'undefined') {
      setModel({
        appId: currentApp?.id || 1,
        modelName: '',
        modelCode: '',
        tableName: '',
        fields: [],
        indexes: [],
        relations: [],
      })
      return
    }
    try {
      const res = await dataModelApi.get(Number(id))
      setModel(res.data || null)
      form.setFieldsValue(res.data)
      if (res.data?.fields && res.data.fields.length > 0) {
        setSelectedFieldIndex(0)
        fieldForm.setFieldsValue(res.data.fields[0])
      }
    } catch (e) {
      console.error(e)
    }
  }, [id, currentApp])

  useEffect(() => {
    loadModel()
  }, [loadModel])

  const loadErDiagram = async () => {
    if (!currentApp) return
    try {
      const res = await dataModelApi.erDiagram(currentApp.id)
      const data = res.data as any
      if (data?.tables?.length) {
        const option = {
          title: { text: 'ER图', left: 'center' },
          tooltip: { show: true },
          series: [
            {
              type: 'graph',
              layout: 'force',
              animation: false,
              label: { show: true, position: 'inside', formatter: '{b}' },
              data: data.tables.map((t: any) => ({
                name: t.tableName,
                symbolSize: 80,
                itemStyle: { color: '#1677ff' },
              })),
              edges: data.relations?.map((r: any) => ({
                source: r.sourceTable,
                target: r.targetTable,
                label: { show: true, formatter: r.relationType, fontSize: 12 },
                lineStyle: { color: '#999' },
              })) || [],
              force: { repulsion: 500, edgeLength: 200 },
            },
          ],
        }
        setErOption(option)
      }
    } catch (e) {
      console.error(e)
    }
  }

  useEffect(() => {
    if (activeTab === 'er') {
      loadErDiagram()
    }
  }, [activeTab])

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      if (!model) {
        const data = { ...model, ...values }
        if (model.id) {
          await dataModelApi.update(data)
          message.success('保存成功')
        } else {
          const res = await dataModelApi.save(data)
          setModel(res.data)
          message.success('创建成功')
          navigate(`/dataModel/designer/${res.data.id}`, { replace: true })
        }
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handlePublish = async () => {
    if (!model?.id) return
    try {
      await dataModelApi.publish(model.id)
      message.success('发布成功')
      loadModel()
    } catch (e) {
      console.error(e)
    }
  }

  const handleViewSql = async () => {
    if (!model?.id) return
    try {
      const res = await dataModelApi.createSql(model.id)
      setSqlContent(res.data || '')
      setSqlVisible(true)
    } catch (e) {
      console.error(e)
    }
  }

  const handleAddField = () => {
    if (!model) {
      const newField: ModelField = {
        fieldName: '新字段',
        fieldCode: `field_${Date.now()}`,
        columnName: `column_${Date.now()}`,
        fieldType: 'VARCHAR',
        length: 255,
        isPrimaryKey: 0,
        isAutoIncrement: 0,
        isNullable: 1,
        isUnique: 0,
      }
      const newFields = [...(model.fields || []), newField]
      setModel({ ...model, fields: newFields })
      setSelectedFieldIndex(newFields.length - 1)
      fieldForm.setFieldsValue(newField)
    }
  }

  const handleDeleteField = (index: number) => {
    if (!model) return
    const newFields = [...(model.fields || [])]
    newFields.splice(index, 1)
    setModel({ ...model, fields: newFields })
    if (selectedFieldIndex === index) {
      setSelectedFieldIndex(newFields.length > 0 ? 0 : null)
      if (newFields.length > 0) {
        fieldForm.setFieldsValue(newFields[0])
      }
    }
  }

  const handleSelectField = (index: number) => {
    setSelectedFieldIndex(index)
    if (model?.fields?.[index]) {
      fieldForm.setFieldsValue(model.fields[index])
    }
  }

  const handleFieldFormSubmit = async () => {
    try {
      const values = await fieldForm.validateFields()
      if (!model || selectedFieldIndex === null) return
      const newFields = [...(model.fields || [])]
      newFields[selectedFieldIndex] = { ...newFields[selectedFieldIndex], ...values }
      setModel({ ...model, fields: newFields })
      message.success('字段更新成功')
    } catch (e) {
      console.error(e)
    }
  }

  const handleLoadTables = async (dataSourceId: number) => {
    setTables([
      { name: 'sys_user', comment: '用户表' },
      { name: 'sys_role', comment: '角色表' },
      { name: 'sys_menu', comment: '菜单表' },
    ])
  }

  const handleImportTable = async (tableName: string) => {
    if (!currentApp) return
    try {
      const res = await dataModelApi.importFromTable(1, tableName)
      setModel(res.data)
      form.setFieldsValue(res.data)
      if (res.data?.fields && res.data.fields.length > 0) {
        setSelectedFieldIndex(0)
        fieldForm.setFieldsValue(res.data.fields[0])
      }
      setImportVisible(false)
      message.success('导入成功')
    } catch (e) {
      console.error(e)
    }
  }

  return (
    <DndProvider backend={HTML5Backend}>
      <Layout style={{ height: '100%' }}>
        <Header
          style={{ background: '#fff', borderBottom: '1px solid #e8e8e8', padding: '0 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}
        >
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/dataModel')}>
              返回
            </Button>
            <Form form={form} layout="inline">
              <Form.Item name="modelName" label="模型名称" rules={[{ required: true }]}>
                <Input placeholder="请输入模型名称" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="modelCode" label="模型编码" rules={[{ required: true }]}>
                <Input placeholder="请输入模型编码" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="tableName" label="表名" rules={[{ required: true }]}>
                <Input placeholder="请输入表名" style={{ width: 180 }} />
              </Form.Item>
            </Form>
          </Space>
          <Space>
            <Button icon={<HistoryOutlined />} onClick={() => setVersionDrawerVisible(true)}>
              版本历史
            </Button>
            <Button icon={<ImportOutlined />} onClick={() => setImportVisible(true)}>
              逆向导入
            </Button>
            <Button icon={<SyncOutlined />} onClick={handleViewSql}>
              SQL预览
            </Button>
            <Button icon={<SaveOutlined />} type="primary" onClick={handleSave}>
              保存
            </Button>
            <Button icon={<PlayCircleOutlined />} type="primary" onClick={handlePublish}>
              发布
            </Button>
          </Space>
        </Header>
        <Layout>
          <Sider width={280} style={{ background: '#fff', borderRight: '1px solid #e8e8e8' }}>
            <div style={{ padding: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <h3 style={{ margin: 0 }}>字段列表</h3>
                <Button type="primary" size="small" icon={<PlusOutlined />} onClick={handleAddField}>
                  添加
                </Button>
              </div>
              <div style={{ maxHeight: 'calc(100vh - 200px)', overflow: 'auto' }}>
                {model?.fields?.map((field, index) => (
                  <FieldItem
                    key={index}
                    field={field}
                    index={index}
                    isSelected={selectedFieldIndex === index}
                    onSelect={() => handleSelectField(index)}
                    onDelete={() => handleDeleteField(index)}
                  />
                ))}
                {(!model?.fields || model.fields.length === 0) && (
                  <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                    暂无字段，请点击添加
                  </div>
                )}
              </div>
            </div>
          </Sider>
          <Content style={{ background: '#f5f5f5', padding: 0 }}>
            <Tabs
              activeKey={activeTab}
              onChange={setActiveTab}
              style={{ background: '#fff', height: '100%' }}
              tabBarStyle={{ margin: 0, padding: '0 16px', borderBottom: '1px solid #e8e8e8' }}
              items={[
                {
                  key: 'design',
                  label: '字段设计',
                  children: (
                    <div style={{ padding: 24, height: 'calc(100% - 48px)', overflow: 'auto' }}>
                      {selectedFieldIndex !== null && model?.fields?.[selectedFieldIndex] && (
                        <Card title={`编辑字段: ${model.fields[selectedFieldIndex].fieldName}`}>
                          <Form form={fieldForm} layout="vertical" style={{ maxWidth: 600 }} onFinish={handleFieldFormSubmit}>
                            <div style={{ display: 'flex', gap: 16 }}>
                              <Form.Item name="fieldName" label="字段名称" rules={[{ required: true }]} style={{ flex: 1 }}>
                                <Input placeholder="请输入字段名称" />
                              </Form.Item>
                              <Form.Item name="fieldCode" label="字段编码" rules={[{ required: true }]} style={{ flex: 1 }}>
                                <Input placeholder="请输入字段编码" />
                              </Form.Item>
                            </div>
                            <div style={{ display: 'flex', gap: 16 }}>
                              <Form.Item name="columnName" label="列名" rules={[{ required: true }]} style={{ flex: 1 }}>
                                <Input placeholder="请输入列名" />
                              </Form.Item>
                              <Form.Item name="fieldType" label="字段类型" rules={[{ required: true }]} style={{ flex: 1 }}>
                                <Select options={fieldTypeOptions} />
                              </Form.Item>
                            </div>
                            <div style={{ display: 'flex', gap: 16 }}>
                              <Form.Item name="length" label="长度" style={{ flex: 1 }}>
                                <InputNumber style={{ width: '100%' }} placeholder="请输入长度" />
                              </Form.Item>
                              <Form.Item name="precision" label="精度" style={{ flex: 1 }}>
                                <InputNumber style={{ width: '100%' }} placeholder="请输入精度" />
                              </Form.Item>
                              <Form.Item name="scale" label="小数位" style={{ flex: 1 }}>
                                <InputNumber style={{ width: '100%' }} placeholder="请输入小数位" />
                              </Form.Item>
                            </div>
                            <Divider />
                            <div style={{ display: 'flex', gap: 24 }}>
                              <Form.Item name="isPrimaryKey" label="主键" valuePropName="checked">
                                <Switch />
                              </Form.Item>
                              <Form.Item name="isAutoIncrement" label="自增" valuePropName="checked">
                                <Switch />
                              </Form.Item>
                              <Form.Item name="isNullable" label="可为空" valuePropName="checked">
                                <Switch />
                              </Form.Item>
                              <Form.Item name="isUnique" label="唯一" valuePropName="checked">
                                <Switch />
                              </Form.Item>
                            </div>
                            <Form.Item name="defaultValue" label="默认值">
                              <Input placeholder="请输入默认值" />
                            </Form.Item>
                            <Form.Item name="comment" label="备注">
                              <Input.TextArea rows={2} placeholder="请输入备注" />
                            </Form.Item>
                            <Form.Item>
                              <Button type="primary" htmlType="submit">
                                保存字段
                              </Button>
                            </Form.Item>
                          </Form>
                        </Card>
                      )}
                      {selectedFieldIndex === null && (
                        <div style={{ textAlign: 'center', padding: '60px 0', color: '#999' }}>
                          请选择或添加字段
                        </div>
                      )}
                    </div>
                  ),
                },
                {
                  key: 'er',
                  label: 'ER图',
                  children: (
                    <div style={{ padding: 24, height: 'calc(100% - 48px)' }}>
                      <ReactECharts option={erOption} style={{ height: '100%' }} />
                    </div>
                  ),
                },
                {
                  key: 'index',
                  label: '索引管理',
                  children: (
                    <div style={{ padding: 24 }}>
                      <Card title="索引列表" extra={<Button type="primary" size="small" icon={<PlusOutlined />}>添加索引</Button>}>
                        <Table
                          dataSource={model?.indexes || []}
                          columns={[
                            { title: '索引名称', dataIndex: 'indexName' },
                            { title: '索引类型', dataIndex: 'indexType' },
                            { title: '字段', dataIndex: 'fields' },
                            { title: '是否唯一', dataIndex: 'isUnique', render: (v: number) => (v === 1 ? '是' : '否') },
                            { title: '操作', render: () => <Button type="link" danger>删除</Button> },
                          ]}
                          rowKey="id"
                        />
                      </Card>
                    </div>
                  ),
                },
              ]}
            />
          </Content>
        </Layout>
      </Layout>

      <Modal title="建表SQL" open={sqlVisible} onCancel={() => setSqlVisible(false)} footer={null} width={800}>
        <pre style={{ background: '#f5f5f5', padding: 16, borderRadius: 4, maxHeight: 400, overflow: 'auto' }}>
          {sqlContent}
        </pre>
      </Modal>

      <Drawer title="逆向导入" open={importVisible} onClose={() => setImportVisible(false)} width={600}>
        <Form layout="vertical">
          <Form.Item label="选择数据源" rules={[{ required: true, message: '请选择数据源' }]}>
            <Select placeholder="请选择数据源" onChange={handleLoadTables}>
              {dataSources.map(ds => (
                <Option key={ds.id} value={ds.id}>{ds.name}</Option>
              ))}
            </Select>
          </Form.Item>
          <div>
            <h4>可选表：</h4>
            {tables.map(table => (
              <div
                key={table.name}
                style={{
                  padding: 12,
                  border: '1px solid #e8e8e8',
                  borderRadius: 4,
                  marginBottom: 8,
                  cursor: 'pointer',
                }}
                onClick={() => handleImportTable(table.name)}
              >
                <strong>{table.name}</strong>
                <span style={{ color: '#888', marginLeft: 8 }}>{table.comment}</span>
              </div>
            ))}
          </div>
        </Form>
      </Drawer>

      <Drawer
        title="版本历史"
        placement="right"
        width={640}
        open={versionDrawerVisible}
        onClose={() => setVersionDrawerVisible(false)}
        destroyOnClose
      >
        {model?.id && (
          <VersionHistoryPanel
            modelId={model.id}
            onRollbackSuccess={(rollbackModel) => {
              setModel(rollbackModel)
              form.setFieldsValue(rollbackModel)
              if (rollbackModel.fields && rollbackModel.fields.length > 0) {
                setSelectedFieldIndex(0)
                fieldForm.setFieldsValue(rollbackModel.fields[0])
              }
            }}
          />
        )}
      </Drawer>
    </DndProvider>
  )
}

export default DataModelDesigner
