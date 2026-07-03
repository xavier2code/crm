import { request } from './client'

export interface FollowUpPage {
  records?: FollowUpVO[]
  total?: number
  size?: number
  current?: number
}

export interface FollowUpVO {
  id?: number
  customerId?: number
  customerName?: string
  projectId?: number
  projectName?: string
  opportunityId?: number
  currentStage?: string
  currentStageName?: string
  nextStage?: string
  nextStageName?: string
  stageFeedback?: string
  followUpDate?: string
  followUpMethod?: string
  followUpMethodName?: string
  contactId?: number
  contactName?: string
  content?: string
  nextPlan?: string
  createdBy?: number
  createdByName?: string
  createdAt?: string
}

export interface FollowUpRequest {
  customerId: number
  projectId?: number
  opportunityId?: number
  currentStage?: string
  nextStage?: string
  stageFeedback?: string
  followUpDate: string
  followUpMethod?: string
  contactId?: number
  content: string
  nextPlan?: string
}

export function pageFollowUps(params: {
  customerId?: number
  current?: number
  size?: number
} = {}) {
  return request<FollowUpPage>({
    url: '/follow-ups',
    method: 'GET',
    params: {
      customerId: params.customerId,
      current: params.current ?? 1,
      size: params.size ?? 10,
    },
  })
}

export function createFollowUp(data: FollowUpRequest) {
  return request<number>({ url: '/follow-ups', method: 'POST', data })
}

export function updateFollowUp(id: number, data: FollowUpRequest) {
  return request<void>({ url: `/follow-ups/${id}`, method: 'PUT', data })
}

export function deleteFollowUp(id: number) {
  return request<void>({ url: `/follow-ups/${id}`, method: 'DELETE' })
}
