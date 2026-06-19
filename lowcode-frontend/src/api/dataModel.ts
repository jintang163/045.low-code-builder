import request from '@/utils/request'

export interface DataModel {
  id?: number
  appId: number
  modelName: string
  modelCode: string
  tableName: string
  description?: string
  dbType?: string
  fields?: ModelField[]
  indexes?: ModelIndex[]
  relations?: ModelRelation[]
  status?: string
  version?: string
}

export interface ModelField {
  id?: number
  modelId?: number
  fieldName: string
  fieldCode: string
  columnName: string
  fieldType: string
  length?: number
  precision?: number
  scale?: number
  isPrimaryKey?: number
  isAutoIncrement?: number
  isNullable?: number
  isUnique?: number
  defaultValue?: string
  comment?: string
  enumValues?: string
  relationModelId?: number
}

export interface ModelIndex {
  id?: number
  modelId?: number
  indexName: string
  indexType: string
  fields: string
  isUnique?: number
}

export interface ModelRelation {
  id?: number
  modelId?: number
  relationType: string
  targetModelId: number
  foreignKey: string
}

export const dataModelApi = {
  list: (appId: number) => request.get<DataModel[]>(`/model/list/${appId}`),
  page: (appId: number, current = 1, size = 10) =>
    request.get(`/model/page`, { params: { appId, current, size } }),
  get: (id: number) => request.get<DataModel>(`/model/${id}`),
  save: (data: DataModel) => request.post<DataModel>('/model', data),
  update: (data: DataModel) => request.put<DataModel>('/model', data),
  delete: (id: number) => request.delete(`/model/${id}`),
  publish: (id: number) => request.post(`/model/${id}/publish`),
  createSql: (id: number) => request.get<string>(`/model/${id}/createSql`),
  dropSql: (id: number) => request.get<string>(`/model/${id}/dropSql`),
  erDiagram: (appId: number) => request.get(`/model/erDiagram/${appId}`),
  importFromTable: (dataSourceId: number, tableName: string) =>
    request.post<DataModel>('/model/import', null, { params: { dataSourceId, tableName } }),
}

export interface DataSource {
  id?: number
  appId: number
  sourceName: string
  sourceCode: string
  sourceType?: string
  dbType: string
  host?: string
  port?: number
  database?: string
  username?: string
  password?: string
  driverClass?: string
  connectionParams?: string
  initialSize?: number
  minIdle?: number
  maxActive?: number
  maxWait?: number
  timeBetweenEvictionRunsMillis?: number
  minEvictableIdleTimeMillis?: number
  maxLifetime?: number
  connectionTimeout?: number
  validationQuery?: string
  testWhileIdle?: boolean
  testOnBorrow?: boolean
  testOnReturn?: boolean
  restApiUrl?: string
  restApiMethod?: string
  restApiHeaders?: string
  restApiBody?: string
  restApiAuthType?: string
  restApiAuthToken?: string
  connectTimeout?: number
  readTimeout?: number
  status?: number | string
  lastHealthCheckTime?: string
  healthCheckStatus?: string
}

export interface TableColumnInfo {
  columnName: string
  dataType: number
  typeName: string
  columnSize: number
  nullable: boolean
  remarks: string
  defaultValue: string
}

export interface PoolStatus {
  activeConnections: number
  idleConnections: number
  totalConnections: number
  threadsAwaitingConnection: number
  poolName: string
  closed: boolean
}

export interface VirtualView {
  id?: number
  appId: number
  viewName: string
  viewCode: string
  viewSql?: string
  viewConfig?: string
  joinConfig?: string
  status?: number
}

export interface VirtualViewJoin {
  leftDataSourceId: number
  leftTable: string
  leftAlias: string
  leftColumn: string
  rightDataSourceId: number
  rightTable: string
  rightAlias: string
  rightColumn: string
  joinType: string
}

export interface FieldMapping {
  id?: number
  appId: number
  dataSourceId: number
  pageId?: number
  componentId?: string
  sourceTable: string
  sourceField: string
  sourceType: string
  targetComponent: string
  targetComponentId: string
  targetProperty: string
  mappingType?: string
  sortOrder?: number
  description?: string
}

