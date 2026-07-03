import type { components } from '@/types/api'

import { request } from './client'

export type ContractPage = components['schemas']['PageContractVO']
export type ContractVO = components['schemas']['ContractVO']
export type ContractRequest = components['schemas']['ContractRequest']

export function getContracts(params: { current?: number; size?: number; status?: number }) {
  return request<ContractPage>({ url: '/contracts', method: 'GET', params })
}

export function getContract(id: number) {
  return request<ContractVO>({ url: `/contracts/${id}`, method: 'GET' })
}

export function createContract(data: ContractRequest) {
  return request<ContractVO>({ url: '/contracts', method: 'POST', data })
}

export function updateContract(id: number, data: ContractRequest) {
  return request<ContractVO>({ url: `/contracts/${id}`, method: 'PUT', data })
}

export function deleteContract(id: number) {
  return request<void>({ url: `/contracts/${id}`, method: 'DELETE' })
}

export function updateContractStatus(id: number, status: number) {
  return request<void>({ url: `/contracts/${id}/status`, method: 'PUT', params: { status } })
}
