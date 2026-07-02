import { request } from './client'

/** 返利视图 */
export interface RebateVO {
  id: number
  channelId?: number
  channelName?: string
  contractId?: number
  productCategory?: string
  rebateRate?: number
  totalAmount?: number
  actualAmount?: number
  /** 确认状态：1=未确认 2=已确认 */
  confirmStatus?: number
  confirmStatusName?: string
  /** 付款状态：1=未付款 2=已付款 */
  paymentStatus?: number
  paymentStatusName?: string
  /** 返利类型：1=业绩完成返利 2=回款返利 3=服务返利 */
  rebateType?: number
  rebateTypeName?: string
  createdAt?: string
}

export interface RebatePage {
  records: RebateVO[]
  total: number
  size: number
  current: number
  pages?: number
}

export interface RebateRequest {
  id?: number
  channelId?: number
  contractId?: number
  productCategory?: string
  rebateRate?: number
  totalAmount?: number
  actualAmount?: number
  confirmStatus?: number
  paymentStatus?: number
  rebateType?: number
}

/** 返利率配置（产品/渠道维度，§9.8 已确认） */
export interface RebateRate {
  id: number
  productCategory: string
  channelId?: number
  rate: number
  effectiveFrom?: string
  effectiveTo?: string
  createdAt?: string
}

export interface RebateRateRequest {
  id?: number
  productCategory: string
  channelId?: number
  rate: number
  effectiveFrom?: string
  effectiveTo?: string
}

/* ===== 返利记录 ===== */

export function pageRebates(params: {
  current?: number
  size?: number
  channelId?: number
  confirmStatus?: number
  paymentStatus?: number
} = {}) {
  return request<RebatePage>({
    url: '/rebates',
    method: 'GET',
    params: {
      current: params.current ?? 1,
      size: params.size ?? 10,
      channelId: params.channelId,
      confirmStatus: params.confirmStatus,
      paymentStatus: params.paymentStatus,
    },
  })
}

/** 渠道负责人专属：仅看自己渠道的返利 */
export function listMyRebates() {
  return request<RebateVO[]>({ url: '/rebates/my', method: 'GET' })
}

export function createRebate(data: RebateRequest) {
  return request<number>({ url: '/rebates', method: 'POST', data })
}

export function updateRebate(id: number, data: RebateRequest) {
  return request<void>({ url: `/rebates/${id}`, method: 'PUT', data })
}

export function updateConfirmStatus(id: number, confirmStatus: number) {
  return request<void>({
    url: `/rebates/${id}/confirm-status`,
    method: 'PUT',
    params: { confirmStatus },
  })
}

export function updatePaymentStatus(id: number, paymentStatus: number) {
  return request<void>({
    url: `/rebates/${id}/payment-status`,
    method: 'PUT',
    params: { paymentStatus },
  })
}

/* ===== 返利率配置 ===== */

export function listRebateRates(channelId?: number) {
  return request<RebateRate[]>({
    url: '/rebate-rate/list',
    method: 'GET',
    params: { channelId },
  })
}

export function getRebateRate(id: number) {
  return request<RebateRate>({ url: `/rebate-rate/${id}`, method: 'GET' })
}

export function saveRebateRate(data: RebateRateRequest) {
  return request<RebateRate>({ url: '/rebate-rate/save', method: 'POST', data })
}

export function deleteRebateRate(id: number) {
  return request<void>({ url: `/rebate-rate/${id}`, method: 'DELETE' })
}

/* ===== 业务常量 ===== */

export const CONFIRM_STATUS = { UNCONFIRMED: 1, CONFIRMED: 2 } as const
export const PAYMENT_STATUS = { UNPAID: 1, PAID: 2 } as const
export const REBATE_TYPE = {
  PERFORMANCE: 1,
  PAYMENT: 2,
  SERVICE: 3,
} as const

export const REBATE_TYPE_LABEL: Record<number, string> = {
  [REBATE_TYPE.PERFORMANCE]: '业绩完成返利',
  [REBATE_TYPE.PAYMENT]: '回款返利',
  [REBATE_TYPE.SERVICE]: '服务返利',
}
