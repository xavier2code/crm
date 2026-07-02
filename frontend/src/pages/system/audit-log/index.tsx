import { useEffect, useMemo, useState } from 'react'
import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  message,
  type TablePaginationConfig,
} from 'antd'
import {
  ReloadOutlined,
  SearchOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'

import {
  pageAuditLogs,
  type AuditLog,
  type AuditLogQuery,
} from '@/api/admin/auditLog'

/** 业务模块（后端 AuditLogAspect 把类名写入 module 字段，这里做友好映射） */
const MODULE_OPTIONS = [
  { value: 'AuthService', label: '认证' },
  { value: 'UserService', label: '用户' },
  { value: 'RoleService', label: '角色' },
  { value: 'UnitService', label: '单位' },
  { value: 'DictionaryService', label: '字典' },
  { value: 'CustomerService', label: '客户' },
  { value: 'OpportunityService', label: '商机' },
  { value: 'ProjectService', label: '项目' },
  { value: 'ContractService', label: '合同' },
  { value: 'RebateService', label: '返利' },
  { value: 'FollowupService', label: '跟进' },
  { value: 'TaskService', label: '任务' },
  { value: 'NotificationService', label: '通知' },
]

const MODULE_LABEL = Object.fromEntries(
  MODULE_OPTIONS.map((o) => [o.value, o.label]),
) as Record<string, string>

interface SearchValues {
  userId?: number
  module?: string
  operation?: string
  dateRange?: [Dayjs, Dayjs]
}

interface PageState {
  current: number
  size: number
  total: number
}

const initialPage: PageState = { current: 1, size: 20, total: 0 }

export default function AuditLogPage() {
  const [searchForm] = Form.useForm<SearchValues>()

  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<AuditLog[]>([])
  const [page, setPage] = useState<PageState>(initialPage)
  const [query, setQuery] = useState<AuditLogQuery>({})

  const [detail, setDetail] = useState<AuditLog | null>(null)

  const fetchData = async (
    next: Partial<AuditLogQuery> & { reset?: boolean } = {},
  ) => {
    const merged: AuditLogQuery = {
      current: next.reset ? 1 : next.current ?? page.current,
      size: next.size ?? page.size,
      userId: next.userId ?? query.userId,
      module: next.module ?? query.module,
      operation: next.operation ?? query.operation,
      startDate: next.startDate ?? query.startDate,
      endDate: next.endDate ?? query.endDate,
    }
    setLoading(true)
    try {
      const res = await pageAuditLogs(merged)
      setData(res.records || [])
      setPage({
        current: res.current || merged.current || 1,
        size: res.size || merged.size || 20,
        total: res.total || 0,
      })
      setQuery(merged)
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载审计日志失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData({ reset: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleSearch = (values: SearchValues) => {
    const [start, end] = values.dateRange || []
    fetchData({
      reset: true,
      userId: values.userId,
      module: values.module,
      operation: values.operation,
      startDate: start ? start.format('YYYY-MM-DD') : undefined,
      endDate: end ? end.format('YYYY-MM-DD') : undefined,
    })
  }

  const handleResetSearch = () => {
    searchForm.resetFields()
    fetchData({
      reset: true,
      userId: undefined,
      module: undefined,
      operation: undefined,
      startDate: undefined,
      endDate: undefined,
    })
  }

  const handleTableChange = (pagination: TablePaginationConfig) => {
    fetchData({
      current: pagination.current,
      size: pagination.pageSize,
    })
  }

  const columns = useMemo(
    () => [
      { title: 'ID', dataIndex: 'id', width: 80 },
      {
        title: '操作时间',
        dataIndex: 'createdAt',
        width: 180,
        render: (v?: string) => (v ? new Date(v).toLocaleString() : '-'),
      },
      {
        title: '操作用户',
        dataIndex: 'username',
        width: 120,
        render: (v?: string) => v || '-',
      },
      {
        title: '模块',
        dataIndex: 'module',
        width: 120,
        render: (v?: string) =>
          v ? (
            <Tag color="geekblue">{MODULE_LABEL[v] ?? v}</Tag>
          ) : (
            <span style={{ color: '#999' }}>-</span>
          ),
      },
      {
        title: '操作类型',
        dataIndex: 'operation',
        width: 110,
        render: (v?: string) => v || '-',
      },
      {
        title: '方法',
        dataIndex: 'method',
        width: 200,
        ellipsis: true,
        render: (v?: string) =>
          v ? (
            <Tooltip title={v}>
              <span style={{ fontFamily: 'monospace' }}>{v}</span>
            </Tooltip>
          ) : (
            '-'
          ),
      },
      {
        title: 'IP',
        dataIndex: 'ip',
        width: 130,
      },
      {
        title: '耗时',
        dataIndex: 'executeTime',
        width: 90,
        render: (v?: number) => (v != null ? `${v} ms` : '-'),
      },
      {
        title: '结果',
        dataIndex: 'status',
        width: 90,
        render: (v?: number) =>
          v === 0 ? (
            <Tag color="red">失败</Tag>
          ) : v === 1 ? (
            <Tag color="green">成功</Tag>
          ) : (
            '-'
          ),
      },
      {
        title: '操作',
        key: 'actions',
        width: 90,
        fixed: 'right' as const,
        render: (_: unknown, record: AuditLog) => (
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => setDetail(record)}
          >
            详情
          </Button>
        ),
      },
    ],
    [],
  )

  return (
    <Card
      title="审计日志"
      extra={
        <Form form={searchForm} layout="inline" onFinish={handleSearch}>
          <Form.Item name="userId" label="用户ID">
            <InputNumber
              placeholder="用户ID"
              min={1}
              style={{ width: 120 }}
            />
          </Form.Item>
          <Form.Item name="module" label="模块">
            <Select
              placeholder="全部模块"
              allowClear
              style={{ width: 160 }}
              options={MODULE_OPTIONS}
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="operation" label="操作">
            <Input
              placeholder="操作描述"
              allowClear
              prefix={<SearchOutlined />}
              style={{ width: 180 }}
            />
          </Form.Item>
          <Form.Item name="dateRange" label="时间">
            <DatePicker.RangePicker
              style={{ width: 260 }}
              placeholder={['开始日期', '结束日期']}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                查询
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleResetSearch}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      }
    >
      <Table<AuditLog>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data}
        scroll={{ x: 1200 }}
        pagination={{
          current: page.current,
          pageSize: page.size,
          total: page.total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (t) => `共 ${t} 条`,
        }}
        onChange={handleTableChange}
      />

      <Drawer
        title="审计日志详情"
        open={!!detail}
        onClose={() => setDetail(null)}
        width={640}
      >
        {detail && (
          <Descriptions
            column={1}
            bordered
            size="small"
            labelStyle={{ width: 120, color: '#666' }}
          >
            <Descriptions.Item label="日志ID">{detail.id}</Descriptions.Item>
            <Descriptions.Item label="操作用户">
              {detail.username ?? '-'}
              {detail.userId ? ` (#${detail.userId})` : ''}
            </Descriptions.Item>
            <Descriptions.Item label="操作时间">
              {detail.createdAt
                ? dayjs(detail.createdAt).format('YYYY-MM-DD HH:mm:ss')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="模块">
              {detail.module
                ? MODULE_LABEL[detail.module] ?? detail.module
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="操作类型">
              {detail.operation ?? '-'}
            </Descriptions.Item>
            <Descriptions.Item label="调用方法">
              <span style={{ fontFamily: 'monospace' }}>
                {detail.method ?? '-'}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="客户端IP">
              {detail.ip ?? '-'}
            </Descriptions.Item>
            <Descriptions.Item label="耗时">
              {detail.executeTime != null ? `${detail.executeTime} ms` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="执行结果">
              {detail.status === 0 ? (
                <Tag color="red">失败</Tag>
              ) : detail.status === 1 ? (
                <Tag color="green">成功</Tag>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            {detail.status === 0 && detail.errorMsg && (
              <Descriptions.Item label="错误信息">
                <pre
                  style={{
                    margin: 0,
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                    color: '#cf1322',
                    fontSize: 12,
                  }}
                >
                  {detail.errorMsg}
                </pre>
              </Descriptions.Item>
            )}
            {detail.params && (
              <Descriptions.Item label="方法入参">
                <pre
                  style={{
                    margin: 0,
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                    fontSize: 12,
                    maxHeight: 280,
                    overflow: 'auto',
                    background: '#fafafa',
                    padding: 8,
                    borderRadius: 4,
                  }}
                >
                  {(() => {
                    try {
                      return JSON.stringify(JSON.parse(detail.params), null, 2)
                    } catch {
                      return detail.params
                    }
                  })()}
                </pre>
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Drawer>
    </Card>
  )
}
