import { Card, Statistic, Row, Col } from 'antd'

export default function DashboardPage() {
  return (
    <div>
      <h2>工作台</h2>
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic title="本月新增客户" value={12} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="进行中商机" value={8} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="待审核报销" value={5} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="本月合同金额" value={128000} prefix="¥" />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
