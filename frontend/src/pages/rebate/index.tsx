import { useMemo, useState } from 'react'
import {
  App,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  type TablePaginationConfig,
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useQuery } from '@tanstack/react-query'

import { PageHeader } from '@/components/PageHeader'
import {
  useCreateRebate,
  useRebates,
  useUpdateConfirmStatus,
  useUpdatePaymentStatus,
  useUpdateRebate,
} from '@/hooks/useRebates'
import { useAuthStore } from '@/stores/auth'
import { listAllChannels, type ChannelVO } from '@/api/admin/channel'
import {
  CONFIRM_STATUS,
  PAYMENT_STATUS,
  REBATE_TYPE,
  REBATE_TYPE_LABEL,
  type RebateRequest,
  type RebateVO,
} from '@/api/rebate'

const { Text } = Typography

/**
 * 渠道管理平台 — 返利查看
 *
 * 业务依据：CRM-渠道版-开发文档.md §9.1 渠道管理平台 + §9.8 返利模块
 *   - 渠道负责人：仅看自己渠道的返利；可改"确认状态"
 *   - CYBD：可看全部 + 创建/编辑/改"付款状态"
 *
 * 字段口径（§9.8 已确认）：
 *   应发 = 业绩总额 × 返利率
 *   实发 = 实际业绩完成额 × 返利率
 */
