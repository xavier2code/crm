import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'

export interface ApiResult<T> {
  code: number
  success: boolean
  message: string
  data: T
}

const client: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('crm-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResult<unknown>
    if (!data.success) {
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const message = error.response?.data?.message || error.message || '网络错误'
    if (error.response?.status === 401) {
      localStorage.removeItem('crm-token')
      window.location.href = '/login'
    }
    return Promise.reject(new Error(message))
  }
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResult<T>>(config)
  return response.data.data
}

export default client
