import request from '@/utils/request'
import { permissionApi } from './permission'

export interface DataQueryParams {
  current?: number
  size?: number
  orderBy?: string
  orderDir?: string
  conditions?: Record<string, any>
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
}

export { permissionApi }
