import { Outlet } from 'react-router-dom'

interface BlankLayoutProps {
  children?: React.ReactNode
}

export default function BlankLayout({ children }: BlankLayoutProps) {
  return children ? <>{children}</> : <Outlet />
}
