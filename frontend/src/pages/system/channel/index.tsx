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
  Switch,
  Table,
  Tabs,
  Tag,
  message,
  type TablePaginationConfig,
} from 'antd'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'

import {
  assignChannel,
  createChannel,
  deleteChannel,
  listAvailableUsers,
  listChannelAssignments,
  pageChannels,
  revokeChannelAssignment,
  updateChannel,
  type ChannelAssignmentVO,
  type ChannelRequest,
  type ChannelVO,
  type UserMini,
} from '@/api/admin/channel'
import { useDictStore } from '@/stores/dict'

/* assignType：1=渠道负责人 2=渠道 BD  （CRM-渠道版-开发文档.md §16.1 / §21.2） */
const ASSIGN_TYPE_OPTIONS = [
  { value: 1, label: '渠道负责人', color: 'gold' },
  { value: 2, label: '渠道 BD', color: 'blue' },
]
const ASSIGN_TYPE_LABEL = Object.fromEntries(
  ASSIGN_TYPE_OPTIONS.map((o) => [o.value, o.label]),
) as Record<number, string>
const ASSIGN_TYPE_COLOR = Object.fromEntries(
  ASSIGN_TYPE_OPTIONS.map((o) => [o.value, o.color]),
) as Record<number, string>

interface SearchValues {
  keyword?: string
  region?: string
}

interface PageState {
  current: number
  size: number
  total: number
}

const initialPage: PageState = { current: 1, size: 10, total: 0 }

