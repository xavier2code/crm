import { Layout, Menu, Dropdown, Avatar, Space } from 'antd'
import { DownOutlined, UserOutlined } from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'

import { useAuthStore } from '@/stores/auth'
import { menus, convertToAntdMenu } from '@/constants/menus'

const { Header, Sider, Content } = Layout

export default function BasicLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, clearAuth } = useAuthStore()

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  const dropdownItems = [
    { key: 'logout', label: <span onClick={handleLogout}>退出登录</span> },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="light" width={220}>
        <div style={{ height: 64, padding: '0 24px', display: 'flex', alignItems: 'center' }}>
          <h3 style={{ margin: 0 }}>CRM</h3>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          defaultOpenKeys={menus.map((m) => m.key)}
          items={convertToAntdMenu(menus)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
          }}
        >
          <Dropdown menu={{ items: dropdownItems }} placement="bottomRight">
            <Space style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} />
              <span>{user?.realName || user?.username}</span>
              <DownOutlined />
            </Space>
          </Dropdown>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: '#fff' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
