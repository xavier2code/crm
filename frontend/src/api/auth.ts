import type { CurrentUser, MenuItem } from '@/types/app'
import type { DictionaryItem } from '@/stores/dict'

import { request } from './client'

export interface LoginParams {
  username: string
  password: string
  captchaUuid: string
  captchaCode: string
}

export interface LoginResult {
  accessToken: string
  refreshToken?: string
  tokenType?: string
  userInfo: CurrentUser
  roles: string[]
  menuTree?: MenuItem[]
  permissionCodes: string[]
}

export interface CaptchaResult {
  image: string
  uuid: string
}

export const login = (params: LoginParams) =>
  request<LoginResult>({ method: 'POST', url: '/auth/login', data: params })

export const fetchCaptcha = () =>
  request<CaptchaResult>({ method: 'GET', url: '/auth/captcha' })

export const fetchCurrentUser = () => request<CurrentUser>({ method: 'GET', url: '/auth/currentUser' })

export const logout = () => request<void>({ method: 'POST', url: '/auth/logout' })

export function fetchDictionaries() {
  return request<Record<string, DictionaryItem[]>>({ url: '/admin/dictionaries', method: 'GET' })
}
