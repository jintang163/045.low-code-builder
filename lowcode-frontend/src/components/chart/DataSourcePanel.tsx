import React, { useState, useEffect, useCallback } from 'react'
import {
  Card,
  Tabs,
  Form,
  Select,
  Input,
  Button,
  Space,
  Table,
  Tag,
  Empty,
  message,
  Spin,
  Collapse,
  Tooltip,
  InputNumber,
  Popconfirm,
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
  FilterOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import { dataApi, DataSourceInfo, DataModelInfo, TableColumnInfo, SqlQueryResult } from '@/api/data'

const { TabPane } = Tabs
const { TextArea } = Input
const { Panel } = Collapse
const { Option } = Select

export type DataSourceType = 'dataModel' | 'dataSource' | 'sql'

export type FieldType = 'dimension' | 'measure'

export type AggregateType = 'none' | 'sum' | 'avg' | 'count' | 'max' | 'min'

export type FilterOperator = 'eq' | 'ne' | 'gt' | 'lt' | 'gte' | 'lte' | 'like' | 'in' | 'notIn'

export interface DataField {
  fieldName: string
  fieldLabel?: string
  fieldType: FieldType
  aggregate: AggregateType
  dataType?: string
}

export interface FilterCondition {
  field: string
  operator: FilterOperator
  value: any
  label?: string
}

export interface DataSourceConfig {
  dataSourceType: DataSourceType
  dataSourceId?: number
  modelId?: number
  tableName?: string
  sql?: string
  fields: DataField[]
  filters: FilterCondition[]
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  limit?: number
}

export interface DataSourcePanelProps {
  appId?: number
  value?: DataSourceConfig
  onChange?: (config: DataSourceConfig) => void
  readOnly?: boolean
  theme?: 'light' | 'dark'
}

const operatorOptions = [
  { label: '等于', value: 'eq' },
  { label: '不等于', value: 'ne' },
  { label: '大于', value: 'gt' },
  { label: '小于', value: 'lt' },
  { label: '大于等于', value: 'gte' },
  { label: '小于等于', value: 'lte' },
  { label: '包含', value: 'like' },
  { label: '在...中', value: 'in' },
  { label: '不在...中', value: 'notIn' },
]

const aggregateOptions = [
  { label: '原值', value: 'none' },
  { label: '求和', value: 'sum' },
  { label: '平均', value: 'avg' },
  { label: '计数', value: 'count' },
  { label: '最大', value: 'max' },
  { label: '最小', value: 'min' },
]

const isNumberType = (typeName: string): boolean => {
  const numTypes = ['INT', 'BIGINT', 'DECIMAL', 'DOUBLE', 'FLOAT', 'NUMBER', 'NUMERIC', 'TINYINT', 'SMALLINT']
  return numTypes.some(t => typeName.toUpperCase().includes(t))
}

const DataSourcePanel: React.FC<DataSourcePanelProps> = ({
  appId,
  value,
  onChange,
  readOnly = false,
  theme = 'light',
}) => {
  const [dataSourceType, setDataSourceType] = useState<DataSourceType>(value?.dataSourceType || 'dataModel')
  const [dataSources, setDataSources] = useState<DataSourceInfo[]>([])
  const [selectedDataSourceId, setSelectedDataSourceId] = useState<number | undefined>(value?.dataSourceId)
  const [dataModels, setDataModels] = useState<DataModelInfo[]>([])
  const [selectedModelId, setSelectedModelId] = useState<number | undefined>(value?.modelId)
  const [tables, setTables] = useState<string[]>([])
  const [selectedTable, setSelectedTable] = useState<string | undefined>(value?.tableName)
  const [tableColumns, setTableColumns] = useState<TableColumnInfo[]>([])
  const [sql, setSql] = useState<string>(value?.sql || '')
  const [fields, setFields] = useState<DataField[]>(value?.fields || [])
  const [filters, setFilters] = useState<FilterCondition[]>(value?.filters || [])
  const [sortBy, setSortBy] = useState<string | undefined>(value?.sortBy)
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc' | undefined>(value?.sortOrder)
  const [limit, setLimit] = useState<number>(value?.limit || 10)

  const [loading, setLoading] = useState(false)
  const [testLoading, setTestLoading] = useState(false)
  const [previewData, setPreviewData] = useState<SqlQueryResult | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)

  useEffect(() => {
    if (appId) {
      loadDataSources()
      loadDataModels()
    }
  }, [appId])

  useEffect(() => {
    if (value) {
      setDataSourceType(value.dataSourceType || 'dataModel')
      setSelectedDataSourceId(value.dataSourceId)
      setSelectedModelId(value.modelId)
      setSelectedTable(value.tableName)
      setSql(value.sql || '')
      setFields(value.fields || [])
      setFilters(value.filters || [])
      setSortBy(value.sortBy)
      setSortOrder(value.sortOrder)
      setLimit(value.limit || 10)
    }
  }, [value])

  const updateConfig = useCallback((updates: Partial<DataSourceConfig>) => {
    const config: DataSourceConfig = {
      dataSourceType,
      dataSourceId: selectedDataSourceId,
      modelId: selectedModelId,
      tableName: selectedTable,
      sql,
      fields,
      filters,
      sortBy,
      sortOrder,
      limit,
      ...updates,
    }
    onChange?.(config)
  }, [dataSourceType, selectedDataSourceId, selectedModelId, selectedTable, sql, fields, filters, sortBy, sortOrder, limit, onChange])

  const loadDataSources = async () => {
    if (!appId) return
    try {
      setLoading(true)
      const res: any = await dataApi.getDataSourceList(appId)
      if (res.code === 0 || res.code === 200) {
        setDataSources(res.data?.filter((ds: DataSourceInfo) => ds.status === 1) || [])
      }
    } catch (e) {
      console.error('加载数据源失败:', e)
      message.error('加载数据源失败')
    } finally {
      setLoading(false)
    }
  }

  const loadDataModels = async () => {
    if (!appId) return
    try {
      const res: any = await dataApi.getModelList(appId)
      if (res.code === 0 || res.code === 200) {
        setDataModels(res.data || [])
      }
    } catch (e) {
      console.error('加载数据模型失败:', e)
    }
  }

  const handleDataSourceChange = async (dsId: number) => {
    setSelectedDataSourceId(dsId)
    setSelectedTable(undefined)
    setTables([])
    setTableColumns([])
    setFields([])
    setPreviewData(null)
    try {
      const res: any = await dataApi.getTableList(dsId)
      if (res.code === 0 || res.code === 200) {
        setTables(res.data || [])
      }
    } catch (e) {
      console.error('加载表列表失败:', e)
      message.error('加载表列表失败')
    }
    updateConfig({ dataSourceId: dsId, tableName: undefined, fields: [] })
  }

  const handleTableChange = async (tableName: string) => {
    setSelectedTable(tableName)
    if (selectedDataSourceId) {
      try {
        const res: any = await dataApi.getTableColumns(selectedDataSourceId, tableName)
        if (res.code === 0 || res.code === 200) {
          const columns = res.data || []
          setTableColumns(columns)
          const defaultFields: DataField[] = columns.map((col: TableColumnInfo) => ({
            fieldName: col.columnName,
            fieldLabel: col.remarks || col.columnName,
            dataType: col.typeName,
            fieldType: isNumberType(col.typeName) ? 'measure' : 'dimension',
            aggregate: isNumberType(col.typeName) ? 'sum' : 'none',
          }))
          setFields(defaultFields)
          updateConfig({ tableName, fields: defaultFields })
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
      const res: any = await dataApi.get(modelId, 1)
      if (res.code === 0 || res.code === 200) {
        const modelData = res.data
        const modelFields = modelData?.fields || []
        const defaultFields: DataField[] = modelFields.map((f: any) => ({
          fieldName: f.fieldCode || f.columnName,
          fieldLabel: f.fieldName || f.columnName,
          dataType: f.fieldType,
          fieldType: isNumberType(f.fieldType) ? 'measure' : 'dimension',
          aggregate: isNumberType(f.fieldType) ? 'sum' : 'none',
        }))
        setFields(defaultFields)
        updateConfig({ modelId, fields: defaultFields })
      }
    } catch (e) {
      console.error('加载模型字段失败:', e)
      message.error('加载模型字段失败')
    }
  }

  const handleTestSql = async () => {
    if (!selectedDataSourceId || !sql) {
      message.warning('请选择数据源并输入SQL语句')
      return
    }
    try {
      setTestLoading(true)
      const res: any = await dataApi.testSql(selectedDataSourceId, sql)
      if (res.code === 0 || res.code === 200) {
        const result = res.data
        setPreviewData(result)
        if (result.columns && result.columns.length > 0) {
          const defaultFields: DataField[] = result.columns.map((col: any) => ({
            fieldName: col.fieldName,
            fieldLabel: col.fieldLabel || col.fieldName,
            dataType: col.fieldType || 'VARCHAR',
            fieldType: isNumberType(col.fieldType || 'VARCHAR') ? 'measure' : 'dimension',
            aggregate: isNumberType(col.fieldType || 'VARCHAR') ? 'sum' : 'none',
          }))
          setFields(defaultFields)
          updateConfig({ fields: defaultFields })
        }
        message.success('SQL执行成功')
      } else {
        message.error(res.message || 'SQL执行失败')
      }
    } catch (e: any) {
      message.error('SQL执行失败: ' + (e.message || '未知错误'))
    } finally {
      setTestLoading(false)
    }
  }

  const handlePreviewData = async () => {
    if (!fields.length) {
      message.warning('请先配置字段')
      return
    }

    try {
      setPreviewLoading(true)
      let result: SqlQueryResult | null = null

      if (dataSourceType === 'sql' && selectedDataSourceId && sql) {
        const res: any = await dataApi.executeSql(selectedDataSourceId, sql)
        if (res.code === 0 || res.code === 200) {
          result = res.data
        }
      } else if (dataSourceType === 'dataSource' && selectedDataSourceId && selectedTable) {
        const limitSql = `SELECT * FROM ${selectedTable} LIMIT ${limit}`
        const res: any = await dataApi.executeSql(selectedDataSourceId, limitSql)
        if (res.code === 0 || res.code === 200) {
          result = res.data
        }
      } else if (dataSourceType === 'dataModel' && selectedModelId) {
        const res: any = await dataApi.queryByModel(selectedModelId, {}, { current: 1, size: limit })
        if (res.code === 0 || res.code === 200) {
          result = res.data
        }
      }

      setPreviewData(result)
    } catch (e: any) {
      message.error('数据预览失败: ' + (e.message || '未知错误'))
    } finally {
      setPreviewLoading(false)
    }
  }

  const handleFieldTypeToggle = (fieldName: string, type: FieldType) => {
    const newFields = fields.map(f => {
      if (f.fieldName === fieldName) {
        const newType = f.fieldType === type ? (type === 'dimension' ? 'measure' : 'dimension') : type
        return {
          ...f,
          fieldType: newType,
          aggregate: newType === 'measure' ? 'sum' : 'none',
        }
      }
      return f
    })
    setFields(newFields)
    updateConfig({ fields: newFields })
  }

  const handleAggregateChange = (fieldName: string, aggregate: AggregateType) => {
    const newFields = fields.map(f => {
      if (f.fieldName === fieldName) {
        return { ...f, aggregate }
      }
      return f
    })
    setFields(newFields)
    updateConfig({ fields: newFields })
  }

  const handleAddFilter = () => {
    const firstField = fields[0]
    if (!firstField) {
      message.warning('请先配置字段')
      return
    }
    const newFilter: FilterCondition = {
      field: firstField.fieldName,
      operator: 'eq',
      value: '',
      label: firstField.fieldLabel || firstField.fieldName,
    }
    const newFilters = [...filters, newFilter]
    setFilters(newFilters)
    updateConfig({ filters: newFilters })
  }

  const handleFilterChange = (index: number, key: keyof FilterCondition, value: any) => {
    const newFilters = [...filters]
    if (key === 'field') {
      const fieldConfig = fields.find(f => f.fieldName === value)
      newFilters[index] = {
        ...newFilters[index],
        field: value,
        label: fieldConfig?.fieldLabel || value,
      }
    } else {
      newFilters[index] = { ...newFilters[index], [key]: value }
    }
    setFilters(newFilters)
    updateConfig({ filters: newFilters })
  }

  const handleRemoveFilter = (index: number) => {
    const newFilters = filters.filter((_, i) => i !== index)
    setFilters(newFilters)
    updateConfig({ filters: newFilters })
  }

  const handleSortByChange = (field: string | undefined) => {
    setSortBy(field)
    updateConfig({ sortBy: field })
  }

  const handleSortOrderChange = (order: 'asc' | 'desc' | undefined) => {
    setSortOrder(order)
    updateConfig({ sortOrder: order })
  }

  const handleLimitChange = (val: number | null) => {
    const newLimit = val || 10
    setLimit(newLimit)
    updateConfig({ limit: newLimit })
  }

  const handleTypeChange = (type: DataSourceType) => {
    setDataSourceType(type)
    setPreviewData(null)
    updateConfig({ dataSourceType: type })
  }

  const panelStyle: React.CSSProperties = {
    background: theme === 'dark' ? '#0d1117' : '#fff',
    border: `1px solid ${theme === 'dark' ? '#ffffff11' : '#e8e8e8'}`,
    borderRadius: 8,
  }

  const textColor = theme === 'dark' ? '#ccc' : '#333'
  const subTextColor = theme === 'dark' ? '#888' : '#999'
  const borderColor = theme === 'dark' ? '#ffffff11' : '#f0f0f0'

  const previewColumns = previewData?.columns?.map(col => ({
    title: col.fieldLabel || col.fieldName,
    dataIndex: col.fieldName,
    key: col.fieldName,
    width: 150,
    ellipsis: true,
  })) || []

  const fieldOptions = fields.map(f => ({
    label: f.fieldLabel || f.fieldName,
    value: f.fieldName,
  }))

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
          activeKey={dataSourceType}
          onChange={(key) => handleTypeChange(key as DataSourceType)}
          size="small"
          disabled={readOnly}
        >
          <TabPane
            tab={
              <Space size={4}>
                <TableOutlined />
                <span>数据模型</span>
              </Space>
            }
            key="dataModel"
          />
          <TabPane
            tab={
              <Space size={4}>
                <DatabaseOutlined />
                <span>数据源表</span>
              </Space>
            }
            key="dataSource"
          />
          <TabPane
            tab={
              <Space size={4}>
                <CodeOutlined />
                <span>SQL查询</span>
              </Space>
            }
            key="sql"
          />
        </Tabs>

        {dataSourceType === 'dataModel' && (
          <div style={{ marginTop: 12 }}>
            <Form.Item label="选择数据模型" style={{ marginBottom: 12 }}>
              <Select
                style={{ width: '100%' }}
                placeholder="请选择数据模型"
                value={selectedModelId}
                onChange={handleModelChange}
                showSearch
                optionFilterProp="children"
                loading={loading}
                disabled={readOnly}
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

        {dataSourceType === 'dataSource' && (
          <div style={{ marginTop: 12 }}>
            <Form.Item label="选择数据源" style={{ marginBottom: 12 }}>
              <Select
                style={{ width: '100%' }}
                placeholder="请选择数据源"
                value={selectedDataSourceId}
                onChange={handleDataSourceChange}
                loading={loading}
                showSearch
                optionFilterProp="children"
                disabled={readOnly}
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
                  value={selectedTable}
                  onChange={handleTableChange}
                  showSearch
                  optionFilterProp="children"
                  disabled={readOnly}
                >
                  {tables.map(table => (
                    <Option key={table} value={table}>{table}</Option>
                  ))}
                </Select>
              </Form.Item>
            )}
          </div>
        )}

        {dataSourceType === 'sql' && (
          <div style={{ marginTop: 12 }}>
            <Form.Item label="选择数据源" style={{ marginBottom: 12 }}>
              <Select
                style={{ width: '100%' }}
                placeholder="请选择数据源"
                value={selectedDataSourceId}
                onChange={handleDataSourceChange}
                loading={loading}
                showSearch
                optionFilterProp="children"
                disabled={readOnly}
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
                value={sql}
                onChange={(e) => {
                  setSql(e.target.value)
                  updateConfig({ sql: e.target.value })
                }}
                disabled={readOnly}
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
              disabled={readOnly}
            >
              测试SQL
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
              <div style={{ maxHeight: 300, overflowY: 'auto' }}>
                {fields.map((field) => (
                  <div
                    key={field.fieldName}
                    style={{
                      padding: '8px 0',
                      borderBottom: `1px solid ${borderColor}`,
                    }}
                  >
                    <div style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      marginBottom: 4,
                    }}>
                      <span style={{ fontWeight: 500, color: textColor }}>
                        {field.fieldLabel || field.fieldName}
                      </span>
                      <Space size="small">
                        <Tooltip title="维度">
                          <Tag
                            color={field.fieldType === 'dimension' ? 'blue' : 'default'}
                            style={{ cursor: readOnly ? 'not-allowed' : 'pointer' }}
                            onClick={() => !readOnly && handleFieldTypeToggle(field.fieldName, 'dimension')}
                          >
                            维度
                          </Tag>
                        </Tooltip>
                        <Tooltip title="度量">
                          <Tag
                            color={field.fieldType === 'measure' ? 'green' : 'default'}
                            style={{ cursor: readOnly ? 'not-allowed' : 'pointer' }}
                            onClick={() => !readOnly && handleFieldTypeToggle(field.fieldName, 'measure')}
                          >
                            度量
                          </Tag>
                        </Tooltip>
                      </Space>
                    </div>
                    <div style={{
                      fontSize: 12,
                      color: subTextColor,
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}>
                      <span>{field.fieldName} ({field.dataType})</span>
                      {field.fieldType === 'measure' && (
                        <Select
                          size="small"
                          value={field.aggregate}
                          style={{ width: 80 }}
                          onChange={(val) => handleAggregateChange(field.fieldName, val as AggregateType)}
                          disabled={readOnly}
                        >
                          {aggregateOptions.map(opt => (
                            <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                          ))}
                        </Select>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </Panel>
          </Collapse>
        )}

        {fields.length > 0 && (
          <Collapse
            size="small"
            style={{ marginTop: 12 }}
            ghost
          >
            <Panel
              header={
                <Space>
                  <FilterOutlined />
                  <span>筛选条件</span>
                  {filters.length > 0 && <Tag color="orange">{filters.length}个条件</Tag>}
                </Space>
              }
              key="filters"
            >
              {filters.length === 0 ? (
                <Empty description="暂无筛选条件" image={Empty.PRESENTED_IMAGE_SIMPLE} />
              ) : (
                <Space direction="vertical" size="small" style={{ width: '100%' }}>
                  {filters.map((filter, index) => (
                    <div key={index} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <Select
                        size="small"
                        style={{ flex: 1, minWidth: 100 }}
                        value={filter.field}
                        onChange={(val) => handleFilterChange(index, 'field', val)}
                        disabled={readOnly}
                      >
                        {fieldOptions.map(opt => (
                          <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                        ))}
                      </Select>
                      <Select
                        size="small"
                        style={{ width: 100 }}
                        value={filter.operator}
                        onChange={(val) => handleFilterChange(index, 'operator', val)}
                        disabled={readOnly}
                      >
                        {operatorOptions.map(opt => (
                          <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                        ))}
                      </Select>
                      <Input
                        size="small"
                        style={{ flex: 1, minWidth: 80 }}
                        placeholder="值"
                        value={filter.value}
                        onChange={(e) => handleFilterChange(index, 'value', e.target.value)}
                        disabled={readOnly}
                      />
                      {!readOnly && (
                        <Popconfirm title="确定删除此筛选条件？" onConfirm={() => handleRemoveFilter(index)}>
                          <Button
                            type="text"
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                          />
                        </Popconfirm>
                      )}
                    </div>
                  ))}
                </Space>
              )}
              {!readOnly && (
                <Button
                  type="dashed"
                  size="small"
                  icon={<PlusOutlined />}
                  onClick={handleAddFilter}
                  style={{ width: '100%', marginTop: 8 }}
                  disabled={fields.length === 0}
                >
                  添加筛选条件
                </Button>
              )}
            </Panel>
          </Collapse>
        )}

        {fields.length > 0 && (
          <Collapse
            size="small"
            style={{ marginTop: 12 }}
            ghost
          >
            <Panel
              header={
                <Space>
                  <SortAscendingOutlined />
                  <span>排序与分页</span>
                </Space>
              }
              key="sort"
            >
              <Form layout="inline" size="small">
                <Form.Item label="排序字段">
                  <Select
                    size="small"
                    style={{ width: 150 }}
                    placeholder="不排序"
                    allowClear
                    value={sortBy}
                    onChange={handleSortByChange}
                    disabled={readOnly}
                  >
                    {fieldOptions.map(opt => (
                      <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                    ))}
                  </Select>
                </Form.Item>
                <Form.Item label="排序方式">
                  <Space>
                    <Button
                      type={sortOrder === 'asc' ? 'primary' : 'default'}
                      size="small"
                      icon={<SortAscendingOutlined />}
                      onClick={() => handleSortOrderChange(sortOrder === 'asc' ? undefined : 'asc')}
                      disabled={readOnly || !sortBy}
                    >
                      升序
                    </Button>
                    <Button
                      type={sortOrder === 'desc' ? 'primary' : 'default'}
                      size="small"
                      icon={<SortDescendingOutlined />}
                      onClick={() => handleSortOrderChange(sortOrder === 'desc' ? undefined : 'desc')}
                      disabled={readOnly || !sortBy}
                    >
                      降序
                    </Button>
                  </Space>
                </Form.Item>
                <Form.Item label="预览条数">
                  <InputNumber
                    min={1}
                    max={100}
                    value={limit}
                    onChange={handleLimitChange}
                    size="small"
                    disabled={readOnly}
                  />
                </Form.Item>
              </Form>
            </Panel>
          </Collapse>
        )}

        {!readOnly && fields.length > 0 && (
          <div style={{ marginTop: 12 }}>
            <Button
              type="primary"
              icon={<EyeOutlined />}
              onClick={handlePreviewData}
              loading={previewLoading}
              block
            >
              预览数据
            </Button>
          </div>
        )}

        {previewData && (
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
                  <Tag color="green">{previewData.rows?.length || 0}条</Tag>
                </Space>
              }
              key="preview"
            >
              <Spin spinning={previewLoading}>
                {previewData.rows && previewData.rows.length > 0 ? (
                  <div
                    style={{
                      maxHeight: 300,
                      overflow: 'auto',
                      fontSize: 12,
                    }}
                  >
                    <Table
                      size="small"
                      dataSource={previewData.rows.slice(0, limit)}
                      columns={previewColumns}
                      pagination={false}
                      scroll={{ x: 'max-content' }}
                    />
                  </div>
                ) : (
                  <Empty description="暂无数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                )}
              </Spin>
            </Panel>
          </Collapse>
        )}
      </Card>
    </div>
  )
}

export default DataSourcePanel
