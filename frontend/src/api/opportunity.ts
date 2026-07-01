import type { components } from '@/types/api'

import { request } from './client'

type OpportunityPage = components['schemas']['PageOpportunityVO']
type OpportunityVO = components['schemas']['OpportunityVO']
type OpportunityRequest = components['schemas']['OpportunityRequest']
type ApproveRequest = components['schemas']['OpportunityApproveRequest']

export function getOpportunities(params: { current?: number; size?: number; status?: number }) {
  return request<OpportunityPage>({ url: '/opportunities', method: 'GET', params })
}

export function getOpportunity(id: number) {
  return request<OpportunityVO>({ url: `/opportunities/${id}`, method: 'GET' })
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

export function approveOpportunity(id: number, data: ApproveRequest) {
  return request<void>({ url: `/opportunities/${id}/approve`, method: 'POST', data })
}

export function rejectOpportunity(id: number, data: ApproveRequest) {
  return request<void>({ url: `/opportunities/${id}/reject`, method: 'POST', data })
}
