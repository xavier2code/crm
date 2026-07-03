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
  Steps,
  Tag,
  Timeline,
} from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  PlayCircleOutlined,
  ProjectOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import { PageHeader } from '@/components/PageHeader'
import { useAuthStore } from '@/stores/auth'
import {
  useApproveOpportunity,
  useDeleteOpportunity,
  useOpportunity,
  useResubmitOpportunity,
  useSubmitOpportunity,
} from '@/hooks/useOpportunities'
import type { OpportunityDetailVO } from '@/api/opportunity'

type ApprovalLog = NonNullable<OpportunityDetailVO['approvalLogs']>[number]

const STATUS_FLOW = [
  { value: 1, title: '草稿', description: '可编辑、提交' },
  { value: 2, title: '审批中', description: '等待 CYBD 审批' },
  { value: 3, title: '生效中', description: '可跟进、可转项目' },
  { value: 4, title: '报备失败', description: '可编辑后重提' },
  { value: 5, title: '报备失效', description: '超过 30 天未跟进' },
  { value: 6, title: '已转化', description: '已转为项目' },
]

const STATUS_COLOR: Record<number, string> = {
  1: 'default',
  2: 'processing',
  3: 'success',
  4: 'error',
  5: 'warning',
  6: 'purple',
}

export default function OpportunityDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()
  const { permissionCodes } = useAuthStore()

  const opportunityId = Number(id)
  const { data: opp, isLoading } = useOpportunity(opportunityId)

  const submitMut = useSubmitOpportunity()
  const resubmitMut = useResubmitOpportunity()
  const approveMut = useApproveOpportunity()
  const deleteMut = useDeleteOpportunity()

  const [approveOpen, setApproveOpen] = useState(false)
  const [approveForm] = Form.useForm<{ action: 2 | 3; comment: string }>()

  if (isLoading) return null
  if (!opp) {
    return (
      <div>
        <PageHeader title="报备详情" showBack />
        <Card>报备不存在或已被删除</Card>
      </div>
    )
  }

  const canEdit = opp.editable && permissionCodes.includes('opportunity:edit')
  const canSubmit = opp.submittable && permissionCodes.includes('opportunity:submit')
  const canResubmit = opp.resubmittable && permissionCodes.includes('opportunity:submit')
  const canApprove = opp.approvable && permissionCodes.includes('opportunity:approve')
  const canDelete = opp.status === 1 && permissionCodes.includes('opportunity:delete')
  const canConvert = opp.status === 3 && permissionCodes.includes('project:create')

  const statusIndex = STATUS_FLOW.findIndex((s) => s.value === opp.status)

  const handleSubmit = async () => {
    try {
      await submitMut.mutateAsync(opportunityId)
      message.success('提交审批成功')
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const handleResubmit = async () => {
    try {
      await resubmitMut.mutateAsync(opportunityId)
      message.success('重提成功，已重新进入审批')
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const handleDelete = async () => {
    try {
      await deleteMut.mutateAsync(opportunityId)
      message.success('删除成功')
      navigate('/opportunity')
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const openApprove = () => {
    approveForm.resetFields()
    approveForm.setFieldsValue({ action: 2 })
    setApproveOpen(true)
  }

  const handleApprove = async () => {
    try {
      const values = await approveForm.validateFields()
      await approveMut.mutateAsync({ id: opportunityId, data: values })
      message.success(values.action === 2 ? '审批通过' : '已驳回')
      setApproveOpen(false)
    } catch (e) {
      if (e instanceof Error) message.error(e.message)
    }
  }

  const handleConvert = () => {
    navigate(`/project?createFromOpportunityId=${opportunityId}`)
  }

  return (
    <div>
      <PageHeader
        title="报备详情"
        breadcrumbs={['商机报备', `报备 #${opp.id}`]}
        showBack
        extra={
          <Space>
            {canEdit && (
              <Button
                icon={<EditOutlined />}
                onClick={() => navigate(`/opportunity/${opportunityId}/edit`)}
              >
                编辑
              </Button>
            )}
            {canSubmit && (
              <Popconfirm
                title="确认提交审批？"
                description="提交后将不可编辑，由 CYBD 在 48h 内审批。"
                onConfirm={handleSubmit}
              >
                <Button type="primary" icon={<PlayCircleOutlined />}>
                  提交审批
                </Button>
              </Popconfirm>
            )}
            {canResubmit && (
              <Popconfirm
                title="确认重提报备？"
                description={
                  opp.status === 5
                    ? '失效后仅 1 次恢复机会，重提后报备将进入审批中状态。'
                    : '驳回后重提将重新进入审批流程，submit_count +1。'
                }
                onConfirm={handleResubmit}
              >
                <Button type="primary" icon={<PlayCircleOutlined />}>
                  重提报备
                </Button>
              </Popconfirm>
            )}
            {canApprove && (
              <Button type="primary" icon={<CheckCircleOutlined />} onClick={openApprove}>
                审批
              </Button>
            )}
            {canConvert && (
              <Button icon={<ProjectOutlined />} onClick={handleConvert}>
                商机转项目
              </Button>
            )}
            {canDelete && (
              <Popconfirm
                title="确认删除该报备？"
                description="仅草稿状态可删除，删除后不可恢复。"
                onConfirm={handleDelete}
              >
                <Button danger icon={<DeleteOutlined />}>删除</Button>
              </Popconfirm>
            )}
          </Space>
        }
      />

      <Card title="状态流转" style={{ marginBottom: 24 }}>
        <Steps
          size="small"
          current={statusIndex}
          items={STATUS_FLOW.map((s) => ({ title: s.title, description: s.description }))}
        />
      </Card>

      <Card title="基本信息" style={{ marginBottom: 24 }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="报备 ID">#{opp.id}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={STATUS_COLOR[opp.status || 1]}>{opp.statusName}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="客户">{opp.customerName}</Descriptions.Item>
          <Descriptions.Item label="业务域">{opp.businessDomainName}</Descriptions.Item>
          <Descriptions.Item label="项目类型">{opp.projectTypeName}</Descriptions.Item>
          <Descriptions.Item label="预计金额">
            {opp.amount != null ? `${opp.amount} 万元` : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="单位">{opp.unitName || '-'}</Descriptions.Item>
          <Descriptions.Item label="警种">{opp.policeTypeName || '-'}</Descriptions.Item>
          <Descriptions.Item label="提交人">{opp.submittedByName}</Descriptions.Item>
          <Descriptions.Item label="提交次数">{opp.submitCount}</Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {opp.createdAt ? dayjs(opp.createdAt).format('YYYY-MM-DD HH:mm') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="生效时间">
            {opp.effectiveAt ? dayjs(opp.effectiveAt).format('YYYY-MM-DD HH:mm') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="失效时间">
            {opp.expiredAt ? dayjs(opp.expiredAt).format('YYYY-MM-DD HH:mm') : '-'}</Descriptions.Item>
          <Descriptions.Item label="冷却期截止">
            {opp.coolingUntil ? dayjs(opp.coolingUntil).format('YYYY-MM-DD HH:mm') : '-'}
            {opp.coolingUntil && dayjs(opp.coolingUntil).isAfter(dayjs()) && (
              <Tag color="red" style={{ marginLeft: 8 }}>冷却中</Tag>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="审批人">{opp.approvedByName || '-'}</Descriptions.Item>
          <Descriptions.Item label="审批时间">
            {opp.approvedAt ? dayjs(opp.approvedAt).format('YYYY-MM-DD HH:mm') : '-'}</Descriptions.Item>
          <Descriptions.Item label="驳回原因" span={2}>
            {opp.rejectReason || '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="审批记录">
        {(opp.approvalLogs || []).length === 0 ? (
          <div style={{ color: '#999' }}>暂无审批记录</div>
        ) : (
          <Timeline
            items={(opp.approvalLogs || []).map((log: ApprovalLog) => ({
              color: log.action === 2 ? 'green' : log.action === 3 ? 'red' : 'blue',
              dot:
                log.action === 2 ? (
                  <CheckCircleOutlined />
                ) : log.action === 3 ? (
                  <CloseCircleOutlined />
                ) : undefined,
              children: (
                <div>
                  <strong>{log.actionName}</strong> · {log.operatorName} ·{' '}
                  {log.createdAt ? dayjs(log.createdAt).format('YYYY-MM-DD HH:mm') : '-'}
                  {log.comment && <div style={{ marginTop: 4 }}>意见：{log.comment}</div>}
                </div>
              ),
            }))}
          />
        )}
      </Card>

      <Modal
        title="审批报备"
        open={approveOpen}
        onCancel={() => setApproveOpen(false)}
        onOk={handleApprove}
        confirmLoading={approveMut.isPending}
        destroyOnClose
      >
        <Form form={approveForm} layout="vertical" preserve={false}>
          <Form.Item
            label="审批结果"
            name="action"
            rules={[{ required: true, message: '请选择审批结果' }]}
          >
            <Select
              placeholder="请选择审批结果"
              options={[
                { value: 2, label: '通过' },
                { value: 3, label: '驳回' },
              ]}
            />
          </Form.Item>
          <Form.Item
            label="审批意见"
            name="comment"
            rules={[
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (getFieldValue('action') === 3 && (!value || value.length < 5)) {
                    return Promise.reject(new Error('驳回原因不能少于 5 个字'))
                  }
                  return Promise.resolve()
                },
              }),
            ]}
          >
            <Input.TextArea rows={4} placeholder="通过可留空，驳回须填写原因（至少 5 个字）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
