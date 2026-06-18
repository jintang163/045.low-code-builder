import request from '@/utils/request'

export interface AppTemplate {
  id?: number
  templateName: string
  templateCode: string
  templateDesc?: string
  icon?: string
  category?: string
  tags?: string
  version?: string
  installCount?: number
  starCount?: number
  screenshot?: string
  templateData?: string
  templateType?: number
  publisher?: string
  publisherId?: number
  status?: number
  publishTime?: string
  createdBy?: number
  createdTime?: string
  updatedBy?: number
  updatedTime?: string
}

export interface TemplateData {
  dataSources?: any[]
  dataModels?: any[]
  pages?: any[]
  businessLogics?: any[]
  workflows?: any[]
  components?: any[]
}

export const templateApi = {
  page: (current: number, size: number, params?: {
    keyword?: string
    category?: string
    templateType?: number
  }) => {
    return request.get('/template/page', {
      params: { current, size, ...params }
    })
  },

  list: (params?: { category?: string; limit?: number }) => {
    return request.get('/template/list', { params })
  },

  detail: (id: number) => {
    return request.get(`/template/${id}`)
  },

  data: (id: number) => {
    return request.get(`/template/${id}/data`)
  },

  publish: (params: {
    appId: number
    templateName: string
    templateCode: string
    templateDesc?: string
    icon?: string
    category?: string
    tags?: string
    version?: string
    templateType?: number
  }) => {
    return request.post('/template/publish', null, { params })
  },

  install: (id: number, params: { appName: string; appCode: string; userId?: number }) => {
    return request.post(`/template/${id}/install`, null, { params })
  },

  export: (id: number) => {
    return request.get(`/template/${id}/export`, { responseType: 'blob' })
  },

  import: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/template/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  update: (template: AppTemplate) => {
    return request.put('/template', template)
  },

  delete: (id: number) => {
    return request.delete(`/template/${id}`)
  },

  stats: () => {
    return request.get('/template/stats')
  },

  initBuiltin: () => {
    return request.post('/template/init-builtin')
  },

  categories: () => {
    return request.get('/template/categories')
  }
}
