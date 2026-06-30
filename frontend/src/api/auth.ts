import { request } from './client'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  name: string
  roles: string[]
}

export interface CurrentUser {
  id: number
  username: string
  name: string
  email?: string
  phone?: string
  roles: string[]
}

export const login = (params: LoginParams) =>
  request<LoginResult>({ method: 'POST', url: '/auth/login', data: params })

export const fetchCurrentUser = () => request<CurrentUser>({ method: 'GET', url: '/auth/currentUser' })

export const logout = () => request<void>({ method: 'POST', url: '/auth/logout' })
