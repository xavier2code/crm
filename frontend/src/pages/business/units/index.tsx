import { useEffect, useMemo, useState } from 'react'
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  message,
  type TablePaginationConfig,
} from 'antd'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'

import {
  assignUnit,
  listUnitAssignments,
  pageUnitAssignments,
  revokeUnitAssignment,
  type UnitAssignRequest,
  type UnitAssignmentVO,
} from '@/api/unitAssignment'
import { getAllUnits } from '@/api/admin/unit'
import type { components } from '@/types/api'
type UnitVO = components['schemas']['UnitVO']
import { listAllChannels, type ChannelVO } from '@/api/admin/channel'
import { listAvailableUsers, type UserMini } from '@/api/admin/channel'

/** assignScope：BD = 大区总→BD；CHANNEL_BD = 渠道负责人→渠道 BD（开发文档 §9.5） */
const SCOPE_OPTIONS = [
  { value: 'BD', label: 'BD', color: 'blue' },
  { value: 'CHANNEL_BD', label: '渠道 BD', color: 'gold' },
] as const
const SCOPE_COLOR: Record<string, string> = Object.fromEntries(
  SCOPE_OPTIONS.map((o) => [o.value, o.color]),
)
const SCOPE_LABEL: Record<string, string> = Object.fromEntries(
  SCOPE_OPTIONS.map((o) => [o.value, o.label]),
)

interface SearchValues {
  keyword?: string
  assignScope?: 'BD' | 'CHANNEL_BD'
  channelId?: number
}

const initialPage = { current: 1, size: 10, total: 0 }

