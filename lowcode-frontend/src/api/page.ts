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
  tree: () => request.get<Record<string, ComponentLibrary[]>>('/componentLibrary/tree'),
  list: (category?: string) =>
    request.get<ComponentLibrary[]>('/componentLibrary/list', { params: { category } }),
  page: (category?: string, current = 1, size = 10) =>
    request.get(`/componentLibrary/page`, { params: { category, current, size } }),
  get: (id: number) => request.get<ComponentLibrary>(`/componentLibrary/${id}`),
  save: (data: ComponentLibrary) => request.post<ComponentLibrary>('/componentLibrary', data),
  update: (data: ComponentLibrary) => request.put<ComponentLibrary>('/componentLibrary', data),
  delete: (id: number) => request.delete(`/componentLibrary/${id}`),
}
