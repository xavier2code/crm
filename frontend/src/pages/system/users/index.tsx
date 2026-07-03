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
  Switch,
  Table,
  Tag,
  message,
} from 'antd'
import type { TableColumnsType } from 'antd'

import {
  createUser,
  deleteUser,
  getUser,
  getUsers,
  resetPassword,
  updateUser,
  updateUserStatus,
  type UserRequest,
  type UserVO,
} from '@/api/admin/user'
import {
  DATA_SCOPE_DIMENSION_LABEL,
  DATA_SCOPE_DIMENSIONS,
  getUserDataPermissions,
  updateUserDataPermissions,
  type DataPermissionUpdateRequest,
  type DataPermissionVO,
  type DataScopeDimensionCode,
} from '@/api/admin/dataPermission'
import { getRoles } from '@/api/admin/role'
import type { RoleVO } from '@/api/admin/role'

const DIMENSION_ORDER: DataScopeDimensionCode[] = [
  DATA_SCOPE_DIMENSIONS.ALL,
  DATA_SCOPE_DIMENSIONS.CHANNEL,
  DATA_SCOPE_DIMENSIONS.REGION,
  DATA_SCOPE_DIMENSIONS.UNIT,
  DATA_SCOPE_DIMENSIONS.BUSINESS_DOMAIN,
  DATA_SCOPE_DIMENSIONS.POLICE_TYPE,
  DATA_SCOPE_DIMENSIONS.SELF,
]

const DIMENSION_VALUE_LABEL: Record<DataScopeDimensionCode, string> = {
  ALL: '授权值（ALL/SELF 无需填写）',
  CHANNEL: '渠道 ID（每行一个）',
  REGION: '区域代码（每行一个）',
  UNIT: '单位 ID（每行一个）',
  BUSINESS_DOMAIN: '业务域代码（每行一个）',
  POLICE_TYPE: '警种代码（每行一个）',
  SELF: '授权值（SELF 无需填写）',
}

