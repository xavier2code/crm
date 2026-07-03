import { App } from 'antd'
import { useNavigate } from 'react-router-dom'

import { useCreateOpportunity } from '@/hooks/useOpportunities'
import type { OpportunityRequest } from '@/api/opportunity'

import OpportunityForm from '../components/OpportunityForm'

export default function OpportunityCreatePage() {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const createMut = useCreateOpportunity()

  const handleSubmit = async (values: OpportunityRequest) => {
    try {
      await createMut.mutateAsync(values)
      message.success('报备创建成功')
      navigate('/opportunity')
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  return (
    <OpportunityForm
      title="新建报备"
      breadcrumbs={['商机报备', '新建报备']}
      loading={createMut.isPending}
      onSubmit={handleSubmit}
    />
  )
}
