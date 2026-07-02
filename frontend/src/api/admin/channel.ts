import { request } from '../client'

/** 渠道视图（后端已拼装 heads/bds） */
export interface ChannelVO {
  id: number
  name: string
  region?: string
  status?: number
  heads?: UserMini[]
  bds?: UserMini[]
  createdAt?: string
  updatedAt?: string
}

export interface UserMini {
  id: number
  username?: string
  realName?: string
  status?: number
}

export interface ChannelPage {
  records: ChannelVO[]
  total: number
  size: number
  current: number
  pages?: number
}

export interface ChannelRequest {
  id?: number
  name: string
  region?: string
  status?: number
}

export interface ChannelAssignmentVO {
  channelId: number
  channelName?: string
  userId: number
  username?: string
  realName?: string
  /** 1=渠道负责人 2=渠道 BD */
  assignType: number
  assignedBy?: number
  assignedByName?: string
  assignedAt?: string
}

export interface ChannelAssignRequest {
  userId: number
  assignType: number
}

export function pageChannels(params: {
  keyword?: string
  region?: string
  current?: number
  size?: number
} = {}) {
  return request<ChannelPage>({
    url: '/admin/channels',
    method: 'GET',
    params: {
      keyword: params.keyword,
      region: params.region,
      current: params.current ?? 1,
      size: params.size ?? 10,
    },
  })
}

export function listAllChannels() {
  return request<ChannelVO[]>({ url: '/admin/channels/all', method: 'GET' })
}

export function getChannel(id: number) {
  return request<ChannelVO>({ url: `/admin/channels/${id}`, method: 'GET' })
}

export function createChannel(data: ChannelRequest) {
  return request<number>({ url: '/admin/channels', method: 'POST', data })
}

export function updateChannel(id: number, data: ChannelRequest) {
  return request<void>({ url: `/admin/channels/${id}`, method: 'PUT', data })
}

export function deleteChannel(id: number) {
  return request<void>({ url: `/admin/channels/${id}`, method: 'DELETE' })
}

/* ========== 用户-渠道分配 ========== */

export function listChannelAssignments(
  channelId: number,
  params: { assignType?: number } = {},
) {
  return request<ChannelAssignmentVO[]>({
    url: `/admin/channels/${channelId}/assignments`,
    method: 'GET',
    params,
  })
}

export function assignChannel(channelId: number, data: ChannelAssignRequest) {
  return request<void>({
    url: `/admin/channels/${channelId}/assignments`,
    method: 'POST',
    data,
  })
}

export function revokeChannelAssignment(
  channelId: number,
  userId: number,
  assignType: number,
) {
  return request<void>({
    url: `/admin/channels/${channelId}/assignments/${userId}`,
    method: 'DELETE',
    params: { assignType },
  })
}

export function listAvailableUsers() {
  return request<UserMini[]>({
    url: `/admin/channels/0/assignments/available-users`,
    method: 'GET',
  })
}
