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
  list: (appId: number) => request.get<DataModel[]>(`/dataModel/list/${appId}`),
  page: (appId: number, current = 1, size = 10) =>
    request.get(`/dataModel/page`, { params: { appId, current, size } }),
  get: (id: number) => request.get<DataModel>(`/dataModel/${id}`),
  save: (data: DataModel) => request.post<DataModel>('/dataModel', data),
  update: (data: DataModel) => request.put<DataModel>('/dataModel', data),
  delete: (id: number) => request.delete(`/dataModel/${id}`),
  publish: (id: number) => request.post(`/dataModel/${id}/publish`),
  createSql: (id: number) => request.get<string>(`/dataModel/${id}/createSql`),
  dropSql: (id: number) => request.get<string>(`/dataModel/${id}/dropSql`),
  erDiagram: (appId: number) => request.get(`/dataModel/erDiagram/${appId}`),
  importFromTable: (dataSourceId: number, tableName: string) =>
    request.post<DataModel>('/dataModel/import', null, { params: { dataSourceId, tableName } }),
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
