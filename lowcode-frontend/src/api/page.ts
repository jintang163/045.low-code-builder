import request from '@/utils/request'

export interface PageInfo {
  id?: number
  appId: number
  pageName: string
  pageCode: string
  pageType?: string
  pagePath?: string
  layoutType?: string
  pageConfig?: string
  isHome?: number
  status?: number
  version?: string
  components?: PageComponent[]
}

export interface PageComponent {
  id?: number
  pageId?: number
  componentId: string
  componentName: string
  componentType: string
  componentVersion?: string
  parentId?: string
  sortOrder?: number
  positionX?: number
  positionY?: number
  width?: number
  height?: number
  propsConfig?: string
  styleConfig?: string
  eventConfig?: string
  dataSourceConfig?: string
  validationConfig?: string
  permissionConfig?: string
}

export interface ComponentLibrary {
  id?: number
  componentType: string
  componentName: string
  componentCategory: string
  componentIcon?: string
  description?: string
  defaultProps?: string
  defaultStyle?: string
  defaultEvents?: string
  status?: number
}

export const pageApi = {
  list: (appId: number) => request.get<PageInfo[]>(`/page/list/${appId}`),
  page: (appId: number, current = 1, size = 10) =>
    request.get(`/page/page`, { params: { appId, current, size } }),
  get: (id: number) => request.get<PageInfo>(`/page/${id}`),
  save: (data: PageInfo) => request.post<PageInfo>('/page', data),
  update: (data: PageInfo) => request.put<PageInfo>('/page', data),
  delete: (id: number) => request.delete(`/page/${id}`),
  publish: (id: number) => request.post<PageInfo>(`/page/${id}/publish`),
  generateCode: (id: number) => request.get<string>(`/page/${id}/generateCode`),
  preview: (id: number) => request.get(`/page/${id}/preview`),
}

export const componentApi = {
  tree: () => request.get<Record<string, ComponentLibrary[]>>('/component/tree'),
  list: (category?: string) =>
    request.get<ComponentLibrary[]>('/component/list', { params: { category } }),
  page: (category?: string, current = 1, size = 10) =>
    request.get(`/component/page`, { params: { category, current, size } }),
  get: (id: number) => request.get<ComponentLibrary>(`/component/${id}`),
  save: (data: ComponentLibrary) => request.post<ComponentLibrary>('/component', data),
  update: (data: ComponentLibrary) => request.put<ComponentLibrary>('/component', data),
  delete: (id: number) => request.delete(`/component/${id}`),
}

export interface CustomComponentVersion {
  id?: number
  componentId: number
  version: string
  changeLog?: string
  packagePath: string
  packageSize?: number
  mainFile?: string
  propSchema?: string
  eventSchema?: string
  exposedEvents?: string
  defaultProps?: string
  defaultStyle?: string
  isDeprecated?: number
  status?: number
  createdTime?: string
}

export interface CustomComponent {
  id?: number
  componentType: string
  componentName: string
  componentCategory: string
  icon?: string
  description?: string
  author?: string
  currentVersion: string
  isSystem?: number
  status?: number
  versions?: CustomComponentVersion[]
  currentVersionInfo?: CustomComponentVersion
  packageUrl?: string
}

export interface CustomComponentUploadDTO {
  componentType: string
  componentName: string
  componentCategory: string
  icon?: string
  description?: string
  author?: string
  version: string
  changeLog?: string
  file: File
}

export interface CustomComponentVersionUpdateDTO {
  componentId: number
  version: string
  changeLog?: string
  file: File
}

export const customComponentApi = {
  upload: (data: FormData) =>
    request.post<CustomComponent>('/custom-component/upload', data, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  updateVersion: (data: FormData) =>
    request.post<CustomComponent>('/custom-component/version', data, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  page: (current = 1, size = 10, category?: string, keyword?: string) =>
    request.get(`/custom-component/page`, { params: { current, size, category, keyword } }),
  tree: () => request.get<Record<string, CustomComponent[]>>('/custom-component/tree'),
  get: (id: number) => request.get<CustomComponent>(`/custom-component/${id}`),
  getByType: (componentType: string, version?: string) =>
    request.get<CustomComponent>(`/custom-component/type/${componentType}`, { params: { version } }),
  getBundleUrl: (id: number, version?: string) =>
    request.get<string>(`/custom-component/${id}/bundle-url`, { params: { version } }),
  download: (componentType: string, version?: string) =>
    request.get(`/custom-component/download/${componentType}`, {
      params: { version },
      responseType: 'blob',
    }),
  delete: (id: number) => request.delete(`/custom-component/${id}`),
  deprecateVersion: (versionId: number) =>
    request.put(`/custom-component/version/${versionId}/deprecate`),
}

export interface AiChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
}

export interface AiPageGenerateDTO {
  sessionId?: string
  userMessage: string
  appId?: number
  currentPageJson?: string
  history?: AiChatMessage[]
}

export interface AiPageGenerateVO {
  sessionId: string
  pageJson: string
  pageName: string
  replyMessage: string
  history: AiChatMessage[]
  success: boolean
  errorMessage?: string
}

export const aiPageApi = {
  generate: (data: AiPageGenerateDTO) =>
    request.post<AiPageGenerateVO>('/ai/page/generate', data),
  createSession: () =>
    request.post<string>('/ai/page/session'),
  clearSession: (sessionId: string) =>
    request.delete(`/ai/page/session/${sessionId}`),
}
