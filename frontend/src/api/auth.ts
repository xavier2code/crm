import type { CurrentUser } from '@/types/app'

import { request } from './client'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  userInfo: CurrentUser
  roles: string[]
  permissionCodes: string[]
}

export const login = (params: LoginParams) =>
  request<LoginResult>({ method: 'POST', url: '/auth/login', data: params })

export const fetchCurrentUser = () => request<CurrentUser>({ method: 'GET', url: '/auth/currentUser' })

export const logout = () => request<void>({ method: 'POST', url: '/auth/logout' })
