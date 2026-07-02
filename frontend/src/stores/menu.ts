import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import type { MenuItem } from '@/types/app'

interface MenuState {
  menus: MenuItem[]
  setMenus: (menus: MenuItem[]) => void
  clearMenus: () => void
}

export const useMenuStore = create<MenuState>()(
  persist(
    (set) => ({
      menus: [],
      setMenus: (menus) => set({ menus }),
      clearMenus: () => set({ menus: [] }),
    }),
    { name: 'crm-menus' }
  )
)
