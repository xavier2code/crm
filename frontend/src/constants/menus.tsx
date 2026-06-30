import type { MenuProps } from 'antd'
import {
  DashboardOutlined,
  TeamOutlined,
  FundOutlined,
  CarryOutOutlined,
  MoneyCollectOutlined,
  SettingOutlined,
} from '@ant-design/icons'

export interface MenuItem {
  key: string
  label: string
  icon?: React.ReactNode
  path?: string
  children?: MenuItem[]
  roles?: string[]
}

export const menus: MenuItem[] = [
  {
    key: '/dashboard',
    label: '工作台',
    icon: <DashboardOutlined />,
    path: '/dashboard',
  },
  {
    key: '/customer',
    label: '客户管理',
    icon: <TeamOutlined />,
    path: '/customer',
    roles: ['ADMIN', 'REGION_HEAD', 'CHANNEL_HEAD', 'CHANNEL_BD', 'CYBD'],
  },
  {
    key: '/opportunity',
    label: '商机信息',
    icon: <FundOutlined />,
    path: '/opportunity',
    roles: ['ADMIN', 'REGION_HEAD', 'CHANNEL_HEAD', 'CHANNEL_BD', 'CYBD', 'ORANGE_EAGLE_SALES'],
  },
  {
    key: '/business',
    label: '商务管理',
    icon: <CarryOutOutlined />,
    path: '/business',
    roles: ['ADMIN', 'CHANNEL_HEAD', 'CYBD'],
  },
  {
    key: '/reimbursement',
    label: '报销管理',
    icon: <MoneyCollectOutlined />,
    path: '/reimbursement',
    roles: ['ADMIN', 'FINANCE'],
  },
  {
    key: '/system',
    label: '后台管理',
    icon: <SettingOutlined />,
    roles: ['ADMIN', 'CYBD'],
    children: [
      { key: '/system/users', label: '用户管理', path: '/system/users' },
      { key: '/system/roles', label: '角色管理', path: '/system/roles' },
      { key: '/system/dictionary', label: '字典维护', path: '/system/dictionary' },
      { key: '/system/units', label: '单位主数据', path: '/system/units' },
    ],
  },
]

export const convertToAntdMenu = (items: MenuItem[]): MenuProps['items'] =>
  items.map((item) => ({
    key: item.key,
    icon: item.icon,
    label: item.path ? <a href={item.path}>{item.label}</a> : item.label,
    children: item.children ? convertToAntdMenu(item.children) : undefined,
  }))
