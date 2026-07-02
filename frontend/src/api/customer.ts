import type { components } from '@/types/api'

import { request } from './client'

export type CustomerPage = components['schemas']['PageCustomerVO']
export type CustomerVO = components['schemas']['CustomerVO']
export type CustomerRequest = components['schemas']['CustomerRequest']
export type ContactRequest = components['schemas']['ContactRequest']
export type ContactVO = components['schemas']['ContactVO']

export function getCustomers(params: { current?: number; size?: number; keyword?: string }) {
  return request<CustomerPage>({ url: '/customers', method: 'GET', params })
}

export function getCustomer(id: number) {
  return request<CustomerVO>({ url: `/customers/${id}`, method: 'GET' })
}

export function createCustomer(data: CustomerRequest) {
  return request<CustomerVO>({ url: '/customers', method: 'POST', data })
}

export function updateCustomer(id: number, data: CustomerRequest) {
  return request<CustomerVO>({ url: `/customers/${id}`, method: 'PUT', data })
}

export function deleteCustomer(id: number) {
  return request<void>({ url: `/customers/${id}`, method: 'DELETE' })
}

export function assignCustomer(id: number, userId: number) {
  return request<void>({ url: `/customers/${id}/assign`, method: 'POST', params: { userId } })
}

export function addContact(customerId: number, data: ContactRequest) {
  return request<void>({ url: `/customers/${customerId}/contacts`, method: 'POST', data })
}

export function updateContact(id: number, data: ContactRequest) {
  return request<void>({ url: `/contacts/${id}`, method: 'PUT', data })
}

export function deleteContact(id: number) {
  return request<void>({ url: `/contacts/${id}`, method: 'DELETE' })
}