export default function UsersPage() {
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState({ current: 1, size: 10, total: 0 })
  const [records, setRecords] = useState<UserVO[]>([])
  const [loading, setLoading] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<UserVO | null>(null)
  const [form] = Form.useForm<UserRequest>()
  const [roles, setRoles] = useState<RoleVO[]>([])

  // 数据权限弹窗
  const [scopeOpen, setScopeOpen] = useState(false)
  const [scopeUser, setScopeUser] = useState<UserVO | null>(null)
  const [scopeList, setScopeList] = useState<DataPermissionVO[]>([])
  const [scopeLoading, setScopeLoading] = useState(false)
  const [scopeForm] = Form.useForm<{ scopeType: DataScopeDimensionCode; valuesText: string }>()

  const loadRoles = useCallback(async () => {
    try {
      setRoles(await getRoles())
    } catch {
      // 角色加载失败不阻塞主流程
    }
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getUsers({
        keyword: keyword || undefined,
        current: page.current,
        size: page.size,
      })
      setRecords(res.records ?? [])
      setPage((p) => ({ ...p, total: res.total ?? 0 }))
    } catch (e) {
      message.error((e as Error).message || '加载用户失败')
    } finally {
      setLoading(false)
    }
  }, [keyword, page])

  useEffect(() => {
    void loadRoles()
  }, [loadRoles])

  useEffect(() => {
    void load()
  }, [load])

  const openCreate = () => {
    setEditingUser(null)
    form.resetFields()
    form.setFieldsValue({ status: 1, roleIds: [] })
    setEditOpen(true)
  }

  const openEdit = async (user: UserVO) => {
    setEditingUser(user)
    form.resetFields()
    try {
      const detail = await getUser(user.id!)
      form.setFieldsValue({
        username: detail.username,
        realName: detail.realName,
        phone: detail.phone,
        email: detail.email,
        status: detail.status ?? 1,
        roleIds: roles
          .filter((r) => (detail.roles ?? []).includes(r.name ?? ''))
          .map((r) => r.id!)
      })
      setEditOpen(true)
    } catch (e) {
      message.error((e as Error).message || '加载用户详情失败')
    }
  }

  const submitEdit = async () => {
    const values = await form.validateFields()
    try {
      if (editingUser) {
        await updateUser(editingUser.id!, values)
        message.success('已更新')
      } else {
        await createUser(values)
        message.success('已创建，初始密码 123456')
      }
      setEditOpen(false)
      void load()
    } catch (e) {
      message.error((e as Error).message || '保存失败')
    }
  }

  const toggleStatus = async (user: UserVO, checked: boolean) => {
    try {
      await updateUserStatus(user.id!, { status: checked ? 1 : 0 })
      message.success(checked ? '已启用' : '已停用')
      void load()
    } catch (e) {
      message.error((e as Error).message || '状态切换失败')
    }
  }

  const onResetPassword = async (user: UserVO) => {
    try {
      await resetPassword(user.id!)
      message.success('密码已重置为 123456')
    } catch (e) {
      message.error((e as Error).message || '重置失败')
    }
  }

  const onDelete = async (user: UserVO) => {
    try {
      await deleteUser(user.id!)
      message.success('已删除')
      void load()
    } catch (e) {
      message.error((e as Error).message || '删除失败')
    }
  }

  const openScope = async (user: UserVO) => {
    setScopeUser(user)
    scopeForm.resetFields()
    setScopeOpen(true)
    setScopeLoading(true)
    try {
      const list = await getUserDataPermissions(user.id!)
      setScopeList(list)
    } catch (e) {
      message.error((e as Error).message || '加载数据权限失败')
    } finally {
      setScopeLoading(false)
    }
  }

  const submitScope = async () => {
    const values = await scopeForm.validateFields()
    const data: DataPermissionUpdateRequest = {
      scopeType: values.scopeType,
      scopeValues: values.valuesText
        ? values.valuesText
            .split(/[\n,]/g)
            .map((s: string) => s.trim())
            .filter(Boolean)
        : [],
    }
    try {
      await updateUserDataPermissions(scopeUser!.id!, data)
      message.success('已更新')
      // 刷新本维度
      setScopeLoading(true)
      const list = await getUserDataPermissions(scopeUser!.id!)
      setScopeList(list)
    } catch (e) {
      message.error((e as Error).message || '更新失败')
    } finally {
      setScopeLoading(false)
    }
  }

  const columns: TableColumnsType<UserVO> = [
    {
      title: '用户名',
      dataIndex: 'username',
      width: 140,
    },
    {
      title: '真实姓名',
      dataIndex: 'realName',
      width: 120,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      width: 140,
    },
    {
      title: '角色',
      dataIndex: 'roles',
      render: (rs?: string[]) => (
        <Space wrap>
          {(rs ?? []).map((r) => (
            <Tag key={r}>{r}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (s: number, record) => (
        <Switch
          checked={s === 1}
          checkedChildren="启用"
          unCheckedChildren="停用"
          onChange={(checked) => void toggleStatus(record, checked)}
        />
      ),
    },
    {
      title: '最后登录',
      dataIndex: 'lastLoginAt',
      width: 180,
      render: (v?: string) => (v ? new Date(v).toLocaleString() : '-'),
    },
    {
      title: '操作',
      fixed: 'right',
      width: 320,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => void openEdit(record)}>编辑</Button>
          <Button size="small" onClick={() => void openScope(record)}>数据权限</Button>
          <Popconfirm
            title="重置密码为 123456？"
            okText="确定"
            cancelText="取消"
            onConfirm={() => void onResetPassword(record)}
          >
            <Button size="small">重置密码</Button>
          </Popconfirm>
          <Popconfirm
            title="删除该用户？"
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => void onDelete(record)}
          >
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Card
      title="用户管理"
      extra={
        <Space>
          <Input.Search
            placeholder="按用户名 / 姓名 / 手机号搜索"
            allowClear
            enterButton
            style={{ width: 280 }}
            onSearch={(v) => {
              setKeyword(v)
              setPage((p) => ({ ...p, current: 1 }))
            }}
          />
          <Button type="primary" onClick={openCreate}>新建用户</Button>
        </Space>
      }
    >
      <Table<UserVO>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={records}
        scroll={{ x: 1100 }}
        pagination={{
          current: page.current,
          pageSize: page.size,
          total: page.total,
          showSizeChanger: true,
          onChange: (current, size) => setPage({ current, size, total: page.total }),
        }}
      />

      <Modal
        title={editingUser ? '编辑用户' : '新建用户'}
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={() => void submitEdit()}
        destroyOnClose
        width={560}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input disabled={!!editingUser} placeholder="登录用户名" />
          </Form.Item>
          <Form.Item label="真实姓名" name="realName" rules={[{ required: true, message: '请输入真实姓名' }]}>
            <Input placeholder="员工真实姓名" />
          </Form.Item>
          <Form.Item label="手机号" name="phone">
            <Input placeholder="11 位手机号" />
          </Form.Item>
          <Form.Item label="邮箱" name="email" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
            <Input placeholder="选填" />
          </Form.Item>
          <Form.Item label="状态" name="status" valuePropName="checked" getValueFromEvent={(v) => (v ? 1 : 0)} getValueProps={(v) => ({ checked: v === 1 })}>
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
          <Form.Item label="角色" name="roleIds">
            <Select
              mode="multiple"
              allowClear
              placeholder="选择角色（可多选）"
              options={roles.map((r) => ({ label: r.name, value: r.id }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={scopeUser ? `数据权限 - ${scopeUser.realName ?? scopeUser.username}` : '数据权限'}
        open={scopeOpen}
        onCancel={() => setScopeOpen(false)}
        onOk={() => void submitScope()}
        width={720}
        destroyOnClose
      >
        <div style={{ marginBottom: 12, color: '#666' }}>
          按 7 个维度（ALL / CHANNEL / REGION / UNIT / 业务域 / 警种 / 本人）逐个维护。
          维度之间取并集；ALL 一旦勾选忽略其他维度。
        </div>
        <div style={{ marginBottom: 16 }}>
          <Table<DataPermissionVO>
            rowKey="scopeType"
            loading={scopeLoading}
            dataSource={scopeList}
            pagination={false}
            size="small"
            columns={[
              { title: '维度', dataIndex: 'scopeTypeLabel', width: 120 },
              {
                title: '已配置',
                dataIndex: 'scopeValues',
                render: (vs: string[]) => (vs && vs.length > 0 ? vs.join('、') : <span style={{ color: '#bbb' }}>未配置</span>),
              },
            ]}
          />
        </div>
        <Form form={scopeForm} layout="vertical" preserve={false}>
          <Form.Item
            label="要修改的维度"
            name="scopeType"
            rules={[{ required: true, message: '请选择维度' }]}
          >
            <Select
              options={DIMENSION_ORDER.map((d) => ({
                value: d,
                label: `${d} - ${DATA_SCOPE_DIMENSION_LABEL[d]}`,
              }))}
              onChange={() => scopeForm.setFieldValue('valuesText', '')}
            />
          </Form.Item>
          <Form.Item
            shouldUpdate={(prev, curr) => prev.scopeType !== curr.scopeType}
            noStyle
          >
            {({ getFieldValue }) => {
              const dim = getFieldValue('scopeType') as DataScopeDimensionCode | undefined
              const placeholder = dim ? DIMENSION_VALUE_LABEL[dim] : '请先选择维度'
              const disabled = !dim || dim === DATA_SCOPE_DIMENSIONS.ALL || dim === DATA_SCOPE_DIMENSIONS.SELF
              return (
                <Form.Item
                  label="授权值"
                  name="valuesText"
                  extra={
                    dim === DATA_SCOPE_DIMENSIONS.ALL || dim === DATA_SCOPE_DIMENSIONS.SELF
                      ? '该维度为"标记型"，无需填值；保存即生效。'
                      : '多值以换行或英文逗号分隔'
                  }
                >
                  <Input.TextArea
                    rows={3}
                    disabled={disabled}
                    placeholder={placeholder}
                  />
                </Form.Item>
              )
            }}
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
