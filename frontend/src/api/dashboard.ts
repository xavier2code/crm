import type { components } from '@/types/api'

import { request } from './client'

type DashboardVO = components['schemas']['DashboardVO']

export function getMyDashboard() {
  return request<DashboardVO>({ url: '/dashboard/my', method: 'GET' })
}
