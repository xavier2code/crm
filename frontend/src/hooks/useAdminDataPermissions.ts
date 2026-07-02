import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getUserDataPermissions,
  updateUserDataPermissions,
  type DataPermissionUpdateRequest,
} from '@/api/admin/dataPermission'

export function useUserDataPermissions(userId: number | undefined) {
  return useQuery({
    queryKey: ['admin', 'user', userId, 'data-permissions'],
    queryFn: () => getUserDataPermissions(userId!),
    enabled: typeof userId === 'number',
  })
}

export function useUpdateUserDataPermissions(userId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: DataPermissionUpdateRequest) =>
      updateUserDataPermissions(userId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['admin', 'user', userId, 'data-permissions'],
      })
    },
  })
}
