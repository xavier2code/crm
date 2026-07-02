import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  App,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
} from 'antd'
import {
  ArrowLeftOutlined,
  EditOutlined,
  PlusOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'

import { DictTag } from '@/components/DictTag'
import { useUsers } from '@/hooks/useAdminUsers'
import {
  useAddContact,
  useAssignCustomer,
  useCustomer,
  useDeleteContact,
  useUpdateContact,
} from '@/hooks/useCustomers'
import { maskPhone } from '@/utils/mask'
import type { ContactRequest, ContactVO } from '@/api/customer'

const CONTACT_TYPE_OPTIONS = [
  { value: 1, label: '重要决策人', color: 'red' },
  { value: 2, label: '业务对接人', color: 'blue' },
  { value: 3, label: '操作员', color: 'green' },
]

const LAYER_COLOR: Record<string, string> = {
  A: 'red',
  B: 'orange',
  C: 'blue',
}

export default function CustomerDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()

  const customerId = Number(id)
  const { data: customer, isLoading, refetch } = useCustomer(customerId)

  const addContactMut = useAddContact()
  const updateContactMut = useUpdateContact()
  const deleteContactMut = useDeleteContact()
  const assignMut = useAssignCustomer()
  const { data: users } = useUsers({ current: 1, size: 1000 })

  const [contactModalOpen, setContactModalOpen] = useState(false)
  const [editingContact, setEditingContact] = useState<ContactVO | null>(null)
  const [contactForm] = Form.useForm<ContactRequest>()

  const [assignOpen, setAssignOpen] = useState(false)
  const [assignUserId, setAssignUserId] = useState<number | undefined>()

  const openAddContact = () => {
    setEditingContact(null)
    contactForm.resetFields()
    contactForm.setFieldsValue({ contactType: 2, isPrimary: 0 })
    setContactModalOpen(true)
  }

  const openEditContact = (record: ContactVO) => {
    setEditingContact(record)
    contactForm.setFieldsValue({
      name: record.name,
      title: record.title,
      phone: record.phone,
      contactType: record.contactType,
      isPrimary: record.isPrimary,
    })
    setContactModalOpen(true)
  }

  const handleContactSubmit = async () => {
    try {
      const values = await contactForm.validateFields()
      if (editingContact) {
        await updateContactMut.mutateAsync({ id: editingContact.id!, data: values })
        message.success('联系人更新成功')
      } else {
        await addContactMut.mutateAsync({ customerId, data: values })
        message.success('联系人添加成功')
      }
      setContactModalOpen(false)
      refetch()
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const handleDeleteContact = async (record: ContactVO) => {
    try {
      await deleteContactMut.mutateAsync(record.id!)
      message.success('联系人删除成功')
      refetch()
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const openAssign = () => {
    setAssignUserId(customer?.ownerUserId)
    setAssignOpen(true)
  }

  const handleAssign = async () => {
    if (!assignUserId) {
      message.warning('请选择跟进人')
      return
    }
    try {
      await assignMut.mutateAsync({ id: customerId, userId: assignUserId })
      message.success('分配成功')
      setAssignOpen(false)
      refetch()
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const contactColumns: ColumnsType<ContactVO> = [
    { title: '姓名', dataIndex: 'name', width: 120 },
    { title: '职务', dataIndex: 'title', width: 140, render: (v?: string) => v || '-' },
    {
      title: '手机号',
      dataIndex: 'phone',
      width: 140,
      render: (v?: string) => maskPhone(v),
    },
    {
      title: '类型',
      dataIndex: 'contactType',
      width: 120,
      render: (v?: number) => {
        const item = CONTACT_TYPE_OPTIONS.find((o) => o.value === v)
        return item ? <Tag color={item.color}>{item.label}</Tag> : '-'
      },
    },
    {
      title: '主联系人',
      dataIndex: 'isPrimary',
      width: 100,
      render: (v?: number) => (v === 1 ? <Tag color="gold">是</Tag> : '否'),
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
      width: 160,
      render: (_: unknown, record: ContactVO) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditContact(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除该联系人?"
            okText="删除"
            okType="danger"
            cancelText="取消"
            onConfirm={() => handleDeleteContact(record)}
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  if (isLoading) return null

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/customer')}>
            返回
          </Button>
          <h2 style={{ margin: 0 }}>{customer?.name}</h2>
        </Space>
      </div>

      <Card
        title="客户信息"
        style={{ marginBottom: 24 }}
        extra={
          <Space>
            <Button icon={<UserSwitchOutlined />} onClick={openAssign}>
              分配跟进人
            </Button>
            <Button
              type="primary"
              icon={<EditOutlined />}
              onClick={() => navigate(`/customer/${customerId}/edit`)}
            >
              编辑
            </Button>
          </Space>
        }
      >
        <Descriptions bordered column={2}>
          <Descriptions.Item label="客户名称">{customer?.name || '-'}</Descriptions.Item>
          <Descriptions.Item label="单位">{customer?.unitName || '-'}</Descriptions.Item>
          <Descriptions.Item label="警种">
            <DictTag type="police_type" value={customer?.policeType} />
          </Descriptions.Item>
          <Descriptions.Item label="客户分层">
            {customer?.customerLayer ? (
              <Tag color={LAYER_COLOR[customer.customerLayer] || 'default'}>
                {customer.customerLayer}类
              </Tag>
            ) : (
              '-'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="跟进人">
            {customer?.ownerUserName || <span style={{ color: '#999' }}>未分配</span>}
          </Descriptions.Item>
          <Descriptions.Item label="区域">{customer?.region || '-'}</Descriptions.Item>
          <Descriptions.Item label="状态">
            {customer?.status === 0 ? (
              <Tag color="default">停用</Tag>
            ) : (
              <Tag color="green">正常</Tag>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {customer?.createdAt
              ? dayjs(customer.createdAt).format('YYYY-MM-DD HH:mm')
              : '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card
        title="联系人"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={openAddContact}>
            添加联系人
          </Button>
        }
      >
        <Table<ContactVO>
          rowKey="id"
          loading={isLoading}
          columns={contactColumns}
          dataSource={customer?.contacts || []}
          pagination={false}
        />
      </Card>

      <Modal
        title={editingContact ? '编辑联系人' : '添加联系人'}
        open={contactModalOpen}
        onCancel={() => setContactModalOpen(false)}
        onOk={handleContactSubmit}
        confirmLoading={addContactMut.isPending || updateContactMut.isPending}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form<ContactRequest> form={contactForm} layout="vertical" preserve={false}>
          <Form.Item
            label="姓名"
            name="name"
            rules={[{ required: true, message: '请输入姓名' }]}
          >
            <Input placeholder="姓名" />
          </Form.Item>
          <Form.Item
            label="职务"
            name="title"
            rules={[{ required: true, message: '请输入职务' }]}
          >
            <Input placeholder="职务" />
          </Form.Item>
          <Form.Item
            label="手机号"
            name="phone"
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
            ]}
          >
            <Input placeholder="手机号" />
          </Form.Item>
          <Form.Item
            label="类型"
            name="contactType"
            rules={[{ required: true, message: '请选择类型' }]}
          >
            <Select placeholder="类型" options={CONTACT_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item
            label="是否主联系人"
            name="isPrimary"
            valuePropName="checked"
            getValueProps={(v) => ({ checked: v === 1 })}
            getValueFromEvent={(e) => (e.target.checked ? 1 : 0)}
          >
            <Input type="checkbox" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="分配跟进人"
        open={assignOpen}
        onCancel={() => setAssignOpen(false)}
        onOk={handleAssign}
        confirmLoading={assignMut.isPending}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            当前跟进人：{customer?.ownerUserName || '未分配'}
          </div>
          <Select
            placeholder="请选择新跟进人"
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
        </Space>
      </Modal>
    </div>
  )
}
