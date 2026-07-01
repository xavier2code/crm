import { create } from 'zustand'

export interface DictionaryItem {
  id: number
  type: string
  code: string
  name: string
  sort: number
  remark?: string
}

interface DictState {
  dicts: Record<string, DictionaryItem[]>
  setDicts: (dicts: Record<string, DictionaryItem[]>) => void
  getDict: (type: string) => DictionaryItem[]
  getDictName: (type: string, code: string | undefined) => string
  clearDicts: () => void
}

export const useDictStore = create<DictState>((set, get) => ({
  dicts: {},
  setDicts: (dicts) => set({ dicts }),
  getDict: (type) => get().dicts[type] || [],
  getDictName: (type, code) => {
    if (!code) return '-'
    const item = get().dicts[type]?.find((d) => d.code === code)
    return item?.name || code
  },
  clearDicts: () => set({ dicts: {} }),
}))
