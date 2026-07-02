import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { App, Button, Card, Input, Modal, Popconfirm, Select, Space, Tag } from 'antd'
import { EditOutlined, EyeOutlined, PlusOutlined, UserSwitchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'

import { DataTable } from '@/components/DataTable'
import { DictTag } from '@/components/DictTag'
import { PageHeader } from '@/components/PageHeader'
import { SearchForm } from '@/components/SearchForm'
import { useUsers } from '@/hooks/useAdminUsers'
import { useCustomers, useDeleteCustomer, useAssignCustomer } from '@/hooks/useCustomers'
import { maskPhone } from '@/utils/mask'
import type { CustomerVO } from '@/api/customer'

const LAYER_COLOR: Record<string, string> = {
  A: 'red',
  B: 'orange',
  C: 'blue',
}

export default function CustomerPage() {
  const navigate = useNavigate()
  const { message } = App.useApp()

  const [params, setParams] = useState({
    current: 1,
    size: 10,
    keyword: '',
  })

  const { data, isLoading, refetch } = useCustomers({
    current: params.current,
    size: params.size,
    keyword: params.keyword || undefined,
  })

  const deleteMut = useDeleteCustomer()
  const assignMut = useAssignCustomer()
  const { data: users } = useUsers({ current: 1, size: 1000 })

  const [assignOpen, setAssignOpen] = useState(false)
  const [assignCustomer, setAssignCustomer] = useState<CustomerVO | null>(null)
  const [assignUserId, setAssignUserId] = useState<number | undefined>()

  const handleSearch = (values: Record<string, unknown>) => {
    setParams((p) => ({
      ...p,
      current: 1,
      keyword: (values.keyword as string) || '',
    }))
  }

  const handleReset = () => {
    setParams({ current: 1, size: 10, keyword: '' })
  }

  const handlePageChange = (page: number, size: number) => {
    setParams((p) => ({ ...p, current: page, size }))
  }

  const openAssign = (record: CustomerVO) => {
    setAssignCustomer(record)
    setAssignUserId(record.ownerUserId)
    setAssignOpen(true)
  }

  const handleAssign = async () => {
    if (!assignCustomer || !assignUserId) {
      message.warning('请选择跟进人')
      return
    }
    try {
      await assignMut.mutateAsync({ id: assignCustomer.id!, userId: assignUserId })
      message.success('分配成功')
      setAssignOpen(false)
      refetch()
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const handleDelete = async (record: CustomerVO) => {
    try {
      await deleteMut.mutateAsync(record.id!)
      message.success('删除成功')
      refetch()
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const columns: ColumnsType<CustomerVO> = [
    { title: '客户名称', dataIndex: 'name', width: 220, ellipsis: true },
    {
      title: '单位',
      dataIndex: 'unitName',
      width: 200,
      render: (v?: string) => v || '-',
    },
    {
      title: '警种',
      dataIndex: 'policeType',
      width: 120,
      render: (v?: string) => <DictTag type="police_type" value={v} />,
    },
    {
      title: '客户分层',
      dataIndex: 'customerLayer',
      width: 110,
      align: 'center' as const,
      render: (v?: string) =>
        v ? <Tag color={LAYER_COLOR[v] || 'default'}>{v}类</Tag> : '-',
    },
    {
      title: '跟进人',
      dataIndex: 'ownerUserName',
      width: 120,
      render: (v?: string) => v || <span style={{ color: '#999' }}>未分配</span>,
    },
    {
      title: '区域',
      dataIndex: 'region',
      width: 120,
      render: (v?: string) => v || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v?: number) =>
        v === 0 ? <Tag color="default">停用</Tag> : <Tag color="green">正常</Tag>,
    },
    {
      title: '主联系人',
      dataIndex: 'contacts',
      width: 160,
      render: (contacts?: CustomerVO['contacts']) => {
        const primary = contacts?.find((c) => c.isPrimary === 1)
        if (!primary) return <span style={{ color: '#999' }}>-</span>
        return (
          <span>
            {primary.name} {maskPhone(primary.phone)}
          </span>
        )
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
      render: (_: unknown, record: CustomerVO) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/customer/${record.id}`)}
          >
            详情
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => navigate(`/customer/${record.id}/edit`)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            icon={<UserSwitchOutlined />}
            onClick={() => openAssign(record)}
          >
            分配
          </Button>
          <Popconfirm
            title="确认删除该客户?"
            description="删除后不可恢复，且会级联删除联系人。"
            okText="删除"
            okType="danger"
            cancelText="取消"
            onConfirm={() => handleDelete(record)}
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="客户管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/customer/create')}>
            新增客户
          </Button>
        }
      />

      <SearchForm
        fields={[
          {
            name: 'keyword',
            label: '关键字',
            component: <Input placeholder="客户名 / 单位名" allowClear />,
          },
        ]}
        onSearch={handleSearch}
        onReset={handleReset}
      />

      <Card>
        <DataTable<CustomerVO>
          data={data}
          loading={isLoading}
          columns={columns}
          scroll={{ x: 1300 }}
          onPageChange={handlePageChange}
        />
      </Card>

      <Modal
        title={assignCustomer ? `分配客户：${assignCustomer.name}` : '分配客户'}
        open={assignOpen}
        onCancel={() => setAssignOpen(false)}
        onOk={handleAssign}
        confirmLoading={assignMut.isPending}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <div>
            <strong>客户：</strong>
            {assignCustomer?.name}
          </div>
          <div>
            <strong>当前跟进人：</strong>
            {assignCustomer?.ownerUserName || '未分配'}
          </div>
          <div>
            <strong>新跟进人：</strong>
            <Select
              placeholder="请选择跟进人"
              style={{ width: '100%' }}
              value={assignUserId}
              onChange={setAssignUserId}
              options={(users?.records || []).map((u) => ({
                value: u.id,
                label: `${u.realName || u.username} (${u.username})`,
              }))}
              showSearch
              optionFilterProp="label"
            />
          </div>
        </Space>
      </Modal>
    </div>
  )
}
