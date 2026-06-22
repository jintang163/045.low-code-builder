import React, { useState, useMemo, useEffect } from 'react'
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
  BarChartOutlined,
  FileTextOutlined,
  ExperimentOutlined,
  RobotOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  WalletOutlined,
  SolutionOutlined,
  EnvironmentOutlined,
} from '@ant-design/icons'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useAppStore } from '@/store/appStore'
import { useTheme } from '@/context/ThemeContext'
import { themeApi } from '@/api'

const { Header, Sider, Content } = Layout

const MainLayout: React.FC = ({ children }: { children?: React.ReactNode }) => {
  const navigate = useNavigate()
  const location = useLocation()
  const { collapsed, setCollapsed, currentApp, setCurrentApp } = useAppStore()
  const { theme, loadTheme, applyTheme } = useTheme()
  const [selectedKeys, setSelectedKeys] = useState<string[]>([location.pathname])

  const layoutMode = useMemo(() => theme?.layoutMode || 'side', [theme])
  const sidebarTheme = useMemo(() => (theme?.sidebarTheme as 'light' | 'dark') || 'dark', [theme])
  const headerTheme = useMemo(() => (theme?.headerTheme as 'light' | 'dark') || 'light', [theme])

  useEffect(() => {
    if (currentApp?.id) {
      loadTheme(currentApp.id).catch((e) => {
        console.warn('加载应用主题失败，使用全局默认', e)
      })
    }
  }, [currentApp?.id, loadTheme])

  const sidebarBg = sidebarTheme === 'dark' ? '#001529' : '#fff'
  const sidebarTextColor = sidebarTheme === 'dark' ? 'rgba(255,255,255,0.85)' : 'rgba(0,0,0,0.85)'
  const sidebarLogoBg = sidebarTheme === 'dark' ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.04)'
  const sidebarLogoColor = sidebarTheme === 'dark' ? '#fff' : 'rgba(0,0,0,0.85)'

  const headerBg = headerTheme === 'dark' ? '#001529' : '#fff'
  const headerTextColor = headerTheme === 'dark' ? 'rgba(255,255,255,0.85)' : 'rgba(0,0,0,0.85)'
  const headerBoxShadow = headerTheme === 'dark' ? '0 1px 4px rgba(0,0,0,0.2)' : '0 1px 4px rgba(0,21,41,.08)'

  const contentBg = theme?.themeMode === 'dark' ? '#141414' : '#f5f5f5'

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
      key: '/abtest',
      icon: <ExperimentOutlined />,
      label: 'A/B测试',
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
      key: '/rpa',
      icon: <RobotOutlined />,
      label: 'RPA自动化',
    },
    {
      key: '/attendance',
      icon: <CalendarOutlined />,
      label: '考勤管理',
      children: [
        {
          key: '/attendance/schedule',
          icon: <CalendarOutlined />,
          label: '排班管理',
        },
        {
          key: '/attendance/stats',
          icon: <ClockCircleOutlined />,
          label: '考勤统计',
        },
        {
          key: '/attendance/leave',
          icon: <SolutionOutlined />,
          label: '请假审批',
        },
        {
          key: '/attendance/salary',
          icon: <WalletOutlined />,
          label: '工资管理',
        },
        {
          key: '/attendance/clockin',
          icon: <EnvironmentOutlined />,
          label: '打卡中心',
        },
      ],
    },
    {
      key: '/report',
      icon: <FileTextOutlined />,
      label: '报表设计',
    },
    {
      key: '/screen',
      icon: <BarChartOutlined />,
      label: '大屏设计',
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
      '/report': '报表设计',
      '/report/designer': '报表设计器',
      '/screen': '大屏设计',
      '/screen/designer': '大屏设计器',
      '/abtest': 'A/B测试',
      '/abtest/designer': 'A/B测试设计器',
      '/abtest/detail': 'A/B测试详情',
      '/rpa': 'RPA自动化',
      '/rpa/designer': 'RPA脚本设计器',
      '/attendance': '考勤管理',
      '/attendance/schedule': '排班管理',
      '/attendance/stats': '考勤统计',
      '/attendance/leave': '请假审批',
      '/attendance/salary': '工资管理',
      '/attendance/clockin': '打卡中心',
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

  const renderSider = () => (
    <Sider
      trigger={null}
      collapsible
      collapsed={collapsed}
      theme={sidebarTheme}
      style={{ background: sidebarBg }}
    >
      <div
        style={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: sidebarLogoColor,
          fontSize: collapsed ? 18 : 20,
          fontWeight: 'bold',
          background: sidebarLogoBg,
        }}
      >
        {collapsed ? 'LC' : '低代码平台'}
      </div>
      <Menu
        theme={sidebarTheme}
        mode="inline"
        selectedKeys={selectedKeys}
        items={menuItems}
        onClick={handleMenuClick}
        style={{ borderRight: 0, background: sidebarBg, color: sidebarTextColor }}
      />
    </Sider>
  )

  const renderHeader = (showToggle = true) => (
    <Header
      style={{
        padding: '0 16px',
        background: headerBg,
        color: headerTextColor,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        boxShadow: headerBoxShadow,
        height: 64,
        lineHeight: '64px',
      }}
    >
      <Space>
        {layoutMode === 'top' && (
          <div style={{ color: headerTextColor, fontSize: 18, fontWeight: 'bold', marginRight: 24 }}>
            低代码平台
          </div>
        )}
        {showToggle && (
          <>
            {collapsed ? (
              <MenuUnfoldOutlined
                onClick={() => setCollapsed(false)}
                style={{ fontSize: 20, cursor: 'pointer', color: headerTextColor }}
              />
            ) : (
              <MenuFoldOutlined
                onClick={() => setCollapsed(true)}
                style={{ fontSize: 20, cursor: 'pointer', color: headerTextColor }}
              />
            )}
          </>
        )}
        {layoutMode === 'top' ? (
          <Menu
            theme={headerTheme}
            mode="horizontal"
            selectedKeys={selectedKeys}
            items={menuItems}
            onClick={handleMenuClick}
            style={{ background: 'transparent', borderBottom: 'none', color: headerTextColor, minWidth: 400 }}
          />
        ) : (
          <Breadcrumb items={getBreadcrumbItems()} />
        )}
      </Space>
      <Space>
        {currentApp && (
          <span style={{ color: theme?.primaryColor || '#1677ff', fontWeight: 500 }}>
            当前应用: {currentApp.appName}
          </span>
        )}
        <Dropdown menu={userMenu}>
          <Space style={{ cursor: 'pointer', color: headerTextColor }}>
            <Avatar size="small" icon={<UserOutlined />} />
            <span>管理员</span>
          </Space>
        </Dropdown>
      </Space>
    </Header>
  )

  const renderContent = () => (
    <Content
      style={{
        margin: 0,
        padding: 16,
        background: contentBg,
        overflow: 'auto',
        height: layoutMode === 'top' ? 'calc(100vh - 64px)' : 'calc(100vh - 64px)',
        minHeight: layoutMode === 'top' ? 'calc(100vh - 64px)' : 'calc(100vh - 64px)',
      }}
    >
      {children}
    </Content>
  )

  if (layoutMode === 'top') {
    return (
      <Layout style={{ height: '100vh', background: contentBg }}>
        {renderHeader(false)}
        {renderContent()}
      </Layout>
    )
  }

  if (layoutMode === 'mix') {
    return (
      <Layout style={{ height: '100vh' }}>
        {renderHeader(true)}
        <Layout>
          {renderSider()}
          <Layout>
            {renderContent()}
          </Layout>
        </Layout>
      </Layout>
    )
  }

  return (
    <Layout style={{ height: '100vh' }}>
      {renderSider()}
      <Layout>
        {renderHeader(true)}
        {renderContent()}
      </Layout>
    </Layout>
  )
}

export default MainLayout
