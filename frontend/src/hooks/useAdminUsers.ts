import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { getUsers, getUser, createUser, updateUser, deleteUser, resetPassword } from '@/api/admin/user'

export function useUsers(params: Parameters<typeof getUsers>[0]) {
  return useQuery({ queryKey: ['admin', 'users', params], queryFn: () => getUsers(params) })
}

export function useUser(id: number | undefined) {
  return useQuery({
    queryKey: ['admin', 'user', id],
    queryFn: () => getUser(id!),
    enabled: !!id,
  })
}

export function useCreateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })
}

export function useUpdateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateUser>[1] }) =>
      updateUser(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })
}

export function useDeleteUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })
}

export function useResetPassword() {
  return useMutation({ mutationFn: resetPassword })
}
