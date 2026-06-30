import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import type { CurrentUser, LoginResult } from '@/api/auth'

interface AuthState {
  token: string | null
  user: CurrentUser | null
  setAuth: (result: LoginResult) => void
  setUser: (user: CurrentUser) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setAuth: (result) => {
        localStorage.setItem('crm-token', result.token)
        set({ token: result.token })
      },
      setUser: (user) => set({ user }),
      clearAuth: () => {
        localStorage.removeItem('crm-token')
        set({ token: null, user: null })
      },
    }),
    { name: 'crm-auth' }
  )
)
