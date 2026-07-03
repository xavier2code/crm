import { App } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'

import { useContract, useUpdateContract } from '@/hooks/useContracts'
import type { ContractRequest } from '@/api/contract'

import ContractForm from '../components/ContractForm'

export default function ContractEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()

  const contractId = Number(id)
  const { data: contract, isLoading } = useContract(contractId)
  const updateMut = useUpdateContract()

  const handleSubmit = async (values: ContractRequest) => {
    try {
      await updateMut.mutateAsync({ id: contractId, data: values })
      message.success('合同更新成功')
      navigate(`/contract/${contractId}`)
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  if (isLoading) return null

  return (
    <ContractForm
      title="编辑合同"
      breadcrumbs={['商务管理', '合同管理', contract?.projectName || '编辑合同']}
      initialData={contract}
      loading={updateMut.isPending}
      onSubmit={handleSubmit}
    />
  )
}
