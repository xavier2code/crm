import type { components } from '@/types/api'

import { request } from './client'

export type DashboardVO = components['schemas']['DashboardVO']
export type ChannelDashboardVO = components['schemas']['ChannelDashboardVO']
export type ProjectStatisticsVO = components['schemas']['ProjectStatisticsVO']

/** 我的工作台 */
export function getMyDashboard() {
  return request<DashboardVO>({ url: '/dashboard/my', method: 'GET' })
}

/**
 * 渠道工作台
 *
 * 业务依据：CRM-渠道版-开发文档.md §9.1 渠道工作台
 *   渠道总览/业绩/成员/客户分布
 * 权限：仅 CHANNEL_HEAD / CYBD（后端 @PreAuthorize 校验）
 */
export function getChannelDashboard(channelId: number) {
  return request<ChannelDashboardVO>({
    url: `/dashboard/channel/${channelId}`,
    method: 'GET',
  })
}

/** 项目统计 */
export function getProjectStatistics() {
  return request<ProjectStatisticsVO>({
    url: '/dashboard/statistics/project',
    method: 'GET',
  })
}
