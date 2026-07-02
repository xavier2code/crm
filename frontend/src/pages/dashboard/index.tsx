import { Card, Col, Row, Skeleton, Statistic, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'

import { PageHeader } from '@/components/PageHeader'
import { useAuthStore } from '@/stores/auth'
import { useDashboard } from '@/hooks/useDashboard'

const { Text } = Typography

/**
 * 我的工作台（默认 /dashboard）
 *
 * 业务依据：CRM-渠道版-开发文档.md §9.1 工作台
 *   - 当前用户是渠道负责人或 CYBD：右上角提供"渠道工作台"入口
 *   - 当前用户是 BD：展示个人业绩
 */
export default function DashboardPage() {
  const { data, isLoading } = useDashboard()
  const { roles } = useAuthStore()
  const navigate = useNavigate()

  if (isLoading) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }

  // 渠道工作台入口：渠道负责人可看自己渠道；CYBD 可看任意
  const showChannelEntry =
    roles.includes('CHANNEL_HEAD') || roles.includes('CYBD')
  const channelId = data?.channelId

  return (
    <div>
      <PageHeader
        title={
          data?.channelName
            ? `工作台 — ${data.channelName}${data.region ? `（${data.region}）` : ''}`
            : '工作台 — 个人业绩'
        }
        extra={
          showChannelEntry && channelId ? (
            <a onClick={() => navigate(`/dashboard/channel/${channelId}`)}>
              查看渠道工作台 →
            </a>
          ) : null
        }
      />

      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic title="总客户数" value={data?.totalCustomers ?? 0} suffix="家" />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="总商机数" value={data?.totalOpportunities ?? 0} />
            <Text type="secondary" style={{ fontSize: 12 }}>
              生效中 {data?.activeOpportunities ?? 0}
            </Text>
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="总项目数" value={data?.totalProjects ?? 0} />
            <Text type="secondary" style={{ fontSize: 12 }}>
              进行中 {data?.inProgressProjects ?? 0}
            </Text>
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="总合同金额"
              value={data?.totalContractAmount ?? 0}
              precision={2}
              suffix="万元"
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col span={8}>
          <Card>
            <Statistic
              title="业绩完成率"
              value={data?.performanceRate ?? 0}
              precision={1}
              suffix="%"
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="待办任务数" value={data?.pendingTasks ?? 0} suffix="项" />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="今日待跟进客户"
              value={data?.todayFollowUpCount ?? 0}
              suffix="位"
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
