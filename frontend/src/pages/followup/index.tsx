import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Card,
  DatePicker,
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
import dayjs, { type Dayjs } from 'dayjs'

import {
  createFollowUp,
  deleteFollowUp,
  pageFollowUps,
  updateFollowUp,
  type FollowUpRequest,
  type FollowUpVO,
} from '@/api/followup'
import { getCustomers } from '@/api/customer'

interface CustomerOption {
  id: number
  name: string
}

interface FollowUpFormValues extends Omit<FollowUpRequest, 'followUpDate'> {
  followUpDate: Dayjs
}

const isStageTransition = (cur?: string, nxt?: string) =>
  !!nxt && !!cur && nxt !== cur

export default function FollowUpPage() {
  const [page, setPage] = useState({ current: 1, size: 10, total: 0 })
  const [records, setRecords] = useState<FollowUpVO[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<FollowUpVO | null>(null)
  const [form] = Form.useForm<FollowUpFormValues>()

  const [customerOptions, setCustomerOptions] = useState<CustomerOption[]>([])
  const [customerLoading, setCustomerLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await pageFollowUps({
        current: page.current,
        size: page.size,
      })
      setRecords(res.records ?? [])
      setPage((p) => ({ ...p, total: res.total ?? 0 }))
    } catch (e) {
      message.error((e as Error).message || '加载跟进记录失败')
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    void load()
  }, [load])

  const searchCustomers = useCallback(async (q: string) => {
    setCustomerLoading(true)
    try {
      const res = await getCustomers({ keyword: q || undefined, size: 50, current: 1 })
      setCustomerOptions(
        (res.records ?? []).map((c) => ({ id: c.id!, name: c.name ?? `#${c.id}` })),
      )
    } catch {
      setCustomerOptions([])
    } finally {
      setCustomerLoading(false)
    }
  }, [])

  useEffect(() => {
    void searchCustomers('')
  }, [searchCustomers])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ followUpDate: dayjs() } as FollowUpFormValues)
    setModalOpen(true)
  }

  const openEdit = (record: FollowUpVO) => {
    setEditing(record)
    form.resetFields()
    form.setFieldsValue({
      customerId: record.customerId,
      projectId: record.projectId,
      opportunityId: record.opportunityId,
      currentStage: record.currentStage,
      nextStage: record.nextStage,
      stageFeedback: record.stageFeedback,
      followUpDate: record.followUpDate ? dayjs(record.followUpDate) : dayjs(),
      followUpMethod: record.followUpMethod,
      contactId: record.contactId,
      content: record.content,
      nextPlan: record.nextPlan,
    } as FollowUpFormValues)
    setModalOpen(true)
  }

  const submit = async () => {
    const values = await form.validateFields()
    if (isStageTransition(values.currentStage, values.nextStage) && !values.stageFeedback?.trim()) {
      message.error('切换阶段时必须填写阶段反馈')
      return
    }
    const payload: FollowUpRequest = {
      ...values,
      followUpDate: values.followUpDate.format('YYYY-MM-DD'),
    }
    try {
      if (editing) {
        await updateFollowUp(editing.id!, payload)
        message.success('已更新')
      } else {
        await createFollowUp(payload)
        message.success('已创建')
      }
      setModalOpen(false)
      void load()
    } catch (e) {
      message.error((e as Error).message || '保存失败')
    }
  }

  const onDelete = async (record: FollowUpVO) => {
    try {
      await deleteFollowUp(record.id!)
      message.success('已删除')
      void load()
    } catch (e) {
      message.error((e as Error).message || '删除失败')
    }
  }

  const columns: TableColumnsType<FollowUpVO> = [
    {
      title: '跟进日期',
      dataIndex: 'followUpDate',
      width: 110,
    },
    {
      title: '客户',
      dataIndex: 'customerName',
      width: 200,
      render: (v: string, record) => (
        <span>
          {v ?? '-'}
          {record.customerId && (
            <Tag style={{ marginLeft: 6 }} color="blue">
              #{record.customerId}
            </Tag>
          )}
        </span>
      ),
    },
    {
      title: '方式',
      dataIndex: 'followUpMethodName',
      width: 80,
    },
    {
      title: '当前阶段',
      dataIndex: 'currentStageName',
      width: 110,
      render: (v?: string) => v ?? '-',
    },
    {
      title: '下一步阶段',
      dataIndex: 'nextStageName',
      width: 110,
      render: (v: string | undefined, record) =>
        record.nextStage ? <Tag color="cyan">{v ?? record.nextStage}</Tag> : <span style={{ color: '#bbb' }}>未推进</span>,
    },
    {
      title: '阶段反馈',
      dataIndex: 'stageFeedback',
      ellipsis: true,
      render: (v?: string) => v || <span style={{ color: '#bbb' }}>-</span>,
    },
    {
      title: '跟进内容',
      dataIndex: 'content',
      ellipsis: true,
    },
    {
      title: '记录人',
      dataIndex: 'createdByName',
      width: 100,
      render: (v?: string) => v ?? '-',
    },
    {
      title: '操作',
      fixed: 'right',
      width: 160,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Popconfirm
            title="删除该跟进记录？"
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
      title={`跟进记录（共 ${page.total} 条）`}
      extra={
        <Space>
          <Input.Search
            placeholder="按跟进内容/客户名过滤（后端暂未接）"
            allowClear
            enterButton
            style={{ width: 280 }}
            onSearch={() => {
              /* TODO: 后端 pageFollowUps 增加 keyword 过滤 */
            }}
          />
          <Button type="primary" onClick={openCreate}>
            新建跟进
          </Button>
        </Space>
      }
    >
      <Table<FollowUpVO>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={records}
        scroll={{ x: 1300 }}
        pagination={{
          current: page.current,
          pageSize: page.size,
          total: page.total,
          showSizeChanger: true,
          onChange: (current, size) => setPage({ current, size, total: page.total }),
        }}
      />

      <Modal
        title={editing ? '编辑跟进' : '新建跟进'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => void submit()}
        destroyOnClose
        width={720}
      >
        <Form<FollowUpFormValues>
          form={form}
          layout="vertical"
          preserve={false}
        >
          <Form.Item
            label="客户"
            name="customerId"
            rules={[{ required: true, message: '请选择客户' }]}
          >
            <Select
              showSearch
              placeholder="按客户名搜索"
              loading={customerLoading}
              filterOption={false}
              onSearch={(v) => void searchCustomers(v)}
              options={customerOptions.map((c) => ({ value: c.id, label: c.name }))}
            />
          </Form.Item>
          <Space size="middle" style={{ display: 'flex' }}>
            <Form.Item label="项目 ID（可选）" name="projectId" style={{ flex: 1 }}>
              <Input type="number" placeholder="项目 ID" />
            </Form.Item>
            <Form.Item label="商机 ID（可选）" name="opportunityId" style={{ flex: 1 }}>
              <Input type="number" placeholder="商机 ID" />
            </Form.Item>
          </Space>
          <Space size="middle" style={{ display: 'flex' }}>
            <Form.Item label="当前阶段" name="currentStage" style={{ flex: 1 }}>
              <Input placeholder="例如 IN_PROGRESS" allowClear />
            </Form.Item>
            <Form.Item label="下一步阶段" name="nextStage" style={{ flex: 1 }}>
              <Input placeholder="例如 CLOSED_WON" allowClear />
            </Form.Item>
          </Space>
          <Form.Item
            label="阶段反馈"
            name="stageFeedback"
            dependencies={['currentStage', 'nextStage'] as const}
            rules={[
              ({ getFieldValue }) => ({
                validator: (_rule, value) => {
                  const cur = getFieldValue('currentStage') as string | undefined
                  const nxt = getFieldValue('nextStage') as string | undefined
                  if (isStageTransition(cur, nxt) && !value?.trim()) {
                    return Promise.reject(new Error('切换阶段时必填'))
                  }
                  return Promise.resolve()
                },
              }),
            ]}
            extra="当下一步阶段与当前阶段不同时必填"
          >
            <Input.TextArea rows={2} placeholder="阶段切换时的反馈" />
          </Form.Item>
          <Space size="middle" style={{ display: 'flex' }}>
            <Form.Item
              label="跟进日期"
              name="followUpDate"
              rules={[{ required: true, message: '请选择跟进日期' }]}
              style={{ flex: 1 }}
            >
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="跟进方式" name="followUpMethod" style={{ flex: 1 }}>
              <Select
                allowClear
                placeholder="拜访 / 电话 / 微信 等"
                options={[
                  { value: 'VISIT', label: '拜访' },
                  { value: 'PHONE', label: '电话' },
                  { value: 'WECHAT', label: '微信' },
                  { value: 'EMAIL', label: '邮件' },
                  { value: 'VIDEO', label: '视频' },
                  { value: 'EVENT', label: '活动' },
                ]}
              />
            </Form.Item>
          </Space>
          <Form.Item
            label="跟进内容"
            name="content"
            rules={[{ required: true, message: '请填写跟进内容' }]}
          >
            <Input.TextArea rows={3} placeholder="本次沟通要点、反馈、问题等" />
          </Form.Item>
          <Form.Item label="下一步计划" name="nextPlan">
            <Input.TextArea rows={2} placeholder="如：下周再次拜访 / 提交方案" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
