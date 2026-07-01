import { useState } from 'react'
import { Button, Form, Row, Col, Card } from 'antd'
import { DownOutlined, UpOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons'

export interface SearchField {
  name: string
  label: string
  component: React.ReactNode
}

interface SearchFormProps {
  fields: SearchField[]
  onSearch: (values: Record<string, unknown>) => void
  onReset?: () => void
  initialValues?: Record<string, unknown>
}

export function SearchForm({ fields, onSearch, onReset, initialValues }: SearchFormProps) {
  const [form] = Form.useForm()
  const [expanded, setExpanded] = useState(false)

  const handleReset = () => {
    form.resetFields()
    onReset?.()
  }

  const visibleFields = expanded ? fields : fields.slice(0, 3)

  return (
    <Card style={{ marginBottom: 16 }}>
      <Form form={form} initialValues={initialValues} onFinish={onSearch}>
        <Row gutter={16}>
          {visibleFields.map((field) => (
            <Col span={8} key={field.name}>
              <Form.Item name={field.name} label={field.label}>
                {field.component}
              </Form.Item>
            </Col>
          ))}
          <Col span={8} style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'flex-start' }}>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
              查询
            </Button>
            <Button style={{ marginLeft: 8 }} onClick={handleReset} icon={<ReloadOutlined />}>
              重置
            </Button>
            {fields.length > 3 && (
              <Button type="link" onClick={() => setExpanded(!expanded)}>
                {expanded ? <><UpOutlined /> 收起</> : <><DownOutlined /> 展开</>}
              </Button>
            )}
          </Col>
        </Row>
      </Form>
    </Card>
  )
}
