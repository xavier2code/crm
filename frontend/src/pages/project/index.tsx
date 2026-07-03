import { useMemo, useState } from 'react'
import {
  App,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type TablePaginationConfig,
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'

import { PageHeader } from '@/components/PageHeader'
import { useAuthStore } from '@/stores/auth'
import {
  useCreateProjectFromOpportunity,
  useProjects,
} from '@/hooks/useProjects'
import { getOpportunities } from '@/api/opportunity'
import type { components } from '@/types/api'

const { Text } = Typography

type ProjectVO = components['schemas']['ProjectVO']
type ProjectRequest = components['schemas']['ProjectRequest']
type OpportunityVO = components['schemas']['OpportunityVO']

/**
 * 项目列表
 *
 * 业务依据：CRM-渠道版-开发文档.md §3.6 项目状态机
 *   - 1=项目中 2=项目完成 3=项目中断 4=项目终止（2/4 终态不可逆）
 *   - 渠道BD 仅看自己的项目；CYBD / 渠道负责人 看全部
 *   - 商机状态 = 生效中(3) 时可"商机转项目"
 */
const statusOptions = [
  { value: 1, label: '项目中', color: 'processing' },
  { value: 2, label: '项目完成', color: 'success' },
  { value: 3, label: '项目中断', color: 'warning' },
  { value: 4, label: '项目终止', color: 'error' },
]

const pNodeOptions = [
  { value: 1, label: 'P1 待价值认可' },
  { value: 2, label: 'P2 价值认可' },
  { value: 3, label: 'P3 完成立项签批' },
  { value: 4, label: 'P4 通过党委会' },
  { value: 5, label: 'P5 通过预算批复' },
  { value: 6, label: 'P6 确认采购中标' },
  { value: 7, label: 'P7 完成合同签订' },
  { value: 8, label: 'P8 完成账号开通' },
]

const statusMap = new Map(statusOptions.map((s) => [s.value, s]))

export default function ProjectPage() {
  const navigate = useNavigate()
  const { permissionCodes } = useAuthStore()
  const canCreate = permissionCodes.includes('project:create')

  const [params, setParams] = useState<{
    current: number
    size: number
    status?: number
    pNode?: number
    keyword?: string
  }>({ current: 1, size: 10 })
  const [createOpen, setCreateOpen] = useState(false)
  const [form] = Form.useForm<{ opportunityId: number; name: string; amount?: number }>()

  const { data, isLoading, refetch } = useProjects(params)
  const createFromOppMut = useCreateProjectFromOpportunity()

  // 候选商机：仅加载"生效中"且当前账号可用的，由后端做权限过滤
  const opportunitiesQuery = useQuery<OpportunityVO[]>({
    queryKey: ['opportunities', 'for-create-project'],
    queryFn: async () => {
      const page = await getOpportunities({ current: 1, size: 100, status: 3 })
      return page.records || []
    },
    enabled: createOpen && canCreate,
  })

  const oppOptions = useMemo(
    () =>
      (opportunitiesQuery.data || []).map((o) => ({
        value: o.id,
        label: `[#${o.id}] ${o.customerName ?? ''} · ${o.businessDomainName ?? ''} · ¥${o.amount ?? 0}`,
      })),
    [opportunitiesQuery.data],
  )

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setParams((p) => ({
      ...p,
      current: pagination.current ?? 1,
      size: pagination.pageSize ?? 10,
    }))
  }

  const handleSearch = () => {
    setParams((p) => ({ ...p, current: 1 }))
  }

  const openCreate = () => {
    form.resetFields()
    setCreateOpen(true)
  }

  const { message } = App.useApp()

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      const oppId = values.opportunityId
      const request: ProjectRequest = {
        opportunityId: oppId,
        name: values.name,
        amount: values.amount,
      }
      const projectId = await createFromOppMut.mutateAsync({ oppId, data: request })
      message.success({ content: `已创建项目 #${projectId}`, type: 'success' })
      setCreateOpen(false)
      refetch()
      navigate(`/project/${projectId}`)
    } catch (e) {
      if (e instanceof Error) message.error({ content: e.message, type: 'error' })
    }
  }

  const columns: ColumnsType<ProjectVO> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
      render: (id?: number) => (id ? <a onClick={() => navigate(`/project/${id}`)}>#{id}</a> : '-'),
    },
    {
      title: '项目名称',
      dataIndex: 'name',
      ellipsis: true,
      render: (name?: string, row?: ProjectVO) => (
        <a onClick={() => row?.id && navigate(`/project/${row.id}`)}>{name ?? '-'}</a>
      ),
    },
    { title: '业务域', dataIndex: 'businessDomainName', width: 120, render: (v?: string) => v ?? '-' },
    {
      title: '金额(万元)',
      dataIndex: 'amount',
      width: 110,
      align: 'right' as const,
      render: (v?: number) => (v == null ? '-' : v.toLocaleString()),
    },
    { title: '负责 BD', dataIndex: 'ownerBdName', width: 100, render: (v?: string) => v ?? '-' },
    { title: '销售', dataIndex: 'salesUserName', width: 100, render: (v?: string) => v ?? '-' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (s?: number) => {
        const opt = s != null ? statusMap.get(s) : undefined
        return opt ? <Tag color={opt.color}>{opt.label}</Tag> : '-'
      },
    },
    {
      title: 'P 节点',
      dataIndex: 'pnode',
      width: 140,
      render: (p?: number) => {
        const opt = pNodeOptions.find((o) => o.value === p)
        return opt ? <Text type="secondary">{opt.label}</Text> : '-'
      },
    },
    {
      title: '预计签单',
      dataIndex: 'expectedSignDate',
      width: 110,
      render: (d?: string) => d ?? '-',
    },
  ]

  return (
    <Card>
      <PageHeader
        title="项目列表"
        breadcrumbs={['业务管理', '项目']}
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
              刷新
            </Button>
            {canCreate && (
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
                商机转项目
              </Button>
            )}
          </Space>
        }
      />

      <Space wrap style={{ marginBottom: 16 }}>
        <Input.Search
          allowClear
          placeholder="项目名称 / ID"
          style={{ width: 240 }}
          onSearch={(keyword) => setParams((p) => ({ ...p, keyword, current: 1 }))}
        />
        <Select
          allowClear
          placeholder="项目状态"
          style={{ width: 140 }}
          value={params.status}
          onChange={(status) => setParams((p) => ({ ...p, status, current: 1 }))}
          options={statusOptions.map(({ color: _c, ...rest }) => rest)}
        />
        <Select
          allowClear
          placeholder="P 级节点"
          style={{ width: 180 }}
          value={params.pNode}
          onChange={(pNode) => setParams((p) => ({ ...p, pNode, current: 1 }))}
          options={pNodeOptions}
        />
        <Button onClick={handleSearch}>查询</Button>
        <Button
          onClick={() =>
            setParams({ current: 1, size: params.size, status: undefined, pNode: undefined, keyword: undefined })
          }
        >
          重置
        </Button>
      </Space>

      <Table<ProjectVO>
        rowKey="id"
        loading={isLoading}
        columns={columns}
        dataSource={data?.records ?? []}
        scroll={{ x: 1100 }}
        pagination={{
          current: params.current,
          pageSize: params.size,
          total: data?.total ?? 0,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
        }}
        onChange={handleTableChange}
      />

      <Modal
        title="商机转项目"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={handleCreate}
        confirmLoading={createFromOppMut.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="opportunityId"
            label="选择商机"
            rules={[{ required: true, message: '请选择要转项目的商机' }]}
          >
            <Select
              placeholder="选择生效中的商机"
              loading={opportunitiesQuery.isLoading}
              options={oppOptions}
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item
            name="name"
            label="项目名称"
            rules={[{ required: true, message: '请输入项目名称' }]}
          >
            <Input placeholder="项目名称" maxLength={128} />
          </Form.Item>
          <Form.Item name="amount" label="金额(万元)">
            <Input type="number" placeholder="可留空，沿用商机金额" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
