import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { getUnits, getAllUnits, getUnit, createUnit, updateUnit, deleteUnit } from '@/api/admin/unit'

export function useUnits(params: Parameters<typeof getUnits>[0]) {
  return useQuery({ queryKey: ['admin', 'units', params], queryFn: () => getUnits(params) })
}

export function useAllUnits() {
  return useQuery({ queryKey: ['admin', 'units', 'all'], queryFn: getAllUnits })
}

export function useUnit(id: number | undefined) {
  return useQuery({
    queryKey: ['admin', 'unit', id],
    queryFn: () => getUnit(id!),
    enabled: !!id,
  })
}

export function useCreateUnit() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createUnit,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'units'] }),
  })
}

export function useUpdateUnit() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateUnit>[1] }) =>
      updateUnit(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'units'] }),
  })
}

export function useDeleteUnit() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteUnit,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'units'] }),
  })
}
