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
import AppList from '@/pages/app/List'
import Login from '@/pages/Login'

const routes = [
  {
    path: '/login',
    element: <Login />,
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
        path: 'dataSource',
        children: [
          {
            path: '',
            element: <DataSourceList />,
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
    ],
  },
]

export default routes
