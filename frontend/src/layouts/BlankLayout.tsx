import { Outlet } from 'react-router-dom'
import type { ReactNode } from 'react'

interface BlankLayoutProps {
  children?: ReactNode
}

export default function BlankLayout({ children }: BlankLayoutProps) {
  return children ? <>{children}</> : <Outlet />
}