export default function RebatePage() {
  const { roles } = useAuthStore()
  const isCybd = roles.includes('CYBD')
  const isChannelHead = roles.includes('CHANNEL_HEAD')

  const [params, setParams] = useState<{
    current: number
    size: number
    channelId?: number
    confirmStatus?: number
    paymentStatus?: number
  }>({ current: 1, size: 10 })
  const [createOpen, setCreateOpen] = useState(false)
  const [editing, setEditing] = useState<RebateVO | null>(null)
  const [form] = Form.useForm<RebateRequest>()

  // 渠道下拉（仅 CYBD 可选）
  const channelsQuery = useQuery<ChannelVO[]>({
    queryKey: ['channels', 'all'],
    queryFn: listAllChannels,
    enabled: isCybd,
  })
  const channelOptions = useMemo(
    () =>
      (channelsQuery.data || []).map((c) => ({
        value: c.id,
        label: c.region ? `${c.name}（${c.region}）` : c.name,
      })),
    [channelsQuery.data],
  )

  const { data, isLoading, refetch } = useRebates(params)
  const createMut = useCreateRebate()
  const updateMut = useUpdateRebate()
  const confirmMut = useUpdateConfirmStatus()
  const paymentMut = useUpdatePaymentStatus()

  // 顶部统计：应发/实发/未确认/已付款
  const totals = useMemo(() => {
    const list = data?.records || []
    return list.reduce(
      (acc, r) => {
        acc.total += Number(r.totalAmount ?? 0)
        acc.actual += Number(r.actualAmount ?? 0)
        if (r.confirmStatus === CONFIRM_STATUS.UNCONFIRMED) {
          acc.unconfirmed += Number(r.totalAmount ?? 0)
        }
        if (r.paymentStatus === PAYMENT_STATUS.PAID) {
          acc.paid += Number(r.actualAmount ?? 0)
        }
        return acc
      },
      { total: 0, actual: 0, unconfirmed: 0, paid: 0 },
    )
  }, [data?.records])

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
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({
      confirmStatus: CONFIRM_STATUS.UNCONFIRMED,
      paymentStatus: PAYMENT_STATUS.UNPAID,
      rebateType: REBATE_TYPE.PERFORMANCE,
    } as RebateRequest)
    setCreateOpen(true)
  }

  const openEdit = (record: RebateVO) => {
    setEditing(record)
    form.setFieldsValue({
      id: record.id,
      channelId: record.channelId,
      contractId: record.contractId,
      productCategory: record.productCategory,
      rebateRate: record.rebateRate,
      totalAmount: record.totalAmount,
      actualAmount: record.actualAmount,
      confirmStatus: record.confirmStatus,
      paymentStatus: record.paymentStatus,
      rebateType: record.rebateType,
    })
    setCreateOpen(true)
  }

  const { message } = App.useApp()
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, data: { ...values, id: editing.id } })
        message.success('已更新')
      } else {
        await createMut.mutateAsync(values)
        message.success('已创建')
      }
      setCreateOpen(false)
      refetch()
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const handleConfirmToggle = async (record: RebateVO) => {
    const next =
      record.confirmStatus === CONFIRM_STATUS.CONFIRMED
        ? CONFIRM_STATUS.UNCONFIRMED
        : CONFIRM_STATUS.CONFIRMED
    await confirmMut.mutateAsync({ id: record.id, confirmStatus: next })
    refetch()
  }

  const handlePaymentToggle = async (record: RebateVO) => {
    const next =
      record.paymentStatus === PAYMENT_STATUS.PAID
        ? PAYMENT_STATUS.UNPAID
        : PAYMENT_STATUS.PAID
    await paymentMut.mutateAsync({ id: record.id, paymentStatus: next })
    refetch()
  }

  const columns: ColumnsType<RebateVO> = [
    { title: '渠道', dataIndex: 'channelName', width: 160 },
    {
      title: '产品类别',
      dataIndex: 'productCategory',
      width: 120,
    },
    {
      title: '返利类型',
      dataIndex: 'rebateType',
      width: 130,
      render: (t?: number) => (t ? REBATE_TYPE_LABEL[t] ?? '-' : '-'),
    },
    {
      title: '返利率',
      dataIndex: 'rebateRate',
      width: 100,
      align: 'right' as const,
      render: (v?: number) =>
        v == null ? '-' : `${(Number(v) * 100).toFixed(2)}%`,
    },
    {
      title: '应发',
      dataIndex: 'totalAmount',
      width: 140,
      align: 'right' as const,
      render: (v?: number) => (v == null ? '-' : `¥ ${Number(v).toLocaleString()}`),
    },
    {
      title: '实发',
      dataIndex: 'actualAmount',
      width: 140,
      align: 'right' as const,
      render: (v?: number) => (v == null ? '-' : `¥ ${Number(v).toLocaleString()}`),
    },
    {
      title: '确认状态',
      dataIndex: 'confirmStatus',
      width: 110,
      render: (v?: number) =>
        v === CONFIRM_STATUS.CONFIRMED ? (
          <Tag color="green">已确认</Tag>
        ) : (
          <Tag color="orange">未确认</Tag>
        ),
    },
    {
      title: '付款状态',
      dataIndex: 'paymentStatus',
      width: 110,
      render: (v?: number) =>
        v === PAYMENT_STATUS.PAID ? <Tag color="blue">已付款</Tag> : <Tag>未付款</Tag>,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: (v?: string) => (v ? new Date(v).toLocaleString() : '-'),
    },
    {
      title: '操作',
      key: 'actions',
      width: 280,
      fixed: 'right' as const,
      render: (_, record) => (
        <Space size="small">
          {isChannelHead && (
            <Popconfirm
              title={`切换为${
                record.confirmStatus === CONFIRM_STATUS.CONFIRMED ? '未确认' : '已确认'
              }?`}
              onConfirm={() => handleConfirmToggle(record)}
            >
              <Button type="link" size="small">
                {record.confirmStatus === CONFIRM_STATUS.CONFIRMED ? '撤销确认' : '确认'}
              </Button>
            </Popconfirm>
          )}
          {isCybd && (
            <>
              <Button type="link" size="small" onClick={() => openEdit(record)}>
                编辑
              </Button>
              <Popconfirm
                title={`切换为${
                  record.paymentStatus === PAYMENT_STATUS.PAID ? '未付款' : '已付款'
                }?`}
                onConfirm={() => handlePaymentToggle(record)}
              >
                <Button type="link" size="small">
                  {record.paymentStatus === PAYMENT_STATUS.PAID ? '撤销付款' : '标记已付款'}
                </Button>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="渠道管理平台 — 返利"
        extra={
          isCybd ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              新建返利记录
            </Button>
          ) : null
        }
      />

      <Card style={{ marginBottom: 16 }}>
        <Space size="middle" wrap>
          <Statistic title="应发总额" value={totals.total} prefix="¥" precision={2} />
          <Statistic title="实发总额" value={totals.actual} prefix="¥" precision={2} />
          <Statistic
            title="未确认金额"
            value={totals.unconfirmed}
            prefix="¥"
            precision={2}
            valueStyle={{ color: totals.unconfirmed > 0 ? '#cf1322' : undefined }}
          />
          <Statistic title="已付款金额" value={totals.paid} prefix="¥" precision={2} />
        </Space>
      </Card>

      <Card>
        <Space wrap style={{ marginBottom: 12 }}>
          {isCybd && (
            <Select
              allowClear
              placeholder="按渠道筛选"
              style={{ width: 220 }}
              value={params.channelId}
              options={channelOptions}
              onChange={(v) => setParams((p) => ({ ...p, channelId: v }))}
            />
          )}
          <Select
            allowClear
            placeholder="确认状态"
            style={{ width: 140 }}
            value={params.confirmStatus}
            options={[
              { value: 1, label: '未确认' },
              { value: 2, label: '已确认' },
            ]}
            onChange={(v) => setParams((p) => ({ ...p, confirmStatus: v }))}
          />
          <Select
            allowClear
            placeholder="付款状态"
            style={{ width: 140 }}
            value={params.paymentStatus}
            options={[
              { value: 1, label: '未付款' },
              { value: 2, label: '已付款' },
            ]}
            onChange={(v) => setParams((p) => ({ ...p, paymentStatus: v }))}
          />
          <Button type="primary" onClick={handleSearch}>
            查询
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={() =>
              setParams({ current: 1, size: 10, channelId: undefined, confirmStatus: undefined, paymentStatus: undefined })
            }
          >
            重置
          </Button>
        </Space>
        <Table<RebateVO>
          rowKey="id"
          loading={isLoading}
          columns={columns}
          dataSource={data?.records || []}
          scroll={{ x: 1200 }}
          pagination={{
            current: params.current,
            pageSize: params.size,
            total: data?.total ?? 0,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
          }}
          onChange={handleTableChange}
        />
      </Card>

      <Modal
        title={editing ? '编辑返利记录' : '新建返利记录'}
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={handleSubmit}
        confirmLoading={createMut.isPending || updateMut.isPending}
        okText="保存"
        cancelText="取消"
        width={520}
      >
        <Form<RebateRequest> form={form} layout="vertical" preserve={false}>
          {isCybd && (
            <Form.Item
              label="渠道"
              name="channelId"
              rules={[{ required: true, message: '请选择渠道' }]}
            >
              <Select options={channelOptions} placeholder="请选择渠道" />
            </Form.Item>
          )}
          <Form.Item
            label="产品类别"
            name="productCategory"
            tooltip="字典 code（如 video_surveillance）"
          >
            <Input placeholder="产品类别编码" disabled={!isCybd} />
          </Form.Item>
          <Form.Item label="返利类型" name="rebateType" rules={[{ required: true }]}>
            <Select
              options={Object.entries(REBATE_TYPE_LABEL).map(([k, v]) => ({
                value: Number(k),
                label: v,
              }))}
              disabled={!isCybd}
            />
          </Form.Item>
          <Form.Item label="返利率 (0-1)" name="rebateRate">
            <InputNumber
              min={0}
              max={1}
              step={0.001}
              precision={4}
              style={{ width: '100%' }}
              disabled={!isCybd}
            />
          </Form.Item>
          <Form.Item label="应发金额" name="totalAmount">
            <InputNumber
              min={0}
              step={0.01}
              precision={2}
              style={{ width: '100%' }}
              prefix="¥"
              disabled={!isCybd}
            />
          </Form.Item>
          <Form.Item label="实发金额" name="actualAmount">
            <InputNumber
              min={0}
              step={0.01}
              precision={2}
              style={{ width: '100%' }}
              prefix="¥"
              disabled={!isCybd}
            />
          </Form.Item>
          <Form.Item label="确认状态" name="confirmStatus">
            <Select
              options={[
                { value: 1, label: '未确认' },
                { value: 2, label: '已确认' },
              ]}
            />
          </Form.Item>
          <Form.Item label="付款状态" name="paymentStatus">
            <Select
              options={[
                { value: 1, label: '未付款' },
                { value: 2, label: '已付款' },
              ]}
              disabled={!isCybd}
            />
          </Form.Item>
          <Text type="secondary" style={{ fontSize: 12 }}>
            应发 = 业绩总额 × 返利率；实发 = 实际业绩完成额 × 返利率（§9.8 已确认公式）。
          </Text>
        </Form>
      </Modal>
    </div>
  )
}
