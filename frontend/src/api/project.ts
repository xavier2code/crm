import type { components } from '@/types/api'

import { request } from './client'

type ProjectPage = components['schemas']['PageProjectVO']
type ProjectVO = components['schemas']['ProjectVO']
type ProjectRequest = components['schemas']['ProjectRequest']
type BiddingNodeRequest = components['schemas']['BiddingNodeRequest']
type ContractNodeRequest = components['schemas']['ContractNodeRequest']

export function getProjects(params: { current?: number; size?: number; status?: number }) {
  return request<ProjectPage>({ url: '/projects', method: 'GET', params })
}

export function getProject(id: number) {
  return request<ProjectVO>({ url: `/projects/${id}`, method: 'GET' })
}

export function createProject(data: ProjectRequest) {
  return request<ProjectVO>({ url: '/projects', method: 'POST', data })
}

export function updateProject(id: number, data: ProjectRequest) {
  return request<ProjectVO>({ url: `/projects/${id}`, method: 'PUT', data })
}

export function updateProjectPNode(id: number, pNode: number) {
  return request<void>({ url: `/projects/${id}/p-node`, method: 'PUT', params: { pNode } })
}

export function updateProjectStage6(id: number, stage6: number) {
  return request<void>({ url: `/projects/${id}/stage-6`, method: 'PUT', params: { stage6 } })
}

export function updateProjectMilestone(id: number, data: unknown) {
  return request<void>({ url: `/projects/${id}/milestone`, method: 'PUT', data })
}

export function saveProjectBiddingNode(id: number, data: BiddingNodeRequest) {
  return request<void>({ url: `/projects/${id}/bidding-node`, method: 'PUT', data })
}

export function saveProjectContractNode(id: number, data: ContractNodeRequest) {
  return request<void>({ url: `/projects/${id}/contract-node`, method: 'PUT', data })
}
