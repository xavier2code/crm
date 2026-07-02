import { useMemo, useState } from 'react'
import {
  App,
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useQuery } from '@tanstack/react-query'
import dayjs, { type Dayjs } from 'dayjs'

import { PageHeader } from '@/components/PageHeader'
import {
  useDeleteRebateRate,
  useRebateRates,
  useSaveRebateRate,
} from '@/hooks/useRebates'
import { listAllChannels, type ChannelVO } from '@/api/admin/channel'
import { useAuthStore } from '@/stores/auth'
import type { RebateRate, RebateRateRequest } from '@/api/rebate'

interface FormValues {
  productCategory: string
  channelId?: number
  rate: number
  effectiveRange?: [Dayjs, Dayjs]
}

/**
 * 返利率配置
 *
 * 业务依据：CRM-渠道版-开发文档.md §9.8
 *   - 按产品分别配置返利率
 *   - 按渠道可覆盖（不同渠道返利率不同）
 *   - 后续可扩展：阶梯返利
 *
 * 权限：仅 CYBD 可配置（后端 @PreAuthorize 限制）
 */
export default function RebateRatesPage() {
  const { roles } = useAuthStore()
  const isCybd = roles.includes('CYBD')
  const [filterChannelId, setFilterChannelId] = useState<number | undefined>(undefined)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | undefined>(undefined)
  const [form] = Form.useForm<FormValues>()

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

  const { data, isLoading, refetch } = useRebateRates(filterChannelId)
  const saveMut = useSaveRebateRate()
  const deleteMut = useDeleteRebateRate()
  const { message } = App.useApp()

  const openCreate = () => {
    setEditingId(undefined)
    form.resetFields()
    form.setFieldsValue({ rate: 0.05 })
    setModalOpen(true)
  }

  const openEdit = (record: RebateRate) => {
    setEditingId(record.id)
    form.setFieldsValue({
      productCategory: record.productCategory,
      channelId: record.channelId,
      rate: record.rate,
      effectiveRange:
        record.effectiveFrom && record.effectiveTo
          ? [dayjs(record.effectiveFrom), dayjs(record.effectiveTo)]
          : undefined,
    })
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    try {
      const v = await form.validateFields()
      const req: RebateRateRequest = {
        id: editingId,
        productCategory: v.productCategory,
        channelId: v.channelId,
        rate: v.rate,
        effectiveFrom: v.effectiveRange?.[0]?.format('YYYY-MM-DD'),
        effectiveTo: v.effectiveRange?.[1]?.format('YYYY-MM-DD'),
      }
      await saveMut.mutateAsync(req)
      message.success('已保存')
      setModalOpen(false)
      refetch()
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const handleDelete = async (id: number) => {
    await deleteMut.mutateAsync(id)
    message.success('已删除')
    refetch()
  }

  const columns: ColumnsType<RebateRate> = [
    {
      title: '产品类别',
      dataIndex: 'productCategory',
      width: 200,
    },
    {
      title: '渠道',
      dataIndex: 'channelId',
      width: 200,
      render: (id?: number) => {
        if (id == null) return <span style={{ color: '#999' }}>全部渠道（默认）</span>
        const ch = (channelsQuery.data || []).find((c) => c.id === id)
        return ch ? ch.name : `#${id}`
      },
    },
    {
      title: '返利率',
      dataIndex: 'rate',
      width: 120,
      align: 'right' as const,
      render: (v?: number) =>
        v == null ? '-' : `${(Number(v) * 100).toFixed(2)}%`,
    },
    {
      title: '生效起',
      dataIndex: 'effectiveFrom',
      width: 120,
      render: (v?: string) => v || '-',
    },
    {
      title: '生效止',
      dataIndex: 'effectiveTo',
      width: 120,
      render: (v?: string) => v || '-',
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
      width: 160,
      fixed: 'right' as const,
      render: (_, record) =>
        isCybd ? (
          <Space size="small">
            <Button type="link" size="small" onClick={() => openEdit(record)}>
              编辑
            </Button>
            <Popconfirm title="确认删除该返利率配置?" onConfirm={() => handleDelete(record.id)}>
              <Button type="link" size="small" danger>
                删除
              </Button>
            </Popconfirm>
          </Space>
        ) : null,
    },
  ]

  return (
    <div>
      <PageHeader
        title="返利率配置"
        extra={
          isCybd ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              新建配置
            </Button>
          ) : null
        }
      />
      <Card>
        <Space wrap style={{ marginBottom: 12 }}>
          <Select
            allowClear
            placeholder="按渠道筛选"
            style={{ width: 220 }}
            value={filterChannelId}
            options={[{ value: undefined as unknown as number, label: '全部（默认配置）' }, ...channelOptions]}
            onChange={(v) => setFilterChannelId(v)}
          />
          <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
            刷新
          </Button>
        </Space>
        <Table<RebateRate>
          rowKey="id"
          loading={isLoading}
          columns={columns}
          dataSource={data || []}
          pagination={false}
          size="small"
          locale={{ emptyText: '暂无配置' }}
        />
      </Card>

      <Modal
        title={editingId ? '编辑返利率' : '新建返利率'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={saveMut.isPending}
        width={480}
      >
        <Form<FormValues> form={form} layout="vertical" preserve={false}>
          <Form.Item
            label="产品类别"
            name="productCategory"
            rules={[{ required: true, message: '请输入产品类别' }]}
            tooltip="字典 code（如 video_surveillance、ai_platform）"
          >
            <Input placeholder="产品类别编码" />
          </Form.Item>
          <Form.Item label="渠道（留空=全部渠道默认值）" name="channelId">
            <Select
              allowClear
              placeholder="不选表示全部渠道"
              options={channelOptions}
            />
          </Form.Item>
          <Form.Item
            label="返利率"
            name="rate"
            rules={[{ required: true, message: '请输入返利率' }]}
          >
            <InputNumber min={0} max={1} step={0.001} precision={4} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="生效区间" name="effectiveRange">
            <DatePicker.RangePicker style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
