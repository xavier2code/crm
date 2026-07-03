import { request } from './client'

/** 通知状态：1=未读 2=已读 */
export type NotificationStatus = 1 | 2

export interface Notification {
  id?: number
  userId?: number
  title?: string
  content?: string
  /** 类型：OPPORTUNITY_EXPIRED / OPPORTUNITY_WARNING / APPROVAL_TIMEOUT 等 */
  type?: string
  status?: NotificationStatus
  /** 关联业务 ID（如商机 ID） */
  relatedId?: number
  createdAt?: string
  readAt?: string
}

export interface NotificationPage {
  records?: Notification[]
  total?: number
  size?: number
  current?: number
}

/** 类型→中文标签 + 业务跳转路径 */
export const NOTIFICATION_TYPE_META: Record<
  string,
  { label: string; color: string; link?: (relatedId?: number) => string }
> = {
  OPPORTUNITY_EXPIRED: {
    label: '商机失效',
    color: 'red',
    link: (id) => (id ? `/opportunity/${id}` : '/opportunity'),
  },
  OPPORTUNITY_WARNING: {
    label: '商机即将失效',
    color: 'orange',
    link: (id) => (id ? `/opportunity/${id}` : '/opportunity'),
  },
  APPROVAL_TIMEOUT: {
    label: '审批超时',
    color: 'volcano',
    link: (id) => (id ? `/opportunity/${id}` : '/opportunity'),
  },
}

export function getUnreadCount() {
  return request<number>({ url: '/notifications/count/unread', method: 'GET' })
}

export function getUnreadNotifications() {
  return request<Notification[]>({
    url: '/notifications/unread',
    method: 'GET',
  })
}

export function pageNotifications(params: {
  status?: NotificationStatus
  current?: number
  size?: number
} = {}) {
  return request<NotificationPage>({
    url: '/notifications',
    method: 'GET',
    params: {
      status: params.status,
      current: params.current ?? 1,
      size: params.size ?? 10,
    },
  })
}

export function markAsRead(id: number) {
  return request<void>({ url: `/notifications/${id}/read`, method: 'POST' })
}

export function markAllAsRead() {
  return request<void>({ url: '/notifications/read-all', method: 'POST' })
}
