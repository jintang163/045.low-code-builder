import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'antd'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code !== 0 && res.code !== 200) {
      message.error(res.message || '请求失败')
      if (res.code === 40100) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    console.error('Response error:', error)
    message.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export const request = {
  get: <T = any>(url: string, config?: AxiosRequestConfig): Promise<Result<T>> =>
    service.get(url, config),
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<Result<T>> =>
    service.post(url, data, config),
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<Result<T>> =>
    service.put(url, data, config),
  delete: <T = any>(url: string, config?: AxiosRequestConfig): Promise<Result<T>> =>
    service.delete(url, config),
}

export default request
