import { useCallback, useEffect, useState } from 'react'
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
  createUnit,
  deleteUnit,
  getUnits,
  updateUnit,
  type UnitRequest,
  type UnitVO,
} from '@/api/admin/unit'

const ADMIN_LEVEL_OPTIONS = [
  { value: 1, label: '省厅' },
  { value: 2, label: '地市' },
  { value: 3, label: '区县' },
]

const ADMIN_LEVEL_LABEL: Record<number, string> = {
  1: '省厅',
  2: '地市',
  3: '区县',
}

interface SearchValues {
  keyword?: string
  region?: string
}

export default function UnitsPage() {
  const [searchForm] = Form.useForm<SearchValues>()
  const [editForm] = Form.useForm<UnitRequest>()

  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<UnitVO[]>([])
  const [current, setCurrent] = useState(1)
  const [size, setSize] = useState(10)
  const [total, setTotal] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [region, setRegion] = useState<string | undefined>()

  const [editing, setEditing] = useState<UnitVO | null>(null)
  const [modalOpen, setModalOpen] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getUnits({
        keyword: keyword || undefined,
        region: region || undefined,
        current,
        size,
      })
      setData(res.records ?? [])
      setTotal(Number(res.total ?? 0))
    } catch (e) {
      message.error((e as Error).message || '加载单位列表失败')
    } finally {
      setLoading(false)
    }
  }, [keyword, region, current, size])

  useEffect(() => {
    void load()
  }, [load])

  const handleSearch = (values: SearchValues) => {
    setKeyword(values.keyword ?? '')
    setRegion(values.region)
    setCurrent(1)
  }

  const handleReset = () => {
    searchForm.resetFields()
    setKeyword('')
    setRegion(undefined)
    setCurrent(1)
  }

  const openCreate = () => {
    setEditing(null)
    editForm.resetFields()
    editForm.setFieldsValue({ status: 1 })
    setModalOpen(true)
  }

  const openEdit = (record: UnitVO) => {
    setEditing(record)
    editForm.resetFields()
    editForm.setFieldsValue({
      name: record.name,
      region: record.region,
      adminLevel: record.adminLevel,
      address: record.address,
      status: record.status,
    })
    setModalOpen(true)
  }

  const handleSave = async () => {
    try {
      const values = await editForm.validateFields()
      if (editing) {
        await updateUnit(Number(editing.id), { ...values, id: editing.id })
        message.success('单位更新成功')
      } else {
        await createUnit(values)
        message.success('单位创建成功')
      }
      setModalOpen(false)
      void load()
    } catch {
      // 校验或接口错误已由 antd / message 提示
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteUnit(id)
      message.success('单位删除成功')
      void load()
    } catch (e) {
      message.error((e as Error).message || '删除失败')
    }
  }

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setCurrent(pagination.current ?? 1)
    setSize(pagination.pageSize ?? 10)
  }

  const columns = [
    {
      title: '单位名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '所属区域',
      dataIndex: 'region',
      key: 'region',
    },
    {
      title: '行政级别',
      dataIndex: 'adminLevel',
      key: 'adminLevel',
      render: (value: number) => ADMIN_LEVEL_LABEL[value] ?? value,
    },
    {
      title: '地址',
      dataIndex: 'address',
      key: 'address',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value: number) =>
        value === 1 ? (
          <Tag color="success">启用</Tag>
        ) : (
          <Tag color="default">停用</Tag>
        ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: UnitVO) => (
        <Space size="small">
          <Button type="link" onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除"
            description={`确定删除单位 "${record.name}" 吗？`}
            onConfirm={() => handleDelete(Number(record.id))}
          >
            <Button type="link" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Card title="单位主数据">
      <Form
        form={searchForm}
        layout="inline"
        onFinish={handleSearch}
        style={{ marginBottom: 16 }}
      >
        <Form.Item name="keyword" label="关键词">
          <Input placeholder="单位名称" allowClear />
        </Form.Item>
        <Form.Item name="region" label="区域">
          <Input placeholder="所属区域" allowClear />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
            查询
          </Button>
        </Form.Item>
        <Form.Item>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
        </Form.Item>
        <Form.Item style={{ marginLeft: 'auto' }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建单位
          </Button>
        </Form.Item>
      </Form>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data}
        pagination={{
          current,
          pageSize: size,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
        }}
        onChange={handleTableChange}
      />

      <Modal
        title={editing ? '编辑单位' : '新建单位'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical" preserve={false}>
          <Form.Item
            name="name"
            label="单位名称"
            rules={[{ required: true, message: '请输入单位名称' }]}
          >
            <Input placeholder="请输入单位名称" />
          </Form.Item>
          <Form.Item name="region" label="所属区域">
            <Input placeholder="请输入所属区域" />
          </Form.Item>
          <Form.Item
            name="adminLevel"
            label="行政级别"
            rules={[{ required: true, message: '请选择行政级别' }]}
          >
            <Select placeholder="请选择行政级别" options={ADMIN_LEVEL_OPTIONS} />
          </Form.Item>
          <Form.Item name="address" label="地址">
            <Input.TextArea rows={2} placeholder="请输入地址" />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            initialValue={1}
          >
            <Select
              options={[
                { value: 1, label: '启用' },
                { value: 0, label: '停用' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
