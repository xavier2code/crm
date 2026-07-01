import { Button, Space, Card } from 'antd'
import { useNavigate } from 'react-router-dom'

interface FormFooterProps {
  loading?: boolean
  onReset?: () => void
  showReset?: boolean
  cancelPath?: string
}

export function FormFooter({ loading, onReset, showReset = true, cancelPath }: FormFooterProps) {
  const navigate = useNavigate()

  return (
    <Card style={{ position: 'sticky', bottom: 0, marginTop: 24 }}>
      <Space>
        <Button type="primary" htmlType="submit" loading={loading}>
          保存
        </Button>
        {showReset && (
          <Button onClick={onReset} disabled={loading}>
            重置
          </Button>
        )}
        <Button onClick={() => (cancelPath ? navigate(cancelPath) : navigate(-1))} disabled={loading}>
          取消
        </Button>
      </Space>
    </Card>
  )
}
