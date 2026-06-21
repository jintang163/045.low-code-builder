import request from '@/utils/request'
import { permissionApi } from './permission'

export interface DataQueryParams {
  current?: number
  size?: number
  orderBy?: string
  orderDir?: string
  conditions?: Record<string, any>
}

export interface SqlQueryParams {
  dataSourceId: number
  sql: string
  params?: Record<string, any>
}

export interface SqlQueryResult {
  columns: { fieldName: string; fieldLabel: string; fieldType: string }[]
  rows: Record<string, any>[]
  total?: number
}

export interface DataSourceInfo {
  id?: number
  appId: number
  sourceName: string
  sourceCode: string
  sourceType?: string
  dbType: string
  status?: number | string
}

export interface DataModelInfo {
  id?: number
  appId: number
  modelName: string
  modelCode: string
  tableName: string
  description?: string
  status?: string
}

export interface TableColumnInfo {
  columnName: string
  typeName: string
  remarks: string
}

export interface ModelFieldInfo {
  fieldName: string
  fieldCode: string
  fieldType: string
  comment?: string
}

export const dataApi = {
  list: (modelId: number, conditions?: Record<string, any>, orderBy?: string, orderDir?: string) =>
    request.post<any[]>(`/data/list/${modelId}`, conditions, { params: { orderBy, orderDir } }),

  page: (modelId: number, params: DataQueryParams) =>
    request.post<any>(`/data/page/${modelId}`, params.conditions, {
      params: {
        current: params.current || 1,
        size: params.size || 10,
        orderBy: params.orderBy,
        orderDir: params.orderDir,
      },
    }),

  get: (modelId: number, id: any) =>
    request.get<any>(`/data/${modelId}/${id}`),

  executeSql: (dataSourceId: number, sql: string, params?: Record<string, any>) =>
    request.post<SqlQueryResult>(`/data/executeSql`, { dataSourceId, sql, params }),

  testSql: (dataSourceId: number, sql: string) =>
    request.post<SqlQueryResult>(`/data/testSql`, { dataSourceId, sql }),

  queryByModel: (modelId: number, conditions?: Record<string, any>, params?: DataQueryParams) =>
    request.post<SqlQueryResult>(`/data/queryByModel/${modelId}`, conditions, { params }),

  getDataSourceList: (appId: number) =>
    request.get<DataSourceInfo[]>(`/datasource/list/${appId}`),

  getModelList: (appId: number) =>
    request.get<DataModelInfo[]>(`/model/list/${appId}`),

  getTableList: (dataSourceId: number) =>
    request.get<string[]>(`/datasource/${dataSourceId}/tables`),

  getTableColumns: (dataSourceId: number, tableName: string) =>
    request.get<TableColumnInfo[]>(`/datasource/${dataSourceId}/tables/${tableName}/columns`),

  getModelFields: (modelId: number) =>
    request.get<ModelFieldInfo[]>(`/model/${modelId}/fields`),
}

export { permissionApi }
