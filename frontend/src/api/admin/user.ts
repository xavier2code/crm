import { request } from '../client'

export interface UserVO {
  id?: number
  username?: string
  realName?: string
  phone?: string
  email?: string
  status?: number
  isInitialPassword?: number
  lastLoginAt?: string
  createdAt?: string
  updatedAt?: string
  roles?: string[]
}

export interface UserRequest {
  username: string
  realName: string
  phone?: string
  email?: string
  status?: number
  roleIds?: number[]
}

export interface UserPage {
  records?: UserVO[]
  total?: number
  size?: number
  current?: number
}

export interface UserStatusRequest {
  status: number
}

export function getUsers(params: {
  keyword?: string
  current?: number
  size?: number
}) {
  return request<UserPage>({ url: '/admin/users', method: 'GET', params })
}

export function getUser(id: number) {
  return request<UserVO>({ url: `/admin/users/${id}`, method: 'GET' })
}

export function createUser(data: UserRequest) {
  return request<void>({ url: '/admin/users', method: 'POST', data })
}

export function updateUser(id: number, data: UserRequest) {
  return request<void>({ url: `/admin/users/${id}`, method: 'PUT', data })
}

export function deleteUser(id: number) {
  return request<void>({ url: `/admin/users/${id}`, method: 'DELETE' })
}

export function resetPassword(id: number) {
  return request<void>({ url: `/admin/users/${id}/reset-password`, method: 'POST' })
}

export function updateUserStatus(id: number, data: UserStatusRequest) {
  return request<void>({ url: `/admin/users/${id}/status`, method: 'PUT', data })
}
