import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Card,
  Empty,
  Segmented,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import type { TableColumnsType } from 'antd'
import { useNavigate } from 'react-router-dom'

import {
  NOTIFICATION_TYPE_META,
  type Notification,
  type NotificationStatus,
} from '@/api/notification'
import {
  useMarkAllAsRead,
  useMarkAsRead,
  useNotificationList,
} from '@/hooks/useNotifications'

type Tab = 'all' | 'unread' | 'read'

const TAB_TO_STATUS: Record<Tab, NotificationStatus | undefined> = {
  all: undefined,
  unread: 1,
  read: 2,
}

const { Paragraph, Text } = Typography

export default function NotificationsPage() {
  const navigate = useNavigate()
  const [tab, setTab] = useState<Tab>('all')
  const [page, setPage] = useState({ current: 1, size: 10, total: 0 })

  const status = TAB_TO_STATUS[tab]
  const query = useNotificationList({
    status,
    current: page.current,
    size: page.size,
  })
  const markAsRead = useMarkAsRead()
  const markAll = useMarkAllAsRead()

  useEffect(() => {
    setPage((p) => (p.current === 1 ? p : { ...p, current: 1 }))
  }, [tab])

  useEffect(() => {
    const r = query.data
    if (r) {
      setPage((p) => (p.total === r.total ? p : { ...p, total: r.total ?? 0 }))
    }
  }, [query.data])

  const records = query.data?.records ?? []

  const onClickItem = useCallback(
    async (n: Notification) => {
      if (!n.id) return
      if (n.status === 1) {
        try {
          await markAsRead.mutateAsync(n.id)
        } catch (e) {
          message.error((e as Error).message || '标记已读失败')
        }
      }
      const meta = n.type ? NOTIFICATION_TYPE_META[n.type] : undefined
      const link = meta?.link?.(n.relatedId)
      if (link) {
        navigate(link)
      }
    },
    [markAsRead, navigate],
  )

  const onMarkAll = async () => {
    try {
      await markAll.mutateAsync()
      message.success('已全部标记为已读')
    } catch (e) {
      message.error((e as Error).message || '操作失败')
    }
  }

  const columns: TableColumnsType<Notification> = [
    {
      title: '类型',
      dataIndex: 'type',
      width: 130,
      render: (t?: string) => {
        if (!t) return '-'
        const meta = NOTIFICATION_TYPE_META[t]
        return meta ? <Tag color={meta.color}>{meta.label}</Tag> : <Tag>{t}</Tag>
      },
    },
    {
      title: '标题',
      dataIndex: 'title',
      width: 220,
      render: (v?: string) => v ?? '-',
    },
    {
      title: '内容',
      dataIndex: 'content',
      ellipsis: true,
      render: (v?: string) =>
        v ? <Paragraph ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>{v}</Paragraph> : '-',
    },
    {
      title: '关联业务',
      dataIndex: 'relatedId',
      width: 110,
      render: (v?: number) => (v ? <Text code>#{v}</Text> : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (s?: NotificationStatus) =>
        s === 1 ? <Tag color="red">未读</Tag> : <Tag color="default">已读</Tag>,
    },
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (v?: string) => (v ? new Date(v).toLocaleString() : '-'),
    },
    {
      title: '已读时间',
      dataIndex: 'readAt',
      width: 170,
      render: (v?: string) => (v ? new Date(v).toLocaleString() : '-'),
    },
    {
      title: '操作',
      fixed: 'right',
      width: 180,
      render: (_, record) => (
        <Space>
          {record.relatedId && (
            <Button size="small" onClick={() => onClickItem(record)}>
              查看
            </Button>
          )}
          {record.status === 1 && record.id && (
            <Button
              size="small"
              loading={markAsRead.isPending}
              onClick={() => markAsRead.mutate(record.id!)}
            >
              标已读
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Card
      title="通知中心"
      extra={
        <Space>
          <Segmented
            value={tab}
            onChange={(v) => setTab(v as Tab)}
            options={[
              { label: '全部', value: 'all' },
              { label: '未读', value: 'unread' },
              { label: '已读', value: 'read' },
            ]}
          />
          <Button
            type="primary"
            loading={markAll.isPending}
            onClick={() => void onMarkAll()}
          >
            全部标为已读
          </Button>
        </Space>
      }
    >
      <Table<Notification>
        rowKey="id"
        loading={query.isLoading}
        columns={columns}
        dataSource={records}
        scroll={{ x: 1200 }}
        locale={{
          emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无通知" />,
        }}
        pagination={{
          current: page.current,
          pageSize: page.size,
          total: page.total,
          showSizeChanger: true,
          onChange: (current, size) => setPage({ current, size, total: page.total }),
        }}
      />
    </Card>
  )
}
