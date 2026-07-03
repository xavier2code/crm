import { useEffect } from 'react'
import { Card, Form, Input, Select } from 'antd'

import { FormFooter } from '@/components/FormFooter'
import { PageHeader } from '@/components/PageHeader'
import { useProjects } from '@/hooks/useProjects'
import type { ContractRequest } from '@/api/contract'
import type { ContractVO } from '@/api/contract'

interface ContractFormProps {
  initialData?: ContractVO
  loading?: boolean
  onSubmit: (values: ContractRequest) => void
  onReset?: () => void
  title: string
  breadcrumbs?: string[]
}

export default function ContractForm({
  initialData,
  loading,
  onSubmit,
  onReset,
  title,
  breadcrumbs,
}: ContractFormProps) {
  const [form] = Form.useForm<ContractRequest>()
  const { data: projects } = useProjects({ current: 1, size: 1000 })

  useEffect(() => {
    if (initialData) {
      form.setFieldsValue({
        projectId: initialData.projectId,
        amount: initialData.amount,
      })
    }
  }, [initialData, form])

  const projectOptions = (projects?.records || []).map((p) => ({
    value: p.id,
    label: `[#${p.id}] ${p.name}`,
  }))

  return (
    <div>
      <PageHeader title={title} breadcrumbs={breadcrumbs} showBack />

      <Form<ContractRequest> form={form} layout="vertical" onFinish={onSubmit} autoComplete="off">
        <Card title="合同信息">
          <Form.Item
            name="projectId"
            label="关联项目"
            rules={[{ required: true, message: '请选择关联项目' }]}
          >
            <Select
              placeholder="请选择项目"
              options={projectOptions}
              showSearch
              optionFilterProp="label"
              disabled={!!initialData}
            />
          </Form.Item>

          <Form.Item
            name="amount"
            label="合同金额（万元）"
            rules={[{ required: true, message: '请输入合同金额' }]}
          >
            <Input type="number" placeholder="请输入合同金额" />
          </Form.Item>
        </Card>

        <FormFooter loading={loading} onReset={onReset || (() => form.resetFields())} />
      </Form>
    </div>
  )
}
