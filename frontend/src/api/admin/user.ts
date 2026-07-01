import type { components } from '@/types/api'

import { request } from '../client'

type UserPage = components['schemas']['PageUserVO']
type UserVO = components['schemas']['UserVO']
type UserRequest = components['schemas']['UserRequest']

export function getUsers(params: { keyword?: string; current?: number; size?: number }) {
  return request<UserPage>({ url: '/admin/users', method: 'GET', params })
}

export function getUser(id: number) {
  return request<UserVO>({ url: `/admin/users/${id}`, method: 'GET' })
}

export function createUser(data: UserRequest) {
  return request<UserVO>({ url: '/admin/users', method: 'POST', data })
}

export function updateUser(id: number, data: UserRequest) {
  return request<UserVO>({ url: `/admin/users/${id}`, method: 'PUT', data })
}

export function deleteUser(id: number) {
  return request<void>({ url: `/admin/users/${id}`, method: 'DELETE' })
}

export function resetPassword(id: number) {
  return request<void>({ url: `/admin/users/${id}/reset-password`, method: 'POST' })
}
