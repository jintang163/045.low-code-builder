import React, { useState, useEffect } from 'react'
import {
  Layout,
  Card,
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  message,
  Tabs,
  Drawer,
  Tag,
  Popconfirm,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SettingOutlined,
  UserOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { useAppStore } from '@/store/appStore'
import { userApi } from '@/api/index'
import { permissionApi, RowPermission } from '@/api/permission'
import { userRoleApi } from '@/api/index'
import PermissionConfigPanel from '@/components/PermissionConfigPanel'

const { Header, Content } = Layout
const { TabPane } = Tabs
const { Option } = Select
const { TextArea } = Input

interface RoleInfo {
  id: number
  roleName: string
  roleCode: string
  roleType: string
  roleSort: number
  status: number
  remark: string
  appId?: number
}

const PermissionPage: React.FC = () => {
  const { currentApp } = useAppStore()
  const [activeTab, setActiveTab] = useState('role')
  const [roles, setRoles] = useState<RoleInfo[]>([])
  const [users, setUsers] = useState<any[]>([])
  const [roleModalVisible, setRoleModalVisible] = useState(false)
  const [userRoleModalVisible, setUserRoleModalVisible] = useState(false)
  const [permissionDrawerVisible, setPermissionDrawerVisible] = useState(false)
  const [editingRole, setEditingRole] = useState<RoleInfo | null>(null)
  const [selectedUser, setSelectedUser] = useState<any>(null)
  const [selectedRole, setSelectedRole] = useState<RoleInfo | null>(null)
  const [form] = Form.useForm()
  const [userRoleForm] = Form.useForm()
  const [loading, setLoading] = useState(false)

  const loadRoles = async () => {
    if (!currentApp?.id) return
    setLoading(true)
    try {
      const res = await userRoleApi.getRoles(currentApp.id)
      if (res.code === 0 || res.code === 200) {
        setRoles(res.data || [])
      }
    } catch (e) {
      console.error('加载角色列表失败:', e)
      message.error('加载角色列表失败')
    } finally {
      setLoading(false)
    }
  }

  const loadUsers = async () => {
    setLoading(true)
    try {
      const res = await userApi.page(1, 100)
      if (res.code === 0 || res.code === 200) {
        setUsers(res.data?.records || res.data || [])
      }
    } catch (e) {
      console.error('加载用户列表失败:', e)
      message.error('加载用户列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (activeTab === 'role') {
      loadRoles()
    } else if (activeTab === 'user') {
      loadUsers()
    }
  }, [activeTab, currentApp?.id])

  const handleAddRole = () => {
    setEditingRole(null)
    form.resetFields()
    form.setFieldsValue({
      appId: currentApp?.id,
      roleType: 'CUSTOM',
      roleSort: roles.length + 1,
      status: 1,
    })
    setRoleModalVisible(true)
  }

  const handleEditRole = (role: RoleInfo) => {
    setEditingRole(role)
    form.setFieldsValue(role)
    setRoleModalVisible(true)
  }

  const handleDeleteRole = async (roleId: number) => {
    try {
      await userRoleApi.deleteRole(roleId)
      message.success('删除成功')
      loadRoles()
    } catch (e: any) {
      message.error('删除失败: ' + e.message)
    }
  }

  const handleSaveRole = async () => {
    try {
      const values = await form.validateFields()
      if (editingRole) {
        await userRoleApi.updateRole(editingRole.id, values)
        message.success('更新成功')
      } else {
        await userRoleApi.createRole(values)
        message.success('创建成功')
      }
      setRoleModalVisible(false)
      loadRoles()
    } catch (e: any) {
      message.error('保存失败: ' + e.message)
    }
  }

  const handleConfigurePermissions = (role: RoleInfo) => {
    setSelectedRole(role)
    setPermissionDrawerVisible(true)
  }

  const handleAssignRoles = (user: any) => {
    setSelectedUser(user)
    userRoleForm.resetFields()
    permissionApi.getUserPermissions(user.id, currentApp?.id || 0).then((res) => {
      if (res.code === 0 || res.code === 200) {
        userRoleForm.setFieldsValue({
          roles: res.data?.roles || [],
        })
      }
    })
    setUserRoleModalVisible(true)
  }

  const handleSaveUserRoles = async () => {
    try {
      const values = await userRoleForm.validateFields()
      const roleIds = roles
        .filter((r) => values.roles?.includes(r.roleCode))
        .map((r) => r.id)
      await permissionApi.assignUserAppRoles(selectedUser.id, currentApp?.id || 0, roleIds)
      message.success('角色分配成功')
      setUserRoleModalVisible(false)
    } catch (e: any) {
      message.error('保存失败: ' + e.message)
    }
  }

  const roleColumns = [
    { title: '角色名称', dataIndex: 'roleName', key: 'roleName' },
    { title: '角色编码', dataIndex: 'roleCode', key: 'roleCode' },
    {
      title: '角色类型',
      dataIndex: 'roleType',
      key: 'roleType',
      render: (type: string) => {
        const typeMap: Record<string, { color: string; text: string }> = {
          SYSTEM_ADMIN: { color: 'red', text: '系统管理员' },
          APP_ADMIN: { color: 'orange', text: '应用管理员' },
          USER: { color: 'blue', text: '普通用户' },
          GUEST: { color: 'default', text: '访客' },
          CUSTOM: { color: 'purple', text: '自定义' },
        }
        const info = typeMap[type] || { color: 'default', text: type }
        return <Tag color={info.color}>{info.text}</Tag>
      },
    },
    { title: '排序', dataIndex: 'roleSort', key: 'roleSort', width: 80 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'success' : 'default'}>{status === 1 ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 240,
      render: (_: any, record: RoleInfo) => (
        <Space>
          <Button
            size="small"
            icon={<SettingOutlined />}
            onClick={() => handleConfigurePermissions(record)}
          >
            权限配置
          </Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => handleEditRole(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除该角色？" onConfirm={() => handleDeleteRole(record.id)}>
            <Button size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const userColumns = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    { title: '手机号', dataIndex: 'phone', key: 'phone' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 0 ? 'success' : 'default'}>{status === 0 ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      render: (_: any, record: any) => (
        <Button
          size="small"
          icon={<TeamOutlined />}
          onClick={() => handleAssignRoles(record)}
        >
          分配角色
        </Button>
      ),
    },
  ]

  const roleTypeOptions = [
    { label: '系统管理员', value: 'SYSTEM_ADMIN' },
    { label: '应用管理员', value: 'APP_ADMIN' },
    { label: '普通用户', value: 'USER' },
    { label: '访客', value: 'GUEST' },
    { label: '自定义', value: 'CUSTOM' },
  ]

  return (
    <Layout style={{ minHeight: '100vh', background: '#f5f5f5' }}>
      <Header
        style={{
          background: '#fff',
          padding: '0 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #e8e8e8',
        }}
      >
        <div style={{ fontSize: 18, fontWeight: 'bold' }}>
          <SettingOutlined style={{ marginRight: 8 }} />
          权限与角色管理
        </div>
        <Space>
          <span style={{ color: '#666' }}>当前应用：</span>
          <Tag color="blue">{currentApp?.appName || '未选择'}</Tag>
        </Space>
      </Header>
      <Content style={{ padding: 24 }}>
        <Card size="small">
          <Tabs activeKey={activeTab} onChange={setActiveTab}>
            <TabPane tab={<span><TeamOutlined /> 角色管理</span>} key="role">
              <div style={{ marginBottom: 12 }}>
                <Button type="primary" icon={<PlusOutlined />} onClick={handleAddRole}>
                  新增角色
                </Button>
              </div>
              <Table
                size="small"
                dataSource={roles}
                columns={roleColumns}
                rowKey="id"
                loading={loading}
                pagination={{ pageSize: 10 }}
              />
            </TabPane>
            <TabPane tab={<span><UserOutlined /> 用户角色分配</span>} key="user">
              <Table
                size="small"
                dataSource={users}
                columns={userColumns}
                rowKey="id"
                loading={loading}
                pagination={{ pageSize: 10 }}
              />
            </TabPane>
          </Tabs>
        </Card>
      </Content>

      <Modal
        title={editingRole ? '编辑角色' : '新增角色'}
        open={roleModalVisible}
        onOk={handleSaveRole}
        onCancel={() => setRoleModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="appId" hidden>
            <Input />
          </Form.Item>
          <Space wrap>
            <Form.Item
              name="roleName"
              label="角色名称"
              rules={[{ required: true, message: '请输入角色名称' }]}
              style={{ marginBottom: 0, width: 200 }}
            >
              <Input placeholder="如：应用管理员" />
            </Form.Item>
            <Form.Item
              name="roleCode"
              label="角色编码"
              rules={[{ required: true, message: '请输入角色编码' }]}
              style={{ marginBottom: 0, width: 200 }}
            >
              <Input placeholder="如：app_admin" />
            </Form.Item>
            <Form.Item
              name="roleType"
              label="角色类型"
              rules={[{ required: true, message: '请选择角色类型' }]}
              style={{ marginBottom: 0, width: 160 }}
            >
              <Select>
                {roleTypeOptions.map((opt) => (
                  <Option key={opt.value} value={opt.value}>
                    {opt.label}
                  </Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item
              name="roleSort"
              label="排序"
              style={{ marginBottom: 0, width: 100 }}
            >
              <InputNumber min={1} />
            </Form.Item>
            <Form.Item
              name="status"
              label="状态"
              valuePropName="checked"
              style={{ marginBottom: 0 }}
            >
              <Switch defaultChecked />
            </Form.Item>
          </Space>
          <Form.Item
            name="remark"
            label="备注"
            style={{ marginTop: 12, marginBottom: 0 }}
          >
            <TextArea rows={3} placeholder="角色描述" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`为用户 [${selectedUser?.username}] 分配角色`}
        open={userRoleModalVisible}
        onOk={handleSaveUserRoles}
        onCancel={() => setUserRoleModalVisible(false)}
        width={500}
      >
        <Form form={userRoleForm} layout="vertical">
          <Form.Item
            name="roles"
            label="选择角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select mode="multiple" placeholder="请选择角色" style={{ width: '100%' }}>
              {roles.map((role) => (
                <Option key={role.id} value={role.roleCode}>
                  {role.roleName} ({role.roleCode})
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={`角色 [${selectedRole?.roleName}] 权限配置`}
        width={900}
        open={permissionDrawerVisible}
        onClose={() => setPermissionDrawerVisible(false)}
      >
        {selectedRole && <PermissionConfigPanel roleId={selectedRole.id} />}
      </Drawer>
    </Layout>
  )
}

export default PermissionPage
