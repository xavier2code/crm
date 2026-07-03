import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tabs,
  Tag,
  message,
} from 'antd'
import type { TableColumnsType } from 'antd'

import {
  TASK_STATUS,
  closeTask,
  completeTask,
  pageTasks,
  pageTodayTasks,
  type TaskStatus,
  type TaskVO,
} from '@/api/task'

interface CloseFormValues {
  reason: string
}

type TabKey = 'today' | 'pending' | 'completed' | 'closed'

const TAB_TO_STATUS: Record<TabKey, TaskStatus | undefined> = {
  today: 1,
  pending: 1,
  completed: 2,
  closed: 3,
}

export default function TaskPage() {
  const [tab, setTab] = useState<TabKey>('today')
  const [page, setPage] = useState({ current: 1, size: 10, total: 0 })
  const [records, setRecords] = useState<TaskVO[]>([])
  const [loading, setLoading] = useState(false)
  const [closeTarget, setCloseTarget] = useState<TaskVO | null>(null)
  const [closeForm] = Form.useForm<CloseFormValues>()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      let res
      if (tab === 'today') {
        res = await pageTodayTasks({ current: page.current, size: page.size })
      } else {
        res = await pageTasks({
          status: TAB_TO_STATUS[tab],
          current: page.current,
          size: page.size,
        })
      }
      setRecords(res.records ?? [])
      setPage((p) => ({ ...p, total: res.total ?? 0 }))
    } catch (e) {
      message.error((e as Error).message || '加载任务失败')
    } finally {
      setLoading(false)
    }
  }, [tab, page])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    // 切换 tab 回到第一页
    setPage((p) => (p.current === 1 ? p : { ...p, current: 1 }))
  }, [tab])

  const onComplete = async (record: TaskVO) => {
    try {
      await completeTask(record.id!)
      message.success('已标记为完成')
      void load()
    } catch (e) {
      message.error((e as Error).message || '操作失败')
    }
  }

  const openClose = (record: TaskVO) => {
    setCloseTarget(record)
    closeForm.resetFields()
  }

  const submitClose = async () => {
    const values = await closeForm.validateFields()
    try {
      await closeTask(closeTarget!.id!, values.reason)
      message.success('已关闭')
      setCloseTarget(null)
      void load()
    } catch (e) {
      message.error((e as Error).message || '关闭失败')
    }
  }

  const columns: TableColumnsType<TaskVO> = [
    {
      title: '计划日期',
      dataIndex: 'planDate',
      width: 110,
      render: (v?: string) => {
        if (!v) return '-'
        const today = new Date().toISOString().slice(0, 10)
        const isOverdue = v < today && tab !== 'completed' && tab !== 'closed'
        return (
          <span style={isOverdue ? { color: '#cf1322', fontWeight: 600 } : undefined}>
            {v}
            {isOverdue && <Tag color="red" style={{ marginLeft: 6 }}>逾期</Tag>}
          </span>
        )
      },
    },
    {
      title: '客户',
      dataIndex: 'customerName',
      width: 200,
      ellipsis: true,
      render: (v?: string) => v ?? '-',
    },
    {
      title: '下一步阶段',
      dataIndex: 'planStageName',
      width: 130,
      render: (v: string | undefined, record) => v ?? record.planStage ?? '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (s?: TaskStatus) => {
        if (!s) return '-'
        const meta = TASK_STATUS[s]
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: '关闭原因',
      dataIndex: 'closeReason',
      ellipsis: true,
      render: (v?: string) => v || <span style={{ color: '#bbb' }}>-</span>,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (v?: string) => (v ? new Date(v).toLocaleString() : '-'),
    },
    {
      title: '操作',
      fixed: 'right',
      width: 180,
      render: (_, record) => (
        <Space>
          {record.status === 1 && (
            <>
              <Button size="small" type="primary" onClick={() => void onComplete(record)}>
                完成
              </Button>
              <Button size="small" danger onClick={() => openClose(record)}>
                关闭
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Card
      title="任务管理"
      extra={<span style={{ color: '#999' }}>仅显示分配给我的任务</span>}
    >
      <Tabs
        activeKey={tab}
        onChange={(k) => setTab(k as TabKey)}
        items={[
          { key: 'today', label: '今日待办' },
          { key: 'pending', label: '待完成' },
          { key: 'completed', label: '已完成' },
          { key: 'closed', label: '已关闭' },
        ]}
      />
      <Table<TaskVO>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={records}
        scroll={{ x: 1100 }}
        pagination={{
          current: page.current,
          pageSize: page.size,
          total: page.total,
          showSizeChanger: true,
          onChange: (current, size) => setPage({ current, size, total: page.total }),
        }}
      />

      <Modal
        title="关闭任务"
        open={!!closeTarget}
        onCancel={() => setCloseTarget(null)}
        onOk={() => void submitClose()}
        destroyOnClose
        width={520}
      >
        <Form<CloseFormValues> form={closeForm} layout="vertical" preserve={false}>
          <Form.Item
            label="关闭原因"
            name="reason"
            rules={[
              { required: true, message: '请填写关闭原因' },
              { whitespace: true, message: '关闭原因不能为空白' },
            ]}
            extra="关闭任务为不可逆操作，必须说明原因（合规要求）"
          >
            <Input.TextArea rows={4} placeholder="例如：客户已与友商签约 / 项目已并入其他任务" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
