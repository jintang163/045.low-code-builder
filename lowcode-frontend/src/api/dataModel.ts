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
  dbType: string
  host: string
  port: number
  database: string
  username: string
  password?: string
  status?: string
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
