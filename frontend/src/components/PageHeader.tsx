import { Breadcrumb, Button, Space } from 'antd'
import { useNavigate } from 'react-router-dom'
import { ArrowLeftOutlined } from '@ant-design/icons'

interface PageHeaderProps {
  title: string
  breadcrumbs?: string[]
  extra?: React.ReactNode
  onBack?: () => void
  showBack?: boolean
}

export function PageHeader({ title, breadcrumbs, extra, onBack, showBack }: PageHeaderProps) {
  const navigate = useNavigate()

  return (
    <div style={{ marginBottom: 24 }}>
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Breadcrumb style={{ marginBottom: 12 }}>
          {breadcrumbs.map((item, index) => (
            <Breadcrumb.Item key={index}>{item}</Breadcrumb.Item>
          ))}
        </Breadcrumb>
      )}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          {showBack && (
            <Button icon={<ArrowLeftOutlined />} onClick={onBack || (() => navigate(-1))}>
              返回
            </Button>
          )}
          <h2 style={{ margin: 0 }}>{title}</h2>
        </Space>
        <div>{extra}</div>
      </div>
    </div>
  )
}
