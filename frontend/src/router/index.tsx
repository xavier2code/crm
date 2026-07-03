import { Suspense, lazy } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Spin } from 'antd'

import BasicLayout from '@/layouts/BasicLayout'
import BlankLayout from '@/layouts/BlankLayout'
import { useAuthStore } from '@/stores/auth'

const LoginPage = lazy(() => import('@/pages/login'))
const ChangePasswordPage = lazy(() => import('@/pages/change-password'))
const DashboardPage = lazy(() => import('@/pages/dashboard'))
const ChannelDashboardPage = lazy(() => import('@/pages/channel-dashboard'))
const RebatePage = lazy(() => import('@/pages/rebate'))
const RebateRatesPage = lazy(() => import('@/pages/rebate-rates'))
const CustomerPage = lazy(() => import('@/pages/customer'))
const CustomerCreatePage = lazy(() => import('@/pages/customer/create'))
const CustomerDetailPage = lazy(() => import('@/pages/customer/detail'))
const CustomerEditPage = lazy(() => import('@/pages/customer/edit'))
const OpportunityPage = lazy(() => import('@/pages/opportunity'))
const OpportunityCreatePage = lazy(() => import('@/pages/opportunity/create'))
const OpportunityDetailPage = lazy(() => import('@/pages/opportunity/detail'))
const OpportunityEditPage = lazy(() => import('@/pages/opportunity/edit'))
const ProjectPage = lazy(() => import('@/pages/project'))
const ProjectDetailPage = lazy(() => import('@/pages/project/detail'))
const ContractPage = lazy(() => import('@/pages/contract'))
const UsersPage = lazy(() => import('@/pages/system/users'))
const RolesPage = lazy(() => import('@/pages/system/roles'))
const DictionaryPage = lazy(() => import('@/pages/system/dictionary'))
const UnitsPage = lazy(() => import('@/pages/system/units'))
const AuditLogPage = lazy(() => import('@/pages/system/audit-log'))
const ChannelPage = lazy(() => import('@/pages/system/channel'))
const FollowUpPage = lazy(() => import('@/pages/followup'))
const TaskPage = lazy(() => import('@/pages/task'))
const NotificationsPage = lazy(() => import('@/pages/notifications'))
const BusinessPage = lazy(() => import('@/pages/business'))
const ReimbursementPage = lazy(() => import('@/pages/reimbursement'))
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
  return token ? <>{children}</> : <Navigate to="/login" replace />
}

export function Router() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            <BlankLayout>
              <LazyWrapper>
                <LoginPage />
              </LazyWrapper>
            </BlankLayout>
          }
        />
        <Route
          path="/change-password"
          element={
            <BlankLayout>
              <LazyWrapper>
                <ChangePasswordPage />
              </LazyWrapper>
            </BlankLayout>
          }
        />
        <Route
          path="/"
          element={
            <AuthGuard>
              <BasicLayout />
            </AuthGuard>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route
            path="dashboard"
            element={
              <LazyWrapper>
                <DashboardPage />
              </LazyWrapper>
            }
          />
          <Route
            path="dashboard/channel/:channelId"
            element={
              <LazyWrapper>
                <ChannelDashboardPage />
              </LazyWrapper>
            }
          />
          <Route
            path="business/rebate"
            element={
              <LazyWrapper>
                <RebatePage />
              </LazyWrapper>
            }
          />
          <Route
            path="business/rebate/rates"
            element={
              <LazyWrapper>
                <RebateRatesPage />
              </LazyWrapper>
            }
          />
          <Route
            path="customer"
            element={
              <LazyWrapper>
                <CustomerPage />
              </LazyWrapper>
            }
          />
          <Route
            path="customer/create"
            element={
              <LazyWrapper>
                <CustomerCreatePage />
              </LazyWrapper>
            }
          />
          <Route
            path="customer/:id"
            element={
              <LazyWrapper>
                <CustomerDetailPage />
              </LazyWrapper>
            }
          />
          <Route
            path="customer/:id/edit"
            element={
              <LazyWrapper>
                <CustomerEditPage />
              </LazyWrapper>
            }
          />
          <Route
            path="opportunity"
            element={
              <LazyWrapper>
                <OpportunityPage />
              </LazyWrapper>
            }
          />
          <Route
            path="opportunity/create"
            element={
              <LazyWrapper>
                <OpportunityCreatePage />
              </LazyWrapper>
            }
          />
          <Route
            path="opportunity/:id"
            element={
              <LazyWrapper>
                <OpportunityDetailPage />
              </LazyWrapper>
            }
          />
          <Route
            path="opportunity/:id/edit"
            element={
              <LazyWrapper>
                <OpportunityEditPage />
              </LazyWrapper>
            }
          />
          <Route
            path="project"
            element={
              <LazyWrapper>
                <ProjectPage />
              </LazyWrapper>
            }
          />
          <Route
            path="project/:id"
            element={
              <LazyWrapper>
                <ProjectDetailPage />
              </LazyWrapper>
            }
          />
          <Route
            path="contract"
            element={
              <LazyWrapper>
                <ContractPage />
              </LazyWrapper>
            }
          />
          <Route
            path="business"
            element={
              <LazyWrapper>
                <BusinessPage />
              </LazyWrapper>
            }
          />
          <Route
            path="reimbursement"
            element={
              <LazyWrapper>
                <ReimbursementPage />
              </LazyWrapper>
            }
          />
          <Route
            path="system/users"
            element={
              <LazyWrapper>
                <UsersPage />
              </LazyWrapper>
            }
          />
          <Route
            path="system/roles"
            element={
              <LazyWrapper>
                <RolesPage />
              </LazyWrapper>
            }
          />
          <Route
            path="system/dictionary"
            element={
              <LazyWrapper>
                <DictionaryPage />
              </LazyWrapper>
            }
          />
          <Route
            path="system/units"
            element={
              <LazyWrapper>
                <UnitsPage />
              </LazyWrapper>
            }
          />
          <Route
            path="system/audit"
            element={
              <LazyWrapper>
                <AuditLogPage />
              </LazyWrapper>
            }
          />
          <Route
            path="system/channel"
            element={
              <LazyWrapper>
                <ChannelPage />
              </LazyWrapper>
            }
          />
          <Route
            path="followup"
            element={
              <LazyWrapper>
                <FollowUpPage />
              </LazyWrapper>
            }
          />
          <Route
            path="task"
            element={
              <LazyWrapper>
                <TaskPage />
              </LazyWrapper>
            }
          />
          <Route
            path="notifications"
            element={
              <LazyWrapper>
                <NotificationsPage />
              </LazyWrapper>
            }
          />
          <Route
            path="403"
            element={
              <LazyWrapper>
                <ForbiddenPage />
              </LazyWrapper>
            }
          />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
