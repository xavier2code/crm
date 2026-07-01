import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { message, Modal } from 'antd'

export interface ApiResult<T> {
  code: number
  success: boolean
  message: string
  data: T
  traceId?: string
  timestamp?: string
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
    if (data.code === 0) {
      return response
    }

    handleBusinessError(data)
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const messageText = error.response?.data?.message || error.message || '网络错误'
    const status = error.response?.status
    const data = error.response?.data

    if (status === 401) {
      localStorage.removeItem('crm-token')
      window.location.href = '/login'
    } else if (status === 403) {
      message.error('无权限访问该资源')
    } else if (data) {
      handleBusinessError(data)
    } else {
      message.error(messageText)
    }

    return Promise.reject(new Error(messageText))
  }
)

function handleBusinessError<T>(data: ApiResult<T>) {
  const code = data.code
  const msg = data.message || '请求失败'

  if (code === 2001 || code === 2003) {
    localStorage.removeItem('crm-token')
    window.location.href = '/login'
    return
  }

  if (code === 2007) {
    window.location.href = '/change-password'
    return
  }

  if (code === 5002) {
    Modal.warning({
      title: '数据已过期',
      content: '数据已被他人修改，请刷新后重试',
    })
    return
  }

  if ((code >= 4000 && code < 7000) || (code >= 1003 && code <= 1999)) {
    const hint = (data.data as { hint?: string })?.hint
    Modal.error({
      title: '操作失败',
      content: hint ? `${msg}（${hint}）` : msg,
    })
    return
  }

  message.error(msg)
}

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResult<T>>(config)
  return response.data.data
}

export default client
