import { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  App,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Tag,
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'

import { DataTable } from '@/components/DataTable'
import { PageHeader } from '@/components/PageHeader'
import { SearchForm } from '@/components/SearchForm'
import { useAuthStore } from '@/stores/auth'
import {
  useContracts,
  useCreateContract,
  useDeleteContract,
  useUpdateContractStatus,
} from '@/hooks/useContracts'
import { useProjects } from '@/hooks/useProjects'
import type { ContractVO } from '@/api/contract'

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

export default function ContractPage() {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const { permissionCodes } = useAuthStore()

  const canCreate = permissionCodes.includes('contract:create')
  const canUpdateStatus = permissionCodes.includes('contract:update') || permissionCodes.includes('contract:status')
  const canDelete = permissionCodes.includes('contract:delete')

  const [params, setParams] = useState<{
    current: number
    size: number
    status?: number
    keyword?: string
  }>({ current: 1, size: 10 })
  const [createOpen, setCreateOpen] = useState(false)
  const [statusOpen, setStatusOpen] = useState(false)
  const [targetContract, setTargetContract] = useState<ContractVO | null>(null)
  const [form] = Form.useForm<{ projectId: number; amount: number }>()
  const [statusForm] = Form.useForm<{ status: number }>()

  const { data, isLoading, refetch } = useContracts(params)
  const createMut = useCreateContract()
  const deleteMut = useDeleteContract()
  const statusMut = useUpdateContractStatus()
  const { data: projects } = useProjects({ current: 1, size: 1000 })

  const projectOptions = useMemo(
    () =>
      (projects?.records || []).map((p) => ({
        value: p.id,
        label: `[#${p.id}] ${p.name}`,
      })),
    [projects],
  )

  const handleSearch = useCallback((values: Record<string, unknown>) => {
    setParams((p) => ({
      ...p,
      current: 1,
      keyword: (values.keyword as string) || undefined,
      status: values.status != null ? Number(values.status) : undefined,
    }))
  }, [])

  const handleReset = useCallback(() => {
    setParams({ current: 1, size: 10 })
  }, [])

  const handlePageChange = useCallback((page: number, size: number) => {
    setParams((p) => ({ ...p, current: page, size }))
  }, [])

  const openCreate = useCallback(() => {
    form.resetFields()
    setCreateOpen(true)
  }, [form])

  const handleCreate = useCallback(async () => {
    try {
      const values = await form.validateFields()
      await createMut.mutateAsync(values)
      message.success('合同创建成功')
      setCreateOpen(false)
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }, [createMut, form, message])

  const openStatusChange = useCallback(
    (record: ContractVO) => {
      setTargetContract(record)
      statusForm.resetFields()
      setStatusOpen(true)
    },
    [statusForm],
  )

  const handleStatusChange = useCallback(async () => {
    if (!targetContract) return
    try {
      const values = await statusForm.validateFields()
      await statusMut.mutateAsync({ id: targetContract.id!, status: values.status })
      message.success('状态更新成功')
      setStatusOpen(false)
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }, [statusForm, statusMut, targetContract, message])

  const handleDelete = useCallback(
    async (record: ContractVO) => {
      try {
        await deleteMut.mutateAsync(record.id!)
        message.success('删除成功')
      } catch (e) {
        if (e instanceof Error) message.error(e.message)
      }
    },
    [deleteMut, message],
  )

  const columns = useMemo<ColumnsType<ContractVO>>(
    () => [
      {
        title: 'ID',
        dataIndex: 'id',
        width: 80,
        render: (id?: number) => (
          <a onClick={() => navigate(`/contract/${id}`)}>#{id}</a>
        ),
      },
      {
        title: '项目',
        dataIndex: 'projectName',
        render: (v?: string) => v || '-',
      },
      {
        title: '金额（万元）',
        dataIndex: 'amount',
        width: 120,
        align: 'right' as const,
        render: (v?: number) => (v == null ? '-' : v.toLocaleString()),
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 100,
        render: (s?: number) => {
          const opt = s != null ? STATUS_MAP.get(s) : undefined
          return opt ? <Tag color={opt.color}>{opt.label}</Tag> : '-'
        },
      },
      {
        title: '创建时间',
        dataIndex: 'createdAt',
        width: 170,
        render: (v?: string) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'),
      },
      {
        title: '操作',
        key: 'actions',
        width: 240,
        fixed: 'right' as const,
        render: (_: unknown, record: ContractVO) => (
          <Space size="small">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => navigate(`/contract/${record.id}`)}
            >
              详情
            </Button>
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => navigate(`/contract/${record.id}/edit`)}
            >
              编辑
            </Button>
            {canUpdateStatus && STATUS_TRANSITIONS[record.status || 0]?.length > 0 && (
              <Button
                type="link"
                size="small"
                onClick={() => openStatusChange(record)}
              >
                变更状态
              </Button>
            )}
            {canDelete && (
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={() => handleDelete(record)}
                loading={deleteMut.isPending && deleteMut.variables === record.id}
              >
                删除
              </Button>
            )}
          </Space>
        ),
      },
    ],
    [navigate, canUpdateStatus, canDelete, deleteMut.isPending, deleteMut.variables, openStatusChange, handleDelete],
  )

  const searchFields = [
    {
      name: 'keyword',
      label: '关键字',
      component: <Input placeholder="项目名 / ID" allowClear />,
    },
    {
      name: 'status',
      label: '状态',
      component: (
        <Select placeholder="请选择状态" allowClear options={STATUS_OPTIONS.map(({ color: _, ...rest }) => rest)} />
      ),
    },
  ]

  const statusTransitions = targetContract
    ? STATUS_TRANSITIONS[targetContract.status || 0] || []
    : []

  return (
    <div>
      <PageHeader
        title="合同管理"
        breadcrumbs={['商务管理', '合同管理']}
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => refetch()}>刷新</Button>
            {canCreate && (
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
                新增合同
              </Button>
            )}
          </Space>
        }
      />

      <SearchForm fields={searchFields} onSearch={handleSearch} onReset={handleReset} />

      <Card>
        <DataTable<ContractVO>
          data={data}
          loading={isLoading}
          columns={columns}
          scroll={{ x: 1000 }}
          onPageChange={handlePageChange}
        />
      </Card>

      <Modal
        title="新增合同"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={handleCreate}
        confirmLoading={createMut.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="projectId"
            label="关联项目"
            rules={[{ required: true, message: '请选择关联项目' }]}
          >
            <Select
              placeholder="请选择项目"
              options={projectOptions}
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item
            name="amount"
            label="合同金额（万元）"
            rules={[{ required: true, message: '请输入合同金额' }]}
          >
            <Input type="number" placeholder="请输入合同金额" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`变更合同 #${targetContract?.id} 状态`}
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
