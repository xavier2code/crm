import { useEffect } from 'react'
import { Button, Card, Form, Input, Select, Space, Tag } from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'

import { DictSelect } from '@/components/DictSelect'
import { FormFooter } from '@/components/FormFooter'
import { PageHeader } from '@/components/PageHeader'
import { useAllUnits } from '@/hooks/useAdminUnits'
import type { CustomerRequest, CustomerVO } from '@/api/customer'

interface CustomerFormProps {
  initialData?: CustomerVO
  loading?: boolean
  onSubmit: (values: CustomerRequest) => void
  onReset?: () => void
  title: string
  breadcrumbs?: string[]
}

const CONTACT_TYPE_OPTIONS = [
  { value: 1, label: '重要决策人', color: 'red' },
  { value: 2, label: '业务对接人', color: 'blue' },
  { value: 3, label: '操作员', color: 'green' },
]

export default function CustomerForm({
  initialData,
  loading,
  onSubmit,
  onReset,
  title,
  breadcrumbs,
}: CustomerFormProps) {
  const [form] = Form.useForm<CustomerRequest>()
  const { data: units } = useAllUnits()

  useEffect(() => {
    if (initialData) {
      form.setFieldsValue({
        unitId: initialData.unitId,
        policeType: initialData.policeType,
        customerLayer: initialData.customerLayer,
        contacts: initialData.contacts?.map((c) => ({
          name: c.name,
          title: c.title,
          phone: c.phone,
          contactType: c.contactType,
          isPrimary: c.isPrimary,
        })),
      })
    }
  }, [initialData, form])

  const unitOptions = (units || []).map((u) => ({
    value: u.id,
    label: u.region ? `${u.name}（${u.region}）` : u.name,
  }))

  const handleFinish = (values: CustomerRequest) => {
    onSubmit(values)
  }

  return (
    <div>
      <PageHeader title={title} breadcrumbs={breadcrumbs} showBack />

      <Form<CustomerRequest>
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        autoComplete="off"
      >
        <Card title="客户信息" style={{ marginBottom: 24 }}>
          <Form.Item
            label="单位"
            name="unitId"
            rules={[{ required: true, message: '请选择单位' }]}
          >
            <Select
              placeholder="请选择单位"
              options={unitOptions}
              showSearch
              optionFilterProp="label"
              allowClear
            />
          </Form.Item>

          <Form.Item
            label="警种"
            name="policeType"
            rules={[{ required: true, message: '请选择警种' }]}
          >
            <DictSelect type="police_type" placeholder="请选择警种" />
          </Form.Item>

          <Form.Item label="客户分层" name="customerLayer">
            <DictSelect type="customer_layer" placeholder="请选择客户分层" allowClear />
          </Form.Item>
        </Card>

        <Card title="联系人" extra={<Tag color="blue">至少填写一个联系人</Tag>}>
          <Form.List
            name="contacts"
            rules={[
              {
                validator: async (_, value) => {
                  if (!value || value.length === 0) {
                    return Promise.reject(new Error('请至少添加一个联系人'))
                  }
                },
              },
            ]}
          >
            {(fields, { add, remove }) => (
              <Space direction="vertical" style={{ width: '100%' }}>
                {fields.map(({ key, name, ...restField }) => (
                  <Space
                    key={key}
                    style={{ display: 'flex', marginBottom: 8 }}
                    align="baseline"
                  >
                    <Form.Item
                      {...restField}
                      name={[name, 'name']}
                      rules={[{ required: true, message: '请输入姓名' }]}
                    >
                      <Input placeholder="姓名" />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'title']}
                      rules={[{ required: true, message: '请输入职务' }]}
                    >
                      <Input placeholder="职务" />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'phone']}
                      rules={[
                        { required: true, message: '请输入手机号' },
                        { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
                      ]}
                    >
                      <Input placeholder="手机号" />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'contactType']}
                      rules={[{ required: true, message: '请选择类型' }]}
                    >
                      <Select
                        placeholder="类型"
                        options={CONTACT_TYPE_OPTIONS}
                        style={{ minWidth: 120 }}
                      />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'isPrimary']}
                      valuePropName="checked"
                      getValueProps={(v) => ({ checked: v === 1 })}
                      getValueFromEvent={(e) => (e.target.checked ? 1 : 0)}
                    >
                      <Input type="checkbox" />
                    </Form.Item>
                    <MinusCircleOutlined onClick={() => remove(name)} />
                  </Space>
                ))}
                <Button
                  type="dashed"
                  onClick={() => add({ contactType: 2, isPrimary: 0 })}
                  block
                  icon={<PlusOutlined />}
                  style={{ marginTop: 8 }}
                >
                  添加联系人
                </Button>
              </Space>
            )}
          </Form.List>
        </Card>

        <FormFooter loading={loading} onReset={onReset || (() => form.resetFields())} />
      </Form>
    </div>
  )
}
