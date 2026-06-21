import React, { useState, useEffect } from 'react'
import {
  Card,
  Tabs,
  Form,
  Select,
  Input,
  Button,
  Space,
  Switch,
  List,
  Tag,
  Empty,
  message,
  Spin,
  Collapse,
  Tooltip,
} from 'antd'
import {
  DatabaseOutlined,
  TableOutlined,
  CodeOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  DeleteOutlined,
  ExperimentOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import { dataSourceApi, DataSource, TableColumnInfo, virtualViewApi, dataModelApi, DataModel, ModelField } from '@/api/dataModel'

const { TabPane } = Tabs
const { TextArea } = Input
const { Panel } = Collapse
const { Option } = Select

export interface DataSourcePanelProps {
  appId?: number
  value?: DataSourceConfig
  onChange?: (config: DataSourceConfig) => void
  dataFields?: DataField[]
  onFieldsChange?: (fields: DataField[]) => void
  theme?: 'light' | 'dark'
}

export interface DataField {
  fieldName: string
  fieldLabel: string
  fieldType: string
  isDimension?: boolean
  isMeasure?: boolean
  aggregation?: 'sum' | 'avg' | 'count' | 'max' | 'min' | 'none'
  format?: string
}

export interface DataSourceConfig {
  sourceType: 'sql' | 'model' | 'datasource'
  dataSourceId?: number
  modelId?: number
  virtualViewId?: number
  sqlQuery?: string
  tableName?: string
  fields: DataField[]
  filters?: FilterCondition[]
  refreshInterval?: number
}

export interface FilterCondition {
  field: string
  operator: string
  value: any
  label?: string
}

const DataSourcePanel: React.FC<DataSourcePanelProps> = ({
  appId,
  value,
  onChange,
  dataFields = [],
  onFieldsChange,
  theme = 'light',
}) => {
  const [sourceType, setSourceType] = useState<'sql' | 'model' | 'datasource'>(value?.sourceType || 'model')
  const [dataSources, setDataSources] = useState<DataSource[]>([])
  const [selectedDataSourceId, setSelectedDataSourceId] = useState<number | null>(value?.dataSourceId || null)
  const [tables, setTables] = useState<string[]>([])
  const [selectedTable, setSelectedTable] = useState<string | null>(value?.tableName || null)
  const [tableColumns, setTableColumns] = useState<TableColumnInfo[]>([])
  const [dataModels, setDataModels] = useState<DataModel[]>([])
  const [selectedModelId, setSelectedModelId] = useState<number | null>(value?.modelId || null)
  const [modelFields, setModelFields] = useState<ModelField[]>([])
  const [sqlQuery, setSqlQuery] = useState<string>(value?.sqlQuery || '')
  const [testLoading, setTestLoading] = useState(false)
  const [previewData, setPreviewData] = useState<any[]>([])
  const [fields, setFields] = useState<DataField[]>(value?.fields || [])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (appId) {
      loadDataSources()
      loadDataModels()
    }
  }, [appId])

  useEffect(() => {
    if (dataFields && dataFields.length > 0) {
      setFields(dataFields)
    }
  }, [dataFields])

  const loadDataSources = async () => {
    if (!appId) return
    try {
      setLoading(true)
      const res: any = await dataSourceApi.list(appId)
      if (res.code === 0 || res.code === 200) {
        setDataSources(res.data?.filter((ds: DataSource) => ds.status === 1) || [])
      }
    } catch (e) {
      console.error('加载数据源失败:', e)
    } finally {
      setLoading(false)
    }
  }

  const loadDataModels = async () => {
    if (!appId) return
    try {
      const res: any = await dataModelApi.list(appId)
      if (res.code === 0 || res.code === 200) {
        setDataModels(res.data || [])
      }
    } catch (e) {
      console.error('加载数据模型失败:', e)
    }
  }

  const handleDataSourceChange = async (dsId: number) => {
    setSelectedDataSourceId(dsId)
    setSelectedTable(null)
    setTables([])
    setTableColumns([])
    try {
      const res: any = await dataSourceApi.getTables(dsId)
      if (res.code === 0 || res.code === 200) {
        setTables(res.data || [])
      }
    } catch (e) {
      console.error('加载表列表失败:', e)
      message.error('加载表列表失败')
    }
  }

  const handleTableChange = async (tableName: string) => {
    setSelectedTable(tableName)
    if (selectedDataSourceId) {
      try {
        const res: any = await dataSourceApi.getTableColumns(selectedDataSourceId, tableName)
        if (res.code === 0 || res.code === 200) {
          const columns = res.data || []
          setTableColumns(columns)
          const defaultFields: DataField[] = columns.map((col: TableColumnInfo) => ({
            fieldName: col.columnName,
            fieldLabel: col.remarks || col.columnName,
            fieldType: col.typeName,
            isDimension: !isNumberType(col.typeName),
            isMeasure: isNumberType(col.typeName),
            aggregation: isNumberType(col.typeName) ? 'sum' : 'none',
          }))
          setFields(defaultFields)
          onFieldsChange?.(defaultFields)
        }
      } catch (e) {
        console.error('加载表字段失败:', e)
        message.error('加载表字段失败')
      }
    }
  }

  const handleModelChange = async (modelId: number) => {
    setSelectedModelId(modelId)
    try {
      const res: any = await dataModelApi.get(modelId)
      if (res.code === 0 || res.code === 200) {
        const model = res.data
        const mFields = model.fields || []
        setModelFields(mFields)
        const defaultFields: DataField[] = mFields.map((f: ModelField) => ({
          fieldName: f.fieldCode,
          fieldLabel: f.fieldName,
          fieldType: f.fieldType,
          isDimension: !isNumberFieldType(f.fieldType),
          isMeasure: isNumberFieldType(f.fieldType),
          aggregation: isNumberFieldType(f.fieldType) ? 'sum' : 'none',
        }))
        setFields(defaultFields)
        onFieldsChange?.(defaultFields)
      }
    } catch (e) {
      console.error('加载模型字段失败:', e)
      message.error('加载模型字段失败')
    }
  }

  const handleTestSql = async () => {
    if (!selectedDataSourceId || !sqlQuery) {
      message.warning('请选择数据源并输入SQL语句')
      return
    }
    try {
      setTestLoading(true)
      const { reportApi } = await import('@/api/report')
      const res: any = await reportApi.testSql(selectedDataSourceId, sqlQuery)
      if (res.code === 0 || res.code === 200) {
        const result = res.data
        setPreviewData(result.rows || [])
        if (result.columns && result.columns.length > 0) {
          const defaultFields: DataField[] = result.columns.map((col: any) => ({
            fieldName: col.fieldName,
            fieldLabel: col.fieldLabel || col.fieldName,
            fieldType: col.fieldType || 'VARCHAR',
            isDimension: true,
            isMeasure: false,
            aggregation: 'none',
          }))
          setFields(defaultFields)
          onFieldsChange?.(defaultFields)
        }
        message.success('SQL执行成功')
      }
    } catch (e: any) {
      message.error('SQL执行失败: ' + e.message)
    } finally {
      setTestLoading(false)
    }
  }

  const handleFieldToggle = (fieldName: string, type: 'dimension' | 'measure') => {
    const newFields = fields.map(f => {
      if (f.fieldName === fieldName) {
        if (type === 'dimension') {
          return { ...f, isDimension: !f.isDimension }
        } else {
          return { 
            ...f, 
            isMeasure: !f.isMeasure,
            aggregation: !f.isMeasure ? 'sum' : 'none',
          }
        }
      }
      return f
    })
    setFields(newFields)
    onFieldsChange?.(newFields)
  }

  const handleAggregationChange = (fieldName: string, aggregation: string) => {
    const newFields = fields.map(f => {
      if (f.fieldName === fieldName) {
        return { ...f, aggregation: aggregation as any }
      }
      return f
    })
    setFields(newFields)
    onFieldsChange?.(newFields)
  }

  const handleSourceTypeChange = (type: 'sql' | 'model' | 'datasource') => {
    setSourceType(type)
    updateConfig(type)
  }

  const updateConfig = (type: 'sql' | 'model' | 'datasource') => {
    const config: DataSourceConfig = {
      sourceType: type,
      dataSourceId: selectedDataSourceId || undefined,
      modelId: selectedModelId || undefined,
      tableName: selectedTable || undefined,
      sqlQuery: sqlQuery,
      fields: fields,
    }
    onChange?.(config)
  }

  const isNumberType = (typeName: string): boolean => {
    const numTypes = ['INT', 'BIGINT', 'DECIMAL', 'DOUBLE', 'FLOAT', 'NUMBER', 'NUMERIC', 'TINYINT', 'SMALLINT']
    return numTypes.some(t => typeName.toUpperCase().includes(t))
  }

  const isNumberFieldType = (fieldType: string): boolean => {
    const numTypes = ['INT', 'BIGINT', 'DECIMAL', 'DOUBLE', 'FLOAT', 'NUMBER']
    return numTypes.some(t => fieldType.toUpperCase().includes(t))
  }

  const panelStyle: React.CSSProperties = {
    background: theme === 'dark' ? '#0d1117' : '#fff',
    border: `1px solid ${theme === 'dark' ? '#ffffff11' : '#e8e8e8'}`,
    borderRadius: 8,
  }

  return (
    <div style={panelStyle}>
      <Card 
        size="small" 
        title={
          <Space>
            <DatabaseOutlined />
            <span>数据源配置</span>
          </Space>
        }
        styles={{ body: { padding: 12 } }}
      >
        <Tabs 
          activeKey={sourceType} 
          onChange={(key) => handleSourceTypeChange(key as any)}
          size="small"
        >
          <TabPane tab="数据模型" key="model" />
          <TabPane tab="数据源表" key="datasource" />
          <TabPane tab="SQL查询" key="sql" />
        </Tabs>

        {sourceType === 'model' && (
          <div style={{ marginTop: 12 }}>
            <Form.Item label="选择模型" style={{ marginBottom: 12 }}>
              <Select
                style={{ width: '100%' }}
                placeholder="请选择数据模型"
                value={selectedModelId || undefined}
                onChange={handleModelChange}
                showSearch
                optionFilterProp="children"
              >
                {dataModels.map(model => (
                  <Option key={model.id} value={model.id!}>
                    {model.modelName} ({model.modelCode})
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </div>
        )}

        {sourceType === 'datasource' && (
          <div style={{ marginTop: 12 }}>
            <Form.Item label="选择数据源" style={{ marginBottom: 12 }}>
              <Select
                style={{ width: '100%' }}
                placeholder="请选择数据源"
                value={selectedDataSourceId || undefined}
                onChange={handleDataSourceChange}
                loading={loading}
                showSearch
                optionFilterProp="children"
              >
                {dataSources.map(ds => (
                  <Option key={ds.id} value={ds.id!}>
                    {ds.sourceName} ({ds.dbType})
                  </Option>
                ))}
              </Select>
            </Form.Item>
            {selectedDataSourceId && (
              <Form.Item label="选择表" style={{ marginBottom: 12 }}>
                <Select
                  style={{ width: '100%' }}
                  placeholder="请选择数据表"
                  value={selectedTable || undefined}
                  onChange={handleTableChange}
                  showSearch
                  optionFilterProp="children"
                >
                  {tables.map(table => (
                    <Option key={table} value={table}>{table}</Option>
                  ))}
                </Select>
              </Form.Item>
            )}
          </div>
        )}

        {sourceType === 'sql' && (
          <div style={{ marginTop: 12 }}>
            <Form.Item label="选择数据源" style={{ marginBottom: 12 }}>
              <Select
                style={{ width: '100%' }}
                placeholder="请选择数据源"
                value={selectedDataSourceId || undefined}
                onChange={handleDataSourceChange}
                loading={loading}
                showSearch
                optionFilterProp="children"
              >
                {dataSources.map(ds => (
                  <Option key={ds.id} value={ds.id!}>
                    {ds.sourceName} ({ds.dbType})
                  </Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item label="SQL语句" style={{ marginBottom: 12 }}>
              <TextArea
                rows={6}
                placeholder="SELECT * FROM table_name"
                value={sqlQuery}
                onChange={(e) => setSqlQuery(e.target.value)}
                style={{ 
                  fontFamily: 'monospace',
                  background: theme === 'dark' ? '#161b2d' : '#fafafa',
                  color: theme === 'dark' ? '#ccc' : '#333',
                }}
              />
            </Form.Item>
            <Button 
              type="primary" 
              icon={<PlayCircleOutlined />}
              onClick={handleTestSql}
              loading={testLoading}
              size="small"
            >
              运行SQL
            </Button>
          </div>
        )}

        {fields.length > 0 && (
          <Collapse 
            defaultActiveKey={['fields']} 
            size="small" 
            style={{ marginTop: 12 }}
            ghost
          >
            <Panel 
              header={
                <Space>
                  <SettingOutlined />
                  <span>字段配置</span>
                  <Tag color="blue">{fields.length}个字段</Tag>
                </Space>
              } 
              key="fields"
            >
              <List
                size="small"
                dataSource={fields}
                renderItem={(field) => (
                  <List.Item
                    key={field.fieldName}
                    style={{ 
                      padding: '8px 0',
                      borderBottom: `1px solid ${theme === 'dark' ? '#ffffff11' : '#f0f0f0'}`,
                    }}
                  >
                    <div style={{ width: '100%' }}>
                      <div style={{ 
                        display: 'flex', 
                        justifyContent: 'space-between', 
                        alignItems: 'center',
                        marginBottom: 4,
                      }}>
                        <span style={{ fontWeight: 500, color: theme === 'dark' ? '#ccc' : '#333' }}>
                          {field.fieldLabel}
                        </span>
                        <Space size="small">
                          <Tooltip title="维度">
                            <Tag 
                              color={field.isDimension ? 'blue' : 'default'}
                              style={{ cursor: 'pointer' }}
                              onClick={() => handleFieldToggle(field.fieldName, 'dimension')}
                            >
                              维度
                            </Tag>
                          </Tooltip>
                          <Tooltip title="度量">
                            <Tag 
                              color={field.isMeasure ? 'green' : 'default'}
                              style={{ cursor: 'pointer' }}
                              onClick={() => handleFieldToggle(field.fieldName, 'measure')}
                            >
                              度量
                            </Tag>
                          </Tooltip>
                        </Space>
                      </div>
                      <div style={{ 
                        fontSize: 12, 
                        color: theme === 'dark' ? '#888' : '#999',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                      }}>
                        <span>{field.fieldName} ({field.fieldType})</span>
                        {field.isMeasure && (
                          <Select
                            size="small"
                            value={field.aggregation}
                            style={{ width: 80 }}
                            onChange={(val) => handleAggregationChange(field.fieldName, val)}
                          >
                            <Option value="sum">求和</Option>
                            <Option value="avg">平均</Option>
                            <Option value="count">计数</Option>
                            <Option value="max">最大</Option>
                            <Option value="min">最小</Option>
                            <Option value="none">原值</Option>
                          </Select>
                        )}
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            </Panel>
          </Collapse>
        )}

        {previewData.length > 0 && (
          <Collapse 
            defaultActiveKey={['preview']} 
            size="small" 
            style={{ marginTop: 12 }}
            ghost
          >
            <Panel 
              header={
                <Space>
                  <ExperimentOutlined />
                  <span>数据预览</span>
                  <Tag color="green">{previewData.length}条</Tag>
                </Space>
              } 
              key="preview"
            >
              <div 
                style={{ 
                  maxHeight: 200, 
                  overflow: 'auto',
                  fontSize: 12,
                  background: theme === 'dark' ? '#161b2d' : '#fafafa',
                  padding: 8,
                  borderRadius: 4,
                }}
              >
                {previewData.slice(0, 20).map((row, idx) => (
                  <div 
                    key={idx} 
                    style={{ 
                      padding: '4px 8px', 
                      borderBottom: idx < 19 ? `1px solid ${theme === 'dark' ? '#ffffff11' : '#f0f0f0'}` : 'none',
                      color: theme === 'dark' ? '#ccc' : '#666',
                      fontFamily: 'monospace',
                    }}
                  >
                    {JSON.stringify(row)}
                  </div>
                ))}
                {previewData.length > 20 && (
                  <div style={{ textAlign: 'center', padding: 8, color: '#999' }}>
                    共 {previewData.length} 条数据，仅显示前 20 条
                  </div>
                )}
              </div>
            </Panel>
          </Collapse>
        )}
      </Card>
    </div>
  )
}

export default DataSourcePanel
