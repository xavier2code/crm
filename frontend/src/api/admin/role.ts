import type { components } from '@/types/api'

import { request } from '../client'

type RoleList = components['schemas']['RoleVO'][]
type RoleVO = components['schemas']['RoleVO']
type RoleRequest = components['schemas']['RoleRequest']

export function getRoles() {
  return request<RoleList>({ url: '/admin/roles', method: 'GET' })
}

export function getRole(id: number) {
  return request<RoleVO>({ url: `/admin/roles/${id}`, method: 'GET' })
}

export function createRole(data: RoleRequest) {
  return request<RoleVO>({ url: '/admin/roles', method: 'POST', data })
}

export function updateRole(id: number, data: RoleRequest) {
  return request<RoleVO>({ url: `/admin/roles/${id}`, method: 'PUT', data })
}

export function deleteRole(id: number) {
  return request<void>({ url: `/admin/roles/${id}`, method: 'DELETE' })
}
