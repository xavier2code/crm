import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  App,
  Button,
  Card,
  Descriptions,
  Form,
  Modal,
  Popconfirm,
  Select,
  Space,
  Tag,
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  SwapOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import { PageHeader } from '@/components/PageHeader'
import { useAuthStore } from '@/stores/auth'
import { useContract, useDeleteContract, useUpdateContractStatus } from '@/hooks/useContracts'

const STATUS_OPTIONS = [
  { value: 1, label: '待签', color: 'default' },
  { value: 2, label: '已签', color: 'success' },
  { value: 3, label: '已开通', color: 'processing' },
  { value: 4, label: '服务中', color: 'blue' },
  { value: 5, label: '已到期', color: 'warning' },
]

const STATUS_MAP = new Map(STATUS_OPTIONS.map((s) => [s.value, s]))

const STATUS_TRANSITIONS: Record<number, { value: number; label: string }[]> = {
  1: [{ value: 2, label: '已签' }],
  2: [{ value: 3, label: '已开通' }],
  3: [{ value: 4, label: '服务中' }],
  4: [{ value: 5, label: '已到期' }],
}

export default function ContractDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()
  const { permissionCodes } = useAuthStore()

  const contractId = Number(id)
  const { data: contract, isLoading } = useContract(contractId)
  const deleteMut = useDeleteContract()
  const statusMut = useUpdateContractStatus()

  const [statusOpen, setStatusOpen] = useState(false)
  const [statusForm] = Form.useForm<{ status: number }>()

  const canEdit = permissionCodes.includes('contract:update')
  const canUpdateStatus = permissionCodes.includes('contract:update') || permissionCodes.includes('contract:status')
  const canDelete = permissionCodes.includes('contract:delete')

  if (isLoading) return null
  if (!contract) {
    return (
      <div>
        <PageHeader title="合同详情" showBack />
        <Card>合同不存在或已被删除</Card>
      </div>
    )
  }

  const handleDelete = async () => {
    try {
      await deleteMut.mutateAsync(contractId)
      message.success('删除成功')
      navigate('/contract')
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const openStatusChange = () => {
    statusForm.resetFields()
    setStatusOpen(true)
  }

  const handleStatusChange = async () => {
    try {
      const values = await statusForm.validateFields()
      await statusMut.mutateAsync({ id: contractId, status: values.status })
      message.success('状态更新成功')
      setStatusOpen(false)
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const statusTransitions = STATUS_TRANSITIONS[contract.status || 0] || []

  return (
    <div>
      <PageHeader
        title="合同详情"
        breadcrumbs={['商务管理', '合同管理', `合同 #${contract.id}`]}
        showBack
        extra={
          <Space>
            {canEdit && (
              <Button icon={<EditOutlined />} onClick={() => navigate(`/contract/${contractId}/edit`)}>
                编辑
              </Button>
            )}
            {canUpdateStatus && statusTransitions.length > 0 && (
              <Button icon={<SwapOutlined />} onClick={openStatusChange}>
                变更状态
              </Button>
            )}
            {canDelete && (
              <Popconfirm
                title="确认删除该合同？"
                description="删除后不可恢复。"
                onConfirm={handleDelete}
              >
                <Button danger icon={<DeleteOutlined />}>删除</Button>
              </Popconfirm>
            )}
          </Space>
        }
      />

      <Card>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="合同 ID">#{contract.id}</Descriptions.Item>
          <Descriptions.Item label="状态">
            {contract.status != null ? (
              <Tag color={STATUS_MAP.get(contract.status)?.color}>{STATUS_MAP.get(contract.status)?.label}</Tag>
            ) : (
              '-'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="项目">{contract.projectName || '-'}</Descriptions.Item>
          <Descriptions.Item label="项目 ID">#{contract.projectId}</Descriptions.Item>
          <Descriptions.Item label="合同金额" span={2}>
            {contract.amount != null ? `${contract.amount} 万元` : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {contract.createdAt ? dayjs(contract.createdAt).format('YYYY-MM-DD HH:mm') : '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Modal
        title="变更合同状态"
        open={statusOpen}
        onCancel={() => setStatusOpen(false)}
        onOk={handleStatusChange}
        confirmLoading={statusMut.isPending}
        destroyOnClose
      >
        <Form form={statusForm} layout="vertical" preserve={false}>
          <Form.Item
            name="status"
            label="新状态"
            rules={[{ required: true, message: '请选择新状态' }]}
          >
            <Select placeholder="请选择新状态" options={statusTransitions} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