export const dataSourceApi = {
  testConnection: (data: DataSource) => request.post<boolean>('/datasource/testConnection', data),
  list: (appId: number) => request.get<DataSource[]>(`/datasource/list/${appId}`),
  page: (appId: number, current = 1, size = 10) =>
    request.get(`/datasource/page`, { params: { appId, current, size } }),
  get: (id: number) => request.get<DataSource>(`/datasource/${id}`),
  save: (data: DataSource) => request.post<DataSource>('/datasource', data),
  update: (data: DataSource) => request.put<DataSource>('/datasource', data),
  delete: (id: number) => request.delete(`/datasource/${id}`),
  getTables: (id: number) => request.get<string[]>(`/datasource/${id}/tables`),
  getTableColumns: (id: number, tableName: string) =>
    request.get<TableColumnInfo[]>(`/datasource/${id}/tables/${tableName}/columns`),
  getTablePrimaryKeys: (id: number, tableName: string) =>
    request.get(`/datasource/${id}/tables/${tableName}/primaryKeys`),
  callRestApi: (id: number, params?: Record<string, any>) =>
    request.post<any[]>(`/datasource/${id}/restApi`, params),
  getPoolStatus: (id: number) =>
    request.get<PoolStatus>(`/datasource/${id}/poolStatus`),
  healthCheck: (id: number) =>
    request.post<Record<string, any>>(`/datasource/${id}/healthCheck`),
  refreshPool: (id: number) =>
    request.post(`/datasource/${id}/refreshPool`),
}

export const virtualViewApi = {
  list: (appId: number) => request.get<VirtualView[]>(`/datasource/virtualView/list/${appId}`),
  get: (id: number) => request.get<VirtualView>(`/datasource/virtualView/${id}`),
  save: (data: VirtualView) => request.post<VirtualView>('/datasource/virtualView', data),
  update: (data: VirtualView) => request.put<VirtualView>('/datasource/virtualView', data),
  delete: (id: number) => request.delete(`/datasource/virtualView/${id}`),
  query: (viewId: number, limit = 1000) =>
    request.post<any[]>(`/datasource/virtualView/${viewId}/query`, null, { params: { limit } }),
}

export const fieldMappingApi = {
  save: (data: FieldMapping) => request.post<FieldMapping>('/datasource/fieldMapping', data),
  update: (data: FieldMapping) => request.put<FieldMapping>('/datasource/fieldMapping', data),
  delete: (id: number) => request.delete(`/datasource/fieldMapping/${id}`),
  getByDataSource: (dataSourceId: number) =>
    request.get<FieldMapping[]>(`/datasource/fieldMapping/dataSource/${dataSourceId}`),
  getByPage: (pageId: number) =>
    request.get<FieldMapping[]>(`/datasource/fieldMapping/page/${pageId}`),
  getByComponent: (pageId: number, componentId: string) =>
    request.get<FieldMapping[]>(`/datasource/fieldMapping/page/${pageId}/component/${componentId}`),
  saveBatchByPage: (pageId: number, mappings: FieldMapping[]) =>
    request.post<boolean>(`/datasource/fieldMapping/batch/page/${pageId}`, mappings),
  saveBatchByDataSource: (dataSourceId: number, pageId: number, mappings: FieldMapping[]) =>
    request.post<boolean>(`/datasource/fieldMapping/batch/dataSource/${dataSourceId}/page/${pageId}`, mappings),
}

export const migrationApi = {
  list: (appId: number, dataSourceId?: number) =>
    request.get('/migration/list', { params: { appId, dataSourceId } }),
  get: (id: number) => request.get(`/migration/${id}`),
  execute: (id: number) => request.post(`/migration/${id}/execute`),
}

export const codeGeneratorApi = {
  generateAll: (modelId: number, config?: any) => request.post(`/generator/code/generateAll/${modelId}`, config),
  generateEntity: (modelId: number, config?: any) => request.post(`/generator/code/generateEntity/${modelId}`, config),
  generateMapper: (modelId: number, config?: any) => request.post(`/generator/code/generateMapper/${modelId}`, config),
  generateService: (modelId: number, config?: any) => request.post(`/generator/code/generateService/${modelId}`, config),
  generateController: (modelId: number, config?: any) => request.post(`/generator/code/generateController/${modelId}`, config),
  generateApiJs: (modelId: number, config?: any) => request.post(`/generator/code/generateApiJs/${modelId}`, config),
}

export const fieldTypeOptions = [
  { label: '文本', value: 'VARCHAR' },
  { label: '长文本', value: 'TEXT' },
  { label: '整数', value: 'INT' },
  { label: '长整数', value: 'BIGINT' },
  { label: '小数', value: 'DECIMAL' },
  { label: '浮点数', value: 'DOUBLE' },
  { label: '日期', value: 'DATE' },
  { label: '日期时间', value: 'DATETIME' },
  { label: '时间', value: 'TIME' },
  { label: '布尔', value: 'BOOLEAN' },
  { label: '枚举', value: 'ENUM' },
  { label: '关联', value: 'RELATION' },
  { label: 'JSON', value: 'JSON' },
]
