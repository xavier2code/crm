import { useMemo, useState } from 'react'
import type { ComponentType } from 'react'
import {
  Layout,
  Menu,
  Dropdown,
  Button,
  Space,
  Badge,
  Popover,
  Empty,
  List,
  Tag,
  Typography,
  type MenuProps,
} from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import * as Icons from '@ant-design/icons'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined,
  BellOutlined,
} from '@ant-design/icons'

import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import type { MenuItem } from '@/types/app'
import {
  useMarkAllAsRead,
  useMarkAsRead,
  useUnreadCount,
  useUnreadList,
} from '@/hooks/useNotifications'
import { NOTIFICATION_TYPE_META } from '@/api/notification'

const { Header, Sider, Content } = Layout

// 数据库中存储的 icon 名称与 @ant-design/icons 实际导出名称的映射
const ICON_NAME_MAP: Record<string, string> = {
  Dashboard: 'DashboardOutlined',
  Customer: 'TeamOutlined',
  Opportunity: 'FileTextOutlined',
  Project: 'ProjectOutlined',
  Business: 'DollarOutlined',
  System: 'SettingOutlined',
  Bell: 'BellOutlined',
}

function renderMenuItems(items: MenuItem[]): NonNullable<MenuProps['items']> {
  return items.map((item) => {
    const mappedIconName = item.icon ? ICON_NAME_MAP[item.icon] || item.icon : null
    const IconComponent = mappedIconName
      ? (Icons as unknown as Record<string, ComponentType>)[mappedIconName]
      : null
    const icon = IconComponent ? <IconComponent /> : undefined

    if (item.children && item.children.length > 0) {
      return {
        key: item.path || String(item.id),
        icon,
        label: item.name,
        children: renderMenuItems(item.children),
      }
    }
    return {
      key: item.path || String(item.id),
      icon,
      label: item.name,
    }
  }) as NonNullable<MenuProps['items']>
}


const { Text } = Typography

function NotificationBell() {
  const navigate = useNavigate()
  const { data: count = 0 } = useUnreadCount()
  const { data: list = [] } = useUnreadList(5)
  const markAsRead = useMarkAsRead()
  const markAll = useMarkAllAsRead()
  const [open, setOpen] = useState(false)

  const content = (
    <div style={{ width: 340 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '0 0 8px 0',
          borderBottom: '1px solid #f0f0f0',
          marginBottom: 8,
        }}
      >
        <Text strong>未读通知（{count}）</Text>
        <Button
          type="link"
          size="small"
          disabled={count === 0 || markAll.isPending}
          onClick={() => markAll.mutate()}
        >
          全部已读
        </Button>
      </div>
      {list.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无未读通知" />
      ) : (
        <List
          dataSource={list}
          renderItem={(n) => {
            const meta = (n.type && NOTIFICATION_TYPE_META[n.type]) || null
            return (
              <List.Item
                style={{ padding: '8px 0', cursor: 'pointer' }}
                onClick={() => {
                  if (n.id) {
                    markAsRead.mutate(n.id)
                  }
                  const link = meta?.link?.(n.relatedId)
                  if (link) {
                    setOpen(false)
                    navigate(link)
                  }
                }}
              >
                <List.Item.Meta
                  title={
                    <Space>
                      {meta && <Tag color={meta.color}>{meta.label}</Tag>}
                      <span>{n.title}</span>
                    </Space>
                  }
                  description={
                    <Typography.Paragraph
                      type="secondary"
                      style={{ marginBottom: 4 }}
                      ellipsis={{ rows: 2 }}
                    >
                      {n.content}
                    </Typography.Paragraph>
                  }
                />
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}
                </Text>
              </List.Item>
            )
          }}
        />
      )}
      <div style={{ textAlign: 'center', marginTop: 8, borderTop: '1px solid #f0f0f0', paddingTop: 8 }}>
        <Button
          type="link"
          onClick={() => {
            setOpen(false)
            navigate('/notifications')
          }}
        >
          查看全部
        </Button>
      </div>
    </div>
  )

  return (
    <Popover
      content={content}
      trigger="click"
      open={open}
      onOpenChange={setOpen}
      placement="bottomRight"
      arrow
    >
      <Badge count={count} size="small" offset={[-2, 2]}>
        <Button type="text" icon={<BellOutlined />} aria-label="通知" />
      </Badge>
    </Popover>
  )
}

export default function BasicLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, clearAuth } = useAuthStore()
  const { menus } = useMenuStore()
  const [collapsed, setCollapsed] = useState(false)

  const menuItems = useMemo(() => renderMenuItems(menus), [menus])

  const selectedKeys = useMemo(() => {
    return [location.pathname]
  }, [location.pathname])

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} theme="light">
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: collapsed ? 14 : 18,
            fontWeight: 'bold',
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          {collapsed ? 'CRM' : 'CRM 管理系统'}
        </div>
        <Menu
          mode="inline"
          theme="light"
          selectedKeys={selectedKeys}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Button type="text" icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />} onClick={() => setCollapsed(!collapsed)} />
          <Space>
            <NotificationBell />
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <Button type="text">
                <UserOutlined /> {user?.realName || user?.username}
              </Button>
            </Dropdown>
          </Space>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: '#fff', minHeight: 280, borderRadius: 8 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
