import { useEffect } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'

import {
  getUnreadCount,
  getUnreadNotifications,
  markAllAsRead,
  markAsRead,
  pageNotifications,
  type Notification,
  type NotificationStatus,
} from '@/api/notification'

const UNREAD_COUNT_KEY = ['notifications', 'unread-count'] as const
const UNREAD_LIST_KEY = ['notifications', 'unread-list'] as const

/**
 * 未读通知数量（轮询 30s 刷新；登录后立即拉一次）。
 * 也作为 Layout Badge 的 source of truth。
 */
export function useUnreadCount() {
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: UNREAD_COUNT_KEY,
    queryFn: () => getUnreadCount(),
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
    staleTime: 10_000,
  })
  useEffect(() => {
    // 触发首次请求
    void queryClient.ensureQueryData({ queryKey: UNREAD_COUNT_KEY, queryFn: () => getUnreadCount() })
  }, [queryClient])
  return query
}

/** 下拉 Popover 用的最近 5 条未读 */
export function useUnreadList(limit = 5) {
  return useQuery({
    queryKey: [...UNREAD_LIST_KEY, limit],
    queryFn: async () => {
      const list = await getUnreadNotifications()
      return list.slice(0, limit)
    },
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
    staleTime: 10_000,
  })
}

/** 全部通知分页（用于 /notifications 页面） */
export function useNotificationList(params: { status?: NotificationStatus; current?: number; size?: number }) {
  return useQuery({
    queryKey: ['notifications', 'list', params],
    queryFn: () => pageNotifications(params),
  })
}

export function useMarkAsRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => markAsRead(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: UNREAD_COUNT_KEY })
      void qc.invalidateQueries({ queryKey: ['notifications', 'unread-list'] })
      void qc.invalidateQueries({ queryKey: ['notifications', 'list'] })
    },
  })
}

export function useMarkAllAsRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => markAllAsRead(),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: UNREAD_COUNT_KEY })
      void qc.invalidateQueries({ queryKey: ['notifications', 'unread-list'] })
      void qc.invalidateQueries({ queryKey: ['notifications', 'list'] })
    },
  })
}

export type { Notification }
