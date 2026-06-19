import React, { useState } from 'react'
import { Tabs, Card, Form, Button, Space, Table, Switch, Select, InputNumber, message, Divider } from 'antd'
import { SaveOutlined } from '@ant-design/icons'
import { useAppStore } from '@/store/appStore'
import { permissionApi, FieldPermission, PagePermission, ComponentPermission } from '@/api/permission'
import RowPermissionEditor from './RowPermissionEditor'
import { dataModelApi } from '@/api/dataModel'
import { pageApi } from '@/api/page'

const { TabPane } = Tabs
const { Option } = Select

interface PermissionConfigPanelProps {
  roleId: number
}

export const PermissionConfigPanel: React.FC<PermissionConfigPanelProps> = ({ roleId }) => {
  const { currentApp } = useAppStore()
  const [activeTab, setActiveTab] = useState('page')
  const [selectedModelId, setSelectedModelId] = useState<number | null>(null)
  const [selectedPageId, setSelectedPageId] = useState<number | null>(null)
  const [models, setModels] = useState<any[]>([])
  const [pages, setPages] = useState<any[]>([])
  const [modelFields, setModelFields] = useState<any[]>([])
  const [pageComponents, setPageComponents] = useState<any[]>([])
  const [fieldPermissions, setFieldPermissions] = useState<FieldPermission[]>([])
  const [pagePermissions, setPagePermissions] = useState<PagePermission[]>([])
  const [componentPermissions, setComponentPermissions] = useState<ComponentPermission[]>([])

  const loadModels = async () => {
    if (!currentApp?.id) return
    try {
      const res = await dataModelApi.list(currentApp.id)
      if (res.code === 0 || res.code === 200) {
        setModels(res.data || [])
      }
    } catch (e) {
      console.error('加载数据模型失败:', e)
    }
  }

  const loadPages = async () => {
    if (!currentApp?.id) return
    try {
      const res = await pageApi.list(currentApp.id)
      if (res.code === 0 || res.code === 200) {
        setPages(res.data || [])
      }
    } catch (e) {
      console.error('加载页面列表失败:', e)
    }
  }

  const loadModelFields = async (modelId: number) => {
    try {
      const res = await dataModelApi.getFields(modelId)
      if (res.code === 0 || res.code === 200) {
        setModelFields(res.data || [])
        initFieldPermissions(modelId, res.data || [])
      }
    } catch (e) {
      console.error('加载模型字段失败:', e)
    }
  }

  const loadPageComponents = async (pageId: number) => {
    try {
      const res = await pageApi.get(pageId)
      if (res.code === 0 || res.code === 200) {
        const components = res.data?.components || []
        setPageComponents(components)
        initComponentPermissions(pageId, components)
      }
    } catch (e) {
      console.error('加载页面组件失败:', e)
    }
  }

  const initFieldPermissions = (modelId: number, fields: any[]) => {
    const perms: FieldPermission[] = fields.map((field) => ({
      appId: currentApp?.id || 0,
      roleId,
      modelId,
      fieldId: field.id,
      canView: 1,
      canEdit: 1,
    }))
    setFieldPermissions(perms)
  }

  const initComponentPermissions = (pageId: number, components: any[]) => {
    const perms: ComponentPermission[] = components.map((comp) => ({
      appId: currentApp?.id || 0,
      roleId,
      pageId,
      componentId: comp.componentId,
      canVisible: 1,
      canDisabled: 0,
    }))
    setComponentPermissions(perms)
  }

  React.useEffect(() => {
    loadModels()
    loadPages()
  }, [currentApp?.id])

  const handleModelChange = async (modelId: number) => {
    setSelectedModelId(modelId)
    await loadModelFields(modelId)
  }

  const handlePageChange = async (pageId: number) => {
    setSelectedPageId(pageId)
    await loadPageComponents(pageId)
    initPagePermission(pageId)
  }

  const initPagePermission = (pageId: number) => {
    const exists = pagePermissions.find((p) => p.pageId === pageId)
    if (!exists) {
      setPagePermissions([
        ...pagePermissions,
        {
          appId: currentApp?.id || 0,
          roleId,
          pageId,
          canAccess: 1,
        },
      ])
    }
  }

  const handleFieldPermissionChange = (fieldId: number, key: 'canView' | 'canEdit', value: number) => {
    setFieldPermissions(
      fieldPermissions.map((p) => (p.fieldId === fieldId ? { ...p, [key]: value } : p))
    )
  }

  const handlePagePermissionChange = (pageId: number, value: number) => {
    setPagePermissions(
      pagePermissions.map((p) => (p.pageId === pageId ? { ...p, canAccess: value } : p))
    )
  }

  const handleComponentPermissionChange = (
    componentId: string,
    key: 'canVisible' | 'canDisabled',
    value: number
  ) => {
    setComponentPermissions(
      componentPermissions.map((p) =>
        p.componentId === componentId ? { ...p, [key]: value } : p
      )
    )
  }

  const handleSaveFieldPermissions = async () => {
    if (!selectedModelId) {
      message.warning('请先选择数据模型')
      return
    }
    try {
      await permissionApi.saveFieldPermissions(roleId, selectedModelId, fieldPermissions)
      message.success('字段权限保存成功')
    } catch (e: any) {
      message.error('保存失败: ' + e.message)
    }
  }

  const handleSavePagePermissions = async () => {
    try {
      await permissionApi.savePagePermissions(roleId, pagePermissions)
      message.success('页面权限保存成功')
    } catch (e: any) {
      message.error('保存失败: ' + e.message)
    }
  }

  const handleSaveComponentPermissions = async () => {
    if (!selectedPageId) {
      message.warning('请先选择页面')
      return
    }
    try {
      await permissionApi.saveComponentPermissions(roleId, selectedPageId, componentPermissions)
      message.success('组件权限保存成功')
    } catch (e: any) {
      message.error('保存失败: ' + e.message)
    }
  }

  const fieldColumns = [
    { title: '字段名称', dataIndex: 'fieldName', key: 'fieldName' },
    { title: '字段编码', dataIndex: 'columnName', key: 'columnName' },
    { title: '字段类型', dataIndex: 'fieldType', key: 'fieldType' },
    {
      title: '可见',
      key: 'canView',
      render: (_: any, record: any) => {
        const perm = fieldPermissions.find((p) => p.fieldId === record.id)
        return (
          <Switch
            checked={perm?.canView === 1}
            onChange={(checked) => handleFieldPermissionChange(record.id, 'canView', checked ? 1 : 0)}
          />
        )
      },
    },
    {
      title: '可编辑',
      key: 'canEdit',
      render: (_: any, record: any) => {
        const perm = fieldPermissions.find((p) => p.fieldId === record.id)
        return (
          <Switch
            checked={perm?.canEdit === 1}
            onChange={(checked) => handleFieldPermissionChange(record.id, 'canEdit', checked ? 1 : 0)}
          />
        )
      },
    },
  ]

  const pageColumns = [
    { title: '页面名称', dataIndex: 'pageName', key: 'pageName' },
    { title: '页面编码', dataIndex: 'pageCode', key: 'pageCode' },
    { title: '页面路径', dataIndex: 'pagePath', key: 'pagePath' },
    {
      title: '可访问',
      key: 'canAccess',
      render: (_: any, record: any) => {
        const perm = pagePermissions.find((p) => p.pageId === record.id)
        return (
          <Switch
            checked={perm?.canAccess === 1}
            onChange={(checked) => handlePagePermissionChange(record.id, checked ? 1 : 0)}
          />
        )
      },
    },
  ]

  const componentColumns = [
    { title: '组件名称', dataIndex: 'componentName', key: 'componentName' },
    { title: '组件类型', dataIndex: 'componentType', key: 'componentType' },
    {
      title: '可见',
      key: 'canVisible',
      render: (_: any, record: any) => {
        const perm = componentPermissions.find((p) => p.componentId === record.componentId)
        return (
          <Switch
            checked={perm?.canVisible === 1}
            onChange={(checked) =>
              handleComponentPermissionChange(record.componentId, 'canVisible', checked ? 1 : 0)
            }
          />
        )
      },
    },
    {
      title: '禁用',
      key: 'canDisabled',
      render: (_: any, record: any) => {
        const perm = componentPermissions.find((p) => p.componentId === record.componentId)
        return (
          <Switch
            checked={perm?.canDisabled === 1}
            onChange={(checked) =>
              handleComponentPermissionChange(record.componentId, 'canDisabled', checked ? 1 : 0)
            }
          />
        )
      },
    },
  ]

  return (
    <Card size="small">
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane tab="页面权限" key="page">
          <Space direction="vertical" style={{ width: '100%' }}>
            <div style={{ marginBottom: 12 }}>
              <Button type="primary" icon={<SaveOutlined />} onClick={handleSavePagePermissions}>
                保存页面权限
              </Button>
            </div>
            <Table
              size="small"
              dataSource={pages}
              columns={pageColumns}
              rowKey="id"
              pagination={false}
            />
          </Space>
        </TabPane>

        <TabPane tab="组件权限" key="component">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space style={{ marginBottom: 12 }}>
              <Select
                placeholder="选择页面"
                style={{ width: 250 }}
                value={selectedPageId}
                onChange={handlePageChange}
              >
                {pages.map((p) => (
                  <Option key={p.id} value={p.id}>
                    {p.pageName}
                  </Option>
                ))}
              </Select>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                onClick={handleSaveComponentPermissions}
                disabled={!selectedPageId}
              >
                保存组件权限
              </Button>
            </Space>
            {selectedPageId && (
              <Table
                size="small"
                dataSource={pageComponents}
                columns={componentColumns}
                rowKey="componentId"
                pagination={false}
              />
            )}
          </Space>
        </TabPane>

        <TabPane tab="字段权限" key="field">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space style={{ marginBottom: 12 }}>
              <Select
                placeholder="选择数据模型"
                style={{ width: 250 }}
                value={selectedModelId}
                onChange={handleModelChange}
              >
                {models.map((m) => (
                  <Option key={m.id} value={m.id}>
                    {m.modelName}
                  </Option>
                ))}
              </Select>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                onClick={handleSaveFieldPermissions}
                disabled={!selectedModelId}
              >
                保存字段权限
              </Button>
            </Space>
            {selectedModelId && (
              <Table
                size="small"
                dataSource={modelFields}
                columns={fieldColumns}
                rowKey="id"
                pagination={false}
              />
            )}
          </Space>
        </TabPane>

        <TabPane tab="行级权限" key="row">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Select
              placeholder="选择数据模型"
              style={{ width: 250, marginBottom: 12 }}
              value={selectedModelId}
              onChange={handleModelChange}
            >
              {models.map((m) => (
                <Option key={m.id} value={m.id}>
                  {m.modelName}
                </Option>
              ))}
            </Select>
            {selectedModelId && (
              <RowPermissionEditor modelId={selectedModelId} roleId={roleId} />
            )}
          </Space>
        </TabPane>
      </Tabs>
    </Card>
  )
}

export default PermissionConfigPanel
