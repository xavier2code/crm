import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  createRebate,
  deleteRebateRate,
  getRebateRate,
  listMyRebates,
  listRebateRates,
  pageRebates,
  saveRebateRate,
  updateConfirmStatus,
  updatePaymentStatus,
  updateRebate,
  type RebateRateRequest,
  type RebateRequest,
} from '@/api/rebate'

/* ===== 返利记录 ===== */

export function useRebates(params: {
  current?: number
  size?: number
  channelId?: number
  confirmStatus?: number
  paymentStatus?: number
}) {
  return useQuery({ queryKey: ['rebates', params], queryFn: () => pageRebates(params) })
}

export function useMyRebates() {
  return useQuery({ queryKey: ['rebates', 'my'], queryFn: listMyRebates })
}

export function useCreateRebate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RebateRequest) => createRebate(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rebates'] }),
  })
}

export function useUpdateRebate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RebateRequest }) => updateRebate(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rebates'] }),
  })
}

export function useUpdateConfirmStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, confirmStatus }: { id: number; confirmStatus: number }) =>
      updateConfirmStatus(id, confirmStatus),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rebates'] }),
  })
}

export function useUpdatePaymentStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, paymentStatus }: { id: number; paymentStatus: number }) =>
      updatePaymentStatus(id, paymentStatus),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rebates'] }),
  })
}

/* ===== 返利率配置 ===== */

export function useRebateRates(channelId?: number) {
  return useQuery({
    queryKey: ['rebate-rates', channelId ?? 'all'],
    queryFn: () => listRebateRates(channelId),
  })
}

export function useRebateRate(id: number | undefined) {
  return useQuery({
    queryKey: ['rebate-rate', id],
    queryFn: () => getRebateRate(id!),
    enabled: typeof id === 'number',
  })
}

export function useSaveRebateRate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RebateRateRequest) => saveRebateRate(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rebate-rates'] }),
  })
}

export function useDeleteRebateRate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteRebateRate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rebate-rates'] }),
  })
}
