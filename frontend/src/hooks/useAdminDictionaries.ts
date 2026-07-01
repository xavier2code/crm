import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getDictionariesByType,
  createDictionary,
  updateDictionary,
  deleteDictionary,
} from '@/api/admin/dictionary'

export function useDictionaries(type: string) {
  return useQuery({
    queryKey: ['admin', 'dictionaries', type],
    queryFn: () => getDictionariesByType(type),
    enabled: !!type,
  })
}

export function useCreateDictionary() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createDictionary,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'dictionaries'] }),
  })
}

export function useUpdateDictionary() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateDictionary>[1] }) =>
      updateDictionary(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'dictionaries'] }),
  })
}

export function useDeleteDictionary() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteDictionary,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'dictionaries'] }),
  })
}
