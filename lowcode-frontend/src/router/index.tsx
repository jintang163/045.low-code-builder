import { Navigate } from 'react-router-dom'
import Layout from '@/layouts/MainLayout'
import Dashboard from '@/pages/Dashboard'
import DataModelList from '@/pages/dataModel/List'
import DataModelDesigner from '@/pages/dataModel/Designer'
import PageList from '@/pages/page/List'
import PageDesigner from '@/pages/page/Designer'
import LogicList from '@/pages/logic/List'
import LogicDesigner from '@/pages/logic/Designer'
import WorkflowList from '@/pages/workflow/List'
import WorkflowDesigner from '@/pages/workflow/Designer'
import DataSourceList from '@/pages/dataSource/List'
import DataSourceDesigner from '@/pages/dataSource/Designer'
import AppList from '@/pages/app/List'
import TemplateMarket from '@/pages/template/Market'
import ComponentList from '@/pages/component/List'
import ComponentDebugger from '@/pages/component/Debugger'
import Login from '@/pages/Login'
import PermissionList from '@/pages/permission/List'
import MobilePreview from '@/pages/mobile/Preview'
import MobileGenerator from '@/pages/mobile/Generator'
import MonitorDashboard from '@/pages/monitor/Dashboard'
import LoadTest from '@/pages/monitor/LoadTest'
import DeployCenter from '@/pages/deploy/DeployCenter'
import ReportList from '@/pages/report/List'
import ReportDesigner from '@/pages/report/Designer'
import ReportPreview from '@/pages/report/Preview'
import DashboardList from '@/pages/dashboard/List'
import DashboardDesigner from '@/pages/dashboard/Designer'
import DashboardDisplay from '@/pages/dashboard/Display'
import ABTestList from '@/pages/abtest/List'
import ABTestDesigner from '@/pages/abtest/Designer'
import ABTestDetail from '@/pages/abtest/Detail'
import ABTestRuntime from '@/pages/abtest/Runtime'
import RpaScriptList from '@/pages/rpa/List'
import RpaDesigner from '@/pages/rpa/Designer'

const routes = [
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/screen/display/:id',
    element: <DashboardDisplay />,
  },
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        path: '',
        element: <Navigate to="/dashboard" replace />,
      },
      {
        path: 'dashboard',
        element: <Dashboard />,
      },
      {
        path: 'app',
        children: [
          {
            path: '',
            element: <AppList />,
          },
        ],
      },
      {
        path: 'template',
        children: [
          {
            path: '',
            element: <TemplateMarket />,
          },
        ],
      },
      {
        path: 'dataSource',
        children: [
          {
            path: '',
            element: <DataSourceList />,
          },
          {
            path: 'designer',
            element: <DataSourceDesigner />,
          },
          {
            path: 'designer/:id',
            element: <DataSourceDesigner />,
          },
        ],
      },
      {
        path: 'dataModel',
        children: [
          {
            path: '',
            element: <DataModelList />,
          },
          {
            path: 'designer/:id',
            element: <DataModelDesigner />,
          },
          {
            path: 'designer',
            element: <DataModelDesigner />,
          },
        ],
      },
      {
        path: 'page',
        children: [
          {
            path: '',
            element: <PageList />,
          },
          {
            path: 'designer/:id',
            element: <PageDesigner />,
          },
          {
            path: 'designer',
            element: <PageDesigner />,
          },
        ],
      },
      {
        path: 'component',
        children: [
          {
            path: '',
            element: <ComponentList />,
          },
          {
            path: 'debugger',
            element: <ComponentDebugger />,
          },
        ],
      },
      {
        path: 'logic',
        children: [
          {
            path: '',
            element: <LogicList />,
          },
          {
            path: 'designer/:id',
            element: <LogicDesigner />,
          },
          {
            path: 'designer',
            element: <LogicDesigner />,
          },
        ],
      },
      {
        path: 'workflow',
        children: [
          {
            path: '',
            element: <WorkflowList />,
          },
          {
            path: 'designer/:id',
            element: <WorkflowDesigner />,
          },
          {
            path: 'designer',
            element: <WorkflowDesigner />,
          },
        ],
      },
      {
        path: 'permission',
        children: [
          {
            path: '',
            element: <PermissionList />,
          },
        ],
      },
      {
        path: 'mobile',
        children: [
          {
            path: 'generator',
            element: <MobileGenerator />,
          },
          {
            path: 'preview',
            element: <MobilePreview />,
          },
        ],
      },
      {
        path: 'monitor',
        children: [
          {
            path: '',
            element: <MonitorDashboard />,
          },
          {
            path: 'loadtest',
            element: <LoadTest />,
          },
        ],
      },
      {
        path: 'deploy',
        children: [
          {
            path: '',
            element: <DeployCenter />,
          },
        ],
      },
      {
        path: 'report',
        children: [
          {
            path: '',
            element: <ReportList />,
          },
          {
            path: 'designer/:id',
            element: <ReportDesigner />,
          },
          {
            path: 'designer',
            element: <ReportDesigner />,
          },
          {
            path: 'preview/:id',
            element: <ReportPreview />,
          },
        ],
      },
      {
        path: 'screen',
        children: [
          {
            path: '',
            element: <DashboardList />,
          },
          {
            path: 'designer/:id',
            element: <DashboardDesigner />,
          },
          {
            path: 'designer',
            element: <DashboardDesigner />,
          },
        ],
      },
      {
        path: 'abtest',
        children: [
          {
            path: '',
            element: <ABTestList />,
          },
          {
            path: 'designer/:id',
            element: <ABTestDesigner />,
          },
          {
            path: 'designer',
            element: <ABTestDesigner />,
          },
          {
            path: 'detail/:id',
            element: <ABTestDetail />,
          },
          {
            path: 'runtime/:id',
            element: <ABTestRuntime />,
          },
        ],
      },
      {
        path: 'rpa',
        children: [
          {
            path: '',
            element: <RpaScriptList />,
          },
          {
            path: 'designer/:id',
            element: <RpaDesigner />,
          },
          {
            path: 'designer',
            element: <RpaDesigner />,
          },
        ],
      },
    ],
  },
]

export default routes
