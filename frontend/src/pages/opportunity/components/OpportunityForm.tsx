import { useEffect } from 'react'
import { Alert, Card, Form, InputNumber, Select } from 'antd'

import { DictSelect } from '@/components/DictSelect'
import { FormFooter } from '@/components/FormFooter'
import { PageHeader } from '@/components/PageHeader'
import type { OpportunityRequest } from '@/api/opportunity'
import type { OpportunityDetailVO } from '@/api/opportunity'

import { CustomerSelect } from './CustomerSelect'

interface OpportunityFormProps {
  initialData?: OpportunityDetailVO
  loading?: boolean
  onSubmit: (values: OpportunityRequest) => void
  onReset?: () => void
  title: string
  breadcrumbs?: string[]
}

const PROJECT_TYPE_OPTIONS = [
  { value: 1, label: '新签' },
  { value: 2, label: '续签' },
  { value: 3, label: '试用' },
]

export default function OpportunityForm({
  initialData,
  loading,
  onSubmit,
  onReset,
  title,
  breadcrumbs,
}: OpportunityFormProps) {
  const [form] = Form.useForm<OpportunityRequest>()

  useEffect(() => {
    if (initialData) {
      form.setFieldsValue({
        customerId: initialData.customerId,
        businessDomain: initialData.businessDomain,
        projectType: initialData.projectType,
        amount: initialData.amount,
      })
    }
  }, [initialData, form])

  return (
    <div>
      <PageHeader title={title} breadcrumbs={breadcrumbs} showBack />

      <Alert
        type="info"
        showIcon
        message="报备保护规则"
        description="同一客户的同一业务域下，只能存在一个审批中或生效中的报备。跨业务域可独立报备。"
        style={{ marginBottom: 24 }}
      />

      <Form<OpportunityRequest>
        form={form}
        layout="vertical"
        onFinish={onSubmit}
        autoComplete="off"
      >
        <Card title="报备信息">
          <Form.Item
            label="客户"
            name="customerId"
            rules={[{ required: true, message: '请选择客户' }]}
          >
            <CustomerSelect placeholder="请选择客户" />
          </Form.Item>

          <Form.Item
            label="业务域"
            name="businessDomain"
            rules={[{ required: true, message: '请选择业务域' }]}
          >
            <DictSelect type="business_domain" placeholder="请选择业务域" />
          </Form.Item>

          <Form.Item
            label="项目类型"
            name="projectType"
            rules={[{ required: true, message: '请选择项目类型' }]}
          >
            <Select placeholder="请选择项目类型" options={PROJECT_TYPE_OPTIONS} />
          </Form.Item>

          <Form.Item label="预计金额（万元）" name="amount">
            <InputNumber
              style={{ width: '100%' }}
              min={0}
              precision={2}
              placeholder="请输入预计金额"
            />
          </Form.Item>
        </Card>

        <FormFooter loading={loading} onReset={onReset || (() => form.resetFields())} />
      </Form>
    </div>
  )
}