export default function ChannelPage() {
  const [searchForm] = Form.useForm<SearchValues>()
  const [editForm] = Form.useForm<ChannelRequest>()
  const getDict = useDictStore((s) => s.getDict)

  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<ChannelVO[]>([])
  const [page, setPage] = useState<PageState>(initialPage)
  const [keyword, setKeyword] = useState('')
  const [region, setRegion] = useState<string | undefined>()

  const [editing, setEditing] = useState<ChannelVO | null>(null)
  const [modalOpen, setModalOpen] = useState(false)

  // 分配抽屉 / 弹窗
  const [assignChannel4, setAssignChannel4] = useState<ChannelVO | null>(null)
  const [assignOpen, setAssignOpen] = useState(false)
  const [assignType, setAssignType] = useState<number>(1)
  const [assignments, setAssignments] = useState<ChannelAssignmentVO[]>([])
  const [assignLoading, setAssignLoading] = useState(false)
  const [availableUsers, setAvailableUsers] = useState<UserMini[]>([])
  const [newUserId, setNewUserId] = useState<number | undefined>()

  const regionOptions = useMemo(
    () => getDict('region').map((d) => ({ value: d.code, label: d.name })),
    [getDict],
  )

  const fetchData = async (
    next: { current?: number; size?: number; keyword?: string; region?: string } = {},
  ) => {
    const current = next.current ?? page.current
    const size = next.size ?? page.size
    const kw = next.keyword ?? keyword
    const rg = next.region ?? region
    setLoading(true)
    try {
      const res = await pageChannels({
        keyword: kw || undefined,
        region: rg,
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
      message.error(e instanceof Error ? e.message : '加载渠道列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData({ current: 1, keyword: '', region: undefined })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleSearch = (values: SearchValues) => {
    setKeyword(values.keyword || '')
    setRegion(values.region)
    fetchData({
      current: 1,
      keyword: values.keyword || '',
      region: values.region,
    })
  }

  const handleResetSearch = () => {
    searchForm.resetFields()
    setKeyword('')
    setRegion(undefined)
    fetchData({ current: 1, keyword: '', region: undefined })
  }

  const handleTableChange = (pagination: TablePaginationConfig) => {
    fetchData({ current: pagination.current, size: pagination.pageSize })
  }

  /* ========== 渠道 CRUD ========== */

  const openCreate = () => {
    setEditing(null)
    editForm.resetFields()
    editForm.setFieldsValue({ status: 1 } as ChannelRequest)
    setModalOpen(true)
  }

  const openEdit = (record: ChannelVO) => {
    setEditing(record)
    editForm.setFieldsValue({
      id: record.id,
      name: record.name,
      region: record.region,
      status: record.status ?? 1,
    } as ChannelRequest)
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await editForm.validateFields()
      if (editing) {
        await updateChannel(editing.id, { ...values, id: editing.id })
        message.success('保存成功')
      } else {
        await createChannel(values)
        message.success('创建成功')
      }
      setModalOpen(false)
      fetchData()
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const handleDelete = (record: ChannelVO) => {
    Modal.confirm({
      title: `确认删除渠道「${record.name}」?`,
      content: '渠道已被用户绑定时需先解除绑定。',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await deleteChannel(record.id)
          message.success('删除成功')
          fetchData()
        } catch (e) {
          if (e instanceof Error) message.error(e.message)
        }
      },
    })
  }

  /* ========== 分配管理 ========== */

  const openAssignModal = async (record: ChannelVO) => {
    setAssignChannel4(record)
    setAssignType(1)
    setNewUserId(undefined)
    setAssignOpen(true)
    await Promise.all([loadAssignments(record.id, 1), loadAvailableUsers()])
  }

  const loadAssignments = async (channelId: number, type: number) => {
    setAssignLoading(true)
    try {
      const list = await listChannelAssignments(channelId, { assignType: type })
      setAssignments(list || [])
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载分配记录失败')
    } finally {
      setAssignLoading(false)
    }
  }

  const loadAvailableUsers = async () => {
    try {
      const users = await listAvailableUsers()
      setAvailableUsers(users || [])
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载可选用户失败')
    }
  }

  const handleAssignSubmit = async () => {
    if (!assignChannel4 || !newUserId) {
      message.warning('请选择要分配的用户')
      return
    }
    try {
      await assignChannel(assignChannel4.id, {
        userId: newUserId,
        assignType,
      })
      message.success('分配成功')
      setNewUserId(undefined)
      loadAssignments(assignChannel4.id, assignType)
      // 同步刷新渠道列表的 heads/bds 计数
      fetchData({ current: page.current })
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const handleRevoke = (record: ChannelAssignmentVO) => {
    if (!assignChannel4) return
    Modal.confirm({
      title: `确认撤销「${record.realName || record.username}」的${
        ASSIGN_TYPE_LABEL[record.assignType]
      }身份?`,
      content: '该操作会硬删除分配记录，且不可恢复。',
      okText: '撤销',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await revokeChannelAssignment(
            assignChannel4.id,
            record.userId,
            record.assignType,
          )
          message.success('已撤销')
          loadAssignments(assignChannel4.id, assignType)
          fetchData({ current: page.current })
        } catch (e) {
          if (e instanceof Error) message.error(e.message)
        }
      },
    })
  }

  const handleTabChange = async (key: string) => {
    const t = Number(key)
    setAssignType(t)
    if (assignChannel4) {
      await loadAssignments(assignChannel4.id, t)
    }
  }

  /* ========== 列定义 ========== */

  const columns = useMemo(
    () => [
      { title: 'ID', dataIndex: 'id', width: 80 },
      { title: '渠道名称', dataIndex: 'name', width: 200 },
      {
        title: '所属区域',
        dataIndex: 'region',
        width: 120,
        render: (v?: string) => {
          if (!v) return <span style={{ color: '#999' }}>-</span>
          const item = regionOptions.find((o) => o.value === v)
          return item?.label || v
        },
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 90,
        render: (v?: number) =>
          v === 0 ? <Tag>停用</Tag> : <Tag color="green">启用</Tag>,
      },
      {
        title: '渠道负责人',
        dataIndex: 'heads',
        width: 220,
        render: (list?: UserMini[]) =>
          list && list.length ? (
            <Space size={4} wrap>
              {list.map((u) => (
                <Tag key={u.id} color="gold">
                  {u.realName || u.username}
                </Tag>
              ))}
            </Space>
          ) : (
            <span style={{ color: '#999' }}>未分配</span>
          ),
      },
      {
        title: '渠道 BD',
        dataIndex: 'bds',
        width: 220,
        render: (list?: UserMini[]) =>
          list && list.length ? (
            <Space size={4} wrap>
              {list.map((u) => (
                <Tag key={u.id} color="blue">
                  {u.realName || u.username}
                </Tag>
              ))}
            </Space>
          ) : (
            <span style={{ color: '#999' }}>未分配</span>
          ),
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
        width: 200,
        fixed: 'right' as const,
        render: (_: unknown, record: ChannelVO) => (
          <Space size="small">
            <Button type="link" size="small" onClick={() => openEdit(record)}>
              编辑
            </Button>
            <Button type="link" size="small" onClick={() => openAssignModal(record)}>
              分配
            </Button>
            <Popconfirm
              title="确认删除该渠道?"
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
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [regionOptions],
  )

  return (
    <Card
      title="渠道分配"
      extra={
        <Space>
          <Form form={searchForm} layout="inline" onFinish={handleSearch}>
            <Form.Item name="keyword">
              <Input
                placeholder="渠道名 / 区域"
                allowClear
                prefix={<SearchOutlined />}
                style={{ width: 200 }}
              />
            </Form.Item>
            <Form.Item name="region">
              <Select
                placeholder="所属区域"
                allowClear
                style={{ width: 160 }}
                options={regionOptions}
                showSearch
                optionFilterProp="label"
              />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">
                  查询
                </Button>
                <Button icon={<ReloadOutlined />} onClick={handleResetSearch}>
                  重置
                </Button>
              </Space>
            </Form.Item>
          </Form>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建渠道
          </Button>
        </Space>
      }
    >
      <Table<ChannelVO>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data}
        scroll={{ x: 1200 }}
        pagination={{
          current: page.current,
          pageSize: page.size,
          total: page.total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (t) => `共 ${t} 条`,
        }}
        onChange={handleTableChange}
      />

      <Modal
        title={editing ? '编辑渠道' : '新建渠道'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        okText="保存"
        cancelText="取消"
        destroyOnClose
        width={520}
      >
        <Form<ChannelRequest>
          form={editForm}
          layout="vertical"
          initialValues={{ status: 1 }}
          preserve={false}
        >
          <Form.Item
            label="渠道名称"
            name="name"
            rules={[{ required: true, message: '请输入渠道名称' }, { max: 128, message: '长度不超过 128' }]}
          >
            <Input placeholder="请输入渠道名称" />
          </Form.Item>
          <Form.Item label="所属区域" name="region">
            <Select
              placeholder="选择区域"
              allowClear
              showSearch
              optionFilterProp="label"
              options={regionOptions}
            />
          </Form.Item>
          <Form.Item
            label="状态"
            name="status"
            valuePropName="checked"
            getValueFromEvent={(v) => (v ? 1 : 0)}
            getValueProps={(v) => ({ checked: v === 1 })}
          >
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={
          assignChannel4
            ? `渠道分配 - ${assignChannel4.name}`
            : '渠道分配'
        }
        open={assignOpen}
        onCancel={() => setAssignOpen(false)}
        footer={null}
        width={720}
        destroyOnClose
      >
        <Tabs
          activeKey={String(assignType)}
          onChange={handleTabChange}
          items={ASSIGN_TYPE_OPTIONS.map((o) => ({
            key: String(o.value),
            label: o.label,
          }))}
        />

        <Space style={{ marginBottom: 12, width: '100%' }} size="middle">
          <Select
            placeholder="选择用户"
            showSearch
            optionFilterProp="label"
            value={newUserId}
            onChange={setNewUserId}
            style={{ width: 280 }}
            options={availableUsers
              .filter(
                (u) => !assignments.some((a) => a.userId === u.id),
              )
              .map((u) => ({
                value: u.id,
                label: `${u.realName || u.username} (${u.username})`,
              }))}
          />
          <Button type="primary" onClick={handleAssignSubmit}>
            添加{ASSIGN_TYPE_LABEL[assignType]}
          </Button>
        </Space>

        <Table<ChannelAssignmentVO>
          rowKey={(r) => `${r.userId}-${r.assignType}`}
          loading={assignLoading}
          size="small"
          pagination={false}
          dataSource={assignments}
          columns={[
            {
              title: '用户',
              dataIndex: 'realName',
              render: (_: unknown, r) => r.realName || r.username,
            },
            { title: '用户名', dataIndex: 'username', width: 140 },
            {
              title: '身份',
              dataIndex: 'assignType',
              width: 120,
              render: (v: number) => (
                <Tag color={ASSIGN_TYPE_COLOR[v]}>{ASSIGN_TYPE_LABEL[v]}</Tag>
              ),
            },
            {
              title: '分配人',
              dataIndex: 'assignedByName',
              width: 120,
              render: (v?: string) => v || '-',
            },
            {
              title: '分配时间',
              dataIndex: 'assignedAt',
              width: 170,
              render: (v?: string) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'),
            },
            {
              title: '操作',
              key: 'actions',
              width: 100,
              render: (_: unknown, r) => (
                <Popconfirm
                  title="确认撤销该身份?"
                  okText="撤销"
                  okType="danger"
                  cancelText="取消"
                  onConfirm={() => handleRevoke(r)}
                >
                  <Button type="link" size="small" danger>
                    撤销
                  </Button>
                </Popconfirm>
              ),
            },
          ]}
        />
      </Modal>
    </Card>
  )
}
