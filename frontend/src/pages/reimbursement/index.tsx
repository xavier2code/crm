import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Upload,
} from 'antd'
import type { TableColumnsType } from 'antd'
import {
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  DollarOutlined,
  EditOutlined,
  PaperClipOutlined,
  PlusOutlined,
  SendOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'

import {
  approveReimbursement,
  createReimbursement,
  deleteAttachment,
  deleteReimbursement,
  getReimbursement,
  markPaid,
  pageReimbursements,
  REIMBURSEMENT_STATUS,
  REIMBURSEMENT_TYPE,
  submitReimbursement,
  updateReimbursement,
  uploadAttachment,
  type ReimbursementApproveRequest,
  type ReimbursementRequest,
  type ReimbursementStatus,
  type ReimbursementVO,
} from '@/api/reimbursement'
import { getProjects } from '@/api/project'

interface ProjectOption {
  id: number
  name: string
}

interface ReimbursementFormValues extends Omit<ReimbursementRequest, 'expenseDate'> {
  expenseDate: Dayjs
}

interface ApproveFormValues {
  comment?: string
}

const beforeUpload = (file: File) => {
  if (file.size > 20 * 1024 * 1024) {
    message.error('文件大小不能超过 20MB')
    return Upload.LIST_IGNORE
  }
  return true
}

const statusTag = (status?: ReimbursementStatus) => {
  if (!status) return null
  const meta = REIMBURSEMENT_STATUS[status]
  return <Tag color={meta.color}>{meta.label}</Tag>
}

export default function ReimbursementPage() {
  const [activeTab, setActiveTab] = useState<ReimbursementStatus | 'ALL'>('PENDING')
  const [page, setPage] = useState({ current: 1, size: 10, total: 0 })
  const [records, setRecords] = useState<ReimbursementVO[]>([])
  const [loading, setLoading] = useState(false)
  const [formModalOpen, setFormModalOpen] = useState(false)
  const [editing, setEditing] = useState<ReimbursementVO | null>(null)
  const [form] = Form.useForm<ReimbursementFormValues>()

  const [drawerOpen, setDrawerOpen] = useState(false)
  const [detail, setDetail] = useState<ReimbursementVO | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const [approveModalOpen, setApproveModalOpen] = useState(false)
  const [approving, setApproving] = useState<ReimbursementVO | null>(null)
  const [approveForm] = Form.useForm<ApproveFormValues>()

  const [projectOptions, setProjectOptions] = useState<ProjectOption[]>([])
  const [projectLoading, setProjectLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const isMineTab = activeTab === 'DRAFT' || activeTab === 'REJECTED' || activeTab === 'ALL'

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await pageReimbursements({
        current: page.current,
        size: page.size,
        status: activeTab === 'ALL' ? undefined : activeTab,
        mine: isMineTab,
      })
      setRecords(res.records ?? [])
      setPage((p) => ({ ...p, total: res.total ?? 0 }))
    } catch (e) {
      message.error((e as Error).message || '加载报销记录失败')
    } finally {
      setLoading(false)
    }
  }, [page, activeTab, isMineTab])

  useEffect(() => {
    void load()
  }, [load])

  const searchProjects = useCallback(async (q: string) => {
    setProjectLoading(true)
    try {
      const res = await getProjects({ keyword: q || undefined, current: 1, size: 50 })
      setProjectOptions(
        (res.records ?? []).map((p) => ({ id: p.id!, name: p.name ?? `#${p.id}` })),
      )
    } catch {
      setProjectOptions([])
    } finally {
      setProjectLoading(false)
    }
  }, [])

  useEffect(() => {
    void searchProjects('')
  }, [searchProjects])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ type: 'TRAVEL', expenseDate: dayjs() } as Partial<ReimbursementFormValues>)
    setFormModalOpen(true)
  }

  const openEdit = (vo: ReimbursementVO) => {
    setEditing(vo)
    form.setFieldsValue({
      projectId: vo.projectId,
      type: vo.type,
      title: vo.title,
      description: vo.description,
      amount: vo.amount,
      expenseDate: vo.expenseDate ? dayjs(vo.expenseDate) : undefined,
    } as ReimbursementFormValues)
    setFormModalOpen(true)
  }

  const submitForm = async () => {
    const values = await form.validateFields()
    const payload: ReimbursementRequest = {
      projectId: values.projectId,
      type: values.type,
      title: values.title,
      description: values.description,
      amount: values.amount,
      expenseDate: values.expenseDate.format('YYYY-MM-DD'),
    }
    setSubmitting(true)
    try {
      if (editing) {
        await updateReimbursement(editing.id!, payload)
        message.success('已保存')
      } else {
        await createReimbursement(payload)
        message.success('已创建草稿')
      }
      setFormModalOpen(false)
      void load()
    } catch (e) {
      message.error((e as Error).message || '保存失败')
    } finally {
      setSubmitting(false)
    }
  }

  const onDelete = async (vo: ReimbursementVO) => {
    try {
      await deleteReimbursement(vo.id!)
      message.success('已删除')
      void load()
    } catch (e) {
      message.error((e as Error).message || '删除失败')
    }
  }

  const onSubmit = async (vo: ReimbursementVO) => {
    try {
      await submitReimbursement(vo.id!)
      message.success('已提交审批')
      void load()
    } catch (e) {
      message.error((e as Error).message || '提交失败')
    }
  }

  const openDetail = async (vo: ReimbursementVO) => {
    setDrawerOpen(true)
    setDetailLoading(true)
    try {
      const res = await getReimbursement(vo.id!)
      setDetail(res)
    } catch (e) {
      message.error((e as Error).message || '加载详情失败')
    } finally {
      setDetailLoading(false)
    }
  }

  const openApprove = (vo: ReimbursementVO) => {
    setApproving(vo)
    approveForm.resetFields()
    setApproveModalOpen(true)
  }

  const onApprove = async (result: 'APPROVED' | 'REJECTED') => {
    const values = await approveForm.validateFields().catch(() => ({ comment: undefined } as ApproveFormValues))
    const payload: ReimbursementApproveRequest = { result, comment: values.comment }
    try {
      await approveReimbursement(approving!.id!, payload)
      message.success(result === 'APPROVED' ? '已通过' : '已驳回')
      setApproveModalOpen(false)
      void load()
    } catch (e) {
      message.error((e as Error).message || '操作失败')
    }
  }

  const onMarkPaid = async (vo: ReimbursementVO) => {
    try {
      await markPaid(vo.id!)
      message.success('已标记为已付款')
      void load()
    } catch (e) {
      message.error((e as Error).message || '操作失败')
    }
  }

  const onUpload = async (file: File): Promise<boolean> => {
    if (!detail?.id) return false
    setUploading(true)
    try {
      const att = await uploadAttachment(detail.id, file)
      message.success('上传成功')
      setDetail({ ...detail, attachments: [...(detail.attachments ?? []), att] })
      return true
    } catch (e) {
      message.error((e as Error).message || '上传失败')
      return false
    } finally {
      setUploading(false)
    }
  }

  const onDeleteAttachment = async (att: { id?: number }) => {
    if (!att.id) return
    try {
      await deleteAttachment(att.id)
      message.success('已删除')
      if (detail) {
        setDetail({
          ...detail,
          attachments: (detail.attachments ?? []).filter((a) => a.id !== att.id),
        })
      }
    } catch (e) {
      message.error((e as Error).message || '删除失败')
    }
  }

  const columns: TableColumnsType<ReimbursementVO> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 70,
    },
    {
      title: '项目',
      dataIndex: 'projectName',
      width: 180,
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 80,
      render: (type?: ReimbursementVO['type']) =>
        type ? <Tag>{REIMBURSEMENT_TYPE[type].label}</Tag> : '-',
    },
    {
      title: '标题',
      dataIndex: 'title',
      width: 200,
      ellipsis: true,
    },
    {
      title: '金额',
      dataIndex: 'amount',
      width: 110,
      align: 'right' as const,
      render: (a?: number) => (a != null ? `¥ ${a.toLocaleString()}` : '-'),
    },
    {
      title: '申请人',
      dataIndex: 'applicantName',
      width: 100,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (s?: ReimbursementStatus) => statusTag(s),
    },
    {
      title: '发生日期',
      dataIndex: 'expenseDate',
      width: 110,
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      width: 160,
      render: (t?: string) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'),
    },
    {
      title: '操作',
      key: 'actions',
      width: 220,
      fixed: 'right' as const,
      render: (_, vo) => {
        const status = vo.status
        const canEdit = status === 'DRAFT' || status === 'REJECTED'
        const canSubmit = canEdit
        const canDelete = canEdit
        const canApprove = status === 'PENDING'
        const canPay = status === 'APPROVED'
        return (
          <Space size="small">
            <Button size="small" type="link" onClick={() => openDetail(vo)}>详情</Button>
            {canEdit && <Button size="small" type="link" icon={<EditOutlined />} onClick={() => openEdit(vo)}>编辑</Button>}
            {canSubmit && <Button size="small" type="link" icon={<SendOutlined />} onClick={() => onSubmit(vo)}>提交</Button>}
            {canApprove && <Button size="small" type="link" icon={<CheckOutlined />} onClick={() => openApprove(vo)}>审批</Button>}
            {canPay && (
              <Popconfirm title="确认已付款？" onConfirm={() => onMarkPaid(vo)}>
                <Button size="small" type="link" icon={<DollarOutlined />}>付款</Button>
              </Popconfirm>
            )}
            {canDelete && (
              <Popconfirm title="确认删除？" onConfirm={() => onDelete(vo)}>
                <Button size="small" type="link" danger icon={<DeleteOutlined />}>删除</Button>
              </Popconfirm>
            )}
          </Space>
        )
      },
    },
  ]

  return (
    <Card
      title="报销管理"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新建报销
        </Button>
      }
    >
      <Tabs
        activeKey={activeTab}
        onChange={(k) => {
          setActiveTab(k as ReimbursementStatus | 'ALL')
          setPage((p) => ({ ...p, current: 1 }))
        }}
        items={[
          { key: 'PENDING', label: '待审批' },
          { key: 'APPROVED', label: '已审批' },
          { key: 'PAID', label: '已付款' },
          { key: 'REJECTED', label: '已驳回' },
          { key: 'DRAFT', label: '我的草稿' },
          { key: 'ALL', label: '全部' },
        ]}
      />
      <Table<ReimbursementVO>
        rowKey="id"
        size="small"
        loading={loading}
        dataSource={records}
        columns={columns}
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
        title={editing ? '编辑报销' : '新建报销'}
        open={formModalOpen}
        onCancel={() => setFormModalOpen(false)}
        onOk={submitForm}
        confirmLoading={submitting}
        width={600}
        destroyOnClose
      >
        <Form<ReimbursementFormValues> form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="projectId"
            label="关联项目"
            rules={[{ required: true, message: '请选择项目' }]}
          >
            <Select
              showSearch
              placeholder="搜索项目名称"
              loading={projectLoading}
              filterOption={false}
              onSearch={searchProjects}
              options={projectOptions.map((p) => ({ value: p.id, label: p.name }))}
            />
          </Form.Item>
          <Form.Item
            name="type"
            label="类型"
            rules={[{ required: true, message: '请选择类型' }]}
          >
            <Select
              options={Object.entries(REIMBURSEMENT_TYPE).map(([k, v]) => ({
                value: k,
                label: v.label,
              }))}
            />
          </Form.Item>
          <Form.Item
            name="title"
            label="标题"
            rules={[{ required: true, max: 255, message: '请填写标题（≤255字）' }]}
          >
            <Input maxLength={255} placeholder="如：6月华东出差" />
          </Form.Item>
          <Form.Item
            name="amount"
            label="金额（元）"
            rules={[{ required: true, type: 'number', min: 0, message: '金额必须 ≥ 0' }]}
          >
            <InputNumber style={{ width: '100%' }} min={0} precision={2} step={100} placeholder="0.00" />
          </Form.Item>
          <Form.Item
            name="expenseDate"
            label="发生日期"
            rules={[{ required: true, message: '请选择发生日期' }]}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} maxLength={2000} showCount placeholder="可选，差旅行程、招待对象等" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`审批报销 #${approving?.id ?? ''}`}
        open={approveModalOpen}
        onCancel={() => setApproveModalOpen(false)}
        footer={[
          <Button key="reject" danger icon={<CloseOutlined />} onClick={() => onApprove('REJECTED')}>
            驳回
          </Button>,
          <Button key="approve" type="primary" icon={<CheckOutlined />} onClick={() => onApprove('APPROVED')}>
            通过
          </Button>,
        ]}
        destroyOnClose
      >
        <Form<ApproveFormValues> form={approveForm} layout="vertical" preserve={false}>
          <Form.Item name="comment" label="审批意见">
            <Input.TextArea rows={3} maxLength={1000} showCount placeholder="选填" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={`报销详情 #${detail?.id ?? ''}`}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={640}
        destroyOnClose
      >
        {detailLoading || !detail ? (
          <div style={{ padding: 24, textAlign: 'center' }}>加载中...</div>
        ) : (
          <>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="项目">{detail.projectName}</Descriptions.Item>
              <Descriptions.Item label="类型">
                {detail.type ? REIMBURSEMENT_TYPE[detail.type].label : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="标题">{detail.title}</Descriptions.Item>
              <Descriptions.Item label="金额">
                {detail.amount != null ? `¥ ${detail.amount.toLocaleString()}` : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="发生日期">{detail.expenseDate ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="状态">{statusTag(detail.status)}</Descriptions.Item>
              <Descriptions.Item label="申请人">{detail.applicantName ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="说明">{detail.description || '-'}</Descriptions.Item>
              <Descriptions.Item label="审批人">{detail.approverName ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="审批时间">
                {detail.approvedAt ? dayjs(detail.approvedAt).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="审批意见">{detail.approvalComment || '-'}</Descriptions.Item>
              <Descriptions.Item label="付款时间">
                {detail.paidAt ? dayjs(detail.paidAt).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="提交时间">
                {detail.createdAt ? dayjs(detail.createdAt).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 16 }}>
              <div style={{ marginBottom: 8, fontWeight: 500 }}>
                <PaperClipOutlined /> 附件（{(detail.attachments ?? []).length}）
              </div>
              <Upload
                beforeUpload={(file) => {
                  if (!beforeUpload(file)) return false
                  return onUpload(file)
                }}
                showUploadList={false}
                disabled={uploading || !(detail.status === 'DRAFT' || detail.status === 'REJECTED')}
              >
                <Button icon={<UploadOutlined />} loading={uploading} disabled={!(detail.status === 'DRAFT' || detail.status === 'REJECTED')}>
                  上传凭证
                </Button>
              </Upload>
              <div style={{ marginTop: 8 }}>
                {(detail.attachments ?? []).map((att) => (
                  <div
                    key={att.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '6px 8px',
                      border: '1px solid #f0f0f0',
                      borderRadius: 4,
                      marginBottom: 4,
                    }}
                  >
                    <a
                      href={`/api/reimbursements/attachments/${att.id}/download`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      <PaperClipOutlined /> {att.fileName}{' '}
                      <span style={{ color: '#999' }}>
                        ({((att.fileSize ?? 0) / 1024).toFixed(1)} KB)
                      </span>
                    </a>
                    {detail.status === 'DRAFT' || detail.status === 'REJECTED' ? (
                      <Popconfirm title="删除附件？" onConfirm={() => onDeleteAttachment(att)}>
                        <Button type="link" danger size="small" icon={<DeleteOutlined />}>
                          删除
                        </Button>
                      </Popconfirm>
                    ) : null}
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </Drawer>
    </Card>
  )
}
