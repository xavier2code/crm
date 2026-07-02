import { useMemo, useState } from 'react'
import type { ComponentType } from 'react'
import { Layout, Menu, Dropdown, Button, Space, Badge, type MenuProps } from 'antd'
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

const { Header, Sider, Content } = Layout

// 数据库中存储的 icon 名称与 @ant-design/icons 实际导出名称的映射
const ICON_NAME_MAP: Record<string, string> = {
  Dashboard: 'DashboardOutlined',
  Customer: 'TeamOutlined',
  Opportunity: 'FileTextOutlined',
  Project: 'ProjectOutlined',
  Business: 'DollarOutlined',
  System: 'SettingOutlined',
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
            <Badge count={0}>
              <Button type="text" icon={<BellOutlined />} />
            </Badge>
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
