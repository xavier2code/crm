import { App } from 'antd'
import { useNavigate } from 'react-router-dom'

import { useCreateContract } from '@/hooks/useContracts'
import type { ContractRequest } from '@/api/contract'

import ContractForm from '../components/ContractForm'

export default function ContractCreatePage() {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const createMut = useCreateContract()

  const handleSubmit = async (values: ContractRequest) => {
    try {
      await createMut.mutateAsync(values)
      message.success('合同创建成功')
      navigate('/contract')
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  return (
    <ContractForm
      title="新增合同"
      breadcrumbs={['商务管理', '合同管理', '新增合同']}
      loading={createMut.isPending}
      onSubmit={handleSubmit}
    />
  )
}
