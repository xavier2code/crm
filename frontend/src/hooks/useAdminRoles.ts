import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { getRoles, getRole, createRole, updateRole, deleteRole } from '@/api/admin/role'

export function useRoles() {
  return useQuery({ queryKey: ['admin', 'roles'], queryFn: getRoles })
}

export function useRole(id: number | undefined) {
  return useQuery({
    queryKey: ['admin', 'role', id],
    queryFn: () => getRole(id!),
    enabled: !!id,
  })
}

export function useCreateRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createRole,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'roles'] }),
  })
}

export function useUpdateRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateRole>[1] }) =>
      updateRole(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'roles'] }),
  })
}

export function useDeleteRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteRole,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'roles'] }),
  })
}
