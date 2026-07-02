import { App } from 'antd'
import { useNavigate } from 'react-router-dom'

import { useCreateCustomer } from '@/hooks/useCustomers'
import type { CustomerRequest } from '@/api/customer'

import CustomerForm from '../CustomerForm'

export default function CustomerCreatePage() {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const createMut = useCreateCustomer()

  const handleSubmit = async (values: CustomerRequest) => {
    try {
      await createMut.mutateAsync(values)
      message.success('客户创建成功')
      navigate('/customer')
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  return (
    <CustomerForm
      title="新增客户"
      breadcrumbs={['客户管理', '新增客户']}
      loading={createMut.isPending}
      onSubmit={handleSubmit}
    />
  )
}
