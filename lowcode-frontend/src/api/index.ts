import request from '@/utils/request'

export interface LoginDTO {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  roles: string[]
  permissions: string[]
}

export interface AppInfo {
  id?: number
  appName: string
  appCode: string
  appDesc?: string
  icon?: string
  status?: number
  version?: string
  deployConfig?: string
  createdBy?: number
  createdAt?: string
  createdTime?: string
  updatedBy?: number
  updatedTime?: string
}

export interface AppGenerateConfig {
  appId: number
  appName: string
  appCode: string
  packageName?: string
  frontendFramework?: string
  backendFramework?: string
  dbType?: string
  includeDocker?: boolean
  includeReadme?: boolean
}

export const authApi = {
  login: (data: LoginDTO) => request.post<LoginVO>('/auth/login', data),
  logout: () => request.post('/auth/logout'),
  validate: (token: string) => request.get<boolean>('/auth/validate', { params: { token } }),
  userInfo: () => request.get<UserInfo>('/auth/userInfo'),
}

export const userApi = {
  page: (pageNum = 1, pageSize = 10, username?: string, status?: number) =>
    request.get('/user/page', { params: { pageNum, pageSize, username, status } }),
  get: (id: number) => request.get(`/user/${id}`),
  create: (user: any, roleIds: number[]) =>
    request.post('/user', { user, roleIds }),
  update: (user: any, roleIds: number[]) =>
    request.put('/user', { user, roleIds }),
  delete: (id: number) => request.delete(`/user/${id}`),
  resetPassword: (id: number, newPassword: string) =>
    request.put('/user/resetPassword', { id, newPassword }),
  getUserRoles: (userId: number) => request.get<string[]>(`/user/${userId}/roles`),
  getUserPermissions: (userId: number) => request.get<string[]>(`/user/${userId}/permissions`),
}

export const appApi = {
  list: () => request.get<AppInfo[]>('/app/list'),
  page: (current = 1, size = 10, keyword?: string) =>
    request.get('/app/page', { params: { current, size, keyword } }),
  get: (id: number) => request.get<AppInfo>(`/app/${id}`),
  create: (data: AppInfo) => request.post<AppInfo>('/app', data),
  update: (data: AppInfo) => request.put<AppInfo>('/app', data),
  delete: (id: number) => request.delete(`/app/${id}`),
  publish: (id: number) => request.post<AppInfo>(`/app/${id}/publish`),
  stop: (id: number) => request.post<AppInfo>(`/app/${id}/stop`),
  generate: (config: AppGenerateConfig) => request.post('/app/generate', config),
  download: (appCode: string) => `/app/download/${appCode}`,
}

export const ossApi = {
  upload: (file: File, path?: string, storageType?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (path) formData.append('path', path)
    if (storageType) formData.append('storageType', storageType)
    return request.post('/oss/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  page: (pageNum = 1, pageSize = 10, fileName?: string, storageType?: string) =>
    request.get('/oss/page', { params: { pageNum, pageSize, fileName, storageType } }),
  get: (id: number) => request.get(`/oss/${id}`),
  delete: (id: number) => request.delete(`/oss/${id}`),
  getUrl: (id: number) => request.get<string>(`/oss/url/${id}`),
  download: (id: number) => `/oss/download/${id}`,
}

export interface RoleInfo {
  id?: number
  appId?: number
  roleName: string
  roleCode: string
  roleType: string
  roleSort?: number
  status?: number
  remark?: string
}

export const userRoleApi = {
  getRoles: (appId: number) =>
    request.get<RoleInfo[]>('/role/list', { params: { appId } }),
  getRole: (id: number) =>
    request.get<RoleInfo>(`/role/${id}`),
  createRole: (data: RoleInfo) =>
    request.post<number>('/role', data),
  updateRole: (id: number, data: RoleInfo) =>
    request.put(`/role/${id}`, data),
  deleteRole: (id: number) =>
    request.delete(`/role/${id}`),
}
