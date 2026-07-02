import type { components } from '@/types/api'

import { request } from './client'

type ProjectPage = components['schemas']['PageProjectVO']
type ProjectDetailVO = components['schemas']['ProjectDetailVO']
type ProjectRequest = components['schemas']['ProjectRequest']
type BiddingNodeRequest = components['schemas']['BiddingNodeRequest']
type ContractNodeRequest = components['schemas']['ContractNodeRequest']
type MilestoneVO = components['schemas']['MilestoneVO']
type PaymentNodeVO = components['schemas']['PaymentNodeVO']
type ProjectScoreRequest = components['schemas']['ProjectScoreRequest']

export function getProjects(params: { current?: number; size?: number; status?: number }) {
  return request<ProjectPage>({ url: '/projects', method: 'GET', params })
}

export function getProject(id: number) {
  return request<ProjectDetailVO>({ url: `/projects/${id}`, method: 'GET' })
}

export function getProjectProcess(id: number) {
  return request<ProjectDetailVO>({ url: `/projects/${id}/process`, method: 'GET' })
}

export function createProject(data: ProjectRequest) {
  return request<number>({ url: '/projects', method: 'POST', data })
}

export function createProjectFromOpportunity(oppId: number, data: ProjectRequest) {
  return request<number>({ url: `/projects/from-opportunity/${oppId}`, method: 'POST', data })
}

export function updateProject(id: number, data: ProjectRequest) {
  return request<void>({ url: `/projects/${id}`, method: 'PUT', data })
}

export function transitionProjectStatus(id: number, status: number, reason: string) {
  return request<void>({ url: `/projects/${id}/status`, method: 'PUT', params: { status, reason } })
}

export function updateProjectPNode(id: number, pNode: number) {
  return request<void>({ url: `/projects/${id}/p-node`, method: 'PUT', params: { pNode } })
}

export function updateProjectStage6(id: number, stage6: string) {
  return request<void>({ url: `/projects/${id}/stage-6`, method: 'PUT', params: { stage6 } })
}

export function updateProjectMilestone(id: number, data: MilestoneVO) {
  return request<void>({ url: `/projects/${id}/milestone`, method: 'PUT', data })
}

export function saveProjectBiddingNode(id: number, data: BiddingNodeRequest) {
  return request<void>({ url: `/projects/${id}/bidding-node`, method: 'PUT', data })
}

export function saveProjectContractNode(id: number, data: ContractNodeRequest) {
  return request<void>({ url: `/projects/${id}/contract-node`, method: 'PUT', data })
}

export function addProjectPaymentNode(id: number, data: PaymentNodeVO) {
  return request<number>({ url: `/projects/${id}/payment-nodes`, method: 'POST', data })
}

export function updateProjectPaymentNode(id: number, data: PaymentNodeVO) {
  return request<void>({ url: `/projects/payment-nodes/${id}`, method: 'PUT', data })
}

export function deleteProjectPaymentNode(id: number) {
  return request<void>({ url: `/projects/payment-nodes/${id}`, method: 'DELETE' })
}

export function submitProjectScore(data: ProjectScoreRequest) {
  return request<void>({ url: '/projects/scores', method: 'POST', data })
}

export function getProjectScoreDimensions() {
  return request<Record<string, { name: string; weight: number }>>({
    url: '/projects/score-dimensions',
    method: 'GET',
  })
}
