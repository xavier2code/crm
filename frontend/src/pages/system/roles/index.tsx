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
} from 'antd'
import type { TableColumnsType } from 'antd'

import {
  createRole,
  deleteRole,
  getRoles,
  updateRole,
  type RoleRequest,
  type RoleVO,
} from '@/api/admin/role'
import {
  DATA_SCOPE_DIMENSION_LABEL,
  DATA_SCOPE_DIMENSIONS,
  type DataScopeDimensionCode,
} from '@/api/admin/dataPermission'

const DIMENSION_OPTIONS: { value: DataScopeDimensionCode; label: string }[] = (
  Object.values(DATA_SCOPE_DIMENSIONS) as DataScopeDimensionCode[]
).map((d) => ({ value: d, label: `${d} - ${DATA_SCOPE_DIMENSION_LABEL[d]}` }))

export default function RolesPage() {
  const [records, setRecords] = useState<RoleVO[]>([])
  const [loading, setLoading] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [editingRole, setEditingRole] = useState<RoleVO | null>(null)
  const [form] = Form.useForm<RoleRequest>()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setRecords(await getRoles())
    } catch (e) {
      message.error((e as Error).message || '加载角色失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const openCreate = () => {
    setEditingRole(null)
    form.resetFields()
    form.setFieldsValue({ dataScopeType: DATA_SCOPE_DIMENSIONS.SELF })
    setEditOpen(true)
  }

  const openEdit = (role: RoleVO) => {
    setEditingRole(role)
    form.resetFields()
    form.setFieldsValue({
      code: role.code,
      name: role.name,
      dataScopeType: (role.dataScopeType as DataScopeDimensionCode) ?? DATA_SCOPE_DIMENSIONS.SELF,
      operationCodes: role.operationCodes ?? [],
      menuIds: role.menuIds ?? [],
    })
    setEditOpen(true)
  }

  const submit = async () => {
    const values = await form.validateFields()
    try {
      if (editingRole) {
        await updateRole(editingRole.id!, values)
        message.success('已更新')
      } else {
        await createRole(values)
        message.success('已创建')
      }
      setEditOpen(false)
      void load()
    } catch (e) {
      message.error((e as Error).message || '保存失败')
    }
  }

  const onDelete = async (role: RoleVO) => {
    try {
      await deleteRole(role.id!)
      message.success('已删除')
      void load()
    } catch (e) {
      message.error((e as Error).message || '删除失败')
    }
  }

  const columns: TableColumnsType<RoleVO> = [
    { title: '编码', dataIndex: 'code', width: 160 },
    { title: '名称', dataIndex: 'name', width: 200 },
    {
      title: '默认数据维度',
      dataIndex: 'dataScopeType',
      width: 180,
      render: (v?: string) => {
        if (!v) return '-'
        const label = DATA_SCOPE_DIMENSION_LABEL[v as DataScopeDimensionCode] ?? v
        return <Tag color="blue">{label}</Tag>
      },
    },
    {
      title: '类型',
      dataIndex: 'isBuiltin',
      width: 100,
      render: (v?: number) =>
        v === 1 ? <Tag color="orange">内置</Tag> : <Tag>自定义</Tag>,
    },
    {
      title: '操作',
      fixed: 'right',
      width: 180,
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            disabled={record.isBuiltin === 1}
            onClick={() => openEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="删除该角色？"
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => void onDelete(record)}
          >
            <Button size="small" danger disabled={record.isBuiltin === 1}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Card
      title="角色管理"
      extra={
        <Button type="primary" onClick={openCreate}>
          新建角色
        </Button>
      }
    >
      <Table<RoleVO>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={records}
        pagination={false}
      />
      <Modal
        title={editingRole ? '编辑角色' : '新建角色'}
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={() => void submit()}
        destroyOnClose
        width={560}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            label="角色编码"
            name="code"
            rules={[{ required: true, message: '请输入角色编码' }]}
          >
            <Input disabled={!!editingRole} placeholder="例如 CYBD / CHANNEL_HEAD" />
          </Form.Item>
          <Form.Item
            label="角色名称"
            name="name"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input placeholder="显示用名称" />
          </Form.Item>
          <Form.Item label="默认数据维度" name="dataScopeType">
            <Select
              options={DIMENSION_OPTIONS}
              placeholder="选择默认数据权限维度"
            />
          </Form.Item>
          <Form.Item
            label="操作权限编码"
            name="operationCodes"
            extra="例如 user:create、order:approve。多个以换行或逗号分隔。"
          >
            <Input.TextArea
              rows={4}
              placeholder="user:create&#10;order:approve"
              onChange={(e) => {
                const lines = e.target.value
                  .split(/[\n,]/g)
                  .map((s) => s.trim())
                  .filter(Boolean)
                form.setFieldValue('operationCodes', lines)
              }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
