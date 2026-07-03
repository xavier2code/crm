import type { components } from '@/types/api'

import { request } from './client'

export type OpportunityPage = components['schemas']['PageOpportunityVO']
export type OpportunityVO = components['schemas']['OpportunityVO']
export type OpportunityDetailVO = components['schemas']['OpportunityDetailVO']
export type OpportunityRequest = components['schemas']['OpportunityRequest']
export type OpportunityApproveRequest = components['schemas']['OpportunityApproveRequest']

export function getOpportunities(params: {
  current?: number
  size?: number
  status?: number
  keyword?: string
}) {
  return request<OpportunityPage>({ url: '/opportunities', method: 'GET', params })
}

export function getOpportunity(id: number) {
  return request<OpportunityDetailVO>({ url: `/opportunities/${id}`, method: 'GET' })
}

export function createOpportunity(data: OpportunityRequest) {
  return request<OpportunityVO>({ url: '/opportunities', method: 'POST', data })
}

export function updateOpportunity(id: number, data: OpportunityRequest) {
  return request<OpportunityVO>({ url: `/opportunities/${id}`, method: 'PUT', data })
}

export function deleteOpportunity(id: number) {
  return request<void>({ url: `/opportunities/${id}`, method: 'DELETE' })
}

export function submitOpportunity(id: number) {
  return request<void>({ url: `/opportunities/${id}/submit`, method: 'POST' })
}

export function resubmitOpportunity(id: number) {
  return request<void>({ url: `/opportunities/${id}/resubmit`, method: 'POST' })
}

export function approveOpportunity(id: number, data: OpportunityApproveRequest) {
  return request<void>({ url: `/opportunities/${id}/approve`, method: 'POST', data })
}
