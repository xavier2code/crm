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
  Tabs,
  Tag,
} from 'antd'
import {
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ProjectOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'

import { DataTable } from '@/components/DataTable'
import { PageHeader } from '@/components/PageHeader'
import { SearchForm } from '@/components/SearchForm'
import { useAuthStore } from '@/stores/auth'
import {
  useApproveOpportunity,
  useDeleteOpportunity,
  useOpportunities,
  useResubmitOpportunity,
  useSubmitOpportunity,
} from '@/hooks/useOpportunities'
import type { OpportunityVO } from '@/api/opportunity'

const STATUS_OPTIONS = [
  { value: 1, label: '草稿' },
  { value: 2, label: '审批中' },
  { value: 3, label: '生效中' },
  { value: 4, label: '报备失败' },
  { value: 5, label: '报备失效' },
]

const STATUS_COLOR: Record<number, string> = {
  1: 'default',
  2: 'processing',
  3: 'success',
  4: 'error',
  5: 'warning',
}

type TabKey = 'my' | 'all' | 'approval'

export default function OpportunityPage() {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const { permissionCodes } = useAuthStore()

  const canCreate = permissionCodes.includes('opportunity:create')
  const canMy = permissionCodes.includes('opportunity:my')
 const canAll = permissionCodes.includes('opportunity:all')
  const canApproval = permissionCodes.includes('opportunity:approval')

  const defaultTab: TabKey = canMy ? 'my' : canAll ? 'all' : canApproval ? 'approval' : 'my'
  const [activeTab, setActiveTab] = useState<TabKey>(defaultTab)

  const [params, setParams] = useState<{
    current: number
    size: number
    status?: number
    keyword?: string
  }>({ current: 1, size: 10 })

  const { data, isLoading, refetch } = useOpportunities(
    activeTab === 'approval' ? { ...params, status: 2 } : params,
  )

  const submitMut = useSubmitOpportunity()
  const resubmitMut = useResubmitOpportunity()
  const approveMut = useApproveOpportunity()
  const deleteMut = useDeleteOpportunity()

  const [approveOpen, setApproveOpen] = useState(false)
  const [approveTarget, setApproveTarget] = useState<OpportunityVO | null>(null)
  const [approveForm] = Form.useForm<{ action: 2 | 3; comment: string }>()

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

  const handleTabChange = useCallback((key: string) => {
    setActiveTab(key as TabKey)
    setParams({ current: 1, size: 10 })
  }, [])

  const handleSubmit = useCallback(
    async (record: OpportunityVO) => {
      try {
        await submitMut.mutateAsync(record.id!)
        message.success('提交审批成功')
      } catch (e) {
        if (e instanceof Error) message.error(e.message)
      }
    },
    [message, submitMut],
  )

  const handleResubmit = useCallback(
    async (record: OpportunityVO) => {
      try {
        await resubmitMut.mutateAsync(record.id!)
        message.success('重提成功，已重新进入审批')
      } catch (e) {
        if (e instanceof Error) message.error(e.message)
      }
    },
    [message, resubmitMut],
  )

  const handleDelete = useCallback(
    async (record: OpportunityVO) => {
      try {
        await deleteMut.mutateAsync(record.id!)
        message.success('删除成功')
        refetch()
      } catch (e) {
        if (e instanceof Error) message.error(e.message)
      }
    },
    [deleteMut, message, refetch],
  )

  const openApprove = useCallback((record: OpportunityVO) => {
    setApproveTarget(record)
    approveForm.resetFields()
    approveForm.setFieldsValue({ action: 2 })
    setApproveOpen(true)
  }, [approveForm])

  const handleApprove = useCallback(async () => {
    if (!approveTarget) return
    try {
      const values = await approveForm.validateFields()
      await approveMut.mutateAsync({ id: approveTarget.id!, data: values })
      message.success(values.action === 2 ? '审批通过' : '已驳回')
      setApproveOpen(false)
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }, [approveForm, approveMut, approveTarget, message])

  const columns = useMemo<ColumnsType<OpportunityVO>>(
    () => [
      {
        title: 'ID',
        dataIndex: 'id',
        width: 80,
        render: (id?: number) => (
          <a onClick={() => navigate(`/opportunity/${id}`)}>#{id}</a>
        ),
      },
      { title: '客户', dataIndex: 'customerName', width: 180, ellipsis: true },
      { title: '业务域', dataIndex: 'businessDomainName', width: 120 },
      { title: '项目类型', dataIndex: 'projectTypeName', width: 100 },
      {
        title: '预计金额',
        dataIndex: 'amount',
        width: 110,
        align: 'right' as const,
        render: (v?: number) => (v == null ? '-' : `${v} 万元`),
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 110,
        render: (v?: number) =>
          v ? <Tag color={STATUS_COLOR[v]}>{STATUS_OPTIONS.find((s) => s.value === v)?.label}</Tag> : '-',
      },
      { title: '提交人', dataIndex: 'submittedByName', width: 120 },
      {
        title: '创建时间',
        dataIndex: 'createdAt',
        width: 170,
        render: (v?: string) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'),
      },
      {
        title: '操作',
        key: 'actions',
        width: 280,
        fixed: 'right' as const,
        render: (_: unknown, record: OpportunityVO) => (
          <Space size="small">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => navigate(`/opportunity/${record.id}`)}
            >
              详情
            </Button>
            {record.editable && permissionCodes.includes('opportunity:edit') && (
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => navigate(`/opportunity/${record.id}/edit`)}
              >
                编辑
              </Button>
            )}
            {record.submittable && permissionCodes.includes('opportunity:submit') && (
              <Button
                type="link"
                size="small"
                icon={<PlayCircleOutlined />}
                onClick={() => handleSubmit(record)}
                loading={submitMut.isPending && submitMut.variables === record.id}
              >
                提交
              </Button>
            )}
            {record.resubmittable && permissionCodes.includes('opportunity:submit') && (
              <Button
                type="link"
                size="small"
                icon={<PlayCircleOutlined />}
                onClick={() => handleResubmit(record)}
                loading={resubmitMut.isPending && resubmitMut.variables === record.id}
              >
                重提
              </Button>
            )}
            {record.approvable && permissionCodes.includes('opportunity:approve') && (
              <Button
                type="link"
                size="small"
                icon={<CheckCircleOutlined />}
                onClick={() => openApprove(record)}
              >
                审批
              </Button>
            )}
            {record.status === 3 && permissionCodes.includes('project:create') && (
              <Button
                type="link"
                size="small"
                icon={<ProjectOutlined />}
                onClick={() => navigate(`/project?createFromOpportunityId=${record.id}`)}
              >
                转项目
              </Button>
            )}
            {record.status === 1 && permissionCodes.includes('opportunity:delete') && (
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
    [
      navigate,
      permissionCodes,
      submitMut.isPending,
      submitMut.variables,
      resubmitMut.isPending,
      resubmitMut.variables,
      deleteMut.isPending,
      deleteMut.variables,
      handleDelete,
      handleResubmit,
      handleSubmit,
      openApprove,
    ],
  )

  const searchFields = useMemo(
    () => [
      {
        name: 'keyword',
        label: '关键字',
        component: <Input placeholder="客户名 / 业务域" allowClear />,
      },
      ...(activeTab !== 'approval'
        ? [
            {
              name: 'status',
              label: '状态',
              component: (
                <Select placeholder="请选择状态" allowClear options={STATUS_OPTIONS} />
              ),
            },
          ]
        : []),
    ],
    [activeTab],
  )

  const tabItems = [
    canMy && { key: 'my', label: '我的报备' },
    canAll && { key: 'all', label: '全部商机' },
    canApproval && { key: 'approval', label: '报备审批' },
  ].filter(Boolean) as { key: string; label: string }[]

  return (
    <div>
      <PageHeader
        title="商机报备"
        breadcrumbs={['业务管理', '商机报备']}
        extra={
          canCreate && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate('/opportunity/create')}
            >
              新建报备
            </Button>
          )
        }
      />

      <Card style={{ marginBottom: 16 }}>
        <Tabs activeKey={activeTab} items={tabItems} onChange={handleTabChange} />
      </Card>

      <SearchForm
        fields={searchFields}
        onSearch={handleSearch}
        onReset={handleReset}
      />

      <Card>
        <DataTable<OpportunityVO>
          data={data}
          loading={isLoading}
          columns={columns}
          scroll={{ x: 1200 }}
          onPageChange={handlePageChange}
        />
      </Card>

      <Modal
        title={`审批报备 #${approveTarget?.id}`}
        open={approveOpen}
        onCancel={() => setApproveOpen(false)}
        onOk={handleApprove}
        confirmLoading={approveMut.isPending}
        destroyOnClose
      >
        <Form form={approveForm} layout="vertical" preserve={false}>
          <Form.Item
            label="审批结果"
            name="action"
            rules={[{ required: true, message: '请选择审批结果' }]}
          >
            <Select
              placeholder="请选择审批结果"
              options={[
                { value: 2, label: '通过' },
                { value: 3, label: '驳回' },
              ]}
            />
          </Form.Item>
          <Form.Item
            label="审批意见"
            name="comment"
            rules={[
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (getFieldValue('action') === 3 && (!value || value.length < 5)) {
                    return Promise.reject(new Error('驳回原因不能少于 5 个字'))
                  }
                  return Promise.resolve()
                },
              }),
            ]}
          >
            <Input.TextArea rows={4} placeholder="通过可留空，驳回须填写原因（至少 5 个字）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
