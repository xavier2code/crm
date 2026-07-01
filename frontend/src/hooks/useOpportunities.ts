import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getOpportunities,
  getOpportunity,
  createOpportunity,
  updateOpportunity,
  deleteOpportunity,
  submitOpportunity,
  approveOpportunity,
  rejectOpportunity,
} from '@/api/opportunity'

export function useOpportunities(params: Parameters<typeof getOpportunities>[0]) {
  return useQuery({ queryKey: ['opportunities', params], queryFn: () => getOpportunities(params) })
}

export function useOpportunity(id: number | undefined) {
  return useQuery({
    queryKey: ['opportunity', id],
    queryFn: () => getOpportunity(id!),
    enabled: !!id,
  })
}

export function useCreateOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOpportunity,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['opportunities'] }),
  })
}

export function useUpdateOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateOpportunity>[1] }) =>
      updateOpportunity(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['opportunities'] })
      queryClient.invalidateQueries({ queryKey: ['opportunity', variables.id] })
    },
  })
}

export function useDeleteOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteOpportunity,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['opportunities'] }),
  })
}

export function useSubmitOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: submitOpportunity,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['opportunities'] }),
  })
}

export function useApproveOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof approveOpportunity>[1] }) =>
      approveOpportunity(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['opportunities'] }),
  })
}

export function useRejectOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof rejectOpportunity>[1] }) =>
      rejectOpportunity(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['opportunities'] }),
  })
}
