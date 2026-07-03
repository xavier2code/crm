import { request } from './client'

export interface UnitAssignmentVO {
  id: number
  unitId: number
  unitName?: string
  unitRegion?: string
  userId: number
  username?: string
  realName?: string
  /** BD = 大区总/BD 链路；CHANNEL_BD = 渠道负责人→渠道 BD 链路 */
  assignScope: 'BD' | 'CHANNEL_BD'
  channelId?: number
  channelName?: string
  assignedBy?: number
  assignedByName?: string
  assignedAt?: string
}

export interface UnitAssignmentPage {
  records: UnitAssignmentVO[]
  total: number
  current: number
  size: number
}

export interface UnitAssignRequest {
  userId: number
  assignScope: 'BD' | 'CHANNEL_BD'
  channelId?: number
}

export function listUnitAssignments(unitId: number) {
  return request<UnitAssignmentVO[]>({
    url: `/units/${unitId}/assignments`,
    method: 'GET',
  })
}

export function pageUnitAssignments(params: {
  unitId?: number
  userId?: number
  channelId?: number
  assignScope?: 'BD' | 'CHANNEL_BD'
  current?: number
  size?: number
} = {}) {
  return request<UnitAssignmentPage>({
    url: '/units/assignments',
    method: 'GET',
    params,
  })
}

export function assignUnit(unitId: number, data: UnitAssignRequest) {
  return request<number>({
    url: `/units/${unitId}/assignments`,
    method: 'POST',
    data,
  })
}

export function revokeUnitAssignment(assignmentId: number) {
  return request<void>({
    url: `/units/assignments/${assignmentId}`,
    method: 'DELETE',
  })
}
