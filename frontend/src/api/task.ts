import { request } from './client'

export type TaskStatus = 1 | 2 | 3

export const TASK_STATUS: Record<TaskStatus, { label: string; color: string }> = {
  1: { label: '待完成', color: 'blue' },
  2: { label: '已完成', color: 'green' },
  3: { label: '已关闭', color: 'default' },
}

export interface TaskPage {
  records?: TaskVO[]
  total?: number
  size?: number
  current?: number
}

export interface TaskVO {
  id?: number
  ownerUserId?: number
  ownerUserName?: string
  customerId?: number
  customerName?: string
  followUpId?: number
  planStage?: string
  planStageName?: string
  planDate?: string
  status?: TaskStatus
  statusName?: string
  closeReason?: string
  createdAt?: string
}

export function pageTasks(params: {
  status?: TaskStatus
  current?: number
  size?: number
} = {}) {
  return request<TaskPage>({
    url: '/tasks',
    method: 'GET',
    params: {
      status: params.status,
      current: params.current ?? 1,
      size: params.size ?? 10,
    },
  })
}

export function pageTodayTasks(params: { current?: number; size?: number } = {}) {
  return request<TaskPage>({
    url: '/tasks/today',
    method: 'GET',
    params: {
      current: params.current ?? 1,
      size: params.size ?? 10,
    },
  })
}

export function completeTask(id: number) {
  return request<void>({ url: `/tasks/${id}/complete`, method: 'POST' })
}

export function closeTask(id: number, reason: string) {
  return request<void>({
    url: `/tasks/${id}/close`,
    method: 'POST',
    params: { reason },
  })
}
