import { App } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'

import { useCustomer, useUpdateCustomer } from '@/hooks/useCustomers'
import type { CustomerRequest } from '@/api/customer'

import CustomerForm from '../CustomerForm'

export default function CustomerEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()

  const customerId = Number(id)
  const { data: customer, isLoading } = useCustomer(customerId)
  const updateMut = useUpdateCustomer()

  const handleSubmit = async (values: CustomerRequest) => {
    try {
      await updateMut.mutateAsync({ id: customerId, data: values })
      message.success('客户更新成功')
      navigate(`/customer/${customerId}`)
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  if (isLoading) return null

  return (
    <CustomerForm
      title="编辑客户"
      breadcrumbs={['客户管理', customer?.name || '编辑客户']}
      initialData={customer}
      loading={updateMut.isPending}
      onSubmit={handleSubmit}
    />
  )
}
