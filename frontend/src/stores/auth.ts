import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import type { CurrentUser } from '@/types/app'
import type { LoginResult } from '@/api/auth'

interface AuthState {
  token: string | null
  user: CurrentUser | null
  roles: string[]
  permissionCodes: string[]
  setAuth: (result: LoginResult) => void
  setUser: (user: CurrentUser) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      roles: [],
      permissionCodes: [],
      setAuth: (result) => {
        localStorage.setItem('crm-token', result.accessToken)
        set({
          token: result.accessToken,
          user: result.userInfo,
          roles: result.roles || [],
          permissionCodes: result.permissionCodes || [],
        })
      },
      setUser: (user) => set({ user }),
      clearAuth: () => {
        localStorage.removeItem('crm-token')
        set({ token: null, user: null, roles: [], permissionCodes: [] })
      },
    }),
    { name: 'crm-auth' }
  )
)
