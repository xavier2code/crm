import type { components } from '@/types/api'

import { request } from '../client'

type UnitPage = components['schemas']['PageUnitVO']
type UnitVO = components['schemas']['UnitVO']
type UnitRequest = components['schemas']['UnitRequest']
type UnitList = components['schemas']['UnitVO'][]

export function getUnits(params: { keyword?: string; region?: string; current?: number; size?: number }) {
  return request<UnitPage>({ url: '/admin/units', method: 'GET', params })
}

export function getAllUnits() {
  return request<UnitList>({ url: '/admin/units/all', method: 'GET' })
}

export function getUnit(id: number) {
  return request<UnitVO>({ url: `/admin/units/${id}`, method: 'GET' })
}

export function createUnit(data: UnitRequest) {
  return request<UnitVO>({ url: '/admin/units', method: 'POST', data })
}

export function updateUnit(id: number, data: UnitRequest) {
  return request<UnitVO>({ url: `/admin/units/${id}`, method: 'PUT', data })
}

export function deleteUnit(id: number) {
  return request<void>({ url: `/admin/units/${id}`, method: 'DELETE' })
}
