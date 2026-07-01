import { create } from 'zustand'

import type { MenuItem } from '@/types/app'

interface MenuState {
  menus: MenuItem[]
  setMenus: (menus: MenuItem[]) => void
  clearMenus: () => void
}

export const useMenuStore = create<MenuState>((set) => ({
  menus: [],
  setMenus: (menus) => set({ menus }),
  clearMenus: () => set({ menus: [] }),
}))
