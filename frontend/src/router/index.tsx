import { Suspense, lazy, useMemo } from 'react'
import { createBrowserRouter, Navigate, RouterProvider, type RouteObject } from 'react-router-dom'
import { Spin } from 'antd'

import BasicLayout from '@/layouts/BasicLayout'
import BlankLayout from '@/layouts/BlankLayout'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { hasPermission } from '@/utils/permission'
import type { MenuItem } from '@/types/app'

const LoginPage = lazy(() => import('@/pages/login'))
const DashboardPage = lazy(() => import('@/pages/dashboard'))
const CustomerPage = lazy(() => import('@/pages/customer'))
const CustomerCreatePage = lazy(() => import('@/pages/customer/create'))
const CustomerDetailPage = lazy(() => import('@/pages/customer/detail'))
const CustomerEditPage = lazy(() => import('@/pages/customer/edit'))
const OpportunityPage = lazy(() => import('@/pages/opportunity'))
const ProjectPage = lazy(() => import('@/pages/project'))
const ContractPage = lazy(() => import('@/pages/contract'))
const UsersPage = lazy(() => import('@/pages/system/users'))
const RolesPage = lazy(() => import('@/pages/system/roles'))
const DictionaryPage = lazy(() => import('@/pages/system/dictionary'))
const UnitsPage = lazy(() => import('@/pages/system/units'))
const ForbiddenPage = lazy(() => import('@/pages/error/403'))

const PageLoading = () => (
  <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 100 }}>
    <Spin size="large" />
  </div>
)

const LazyWrapper = ({ children }: { children: React.ReactNode }) => (
  <Suspense fallback={<PageLoading />}>{children}</Suspense>
)

function AuthGuard({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((state) => state.token)
  return token ? children : <Navigate to="/login" replace />
}

function PermissionGuard({ requiredCode, children }: { requiredCode?: string; children: React.ReactNode }) {
  const permissionCodes = useAuthStore((state) => state.permissionCodes)
  if (requiredCode && !hasPermission(permissionCodes, requiredCode)) {
    return <Navigate to="/403" replace />
  }
  return children
}

function buildRoutesFromMenus(menus: MenuItem[]): RouteObject[] {
  const map: Record<string, React.ReactNode> = {
    '/dashboard': <DashboardPage />,
    '/customer': <CustomerPage />,
    '/customer/create': <CustomerCreatePage />,
    '/customer/:id': <CustomerDetailPage />,
    '/customer/:id/edit': <CustomerEditPage />,
    '/opportunity': <OpportunityPage />,
    '/project': <ProjectPage />,
    '/contract': <ContractPage />,
    '/system/users': <UsersPage />,
    '/system/roles': <RolesPage />,
    '/system/dictionary': <DictionaryPage />,
    '/system/units': <UnitsPage />,
  }

  const routes: RouteObject[] = []

  function walk(items: MenuItem[]) {
    items.forEach((item) => {
      if (item.children) {
        walk(item.children)
      } else if (item.path && map[item.path]) {
        routes.push({
          path: item.path,
          element: (
            <LazyWrapper>
              <PermissionGuard requiredCode={item.permission}>{map[item.path]}</PermissionGuard>
            </LazyWrapper>
          ),
        })
      }
    })
  }

  walk(menus)
  return routes
}

export function useRoutes() {
  const { menus } = useMenuStore()

  return useMemo<RouteObject[]>(() => {
    const dynamicRoutes = buildRoutesFromMenus(menus)

    return [
      {
        path: '/login',
        element: (
          <BlankLayout>
            <LazyWrapper>
              <LoginPage />
            </LazyWrapper>
          </BlankLayout>
        ),
      },
      {
        path: '/',
        element: (
          <AuthGuard>
            <BasicLayout />
          </AuthGuard>
        ),
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          ...dynamicRoutes,
          {
            path: '403',
            element: (
              <LazyWrapper>
                <ForbiddenPage />
              </LazyWrapper>
            ),
          },
          {
            path: '*',
            element: <Navigate to="/dashboard" replace />,
          },
        ],
      },
      {
        path: '*',
        element: <Navigate to="/login" replace />,
      },
    ]
  }, [menus])
}

export function Router() {
  const routes = useRoutes()
  const router = useMemo(() => createBrowserRouter(routes), [routes])
  return <RouterProvider router={router} />
}
