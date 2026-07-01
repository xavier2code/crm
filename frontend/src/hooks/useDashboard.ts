import { useQuery } from '@tanstack/react-query'

import { getMyDashboard } from '@/api/dashboard'

export function useDashboard() {
  return useQuery({ queryKey: ['dashboard', 'my'], queryFn: getMyDashboard })
}
