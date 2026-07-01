import type { components } from '@/types/api'
import type { DictionaryItem } from '@/stores/dict'

import { request } from '../client'

type DictionaryList = components['schemas']['DictionaryVO'][]
type DictionaryRequest = components['schemas']['DictionaryRequest']

export function getDictionariesByType(type: string) {
  return request<DictionaryList>({ url: `/admin/dictionaries/${type}`, method: 'GET' })
}

export function createDictionary(data: DictionaryRequest) {
  return request<DictionaryItem>({ url: '/admin/dictionaries', method: 'POST', data })
}

export function updateDictionary(id: number, data: DictionaryRequest) {
  return request<DictionaryItem>({ url: `/admin/dictionaries/${id}`, method: 'PUT', data })
}

export function deleteDictionary(id: number) {
  return request<void>({ url: `/admin/dictionaries/${id}`, method: 'DELETE' })
}
