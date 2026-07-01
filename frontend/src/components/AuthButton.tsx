import { Button, type ButtonProps } from 'antd'

import { useAuthStore } from '@/stores/auth'
import { hasPermission } from '@/utils/permission'

interface AuthButtonProps extends ButtonProps {
  code: string
  fallback?: 'hidden' | 'disabled'
}

export function AuthButton({ code, fallback = 'hidden', children, ...rest }: AuthButtonProps) {
  const permissionCodes = useAuthStore((state) => state.permissionCodes)
  const allowed = hasPermission(permissionCodes, code)

  if (!allowed && fallback === 'hidden') {
    return null
  }

  return (
    <Button disabled={!allowed} {...rest}>
      {children}
    </Button>
  )
}
