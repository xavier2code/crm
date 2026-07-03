import { request } from '../client'

import type { DataScopeDimensionCode } from './dataPermission'

export interface RoleVO {
  id?: number
  code?: string
  name?: string
  isBuiltin?: number
  dataScopeType?: DataScopeDimensionCode
  menuIds?: number[]
  operationCodes?: string[]
}

export interface RoleRequest {
  code: string
  name: string
  dataScopeType?: DataScopeDimensionCode
  menuIds?: number[]
  operationCodes?: string[]
}

export function getRoles() {
  return request<RoleVO[]>({ url: '/admin/roles', method: 'GET' })
}

export function getRole(id: number) {
  return request<RoleVO>({ url: `/admin/roles/${id}`, method: 'GET' })
}

export function createRole(data: RoleRequest) {
  return request<void>({ url: '/admin/roles', method: 'POST', data })
}

export function updateRole(id: number, data: RoleRequest) {
  return request<void>({ url: `/admin/roles/${id}`, method: 'PUT', data })
}

export function deleteRole(id: number) {
  return request<void>({ url: `/admin/roles/${id}`, method: 'DELETE' })
}
