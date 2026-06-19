import request from '@/utils/request'

export interface UserPermissionVO {
  userId: number
  username: string
  roles: string[]
  permissions: string[]
  accessiblePageIds: number[]
  componentPermissions: Record<string, ComponentPermissionVO>
  fieldPermissions: Record<number, FieldPermissionVO>
  rowLevelSqlFilter: string
  modelRowLevelFilters: Record<number, string>
}

export interface ComponentPermissionVO {
  componentId: string
  visible: boolean
  disabled: boolean
}

export interface FieldPermissionVO {
  fieldId: number
  canView: boolean
  canEdit: boolean
}

export interface RowPermission {
  id?: number
  appId: number
  roleId: number
  modelId: number
  permissionName: string
  permissionCode: string
  expression: string
  conditionType: string
  priority: number
  status: number
  remark: string
}

export interface ExpressionParseResult {
  valid: boolean
  errorMessage: string
  sqlCondition: string
  userVariables: string[]
  dataVariables: string[]
}

export interface FieldPermission {
  id?: number
  appId: number
  roleId: number
  modelId: number
  fieldId: number
  canView: number
  canEdit: number
}

export interface PagePermission {
  id?: number
  appId: number
  roleId: number
  pageId: number
  canAccess: number
}

export interface ComponentPermission {
  id?: number
  appId: number
  roleId: number
  pageId: number
  componentId: string
  canVisible: number
  canDisabled: number
  visibleExpression?: string
}

export const permissionApi = {
  getCurrentUserPermissions: (appId: number) =>
    request.get<UserPermissionVO>('/permission/current', { params: { appId } }),

  getUserPermissions: (userId: number, appId: number) =>
    request.get<UserPermissionVO>(`/permission/user/${userId}`, { params: { appId } }),

  checkPagePermission: (appId: number, pageId: number) =>
    request.get<boolean>('/permission/check/page', { params: { appId, pageId } }),

  checkComponentPermissions: (appId: number, pageId: number, componentIds: string[]) =>
    request.post<Record<string, boolean>>('/permission/check/components', componentIds, {
      params: { appId, pageId },
    }),

  checkFieldPermission: (appId: number, modelId: number, fieldId: number, action: string) =>
    request.get<boolean>('/permission/check/field', { params: { appId, modelId, fieldId, action } }),

  getFieldPermissions: (appId: number, modelId: number) =>
    request.get<Record<number, FieldPermissionVO>>('/permission/fields', { params: { appId, modelId } }),

  getRowLevelFilter: (appId: number, modelId: number) =>
    request.get<string>('/permission/row/filter', { params: { appId, modelId } }),

  applyRowLevelFilter: (appId: number, modelId: number, dataList: any[]) =>
    request.post<any[]>('/permission/row/filterData', dataList, { params: { appId, modelId } }),

  createRowPermission: (data: RowPermission) =>
    request.post<number>('/permission/row', data),

  updateRowPermission: (id: number, data: RowPermission) =>
    request.put(`/permission/row/${id}`, data),

  deleteRowPermission: (id: number) =>
    request.delete(`/permission/row/${id}`),

  getRowPermissions: (appId?: number, roleId?: number, modelId?: number) =>
    request.get<RowPermission[]>('/permission/row/list', { params: { appId, roleId, modelId } }),

  validateExpression: (expression: string) =>
    request.post<ExpressionParseResult>('/permission/expression/validate', { expression }),

  evaluateExpression: (expression: string, data: Record<string, any>) =>
    request.post<boolean>('/permission/expression/evaluate', { expression, data }),

  assignUserAppRoles: (userId: number, appId: number, roleIds: number[]) =>
    request.post('/permission/user/roles', roleIds, { params: { userId, appId } }),

  saveFieldPermissions: (roleId: number, modelId: number, permissions: FieldPermission[]) =>
    request.post('/permission/fields/batch', permissions, { params: { roleId, modelId } }),

  savePagePermissions: (roleId: number, permissions: PagePermission[]) =>
    request.post('/permission/pages/batch', permissions, { params: { roleId } }),

  saveComponentPermissions: (roleId: number, pageId: number, permissions: ComponentPermission[]) =>
    request.post('/permission/components/batch', permissions, { params: { roleId, pageId } }),
}
