import { useNavigate } from 'react-router-dom'
import { Card, Col, Row } from 'antd'
import {
  ApartmentOutlined,
  FileTextOutlined,
  MoneyCollectOutlined,
  PercentageOutlined,
} from '@ant-design/icons'

const modules = [
  { title: '合同管理', path: '/contract', icon: <FileTextOutlined /> },
  { title: '返利管理', path: '/business/rebate', icon: <PercentageOutlined /> },
  { title: '单位分配', path: '/business/units', icon: <ApartmentOutlined /> },
  { title: '报销管理', path: '/reimbursement', icon: <MoneyCollectOutlined /> },
]

export default function BusinessPage() {
  const navigate = useNavigate()
  return (
    <Card title="商务管理">
      <Row gutter={[16, 16]}>
        {modules.map((m) => (
          <Col key={m.path} xs={24} sm={12} lg={8}>
            <Card hoverable onClick={() => navigate(m.path)}>
              <Card.Meta avatar={m.icon} title={m.title} />
            </Card>
          </Col>
        ))}
      </Row>
    </Card>
  )
}
