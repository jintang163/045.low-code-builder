import React, { useState, useEffect } from 'react'
import { Form, Input, Select, InputNumber, Button, Space, message, Card, Switch, Tag, Alert, Divider } from 'antd'
import { PlusOutlined, DeleteOutlined, PlayCircleOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import { permissionApi, RowPermission, ExpressionParseResult } from '@/api/permission'
import { useAppStore } from '@/store/appStore'

const { Option } = Select
const { TextArea } = Input

interface RowPermissionEditorProps {
  modelId: number
  roleId: number
  onSave?: () => void
}

const userVariables = [
  { label: '用户ID', value: '{user.id}', desc: '当前登录用户ID' },
  { label: '用户名', value: '{user.username}', desc: '当前登录用户名' },
  { label: '用户昵称', value: '{user.nickname}', desc: '当前登录用户昵称' },
  { label: '用户邮箱', value: '{user.email}', desc: '当前登录用户邮箱' },
  { label: '用户手机号', value: '{user.phone}', desc: '当前登录用户手机号' },
  { label: '用户部门ID', value: '{user.deptId}', desc: '当前登录用户所属部门ID' },
  { label: '用户类型', value: '{user.userType}', desc: '当前登录用户类型' },
]

const operators = [
  { label: '等于', value: '=', desc: '等于' },
  { label: '不等于', value: '!=', desc: '不等于' },
  { label: '大于', value: '>', desc: '大于' },
  { label: '小于', value: '<', desc: '小于' },
  { label: '大于等于', value: '>=', desc: '大于等于' },
  { label: '小于等于', value: '<=', desc: '小于等于' },
]

const conditionTypes = [
  { label: '必须满足 (AND)', value: 'AND' },
  { label: '满足其一 (OR)', value: 'OR' },
]

export const RowPermissionEditor: React.FC<RowPermissionEditorProps> = ({ modelId, roleId, onSave }) => {
  const { currentApp } = useAppStore()
  const [form] = Form.useForm()
  const [permissions, setPermissions] = useState<RowPermission[]>([])
  const [editingId, setEditingId] = useState<number | null>(null)
  const [validateResult, setValidateResult] = useState<ExpressionParseResult | null>(null)
  const [testData, setTestData] = useState<string>('{"deptId": 1, "createdBy": 1}')
  const [testResult, setTestResult] = useState<boolean | null>(null)

  const loadPermissions = async () => {
    if (!currentApp?.id) return
    try {
      const res = await permissionApi.getRowPermissions(currentApp.id, roleId, modelId)
      if (res.code === 0 || res.code === 200) {
        setPermissions(res.data || [])
      }
    } catch (e) {
      console.error('加载行级权限失败:', e)
    }
  }

  useEffect(() => {
    loadPermissions()
  }, [modelId, roleId, currentApp?.id])

  const handleAdd = () => {
    setEditingId(null)
    form.resetFields()
    form.setFieldsValue({
      appId: currentApp?.id,
      roleId,
      modelId,
      conditionType: 'AND',
      priority: permissions.length + 1,
      status: 1,
    })
    setValidateResult(null)
    setTestResult(null)
  }

  const handleEdit = (perm: RowPermission) => {
    setEditingId(perm.id || null)
    form.setFieldsValue(perm)
    setValidateResult(null)
    setTestResult(null)
  }

  const handleDelete = async (id: number) => {
    try {
      await permissionApi.deleteRowPermission(id)
      message.success('删除成功')
      loadPermissions()
      if (editingId === id) {
        setEditingId(null)
        form.resetFields()
      }
    } catch (e: any) {
      message.error('删除失败: ' + e.message)
    }
  }

  const handleValidate = async () => {
    try {
      const values = await form.validateFields()
      const res = await permissionApi.validateExpression(values.expression)
      if (res.code === 0 || res.code === 200) {
        setValidateResult(res.data)
        if (res.data?.valid) {
          message.success('表达式验证通过')
        } else {
          message.error('表达式验证失败: ' + res.data?.errorMessage)
        }
      }
    } catch (e: any) {
      message.error('验证失败: ' + e.message)
    }
  }

  const handleTest = async () => {
    try {
      const values = await form.validateFields()
      let data
      try {
        data = JSON.parse(testData)
      } catch {
        message.error('测试数据格式错误，请输入有效的JSON')
        return
      }
      const res = await permissionApi.evaluateExpression(values.expression, data)
      if (res.code === 0 || res.code === 200) {
        setTestResult(res.data)
        message.info(`测试结果: ${res.data ? '✅ 有权限' : '❌ 无权限'}`)
      }
    } catch (e: any) {
      message.error('测试失败: ' + e.message)
    }
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await permissionApi.updateRowPermission(editingId, values)
        message.success('更新成功')
      } else {
        await permissionApi.createRowPermission(values)
        message.success('创建成功')
      }
      loadPermissions()
      setEditingId(null)
      form.resetFields()
      setValidateResult(null)
      setTestResult(null)
      onSave?.()
    } catch (e: any) {
      message.error('保存失败: ' + e.message)
    }
  }

  const insertVariable = (variable: string) => {
    const expression = form.getFieldValue('expression') || ''
    form.setFieldsValue({ expression: expression + variable })
  }

  const insertOperator = (op: string) => {
    const expression = form.getFieldValue('expression') || ''
    form.setFieldsValue({ expression: expression + ' ' + op + ' ' })
  }

  const insertDataField = (field: string) => {
    const expression = form.getFieldValue('expression') || ''
    form.setFieldsValue({ expression: expression + `{data.${field}}` })
  }

  return (
    <div>
      <Card
        title="行级权限列表"
        size="small"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增权限
          </Button>
        }
        style={{ marginBottom: 16 }}
      >
        {permissions.length === 0 ? (
          <Alert type="info" message="暂无行级权限配置" showIcon />
        ) : (
          <Space direction="vertical" style={{ width: '100%' }}>
            {permissions.map((perm) => (
              <Card
                key={perm.id}
                size="small"
                extra={
                  <Space>
                    <Tag color={perm.conditionType === 'AND' ? 'blue' : 'green'}>
                      {perm.conditionType}
                    </Tag>
                    <Tag color={perm.status === 1 ? 'success' : 'default'}>
                      {perm.status === 1 ? '启用' : '禁用'}
                    </Tag>
                    <Button size="small" onClick={() => handleEdit(perm)}>
                      编辑
                    </Button>
                    <Button
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => handleDelete(perm.id!)}
                    />
                  </Space>
                }
              >
                <div style={{ fontWeight: 'bold' }}>{perm.permissionName}</div>
                <div style={{ color: '#666', marginTop: 4 }}>
                  <code>{perm.expression}</code>
                </div>
                {perm.remark && <div style={{ color: '#999', marginTop: 4 }}>{perm.remark}</div>}
              </Card>
            ))}
          </Space>
        )}
      </Card>

      <Card
        title={editingId ? '编辑行级权限' : '新增行级权限'}
        size="small"
        style={{ marginBottom: 16 }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="appId" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="roleId" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="modelId" hidden>
            <Input />
          </Form.Item>

          <Space direction="vertical" style={{ width: '100%' }}>
            <Space wrap>
              <Form.Item name="permissionName" label="权限名称" style={{ marginBottom: 0 }}>
                <Input placeholder="如：查看本部门数据" style={{ width: 200 }} />
              </Form.Item>
              <Form.Item name="permissionCode" label="权限编码" style={{ marginBottom: 0 }}>
                <Input placeholder="如：view_dept_data" style={{ width: 200 }} />
              </Form.Item>
              <Form.Item name="conditionType" label="条件类型" style={{ marginBottom: 0 }}>
                <Select style={{ width: 160 }}>
                  {conditionTypes.map((c) => (
                    <Option key={c.value} value={c.value}>
                      {c.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
              <Form.Item name="priority" label="优先级" style={{ marginBottom: 0 }}>
                <InputNumber min={1} style={{ width: 100 }} />
              </Form.Item>
              <Form.Item name="status" label="状态" valuePropName="checked" style={{ marginBottom: 0 }}>
                <Switch defaultChecked />
              </Form.Item>
            </Space>

            <Divider orientation="left" plain>
              快速插入
            </Divider>

            <div style={{ marginBottom: 8 }}>
              <span style={{ marginRight: 8, color: '#666' }}>用户变量：</span>
              {userVariables.map((v) => (
                <Button
                  key={v.value}
                  size="small"
                  type="link"
                  title={v.desc}
                  onClick={() => insertVariable(v.value)}
                >
                  {v.label}
                </Button>
              ))}
            </div>

            <div style={{ marginBottom: 8 }}>
              <span style={{ marginRight: 8, color: '#666' }}>操作符：</span>
              {operators.map((op) => (
                <Button
                  key={op.value}
                  size="small"
                  type="link"
                  title={op.desc}
                  onClick={() => insertOperator(op.value)}
                >
                  {op.label}
                </Button>
              ))}
            </div>

            <div style={{ marginBottom: 12 }}>
              <span style={{ marginRight: 8, color: '#666' }}>数据字段：</span>
              <Button size="small" type="link" onClick={() => insertDataField('deptId')}>
                deptId
              </Button>
              <Button size="small" type="link" onClick={() => insertDataField('createdBy')}>
                createdBy
              </Button>
              <Button size="small" type="link" onClick={() => insertDataField('id')}>
                id
              </Button>
              <Button size="small" type="link" onClick={() => insertDataField('status')}>
                status
              </Button>
            </div>

            <Form.Item
              name="expression"
              label="权限表达式"
              rules={[{ required: true, message: '请输入权限表达式' }]}
              help="示例：{user.deptId} = {data.deptId} 或 {user.id} = {data.createdBy}"
            >
              <TextArea rows={3} placeholder="例如：{user.deptId} = {data.deptId}" />
            </Form.Item>

            {validateResult && (
              <Alert
                type={validateResult.valid ? 'success' : 'error'}
                showIcon
                message={validateResult.valid ? '表达式有效' : '表达式无效'}
                description={
                  <div>
                    {validateResult.errorMessage && <div>错误：{validateResult.errorMessage}</div>}
                    {validateResult.valid && (
                      <>
                        <div>SQL条件：<code>{validateResult.sqlCondition}</code></div>
                        <div>用户变量：{validateResult.userVariables?.join(', ') || '无'}</div>
                        <div>数据变量：{validateResult.dataVariables?.join(', ') || '无'}</div>
                      </>
                    )}
                  </div>
                }
                style={{ marginBottom: 12 }}
              />
            )}

            <Space>
              <Button type="primary" onClick={handleSave}>
                {editingId ? '更新' : '创建'}
              </Button>
              <Button icon={<PlayCircleOutlined />} onClick={handleValidate}>
                验证表达式
              </Button>
              <Button
                onClick={() => {
                  setEditingId(null)
                  form.resetFields()
                  setValidateResult(null)
                  setTestResult(null)
                }}
              >
                取消
              </Button>
            </Space>
          </Space>
        </Form>
      </Card>

      <Card title="表达式测试" size="small">
        <div style={{ marginBottom: 8 }}>
          <span style={{ color: '#666' }}>测试数据（JSON格式）：</span>
        </div>
        <TextArea
          rows={3}
          value={testData}
          onChange={(e) => setTestData(e.target.value)}
          placeholder='{"deptId": 1, "createdBy": 1, "status": 1}'
          style={{ marginBottom: 8 }}
        />
        <Space>
          <Button type="primary" onClick={handleTest}>
            执行测试
          </Button>
          {testResult !== null && (
            <Tag icon={testResult ? <CheckCircleOutlined /> : <ExclamationCircleOutlined />} color={testResult ? 'success' : 'error'}>
              {testResult ? '有权限' : '无权限'}
            </Tag>
          )}
        </Space>
      </Card>
    </div>
  )
}

export default RowPermissionEditor
