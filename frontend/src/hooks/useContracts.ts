import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getContracts,
  getContract,
  createContract,
  updateContract,
  updateContractStatus,
} from '@/api/contract'

export function useContracts(params: Parameters<typeof getContracts>[0]) {
  return useQuery({ queryKey: ['contracts', params], queryFn: () => getContracts(params) })
}

export function useContract(id: number | undefined) {
  return useQuery({
    queryKey: ['contract', id],
    queryFn: () => getContract(id!),
    enabled: !!id,
  })
}

export function useCreateContract() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createContract,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['contracts'] }),
  })
}

export function useUpdateContract() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateContract>[1] }) =>
      updateContract(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['contracts'] })
      queryClient.invalidateQueries({ queryKey: ['contract', variables.id] })
    },
  })
}

export function useUpdateContractStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => updateContractStatus(id, status),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['contracts'] })
      queryClient.invalidateQueries({ queryKey: ['contract', variables.id] })
    },
  })
}
