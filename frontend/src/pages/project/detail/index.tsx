import { useMemo, useState } from 'react'
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Select,
  DatePicker,
  Tabs,
  Tag,
  Space,
  Table,
  Modal,
  message,
  Checkbox,
  Slider,
  Row,
  Col,
  Divider,
  Descriptions,
} from 'antd'
import { EditOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons'
import { useParams } from 'react-router-dom'
import dayjs from 'dayjs'

import { PageHeader } from '@/components/PageHeader'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'
import {
  useProjectProcess,
  useUpdateProject,
  useUpdateProjectPNode,
  useUpdateProjectStage6,
  useUpdateProjectMilestone,
  useSaveProjectBiddingNode,
  useSaveProjectContractNode,
  useAddProjectPaymentNode,
  useUpdateProjectPaymentNode,
  useDeleteProjectPaymentNode,
  useSubmitProjectScore,
  useProjectScoreDimensions,
} from '@/hooks/useProjects'
import type { components } from '@/types/api'

type PaymentNodeVO = components['schemas']['PaymentNodeVO']
type MilestoneVO = components['schemas']['MilestoneVO']

const statusOptions = [
  { value: 1, label: '项目中', color: 'processing' },
  { value: 2, label: '项目完成', color: 'success' },
  { value: 3, label: '项目中断', color: 'warning' },
  { value: 4, label: '项目终止', color: 'error' },
]

const pNodeOptions = [
  { value: 1, label: 'P1 待价值认可' },
  { value: 2, label: 'P2 价值认可' },
  { value: 3, label: 'P3 完成立项签批' },
  { value: 4, label: 'P4 通过党委会' },
  { value: 5, label: 'P5 通过预算批复' },
  { value: 6, label: 'P6 确认采购中标' },
  { value: 7, label: 'P7 完成合同签订' },
  { value: 8, label: 'P8 完成账号开通' },
]

const paymentStatusOptions = [
  { value: 1, label: '待回款' },
  { value: 2, label: '已到账' },
  { value: 3, label: '逾期' },
]

const milestoneLabels: { key: keyof MilestoneVO; label: string }[] = [
  { key: 'preOpenBusiness', label: '提前开通业务' },
  { key: 'biddingPublished', label: '招标挂网' },
  { key: 'bidSubmitted', label: '项目投标' },
  { key: 'bidWonPublished', label: '中标挂网' },
  { key: 'contractSigned', label: '签订合同' },
  { key: 'serviceOpened', label: '正常开通' },
  { key: 'acceptanceDone', label: '项目验收' },
  { key: 'invoiceIssued', label: '开具发票' },
  { key: 'paymentDone', label: '支付手续' },
]

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>()
  const projectId = Number(id)
  const [baseForm] = Form.useForm()
  const [biddingForm] = Form.useForm()
  const [contractForm] = Form.useForm()
  const [scoreForm] = Form.useForm()
  const [milestoneForm] = Form.useForm()
  const [paymentForm] = Form.useForm()

  const { permissionCodes } = useAuthStore()
  const { getDict } = useDictStore()
  const canEdit = permissionCodes.includes('project:edit')

  const toBooleanFlags = (obj: Record<string, unknown> | null | undefined, dateKeys?: string[]) => {
    if (!obj) return {}
    const result: Record<string, unknown> = { ...obj }
    Object.keys(result).forEach((key) => {
      const v = result[key]
      if (v === 0 || v === 1) {
        result[key] = v === 1
      } else if (dateKeys?.includes(key) && typeof v === 'string') {
        result[key] = dayjs(v)
      }
    })
    return result
  }

  const { data: project, isLoading } = useProjectProcess(projectId)
  const { data: scoreDimensions } = useProjectScoreDimensions()
  const updateProjectMutation = useUpdateProject()
  const updatePNodeMutation = useUpdateProjectPNode()
  const updateStage6Mutation = useUpdateProjectStage6()
  const updateMilestoneMutation = useUpdateProjectMilestone()
  const saveBiddingNodeMutation = useSaveProjectBiddingNode()
  const saveContractNodeMutation = useSaveProjectContractNode()
  const addPaymentNodeMutation = useAddProjectPaymentNode()
  const updatePaymentNodeMutation = useUpdateProjectPaymentNode()
  const deletePaymentNodeMutation = useDeleteProjectPaymentNode()
  const submitScoreMutation = useSubmitProjectScore()

  const [paymentModalOpen, setPaymentModalOpen] = useState(false)
  const [editingPayment, setEditingPayment] = useState<PaymentNodeVO | null>(null)

  const businessDomainOptions = useMemo(() => getDict('business_domain').map((d) => ({ value: d.code, label: d.name })), [getDict])
  const stage6Options = useMemo(() => getDict('stage_6').map((d) => ({ value: d.code, label: d.name })), [getDict])
  const adminLevelOptions = useMemo(() => getDict('admin_level').map((d) => ({ value: Number(d.code), label: d.name })), [getDict])
  const salesMethodOptions = useMemo(() => getDict('sales_method').map((d) => ({ value: d.code, label: d.name })), [getDict])
  const customerLayerOptions = useMemo(
    () => getDict('customer_layer').map((d) => ({ value: d.code, label: d.name })),
    [getDict]
  )
  const purchaseMethodOptions = useMemo(
    () => getDict('purchase_method').map((d) => ({ value: Number(d.code), label: d.name })),
    [getDict]
  )

  const handleUpdateBase = async (values: Record<string, unknown>) => {
    try {
      const payload = {
        ...values,
        opportunityId: project?.opportunityId,
        expectedSignDate: values.expectedSignDate ? (values.expectedSignDate as dayjs.Dayjs).format('YYYY-MM-DD') : undefined,
      } as components['schemas']['ProjectRequest']
      await updateProjectMutation.mutateAsync({ id: projectId, data: payload })
      message.success('保存成功')
    } catch {
      // ignored
    }
  }

  const handleUpdatePNode = async (pNode: number) => {
    try {
      await updatePNodeMutation.mutateAsync({ id: projectId, pNode })
      message.success('P级节点更新成功')
    } catch {
      // ignored
    }
  }

  const handleUpdateStage6 = async (stage6: string) => {
    try {
      await updateStage6Mutation.mutateAsync({ id: projectId, stage6 })
      message.success('阶段更新成功')
    } catch {
      // ignored
    }
  }

  const handleUpdateMilestone = async (values: Record<string, unknown>) => {
    try {
      const payload: MilestoneVO = {}
      milestoneLabels.forEach((m) => {
        const v = values[m.key]
        payload[m.key] = v === true ? 1 : v === false ? 0 : undefined
      })
      await updateMilestoneMutation.mutateAsync({ id: projectId, data: payload })
      message.success('里程碑更新成功')
    } catch {
      // ignored
    }
  }

  const handleSaveBiddingNode = async (values: Record<string, unknown>) => {
    try {
      const payload = { ...values } as components['schemas']['BiddingNodeRequest']
      if (values.noticeOriginalArchived !== undefined) {
        payload.noticeOriginalArchived = values.noticeOriginalArchived === true ? 1 : 0
      }
      await saveBiddingNodeMutation.mutateAsync({ id: projectId, data: payload })
      message.success('招投标节点保存成功')
    } catch {
      // ignored
    }
  }

  const handleSaveContractNode = async (values: Record<string, unknown>) => {
    try {
      const payload = { ...values } as components['schemas']['ContractNodeRequest']
      if (values.originalArchived !== undefined) payload.originalArchived = values.originalArchived === true ? 1 : 0
      if (values.hasWarranty !== undefined) payload.hasWarranty = values.hasWarranty === true ? 1 : 0
      if (values.hasSettlementAudit !== undefined) payload.hasSettlementAudit = values.hasSettlementAudit === true ? 1 : 0
      await saveContractNodeMutation.mutateAsync({ id: projectId, data: payload })
      message.success('合同节点保存成功')
    } catch {
      // ignored
    }
  }

  const handleSubmitScore = async (values: Record<string, unknown>) => {
    try {
      const scores = Object.entries(values).map(([dimension, score]) => ({
        dimension,
        score: Number(score),
      }))
      await submitScoreMutation.mutateAsync({ projectId, scores })
      message.success('评分提交成功')
      scoreForm.resetFields()
    } catch {
      // ignored
    }
  }

  const openPaymentModal = (record?: PaymentNodeVO) => {
    setEditingPayment(record || null)
    paymentForm.resetFields()
    if (record) {
      paymentForm.setFieldsValue({
        ...record,
        receivedDate: record.receivedDate ? dayjs(record.receivedDate) : undefined,
      })
    }
    setPaymentModalOpen(true)
  }

  const handleSavePayment = async (values: Record<string, unknown>) => {
    try {
      const payload = {
        ...values,
        receivedDate: values.receivedDate ? (values.receivedDate as dayjs.Dayjs).format('YYYY-MM-DD') : undefined,
      } as PaymentNodeVO
      if (editingPayment?.id) {
        await updatePaymentNodeMutation.mutateAsync({ projectId, nodeId: editingPayment.id, data: payload })
      } else {
        await addPaymentNodeMutation.mutateAsync({ id: projectId, data: payload })
      }
      message.success('回款节点保存成功')
      setPaymentModalOpen(false)
      setEditingPayment(null)
      paymentForm.resetFields()
    } catch {
      // ignored
    }
  }

  const handleDeletePayment = async (nodeId: number) => {
    try {
      await deletePaymentNodeMutation.mutateAsync({ projectId, nodeId })
      message.success('删除成功')
    } catch {
      // ignored
    }
  }

  const paymentColumns = [
    { title: '期数', dataIndex: 'paymentNo', key: 'paymentNo' },
    { title: '金额（万元）', dataIndex: 'amount', key: 'amount' },
    {
      title: '到账日期',
      dataIndex: 'receivedDate',
      key: 'receivedDate',
      render: (value: string) => value || '-',
    },
    { title: '发票号', dataIndex: 'invoiceNo', key: 'invoiceNo', render: (value: string) => value || '-' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value: number) => paymentStatusOptions.find((s) => s.value === value)?.label || '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: PaymentNodeVO) => (
        <Space>
          {canEdit && (
            <Button type="link" icon={<EditOutlined />} onClick={() => openPaymentModal(record)}>
              编辑
            </Button>
          )}
          {canEdit && (
            <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDeletePayment(record.id!)}>
              删除
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const statusItem = statusOptions.find((s) => s.value === project?.status)
  const pNodeValue = project?.pnode as number | undefined

  return (
    <div>
      <PageHeader
        title="项目详情"
        showBack
        extra={
          <Tag color={statusItem?.color as string}>{statusItem?.label}</Tag>
        }
      />

      {isLoading ? (
        <div>加载中...</div>
      ) : !project ? (
        <div>项目不存在</div>
      ) : (
        <Tabs
          defaultActiveKey="base"
          items={[
            {
              key: 'base',
              label: '基础信息',
              children: (
                <Space direction="vertical" style={{ width: '100%' }} size="large">
                  <Card title="项目基础信息" extra={canEdit && <Button type="primary" onClick={() => baseForm.submit()}>保存</Button>}>
                    <Form
                      form={baseForm}
                      layout="vertical"
                      onFinish={handleUpdateBase}
                      initialValues={{
                        ...project,
                        expectedSignDate: project.expectedSignDate ? dayjs(project.expectedSignDate) : undefined,
                      }}
                    >
                      <Row gutter={16}>
                        <Col span={12}>
                          <Form.Item name="name" label="项目名称" rules={[{ required: true }]}>
                            <Input disabled={!canEdit} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="businessDomain" label="业务域">
                            <Select disabled={!canEdit} allowClear options={businessDomainOptions} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="productCategory" label="产品类别">
                            <Input disabled={!canEdit} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="adminLevel" label="行政级别">
                            <Select disabled={!canEdit} allowClear options={adminLevelOptions} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="amount" label="金额（万元）">
                            <InputNumber disabled={!canEdit} min={0} precision={2} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="performanceCount" label="业绩数">
                            <InputNumber disabled={!canEdit} min={0} precision={0} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="salesMethod" label="销售方式">
                            <Select disabled={!canEdit} allowClear options={salesMethodOptions} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="customerLayer" label="客户分层">
                            <Select disabled={!canEdit} allowClear options={customerLayerOptions} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="expectedSignDate" label="预计签单日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                      </Row>
                    </Form>
                  </Card>

                  <Card title="阶段与状态">
                    <Descriptions column={2} bordered>
                      <Descriptions.Item label="P级节点">
                        <Select
                          value={pNodeValue}
                          disabled={!canEdit || project.status === 2 || project.status === 4}
                          options={pNodeOptions}
                          onChange={handleUpdatePNode}
                          style={{ width: 220 }}
                        />
                      </Descriptions.Item>
                      <Descriptions.Item label="6大阶段">
                        <Select
                          value={project.stage6}
                          disabled={!canEdit || project.status === 2 || project.status === 4}
                          options={stage6Options}
                          onChange={handleUpdateStage6}
                          style={{ width: 220 }}
                        />
                      </Descriptions.Item>
                      <Descriptions.Item label="当前双精评分">{project.currentScore ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="里程碑完成度">{project.completionRate ?? 0}%</Descriptions.Item>
                    </Descriptions>
                  </Card>
                </Space>
              ),
            },
            {
              key: 'process',
              label: '过程管理',
              children: (
                <Space direction="vertical" style={{ width: '100%' }} size="large">
                  <Card title="9项里程碑" extra={canEdit && <Button type="primary" onClick={() => milestoneForm.submit()}>保存</Button>}>
                    <Form
                      form={milestoneForm}
                      layout="inline"
                      onFinish={handleUpdateMilestone}
                      initialValues={toBooleanFlags(project.milestone || {})}
                    >
                      <Row gutter={[16, 16]} style={{ width: '100%' }}>
                        {milestoneLabels.map((m) => (
                          <Col span={8} key={m.key}>
                            <Form.Item name={m.key} valuePropName="checked" style={{ marginBottom: 0 }}>
                              <Checkbox disabled={!canEdit}>{m.label}</Checkbox>
                            </Form.Item>
                          </Col>
                        ))}
                      </Row>
                    </Form>
                  </Card>

                  <Card title="招投标节点" extra={canEdit && <Button type="primary" onClick={() => biddingForm.submit()}>保存</Button>}>
                    <Form
                      form={biddingForm}
                      layout="vertical"
                      onFinish={handleSaveBiddingNode}
                      initialValues={toBooleanFlags(project.biddingNode || {}, [
                        'announcementDate',
                        'registrationStart',
                        'registrationEnd',
                        'bidDate',
                        'bidResultStart',
                        'bidResultEnd',
                        'noticeReceivedDate',
                      ])}
                    >
                      <Row gutter={16}>
                        <Col span={12}>
                          <Form.Item name="biddingAgency" label="招标（代理）机构">
                            <Input disabled={!canEdit} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="purchaseMethod" label="采购方式">
                            <Select disabled={!canEdit} allowClear options={purchaseMethodOptions} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="announcementDate" label="招标/需求公告日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="bidDate" label="投标日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="registrationStart" label="报名开始日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="registrationEnd" label="报名结束日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="bidResultStart" label="中标公告开始日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="bidResultEnd" label="中标公告结束日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="noticeReceivedDate" label="领取中标通知书日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="noticeOriginalArchived" label="原件上交公司" valuePropName="checked">
                            <Checkbox disabled={!canEdit}>已上交</Checkbox>
                          </Form.Item>
                        </Col>
                      </Row>
                    </Form>
                  </Card>

                  <Card title="合同节点" extra={canEdit && <Button type="primary" onClick={() => contractForm.submit()}>保存</Button>}>
                    <Form
                      form={contractForm}
                      layout="vertical"
                      onFinish={handleSaveContractNode}
                      initialValues={toBooleanFlags(project.contractNode || {}, [
                        'draftDate',
                        'approveDate',
                        'invoiceDate',
                        'receivedDate',
                      ])}
                    >
                      <Row gutter={16}>
                        <Col span={12}>
                          <Form.Item name="draftDate" label="起草日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="approveDate" label="审定日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="originalArchived" label="原件上交公司" valuePropName="checked">
                            <Checkbox disabled={!canEdit}>已上交</Checkbox>
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="hasWarranty" label="是否有质保金" valuePropName="checked">
                            <Checkbox disabled={!canEdit}>有</Checkbox>
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="warrantyAmount" label="质保金金额（万元）">
                            <InputNumber disabled={!canEdit} min={0} precision={2} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="hasSettlementAudit" label="是否有竣工结算审计" valuePropName="checked">
                            <Checkbox disabled={!canEdit}>有</Checkbox>
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="invoiceDate" label="开具发票日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name="receivedDate" label="收款到账日期">
                            <DatePicker disabled={!canEdit} style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={24}>
                          <Form.Item name="reviewDept" label="局内审核部门及流程">
                            <Input.TextArea disabled={!canEdit} rows={2} />
                          </Form.Item>
                        </Col>
                        <Col span={24}>
                          <Form.Item name="paymentMethod" label="付款方式">
                            <Input disabled={!canEdit} />
                          </Form.Item>
                        </Col>
                        <Col span={24}>
                          <Form.Item name="paymentRatio" label="付款比例">
                            <Input disabled={!canEdit} />
                          </Form.Item>
                        </Col>
                        <Col span={24}>
                          <Form.Item name="paymentTerms" label="付款条件">
                            <Input disabled={!canEdit} />
                          </Form.Item>
                        </Col>
                        <Col span={24}>
                          <Form.Item name="paymentNodes" label="付款节点">
                            <Input.TextArea disabled={!canEdit} rows={2} />
                          </Form.Item>
                        </Col>
                        <Col span={24}>
                          <Form.Item name="acceptanceDept" label="验收部门及流程">
                            <Input.TextArea disabled={!canEdit} rows={2} />
                          </Form.Item>
                        </Col>
                        <Col span={24}>
                          <Form.Item name="paymentVoucherDept" label="收款单据流转部门">
                            <Input disabled={!canEdit} />
                          </Form.Item>
                        </Col>
                      </Row>
                    </Form>
                  </Card>

                  <Card
                    title="回款节点"
                    extra={
                      canEdit && (
                        <Button type="primary" icon={<PlusOutlined />} onClick={() => openPaymentModal()}>
                          新增回款
                        </Button>
                      )
                    }
                  >
                    <Table
                      rowKey="id"
                      dataSource={project.paymentNodes || []}
                      columns={paymentColumns}
                      pagination={false}
                    />
                  </Card>
                </Space>
              ),
            },
            {
              key: 'score',
              label: '双精评分',
              children: (
                <Space direction="vertical" style={{ width: '100%' }} size="large">
                  <Card title={`当前总分：${project.currentScore ?? 0}`}>
                    <Descriptions column={2} bordered>
                      {(project.scoreHistory || []).slice(0, 1)[0]?.scores?.map((item) => (
                        <Descriptions.Item key={item.dimension} label={`${item.dimensionName}（${item.weight}%）`}>
                          {item.score}
                        </Descriptions.Item>
                      ))}
                    </Descriptions>
                  </Card>

                  {canEdit && scoreDimensions && (
                    <Card title="提交本周评分" extra={<Button type="primary" onClick={() => scoreForm.submit()}>提交</Button>}>
                      <Form form={scoreForm} layout="vertical" onFinish={handleSubmitScore}>
                        {Object.entries(scoreDimensions).map(([dimension, config]) => (
                          <Form.Item
                            key={dimension}
                            name={dimension}
                            label={`${config.name}（权重 ${config.weight}%）`}
                            initialValue={0}
                            rules={[
                              { required: true, message: '请输入评分' },
                              {
                                type: 'number',
                                min: 0,
                                max: 100,
                                message: '评分必须在 0-100 之间',
                              },
                            ]}
                          >
                            <Slider min={0} max={100} marks={{ 0: '0', 50: '50', 100: '100' }} />
                          </Form.Item>
                        ))}
                      </Form>
                    </Card>
                  )}

                  <Card title="评分历史">
                    {(project.scoreHistory || []).map((week) => (
                      <div key={week.snapshotWeek} style={{ marginBottom: 16 }}>
                        <Divider>
                          {week.snapshotWeek} — 总分 {week.totalScore}
                        </Divider>
                        <Row gutter={[16, 8]}>
                          {week.scores?.map((s) => (
                            <Col span={8} key={s.dimension}>
                              {s.dimensionName}: {s.score}（{s.weight}%）
                            </Col>
                          ))}
                        </Row>
                      </div>
                    ))}
                  </Card>
                </Space>
              ),
            },
          ]}
        />
      )}

      <Modal
        title={editingPayment ? '编辑回款节点' : '新增回款节点'}
        open={paymentModalOpen}
        onCancel={() => {
          setPaymentModalOpen(false)
          setEditingPayment(null)
          paymentForm.resetFields()
        }}
        onOk={() => paymentForm.submit()}
        destroyOnClose
      >
        <Form form={paymentForm} layout="vertical" onFinish={handleSavePayment}>
          <Form.Item name="paymentNo" label="期数" rules={[{ required: true }]}>
            <InputNumber min={1} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="amount" label="金额（万元）" rules={[{ required: true }]}>
            <InputNumber min={0} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="receivedDate" label="到账日期">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="invoiceNo" label="发票号">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select options={paymentStatusOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
