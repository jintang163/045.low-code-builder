import React, { useEffect, useState } from 'react'
import {
  Row,
  Col,
  Input,
  Button,
  Space,
  Tag,
  Statistic,
  Card,
  Tabs,
  Pagination,
  Modal,
  Form,
  Input as AntdInput,
  message,
  Drawer,
  Descriptions,
  Badge,
  Spin,
  Upload,
  Tooltip
} from 'antd'
import {
  SearchOutlined,
  AppstoreOutlined,
  DownloadOutlined,
  StarOutlined,
  UploadOutlined,
  ImportOutlined,
  ExportOutlined,
  PlusOutlined,
  RocketOutlined,
  ShareAltOutlined
} from '@ant-design/icons'
import TemplateCard from '@/components/TemplateCard'
import { AppTemplate, templateApi } from '@/api/template'
import { useAppStore } from '@/store/appStore'
import { useNavigate } from 'react-router-dom'
import type { UploadProps } from 'antd'

const { Search } = Input
const { TextArea } = AntdInput

const TemplateMarket: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [templates, setTemplates] = useState<AppTemplate[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(12)
  const [keyword, setKeyword] = useState('')
  const [activeCategory, setActiveCategory] = useState<string>('')
  const [categories, setCategories] = useState<{ key: string; name: string }[]>([])
  const [stats, setStats] = useState<any>({})

  const [installModalVisible, setInstallModalVisible] = useState(false)
  const [selectedTemplate, setSelectedTemplate] = useState<AppTemplate | null>(null)
  const [installForm] = Form.useForm()
  const [installing, setInstalling] = useState(false)

  const [detailVisible, setDetailVisible] = useState(false)
  const [detailTemplate, setDetailTemplate] = useState<AppTemplate | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const [publishModalVisible, setPublishModalVisible] = useState(false)
  const [publishForm] = Form.useForm()
  const [publishing, setPublishing] = useState(false)

  useEffect(() => {
    loadCategories()
    loadStats()
  }, [])

  useEffect(() => {
    loadTemplates()
  }, [current, pageSize, keyword, activeCategory])

  const loadCategories = async () => {
    try {
      const res = await templateApi.categories()
      if (res.code === 0 || res.code === 200) {
        setCategories(res.data || [])
      }
    } catch (e) {
      console.error(e)
    }
  }

  const loadStats = async () => {
    try {
      const res = await templateApi.stats()
      if (res.code === 0 || res.code === 200) {
        setStats(res.data || {})
      }
    } catch (e) {
      console.error(e)
    }
  }

  const loadTemplates = async () => {
    setLoading(true)
    try {
      const res = await templateApi.page(current, pageSize, {
        keyword: keyword || undefined,
        category: activeCategory || undefined,
      })
      if (res.code === 0 || res.code === 200) {
        setTemplates(res.data?.records || [])
        setTotal(res.data?.total || 0)
      }
    } catch (e) {
      console.error(e)
      message.error('加载模板列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleInstall = (template: AppTemplate) => {
    setSelectedTemplate(template)
    installForm.setFieldsValue({
      appName: template.templateName,
      appCode: template.templateCode + '_' + Date.now().toString().slice(-6),
    })
    setInstallModalVisible(true)
  }

  const handleInstallSubmit = async () => {
    if (!selectedTemplate) return
    try {
      const values = await installForm.validateFields()
      setInstalling(true)
      const res = await templateApi.install(selectedTemplate.id!, values)
      if (res.code === 0 || res.code === 200) {
        message.success(`安装成功！已创建应用：${values.appName}`)
        setInstallModalVisible(false)
        navigate('/app')
      }
    } catch (e: any) {
      message.error(e.message || '安装失败')
    } finally {
      setInstalling(false)
    }
  }

  const handleViewDetail = async (template: AppTemplate) => {
    setDetailTemplate(template)
    setDetailVisible(true)
    setDetailLoading(true)
    try {
      const res = await templateApi.detail(template.id!)
      if (res.code === 0 || res.code === 200) {
        setDetailTemplate(res.data)
      }
    } catch (e) {
      console.error(e)
    } finally {
      setDetailLoading(false)
    }
  }

  const handlePublish = () => {
    if (!currentApp) {
      message.warning('请先选择一个应用')
      return
    }
    publishForm.setFieldsValue({
      templateName: currentApp.appName,
      templateCode: currentApp.appCode + '_template',
      templateDesc: currentApp.appDesc || '',
      category: 'business',
      version: '1.0.0',
      tags: '',
    })
    setPublishModalVisible(true)
  }

  const handlePublishSubmit = async () => {
    if (!currentApp) return
    try {
      const values = await publishForm.validateFields()
      setPublishing(true)
      const res = await templateApi.publish({
        appId: currentApp.id!,
        ...values,
        templateType: 1,
      })
      if (res.code === 0 || res.code === 200) {
        message.success('发布为模板成功！')
        setPublishModalVisible(false)
        loadTemplates()
      }
    } catch (e: any) {
      message.error(e.message || '发布失败')
    } finally {
      setPublishing(false)
    }
  }

  const handleImport: UploadProps['beforeUpload'] = (file) => {
    const reader = new FileReader()
    reader.onload = async (e) => {
      try {
        const res = await templateApi.import(file)
        if (res.code === 0 || res.code === 200) {
          message.success('导入成功')
          loadTemplates()
        }
      } catch (err: any) {
        message.error(err.message || '导入失败')
      }
    }
    reader.readAsText(file)
    return false
  }

  const handleExport = async (template: AppTemplate) => {
    try {
      const res: any = await templateApi.export(template.id!)
      const blob = new Blob([res.data], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${template.templateCode}-template.json`
      a.click()
      URL.revokeObjectURL(url)
      message.success('导出成功')
    } catch (e) {
      message.error('导出失败')
    }
  }

  const categoryTabs = categories.map(cat => ({
    key: cat.key,
    label: cat.name,
  }))

  const tagList = (tags?: string) => {
    if (!tags) return null
    return tags.split(',').filter(t => t.trim()).map((tag, idx) => (
      <Tag key={idx} color="blue">{tag}</Tag>
    ))
  }

  return (
    <div style={{ padding: 0 }}>
      <div style={{
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        borderRadius: 12,
        padding: '40px 32px',
        marginBottom: 24,
        color: '#fff'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h1 style={{ color: '#fff', fontSize: 28, fontWeight: 700, margin: '0 0 8px 0' }}>
              <AppstoreOutlined style={{ marginRight: 12 }} />
              应用模板市场
            </h1>
            <p style={{ color: 'rgba(255,255,255,0.85)', fontSize: 14, margin: 0 }}>
              内置OA、CRM、进销存等行业模板，一键安装即用，快速搭建您的业务应用
            </p>
          </div>
          <Space size={16}>
            <Statistic
              title={<span style={{ color: 'rgba(255,255,255,0.85)' }}>模板总数</span>}
              value={stats.totalTemplates || 0}
              valueStyle={{ color: '#fff', fontSize: 24 }}
            />
            <Statistic
              title={<span style={{ color: 'rgba(255,255,255,0.85)' }}>累计安装</span>}
              value={stats.totalInstalls || 0}
              valueStyle={{ color: '#fff', fontSize: 24 }}
            />
          </Space>
        </div>

        <div style={{ marginTop: 24, display: 'flex', gap: 12, alignItems: 'center' }}>
          <Search
            placeholder="搜索模板名称、编码或描述..."
            size="large"
            allowClear
            style={{ width: 480, borderRadius: 8 }}
            onSearch={(value) => {
              setKeyword(value)
              setCurrent(1)
            }}
            onChange={(e) => !e.target.value && setKeyword('')}
            enterButton={<SearchOutlined />}
          />
          <Button
            type="primary"
            size="large"
            icon={<PlusOutlined />}
            onClick={handlePublish}
            style={{
              background: 'rgba(255,255,255,0.2)',
              border: '1px solid rgba(255,255,255,0.3)',
              color: '#fff',
              borderRadius: 8,
            }}
          >
            发布为模板
          </Button>
          <Upload showUploadList={false} beforeUpload={handleImport} accept=".json">
            <Button
              size="large"
              icon={<ImportOutlined />}
              style={{
                background: 'rgba(255,255,255,0.2)',
                border: '1px solid rgba(255,255,255,0.3)',
                color: '#fff',
                borderRadius: 8,
              }}
            >
              导入模板
            </Button>
          </Upload>
        </div>
      </div>

      <Card
        bodyStyle={{ padding: '16px 24px 0 24px' }}
        style={{ marginBottom: 16, borderRadius: 8 }}
      >
        <Tabs
          activeKey={activeCategory || ''}
          onChange={(key) => {
            setActiveCategory(key)
            setCurrent(1)
          }}
          items={categoryTabs}
        />
      </Card>

      <Spin spinning={loading}>
        <Row gutter={[16, 16]}>
          {templates.map(template => (
            <Col xs={24} sm={12} md={8} lg={6} xl={6} key={template.id}>
              <TemplateCard
                template={template}
                onInstall={handleInstall}
                onView={handleViewDetail}
                mode="market"
              />
            </Col>
          ))}
        </Row>

        {!loading && templates.length === 0 && (
          <div style={{ textAlign: 'center', padding: '60px 0', color: '#8c8c8c' }}>
            <AppstoreOutlined style={{ fontSize: 48, marginBottom: 16, color: '#d9d9d9' }} />
            <p>暂无模板数据</p>
          </div>
        )}

        <div style={{ textAlign: 'center', marginTop: 24 }}>
          <Pagination
            current={current}
            pageSize={pageSize}
            total={total}
            onChange={(page, size) => {
              setCurrent(page)
              setPageSize(size)
            }}
            showSizeChanger
            showQuickJumper
            showTotal={(t) => `共 ${t} 个模板`}
          />
        </div>
      </Spin>

      <Modal
        title={
          <Space>
            <RocketOutlined style={{ color: '#1677ff' }} />
            安装模板 - {selectedTemplate?.templateName}
          </Space>
        }
        open={installModalVisible}
        onOk={handleInstallSubmit}
        onCancel={() => setInstallModalVisible(false)}
        confirmLoading={installing}
        okText="立即安装"
        width={520}
      >
        <Form form={installForm} layout="vertical">
          <Form.Item
            name="appName"
            label="应用名称"
            rules={[{ required: true, message: '请输入应用名称' }]}
          >
            <Input placeholder="请输入应用名称" />
          </Form.Item>
          <Form.Item
            name="appCode"
            label="应用编码"
            rules={[{ required: true, message: '请输入应用编码' }]}
          >
            <Input placeholder="请输入应用编码" />
          </Form.Item>
          <div style={{
            padding: '12px 16px',
            background: '#e6f7ff',
            border: '1px solid #91d5ff',
            borderRadius: 6,
            fontSize: 13,
            color: '#0050b3'
          }}>
            <div style={{ fontWeight: 600, marginBottom: 6 }}>模板包含内容：</div>
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              <li>预置数据模型（数据表+字段）</li>
              <li>标准业务流程</li>
              <li>预置角色权限</li>
            </ul>
          </div>
        </Form>
      </Modal>

      <Drawer
        title={
          <Space>
            <Badge status="processing" color="blue" />
            模板详情
          </Space>
        }
        width={640}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
        extra={
          <Space>
            <Tooltip title="导出">
              <Button
                icon={<ExportOutlined />}
                onClick={() => detailTemplate && handleExport(detailTemplate)}
              />
            </Tooltip>
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={() => detailTemplate && handleInstall(detailTemplate)}
            >
              一键安装
            </Button>
          </Space>
        }
      >
        {detailLoading ? (
          <div style={{ textAlign: 'center', padding: 60 }}>
            <Spin tip="加载中..." />
          </div>
        ) : detailTemplate ? (
          <div>
            <div style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: 16,
              marginBottom: 24,
              paddingBottom: 24,
              borderBottom: '1px solid #f0f0f0'
            }}>
              <div style={{
                width: 64,
                height: 64,
                borderRadius: 12,
                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                fontSize: 28,
              }}>
                <AppstoreOutlined />
              </div>
              <div style={{ flex: 1 }}>
                <h2 style={{ margin: '0 0 8px 0', fontSize: 20 }}>
                  {detailTemplate.templateName}
                </h2>
                <div style={{ color: '#8c8c8c', marginBottom: 12 }}>
                  {detailTemplate.templateCode} · v{detailTemplate.version}
                </div>
                <Space wrap>
                  {tagList(detailTemplate.tags)}
                  {detailTemplate.templateType !== undefined && (
                    <Tag color={detailTemplate.templateType === 0 ? 'gold' : 'blue'}>
                      {detailTemplate.templateType === 0 ? '官方模板' : detailTemplate.templateType === 1 ? '用户模板' : '团队模板'}
                    </Tag>
                  )}
                </Space>
              </div>
            </div>

            <Descriptions column={2} size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="发布者">
                {detailTemplate.publisher || '未知'}
              </Descriptions.Item>
              <Descriptions.Item label="分类">
                {detailTemplate.category || '其他'}
              </Descriptions.Item>
              <Descriptions.Item label="安装次数">
                <Space>
                  <DownloadOutlined />
                  {detailTemplate.installCount || 0}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="点赞数">
                <Space>
                  <StarOutlined />
                  {detailTemplate.starCount || 0}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="发布时间">
                {detailTemplate.publishTime || detailTemplate.createdTime || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="更新时间">
                {detailTemplate.updatedTime || '-'}
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginBottom: 16 }}>
              <h4 style={{ marginBottom: 8 }}>模板描述</h4>
              <p style={{ color: '#595959', lineHeight: 1.8, margin: 0 }}>
                {detailTemplate.templateDesc || '暂无描述'}
              </p>
            </div>

            <Card
              size="small"
              title={
                <Space>
                  <ShareAltOutlined />
                  功能亮点
                </Space>
              }
              style={{ marginTop: 16 }}
            >
              <ul style={{ margin: 0, paddingLeft: 18, color: '#595959', lineHeight: 2 }}>
                <li>完整的数据模型设计，开箱即用</li>
                <li>预置标准业务流程和审批流</li>
                <li>支持自定义扩展，灵活适配业务</li>
                <li>一键安装，自动生成前后端代码</li>
              </ul>
            </Card>
          </div>
        ) : null}
      </Drawer>

      <Modal
        title={
          <Space>
            <UploadOutlined style={{ color: '#52c41a' }} />
            发布当前应用为模板
          </Space>
        }
        open={publishModalVisible}
        onOk={handlePublishSubmit}
        onCancel={() => setPublishModalVisible(false)}
        confirmLoading={publishing}
        okText="发布模板"
        width={520}
      >
        <div style={{
          padding: '12px 16px',
          background: '#f6ffed',
          border: '1px solid #b7eb8f',
          borderRadius: 6,
          marginBottom: 16,
          fontSize: 13,
          color: '#389e0d'
        }}>
          <strong>当前应用：</strong>{currentApp?.appName}（{currentApp?.appCode}）
          <br />
          发布后将自动脱敏敏感数据（如密码等）
        </div>
        <Form form={publishForm} layout="vertical">
          <Form.Item
            name="templateName"
            label="模板名称"
            rules={[{ required: true, message: '请输入模板名称' }]}
          >
            <Input placeholder="请输入模板名称" />
          </Form.Item>
          <Form.Item
            name="templateCode"
            label="模板编码"
            rules={[{ required: true, message: '请输入模板编码' }]}
          >
            <Input placeholder="请输入模板编码，英文标识" />
          </Form.Item>
          <Form.Item
            name="templateDesc"
            label="模板描述"
          >
            <TextArea rows={3} placeholder="请输入模板描述，将展示在市场列表" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="category"
                label="分类"
                rules={[{ required: true, message: '请选择分类' }]}
              >
                <select style={{ width: '100%', height: 32, borderRadius: 6, border: '1px solid #d9d9d9' }}>
                  <option value="oa">OA办公</option>
                  <option value="crm">CRM客户</option>
                  <option value="inventory">进销存</option>
                  <option value="business">业务系统</option>
                  <option value="system">系统工具</option>
                  <option value="other">其他</option>
                </select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="version"
                label="版本号"
                rules={[{ required: true, message: '请输入版本号' }]}
              >
                <Input placeholder="1.0.0" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="tags"
            label="标签"
            extra="多个标签用逗号分隔"
          >
            <Input placeholder="如：OA,审批,考勤" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default TemplateMarket
