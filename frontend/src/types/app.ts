export interface MenuItem {
  id: number
  name: string
  path?: string
  icon?: string
  children?: MenuItem[]
  permission?: string
}

export interface CurrentUser {
  id: number
  username: string
  realName?: string
  phone?: string
  email?: string
  roles: string[]
}

export interface AuthState {
  token: string | null
  user: CurrentUser | null
  roles: string[]
  permissionCodes: string[]
}
