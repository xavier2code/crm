import { useQuery } from '@tanstack/react-query'

import { getChannelDashboard, getMyDashboard, getProjectStatistics } from '@/api/dashboard'

/** 我的工作台 */
export function useDashboard() {
  return useQuery({ queryKey: ['dashboard', 'my'], queryFn: getMyDashboard })
}

/**
 * 渠道工作台（§9.1）
 * 仅在 channelId 存在时启用（避免无效请求）
 */
export function useChannelDashboard(channelId: number | undefined) {
  return useQuery({
    queryKey: ['dashboard', 'channel', channelId],
    queryFn: () => getChannelDashboard(channelId!),
    enabled: typeof channelId === 'number',
  })
}

/** 项目统计 */
export function useProjectStatistics() {
  return useQuery({
    queryKey: ['dashboard', 'statistics', 'project'],
    queryFn: getProjectStatistics,
  })
}
