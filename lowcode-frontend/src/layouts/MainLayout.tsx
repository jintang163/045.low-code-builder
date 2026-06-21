import React, { useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, Space, Breadcrumb } from 'antd'
import {
  DashboardOutlined,
  AppstoreOutlined,
  DatabaseOutlined,
  TableOutlined,
  FormOutlined,
  BranchesOutlined,
  SettingOutlined,
  UserOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  LogoutOutlined,
  ShopOutlined,
  CodeSandboxOutlined,
  BugOutlined,
  SafetyOutlined,
  MobileOutlined,
  CodeOutlined,
  EyeOutlined,
  RocketOutlined,
  MonitorOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useAppStore } from '@/store/appStore'

const { Header, Sider, Content } = Layout

const MainLayout: React.FC = ({ children }: { children?: React.ReactNode }) => {
  const navigate = useNavigate()
  const location = useLocation()
  const { collapsed, setCollapsed, currentApp, setCurrentApp } = useAppStore()
  const [selectedKeys, setSelectedKeys] = useState<string[]>([location.pathname])

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘',
    },
    {
      key: '/app',
      icon: <AppstoreOutlined />,
      label: '应用管理',
    },
    {
      key: '/template',
      icon: <ShopOutlined />,
      label: '模板市场',
    },
    {
      key: '/dataSource',
      icon: <DatabaseOutlined />,
      label: '数据源',
    },
    {
      key: '/dataModel',
      icon: <TableOutlined />,
      label: '数据模型',
    },
    {
      key: '/page',
      icon: <FormOutlined />,
      label: '页面设计',
    },
    {
      key: '/logic',
      icon: <BranchesOutlined />,
      label: '业务逻辑',
    },
    {
      key: '/workflow',
      icon: <BranchesOutlined />,
      label: '工作流',
    },
    {
      key: '/component',
      icon: <CodeSandboxOutlined />,
      label: '组件管理',
      children: [
        {
          key: '/component',
          icon: <CodeSandboxOutlined />,
          label: '组件列表',
        },
        {
          key: '/component/debugger',
          icon: <BugOutlined />,
          label: '组件调试',
        },
      ],
    },
    {
      key: '/permission',
      icon: <SafetyOutlined />,
      label: '权限管理',
    },
    {
      key: '/mobile',
      icon: <MobileOutlined />,
      label: '移动端',
      children: [
        {
          key: '/mobile/generator',
          icon: <CodeOutlined />,
          label: '代码生成器',
        },
        {
          key: '/mobile/preview',
          icon: <EyeOutlined />,
          label: '模拟器预览',
        },
      ],
    },
    {
      key: '/deploy',
      icon: <RocketOutlined />,
      label: '部署中心',
    },
    {
      key: '/settings',
      icon: <SettingOutlined />,
      label: '系统设置',
    },
    {
      key: '/monitor',
      icon: <MonitorOutlined />,
      label: '监控中心',
      children: [
        {
          key: '/monitor',
          icon: <MonitorOutlined />,
          label: '监控大屏',
        },
        {
          key: '/monitor/loadtest',
          icon: <ThunderboltOutlined />,
          label: '压力测试',
        },
      ],
    },
  ]

  const userMenu = {
    items: [
      {
        key: 'profile',
        icon: <UserOutlined />,
        label: '个人中心',
      },
      {
        type: 'divider' as const,
      },
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
        onClick: () => {
          localStorage.removeItem('token')
          navigate('/login')
        },
      },
    ],
  }

  const handleMenuClick = ({ key }: { key: string }) => {
    setSelectedKeys([key])
    navigate(key)
  }

  const getBreadcrumbItems = () => {
    const pathMap: Record<string, string> = {
      '/dashboard': '仪表盘',
      '/app': '应用管理',
      '/template': '模板市场',
      '/dataSource': '数据源',
      '/dataModel': '数据模型',
      '/dataModel/designer': '模型设计器',
      '/page': '页面设计',
      '/page/designer': '页面设计器',
      '/logic': '业务逻辑',
      '/logic/designer': '逻辑设计器',
      '/workflow': '工作流',
      '/workflow/designer': '工作流设计器',
      '/component': '组件管理',
      '/component/debugger': '组件调试',
      '/permission': '权限管理',
      '/mobile': '移动端',
      '/mobile/generator': '代码生成器',
      '/mobile/preview': '模拟器预览',
      '/deploy': '部署中心',
      '/monitor': '监控中心',
      '/monitor/loadtest': '压力测试',
    }
    const parts = location.pathname.split('/').filter(Boolean)
    const items = []
    let currentPath = ''
    for (let i = 0; i < parts.length; i++) {
      currentPath += '/' + parts[i]
      if (i === 0 || !pathMap[currentPath]) {
        items.push({
          title: pathMap[currentPath] || parts[i],
        })
      } else {
        items.push({
          title: <Link to={currentPath}>{pathMap[currentPath]}</Link>,
        })
      }
    }
    return items
  }

  return (
    <Layout style={{ height: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} theme="dark">
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: collapsed ? 18 : 20,
            fontWeight: 'bold',
            background: 'rgba(255, 255, 255, 0.1)',
          }}
        >
          {collapsed ? 'LC' : '低代码平台'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          items={menuItems}
          onClick={handleMenuClick}
          style={{ borderRight: 0 }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 16px',
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)',
          }}
        >
          <Space>
            {collapsed ? (
              <MenuUnfoldOutlined onClick={() => setCollapsed(false)} style={{ fontSize: 20, cursor: 'pointer' }} />
            ) : (
              <MenuFoldOutlined onClick={() => setCollapsed(true)} style={{ fontSize: 20, cursor: 'pointer' }} />
            )}
            <Breadcrumb items={getBreadcrumbItems()} />
          </Space>
          <Space>
            {currentApp && (
              <span style={{ color: '#1677ff', fontWeight: 500 }}>
                当前应用: {currentApp.appName}
              </span>
            )}
            <Dropdown menu={userMenu}>
              <Space style={{ cursor: 'pointer' }}>
                <Avatar size="small" icon={<UserOutlined />} />
                <span>管理员</span>
              </Space>
            </Dropdown>
          </Space>
        </Header>
        <Content
          style={{
            margin: 0,
            padding: 16,
            background: '#f5f5f5',
            overflow: 'auto',
            height: 'calc(100vh - 64px)',
          }}
        >
          {children}
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout
