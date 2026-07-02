import { useMemo } from 'react'
import { Card, Col, Row, Skeleton, Statistic, Table, Tag, Typography, Empty } from 'antd'
import { useParams } from 'react-router-dom'
import type { ColumnsType } from 'antd/es/table'

import { PageHeader } from '@/components/PageHeader'
import { useChannelDashboard } from '@/hooks/useDashboard'

const { Text } = Typography

/**
 * 渠道工作台
 *
 * 业务依据：CRM-渠道版-开发文档.md §9.1 渠道工作台
 *   渠道总览 / 业绩 / 成员 / 客户分布
 * 权限：CHANNEL_HEAD（看自己负责的渠道） / CYBD（看任意渠道）
 * 路由：/dashboard/channel/:channelId
 */
export default function ChannelDashboardPage() {
  const { channelId: raw } = useParams<{ channelId: string }>()
  const channelId = useMemo(() => {
    const n = Number(raw)
    return Number.isFinite(n) && n > 0 ? n : undefined
  }, [raw])

  const { data, isLoading } = useChannelDashboard(channelId)

  if (isLoading) {
    return <Skeleton active paragraph={{ rows: 10 }} />
  }
  if (!data) {
    return <Empty description="暂无渠道数据" />
  }

  return (
    <div>
      <PageHeader
        title={
          data.channelName
            ? `渠道工作台 — ${data.channelName}${data.region ? `（${data.region}）` : ''}`
            : '渠道工作台'
        }
      />

      <Section title="渠道总览">
        <Row gutter={16}>
          <Col span={6}>
            <Card>
              <Statistic title="渠道成员数" value={data.memberCount ?? 0} suffix="人" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="总客户数" value={data.totalCustomers ?? 0} suffix="家" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="总商机数" value={data.totalOpportunities ?? 0} />
              <Text type="secondary" style={{ fontSize: 12 }}>
                生效中 {data.activeOpportunities ?? 0} · 失效 {data.expiredOpportunities ?? 0}
              </Text>
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="总项目数" value={data.totalProjects ?? 0} />
              <Text type="secondary" style={{ fontSize: 12 }}>
                进行中 {data.inProgressProjects ?? 0} · 已完成 {data.completedProjects ?? 0}
              </Text>
            </Card>
          </Col>
        </Row>
      </Section>

      <Section title="业绩">
        <Row gutter={16}>
          <Col span={6}>
            <Card>
              <Statistic
                title="总合同金额"
                value={data.totalContractAmount ?? 0}
                precision={2}
                suffix="万元"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="本年度合同金额"
                value={data.yearContractAmount ?? 0}
                precision={2}
                suffix="万元"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="应发返利总额"
                value={data.totalRebateAmount ?? 0}
                precision={2}
                suffix="元"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="未确认返利金额"
                value={data.unconfirmedRebateAmount ?? 0}
                precision={2}
                suffix="元"
                valueStyle={{
                  color: (data.unconfirmedRebateAmount ?? 0) > 0 ? '#cf1322' : undefined,
                }}
              />
            </Card>
          </Col>
        </Row>
      </Section>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={12}>
          <Card title="客户分布（按区域）">
            <DistributionMap data={data.customerRegionDistribution} emptyText="暂无客户" />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="项目状态分布">
            <DistributionMap data={data.projectStatusDistribution} emptyText="暂无项目" />
          </Card>
        </Col>
      </Row>

      <Section title="成员业绩分布">
        <MemberPerformanceTable data={(data.memberPerformances ?? []) as MemberRow[]} />
      </Section>
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 16 }}>
      <Typography.Title level={5} style={{ marginTop: 8, marginBottom: 8 }}>
        {title}
      </Typography.Title>
      {children}
    </div>
  )
}

function DistributionMap({
  data,
  emptyText,
}: {
  data?: Record<string, number>
  emptyText: string
}) {
  const entries = useMemo(() => {
    if (!data) return []
    return Object.entries(data).sort((a, b) => b[1] - a[1])
  }, [data])

  if (entries.length === 0) {
    return <Empty description={emptyText} image={Empty.PRESENTED_IMAGE_SIMPLE} />
  }
  return (
    <div>
      {entries.map(([label, value]) => {
        const max = Math.max(...entries.map(([, v]) => v), 1)
        const pct = Math.round((value / max) * 100)
        return (
          <div key={label} style={{ marginBottom: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
              <span>{label}</span>
              <Text strong>{value}</Text>
            </div>
            <div
              style={{
                background: '#f0f0f0',
                borderRadius: 4,
                height: 8,
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  width: `${pct}%`,
                  height: '100%',
                  background: '#1677ff',
                  transition: 'width 0.3s',
                }}
              />
            </div>
          </div>
        )
      })}
    </div>
  )
}

interface MemberRow {
  userId: number
  userName?: string
  customerCount?: number
  opportunityCount?: number
  projectCount?: number
  contractAmount?: number
}

function MemberPerformanceTable({ data }: { data: MemberRow[] }) {
  const columns: ColumnsType<MemberRow> = [
    { title: '成员', dataIndex: 'userName', width: 140 },
    {
      title: '客户数',
      dataIndex: 'customerCount',
      width: 100,
      align: 'right' as const,
      render: (v?: number) => v ?? 0,
    },
    {
      title: '商机数',
      dataIndex: 'opportunityCount',
      width: 100,
      align: 'right' as const,
      render: (v?: number) => v ?? 0,
    },
    {
      title: '项目数',
      dataIndex: 'projectCount',
      width: 100,
      align: 'right' as const,
      render: (v?: number) => v ?? 0,
    },
    {
      title: '合同金额（万元）',
      dataIndex: 'contractAmount',
      align: 'right' as const,
      render: (v?: number) => (
        <Tag color={(v ?? 0) > 0 ? 'blue' : 'default'}>
          {(v ?? 0).toLocaleString(undefined, {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })}
        </Tag>
      ),
    },
  ]
  return (
    <Table<MemberRow>
      rowKey="userId"
      columns={columns}
      dataSource={data}
      pagination={false}
      size="small"
      locale={{ emptyText: '暂无成员数据' }}
    />
  )
}