export default function BusinessUnitsPage() {
  const [searchForm] = Form.useForm<SearchValues>()
  const [assignForm] = Form.useForm<UnitAssignRequest>()

  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<UnitAssignmentVO[]>([])
  const [page, setPage] = useState(initialPage)
  const [filters, setFilters] = useState<SearchValues>({})

  const [units, setUnits] = useState<UnitVO[]>([])
  const [channels, setChannels] = useState<ChannelVO[]>([])
  const [availableUsers, setAvailableUsers] = useState<UserMini[]>([])

  const [assignOpen, setAssignOpen] = useState(false)
  const [assignUnitId, setAssignUnitId] = useState<number | null>(null)
  const [assignScope, setAssignScope] = useState<'BD' | 'CHANNEL_BD'>('BD')
  const [assignLoading, setAssignLoading] = useState(false)
  const [preAssignments, setPreAssignments] = useState<UnitAssignmentVO[]>([])

  const unitOptions = useMemo(
    () =>
      units.map((u) => ({ value: u.id, label: u.name, region: u.region })),
    [units],
  )
  const channelOptions = useMemo(
    () => channels.map((c) => ({ value: c.id, label: c.name })),
    [channels],
  )
  const userOptions = useMemo(
    () =>
      availableUsers.map((u) => ({
        value: u.id,
        label: u.realName ? `${u.realName}（${u.username}）` : u.username || `#${u.id}`,
      })),
    [availableUsers],
  )

  const loadData = async (
    next: Partial<SearchValues & { current: number; size: number }> = {},
  ) => {
    const current = next.current ?? page.current
    const size = next.size ?? page.size
    const merged = { ...filters, ...next } as SearchValues
    setLoading(true)
    try {
      const res = await pageUnitAssignments({
        assignScope: merged.assignScope,
        channelId: merged.channelId,
        current,
        size,
      })
      setData(res.records || [])
      setPage({
        current: res.current || current,
        size: res.size || size,
        total: res.total || 0,
      })
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载分配记录失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    getAllUnits().then((res) => setUnits(res as UnitVO[])).catch(() => {})
    listAllChannels()
      .then((res) => setChannels(res as ChannelVO[]))
      .catch(() => {})
    listAvailableUsers()
      .then((res) => setAvailableUsers(res as UserMini[]))
      .catch(() => {})
    loadData({ current: 1 })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const onSearch = (values: SearchValues) => {
    setFilters(values)
    loadData({ current: 1, ...values })
  }

  const onReset = () => {
    searchForm.resetFields()
    setFilters({})
    loadData({ current: 1, keyword: '', assignScope: undefined, channelId: undefined })
  }

  const openAssignModal = async (unitId?: number) => {
    setAssignUnitId(unitId ?? null)
    setAssignScope('BD')
    assignForm.resetFields()
    assignForm.setFieldsValue({ assignScope: 'BD' })
    if (unitId) {
      try {
        const list = await listUnitAssignments(unitId)
        setPreAssignments(list)
      } catch {
        setPreAssignments([])
      }
    } else {
      setPreAssignments([])
    }
    setAssignOpen(true)
  }

  const onAssignSubmit = async () => {
    const values = await assignForm.validateFields()
    if (!assignUnitId) {
      message.warning('请先选择单位')
      return
    }
    setAssignLoading(true)
    try {
      await assignUnit(assignUnitId, values)
      message.success('分配成功')
      setAssignOpen(false)
      loadData({ current: 1 })
    } catch (e) {
      message.error(e instanceof Error ? e.message : '分配失败')
    } finally {
      setAssignLoading(false)
    }
  }

  const onRevoke = async (record: UnitAssignmentVO) => {
    try {
      await revokeUnitAssignment(record.id)
      message.success('已撤销')
      loadData()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '撤销失败')
    }
  }

  const columns = [
    {
      title: '单位',
      dataIndex: 'unitName',
      width: 200,
      render: (v: string | undefined, r: UnitAssignmentVO) => (
        <Space direction="vertical" size={0}>
          <span>{v || `#${r.unitId}`}</span>
          {r.unitRegion && <Tag color="default">{r.unitRegion}</Tag>}
        </Space>
      ),
    },
    {
      title: '被分配人',
      dataIndex: 'realName',
      width: 160,
      render: (v: string | undefined, r: UnitAssignmentVO) => v || r.username || `#${r.userId}`,
    },
    {
      title: '分配范围',
      dataIndex: 'assignScope',
      width: 110,
      render: (v: string) => (
        <Tag color={SCOPE_COLOR[v] || 'default'}>{SCOPE_LABEL[v] || v}</Tag>
      ),
    },
    {
      title: '渠道',
      dataIndex: 'channelName',
      width: 160,
      render: (v: string | undefined) => v || '—',
    },
    {
      title: '分配人',
      dataIndex: 'assignedByName',
      width: 120,
      render: (v: string | undefined) => v || '—',
    },
    {
      title: '分配时间',
      dataIndex: 'assignedAt',
      width: 180,
      render: (v: string | undefined) => (v ? new Date(v).toLocaleString() : '—'),
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      fixed: 'right' as const,
      render: (record: UnitAssignmentVO) => (
        <Space size="small">
          <Popconfirm
            title="确认撤销该分配？"
            okText="撤销"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => onRevoke(record)}
          >
            <a>撤销</a>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const handleTableChange = (p: TablePaginationConfig) => {
    loadData({ current: p.current ?? 1, size: p.pageSize ?? 10 })
  }

  return (
    <Card
      title="单位分配（业务侧）"
      extra={
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => loadData({ current: 1, ...filters })}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openAssignModal()}>
            新建分配
          </Button>
        </Space>
      }
    >
      <Form
        form={searchForm}
        layout="inline"
        onFinish={onSearch}
        style={{ marginBottom: 16, rowGap: 8 }}
      >
        <Form.Item name="keyword" label="关键字">
          <Input placeholder="单位/被分配人" allowClear style={{ width: 200 }} />
        </Form.Item>
        <Form.Item name="assignScope" label="范围">
          <Select
            allowClear
            placeholder="全部"
            options={SCOPE_OPTIONS as unknown as { value: string; label: string }[]}
            style={{ width: 140 }}
          />
        </Form.Item>
        <Form.Item name="channelId" label="渠道">
          <Select
            allowClear
            placeholder="全部"
            options={channelOptions}
            style={{ width: 180 }}
            showSearch
            optionFilterProp="label"
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
              查询
            </Button>
            <Button onClick={onReset}>重置</Button>
          </Space>
        </Form.Item>
      </Form>

      <Table
        rowKey="id"
        loading={loading}
        dataSource={data}
        columns={columns}
        scroll={{ x: 1100 }}
        pagination={{
          current: page.current,
          pageSize: page.size,
          total: page.total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
        }}
        onChange={handleTableChange}
      />

      <Modal
        title="新建单位分配"
        open={assignOpen}
        onCancel={() => setAssignOpen(false)}
        onOk={onAssignSubmit}
        confirmLoading={assignLoading}
        width={520}
        destroyOnClose
      >
        <Form form={assignForm} layout="vertical" preserve={false}>
          <Form.Item
            label="单位"
            required
          >
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="选择单位"
              value={assignUnitId ?? undefined}
              onChange={(v) => setAssignUnitId(v)}
              options={unitOptions}
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
            />
          </Form.Item>
          <Form.Item
            name="assignScope"
            label="分配范围"
            rules={[{ required: true, message: '请选择分配范围' }]}
          >
            <Select
              options={SCOPE_OPTIONS as unknown as { value: string; label: string }[]}
              onChange={(v) => setAssignScope(v as 'BD' | 'CHANNEL_BD')}
            />
          </Form.Item>
          {assignScope === 'CHANNEL_BD' && (
            <Form.Item
              name="channelId"
              label="渠道"
              rules={[{ required: true, message: '请选择渠道' }]}
            >
              <Select
                showSearch
                optionFilterProp="label"
                placeholder="选择渠道"
                options={channelOptions}
              />
            </Form.Item>
          )}
          <Form.Item
            name="userId"
            label="被分配人"
            rules={[{ required: true, message: '请选择被分配人' }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="选择用户"
              options={userOptions}
            />
          </Form.Item>
          {preAssignments.length > 0 && (
            <div style={{ marginTop: 8, color: '#999' }}>
              该单位当前分配：
              {preAssignments.map((p) => (
                <Tag
                  key={p.id}
                  color={SCOPE_COLOR[p.assignScope] || 'default'}
                  style={{ marginLeft: 4 }}
                >
                  {p.realName || p.username} · {SCOPE_LABEL[p.assignScope] || p.assignScope}
                  {p.channelName ? ` · ${p.channelName}` : ''}
                </Tag>
              ))}
            </div>
          )}
        </Form>
      </Modal>
    </Card>
  )
}
