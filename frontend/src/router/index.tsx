import { Suspense, lazy } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { Spin } from 'antd'

import BasicLayout from '@/layouts/BasicLayout'
import { useAuthStore } from '@/stores/auth'

const LoginPage = lazy(() => import('@/pages/login'))
const DashboardPage = lazy(() => import('@/pages/dashboard'))
const CustomerPage = lazy(() => import('@/pages/customer'))
const OpportunityPage = lazy(() => import('@/pages/opportunity'))
const BusinessPage = lazy(() => import('@/pages/business'))
const ReimbursementPage = lazy(() => import('@/pages/reimbursement'))
const UsersPage = lazy(() => import('@/pages/system/users'))
const RolesPage = lazy(() => import('@/pages/system/roles'))
const DictionaryPage = lazy(() => import('@/pages/system/dictionary'))
const UnitsPage = lazy(() => import('@/pages/system/units'))

const PageLoading = () => (
  <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 100 }}>
    <Spin size="large" />
  </div>
)

const AuthGuard = ({ children }: { children: React.ReactNode }) => {
  const token = useAuthStore((state) => state.token)
  return token ? children : <Navigate to="/login" replace />
}

const LazyWrapper = ({ children }: { children: React.ReactNode }) => (
  <Suspense fallback={<PageLoading />}>{children}</Suspense>
)

export const router = createBrowserRouter([
  {
    path: '/login',
    element: (
      <LazyWrapper>
        <LoginPage />
      </LazyWrapper>
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
      {
        path: 'dashboard',
        element: (
          <LazyWrapper>
            <DashboardPage />
          </LazyWrapper>
        ),
      },
      {
        path: 'customer',
        element: (
          <LazyWrapper>
            <CustomerPage />
          </LazyWrapper>
        ),
      },
      {
        path: 'opportunity',
        element: (
          <LazyWrapper>
            <OpportunityPage />
          </LazyWrapper>
        ),
      },
      {
        path: 'business',
        element: (
          <LazyWrapper>
            <BusinessPage />
          </LazyWrapper>
        ),
      },
      {
        path: 'reimbursement',
        element: (
          <LazyWrapper>
            <ReimbursementPage />
          </LazyWrapper>
        ),
      },
      {
        path: 'system/users',
        element: (
          <LazyWrapper>
            <UsersPage />
          </LazyWrapper>
        ),
      },
      {
        path: 'system/roles',
        element: (
          <LazyWrapper>
            <RolesPage />
          </LazyWrapper>
        ),
      },
      {
        path: 'system/dictionary',
        element: (
          <LazyWrapper>
            <DictionaryPage />
          </LazyWrapper>
        ),
      },
      {
        path: 'system/units',
        element: (
          <LazyWrapper>
            <UnitsPage />
          </LazyWrapper>
        ),
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/dashboard" replace />,
  },
])
