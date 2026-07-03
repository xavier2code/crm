import { App } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'

import { useOpportunity, useUpdateOpportunity } from '@/hooks/useOpportunities'
import type { OpportunityRequest } from '@/api/opportunity'

import OpportunityForm from '../components/OpportunityForm'

export default function OpportunityEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()

  const opportunityId = Number(id)
  const { data: opportunity, isLoading } = useOpportunity(opportunityId)
  const updateMut = useUpdateOpportunity()

  const handleSubmit = async (values: OpportunityRequest) => {
    try {
      await updateMut.mutateAsync({ id: opportunityId, data: values })
      message.success('报备更新成功')
      navigate(`/opportunity/${opportunityId}`)
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  if (isLoading) return null

  return (
    <OpportunityForm
      title="编辑报备"
      breadcrumbs={['商机报备', opportunity?.customerName || '编辑报备']}
      initialData={opportunity}
      loading={updateMut.isPending}
      onSubmit={handleSubmit}
    />
  )
}
