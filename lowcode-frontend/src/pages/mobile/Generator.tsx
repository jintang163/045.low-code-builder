import React, { useState, useEffect } from 'react'
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  Space,
  Switch,
  message,
  Typography,
  Divider,
  Row,
  Col,
  Tag,
  Spin,
  Alert,
  Checkbox,
} from 'antd'
import {
  MobileOutlined,
  CodeOutlined,
  EyeOutlined,
  TouchOutlined,
  HandOutlined,
  AppleOutlined,
  AndroidOutlined,
  StarOutlined,
  AppstoreOutlined,
  DatabaseOutlined,
  FormOutlined,
  DownloadOutlined,
  FileTextOutlined,
  WechatOutlined,
  AlipayCircleOutlined,
  Html5Outlined,
  GlobalOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useAppStore } from '@/store/appStore'
import { appApi, AppInfo } from '@/api'
import { dataModelApi, DataModel } from '@/api/dataModel'
import { pageApi, PageInfo } from '@/api/page'
import { uniappApi, UniAppConfig, UniAppGenerateResult, TargetPlatform, PreviewInfo } from '@/api/uniapp'
import type { Platform } from '@/hooks/useMobileSimulator'

const { Title, Text } = Typography
const { Option } = Select
const { TextArea } = Input

const targetPlatformOptions: { label: string; value: TargetPlatform; icon: React.ReactNode }[] = [
  { label: '微信小程序', value: 'wechat', icon: <WechatOutlined /> },
  { label: '支付宝小程序', value: 'alipay', icon: <AlipayCircleOutlined /> },
  { label: 'H5', value: 'h5', icon: <Html5Outlined /> },
  { label: 'App', value: 'app', icon: <GlobalOutlined /> },
]

const platformOptions = [
  { label: 'iOS', value: 'ios', icon: <AppleOutlined /> },
  { label: 'Android', value: 'android', icon: <AndroidOutlined /> },
  { label: 'HarmonyOS', value: 'harmony', icon: <StarOutlined /> },
]

const platformIcons: Record<Platform, React.ReactNode> = {
  ios: <AppleOutlined />,
  android: <AndroidOutlined />,
  harmony: <StarOutlined />,
}

const platformNames: Record<Platform, string> = {
  ios: 'iOS',
  android: 'Android',
  harmony: 'HarmonyOS',
}

const targetPlatformIcons: Record<TargetPlatform, React.ReactNode> = {
  wechat: <WechatOutlined />,
  alipay: <AlipayCircleOutlined />,
  h5: <Html5Outlined />,
  app: <GlobalOutlined />,
}

const targetPlatformNames: Record<TargetPlatform, string> = {
  wechat: '微信小程序',
  alipay: '支付宝小程序',
  h5: 'H5',
  app: 'App',
}

const MobileGenerator: React.FC = () => {
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [creatingPreview, setCreatingPreview] = useState(false)
  const [appList, setAppList] = useState<AppInfo[]>([])
  const [dataModelList, setDataModelList] = useState<DataModel[]>([])
  const [pageList, setPageList] = useState<PageInfo[]>([])
  const [selectedApp, setSelectedApp] = useState<AppInfo | null>(null)
  const [generateResult, setGenerateResult] = useState<UniAppGenerateResult | null>(null)
  const [previewInfo, setPreviewInfo] = useState<PreviewInfo | null>(null)

  const fetchAppList = async () => {
    try {
      setLoading(true)
      const res = await appApi.list()
      if (res.code === 0 || res.code === 200) {
        setAppList(res.data || [])
      }
    } catch (e: any) {
      message.error('获取应用列表失败: ' + e.message)
    } finally {
      setLoading(false)
    }
  }

  const fetchDataModelList = async (appId: number) => {
    try {
      const res = await dataModelApi.list(appId)
      if (res.code === 0 || res.code === 200) {
        setDataModelList(res.data || [])
      }
    } catch (e: any) {
      message.error('获取数据模型列表失败: ' + e.message)
    }
  }

  const fetchPageList = async (appId: number) => {
    try {
      const res = await pageApi.list(appId)
      if (res.code === 0 || res.code === 200) {
        setPageList(res.data || [])
      }
    } catch (e: any) {
      message.error('获取页面列表失败: ' + e.message)
    }
  }

  useEffect(() => {
    fetchAppList()
  }, [])

  useEffect(() => {
    if (currentApp) {
      setSelectedApp(currentApp)
      form.setFieldsValue({
        appName: currentApp.appName,
        appId: currentApp.appCode,
      })
      fetchDataModelList(currentApp.id!)
      fetchPageList(currentApp.id!)
    }
  }, [currentApp])

  const handleAppChange = (appId: number) => {
    const app = appList.find(a => a.id === appId)
    if (app) {
      setSelectedApp(app)
      form.setFieldsValue({
        appName: app.appName,
        appId: app.appCode,
      })
      fetchDataModelList(app.id!)
      fetchPageList(app.id!)
    }
  }

  const createPreviewSession = async (result: UniAppGenerateResult) => {
    try {
      setCreatingPreview(true)
      const res = await uniappApi.createPreview({
        appId: result.appId || selectedApp?.id || 1,
        pageId: 1,
        platform: 'h5',
        appCode: result.appCode,
      })
      if (res.code === 0 || res.code === 200) {
        setPreviewInfo(res.data)
        message.success('预览会话创建成功')
      }
    } catch (e: any) {
      console.error('创建预览会话失败:', e)
      message.warning('预览会话创建失败: ' + (e.message || '未知错误'))
    } finally {
      setCreatingPreview(false)
    }
  }

  const handleGenerate = async () => {
    try {
      const values = await form.validateFields()
      setGenerating(true)
      setGenerateResult(null)
      setPreviewInfo(null)

      const config: UniAppConfig = {
        appName: values.appName,
        appId: values.appId,
        platforms: values.platforms,
        targetPlatforms: values.targetPlatforms,
        dataModelIds: values.dataModelIds,
        pageIds: values.pageIds,
        touchEventsEnabled: values.touchEventsEnabled ?? true,
        gesturesEnabled: values.gesturesEnabled ?? true,
        description: values.description,
        version: values.version || '1.0.0',
      }

      const res = await uniappApi.generateUniApp(config)
      if (res.code === 0 || res.code === 200) {
        const result: UniAppGenerateResult = res.data
        setGenerateResult(result)
        message.success('uni-app 项目生成成功！')

        await createPreviewSession(result)
      }
    } catch (e: any) {
      message.error('生成失败: ' + e.message)
    } finally {
      setGenerating(false)
    }
  }

  const handleGoToPreview = () => {
    const token = previewInfo?.previewToken || generateResult?.previewToken
    if (token) {
      navigate(`/mobile/preview?token=${encodeURIComponent(token)}`)
    } else {
      navigate('/mobile/preview')
    }
  }

  const handleDownload = async () => {
    try {
      if (generateResult?.downloadUrl) {
        const link = document.createElement('a')
        link.href = generateResult.downloadUrl
        link.download = `${generateResult.appCode}.zip`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        message.success('下载已开始')
        return
      }

      if (!generateResult?.appCode) return
      const res = await uniappApi.downloadUniApp(generateResult.appCode)
      const blob = new Blob([res.data as BlobPart], {
        type: 'application/zip',
      })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${generateResult.appCode}.zip`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      message.success('下载已开始')
    } catch (e: any) {
      message.error('下载失败: ' + e.message)
    }
  }

  return (
    <div style={{ padding: 24, maxWidth: 1200, margin: '0 auto' }}>
      <div style={{ marginBottom: 24, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Space>
          <MobileOutlined style={{ fontSize: 28, color: '#1677ff' }} />
          <div>
            <Title level={3} style={{ margin: 0 }}>
              移动端代码生成器
            </Title>
            <Text type="secondary">
              配置应用信息，一键生成跨平台 uni-app 项目
            </Text>
          </div>
        </Space>
        <Button icon={<EyeOutlined />} onClick={() => navigate('/mobile/preview')}>
          预览页面
        </Button>
      </div>

      <Spin spinning={loading}>
        <Row gutter={[24, 24]}>
          <Col span={16}>
            <Card title="基础配置" style={{ marginBottom: 24 }}>
              <Form form={form} layout="vertical">
                <Form.Item
                  name="selectedAppId"
                  label="选择应用"
                  rules={[{ required: true, message: '请选择应用' }]}
                >
                  <Select
                    placeholder="请选择要生成移动端的应用"
                    onChange={handleAppChange}
                    allowClear
                  >
                    {appList.map(app => (
                      <Option key={app.id} value={app.id}>
                        <Space>
                          <AppstoreOutlined />
                          <span>{app.appName}</span>
                          <Tag color="blue">{app.appCode}</Tag>
                        </Space>
                      </Option>
                    ))}
                  </Select>
                </Form.Item>

                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item
                      name="appName"
                      label="应用名称"
                      rules={[
                        { required: true, message: '请输入应用名称' },
                        { max: 50, message: '应用名称不能超过50个字符' },
                      ]}
                    >
                      <Input placeholder="请输入应用名称，如：我的应用" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item
                      name="appId"
                      label="AppID"
                      rules={[
                        { required: true, message: '请输入AppID' },
                        { pattern: /^[a-zA-Z][a-zA-Z0-9.]*$/, message: 'AppID必须以字母开头，只能包含字母、数字和点' },
                      ]}
                      help="uni-app 应用标识，如：com.example.myapp"
                    >
                      <Input placeholder="请输入AppID，如：com.example.myapp" />
                    </Form.Item>
                  </Col>
                </Row>

                <Form.Item
                  name="version"
                  label="版本号"
                  initialValue="1.0.0"
                >
                  <Input placeholder="请输入版本号，如：1.0.0" />
                </Form.Item>

                <Form.Item
                  name="description"
                  label="应用描述"
                >
                  <TextArea rows={3} placeholder="请输入应用描述" maxLength={200} showCount />
                </Form.Item>
              </Form>
            </Card>

            <Card title="运行平台（App 操作系统）" style={{ marginBottom: 24 }}>
              <Form form={form} layout="vertical">
                <Form.Item
                  name="platforms"
                  label="选择 App 运行平台"
                  rules={[{ required: true, message: '请至少选择一个平台' }]}
                >
                  <Select
                    mode="multiple"
                    placeholder="请选择 App 运行的操作系统平台"
                    style={{ width: '100%' }}
                    optionLabelProp="label"
                  >
                    {platformOptions.map(option => (
                      <Option key={option.value} value={option.value} label={option.label}>
                        <Space>
                          {option.icon}
                          <span>{option.label}</span>
                        </Space>
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Form>
            </Card>

            <Card title="目标发布平台" style={{ marginBottom: 24 }}>
              <Form form={form} layout="vertical">
                <Form.Item
                  name="targetPlatforms"
                  label="选择目标发布平台"
                  rules={[{ required: true, message: '请至少选择一个目标发布平台' }]}
                >
                  <Checkbox.Group style={{ width: '100%' }}>
                    <Row gutter={[16, 16]}>
                      {targetPlatformOptions.map(option => (
                        <Col span={12} key={option.value}>
                          <Checkbox value={option.value} style={{ width: '100%' }}>
                            <Space>
                              {option.icon}
                              <span>{option.label}</span>
                            </Space>
                          </Checkbox>
                        </Col>
                      ))}
                    </Row>
                  </Checkbox.Group>
                </Form.Item>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  选择需要发布的目标平台，将对应生成各平台的代码和配置
                </Text>
              </Form>
            </Card>

            <Card title="数据与页面" style={{ marginBottom: 24 }}>
              <Form form={form} layout="vertical">
                <Form.Item
                  name="dataModelIds"
                  label={
                    <Space>
                      <DatabaseOutlined />
                      <span>选择数据模型</span>
                    </Space>
                  }
                  help="选择需要生成移动端页面的数据模型，不选则默认包含全部"
                >
                  <Select
                    mode="multiple"
                    placeholder="请选择数据模型（可选，默认全部）"
                    style={{ width: '100%' }}
                    allowClear
                    disabled={!selectedApp}
                  >
                    {dataModelList.map(model => (
                      <Option key={model.id} value={model.id}>
                        <Space>
                          <DatabaseOutlined />
                          <span>{model.modelName}</span>
                          <Tag color="green">{model.modelCode}</Tag>
                        </Space>
                      </Option>
                    ))}
                  </Select>
                </Form.Item>

                <Form.Item
                  name="pageIds"
                  label={
                    <Space>
                      <FormOutlined />
                      <span>选择页面</span>
                    </Space>
                  }
                  help="选择需要生成的页面，不选则默认包含全部"
                >
                  <Select
                    mode="multiple"
                    placeholder="请选择页面（可选，默认全部）"
                    style={{ width: '100%' }}
                    allowClear
                    disabled={!selectedApp}
                  >
                    {pageList.map(page => (
                      <Option key={page.id} value={page.id}>
                        <Space>
                          <FormOutlined />
                          <span>{page.pageName}</span>
                          <Tag color="blue">{page.pageCode}</Tag>
                        </Space>
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Form>
            </Card>

            <Card title="交互配置" style={{ marginBottom: 24 }}>
              <Form form={form} layout="vertical">
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item
                      name="touchEventsEnabled"
                      label={
                        <Space>
                          <TouchOutlined />
                          <span>触屏事件</span>
                        </Space>
                      }
                      valuePropName="checked"
                      initialValue={true}
                    >
                      <Switch
                        checkedChildren="启用"
                        unCheckedChildren="禁用"
                      />
                    </Form.Item>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      启用后支持 touchstart、touchmove、touchend 等触屏事件
                    </Text>
                  </Col>
                  <Col span={12}>
                    <Form.Item
                      name="gesturesEnabled"
                      label={
                        <Space>
                          <HandOutlined />
                          <span>手势识别</span>
                        </Space>
                      }
                      valuePropName="checked"
                      initialValue={true}
                    >
                      <Switch
                        checkedChildren="启用"
                        unCheckedChildren="禁用"
                      />
                    </Form.Item>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      启用后支持滑动、点击、长按、缩放等手势识别
                    </Text>
                  </Col>
                </Row>
              </Form>
            </Card>

            <Card>
              <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
                <Button size="large" onClick={() => form.resetFields()}>
                  重置
                </Button>
                <Button
                  type="primary"
                  size="large"
                  icon={<CodeOutlined />}
                  onClick={handleGenerate}
                  loading={generating}
                >
                  生成 uni-app 项目
                </Button>
              </Space>
            </Card>
          </Col>

          <Col span={8}>
            <Card
              title={
                <Space>
                  <EyeOutlined />
                  <span>生成结果</span>
                </Space>
              }
              style={{ marginBottom: 24 }}
              extra={creatingPreview && <Spin size="small" />}
            >
              {!generateResult ? (
                <div style={{ textAlign: 'center', padding: '40px 0' }}>
                  <MobileOutlined style={{ fontSize: 48, color: '#d9d9d9', marginBottom: 16 }} />
                  <div>
                    <Text type="secondary">
                      配置完成后点击生成按钮，<br />生成结果将显示在这里
                    </Text>
                  </div>
                </div>
              ) : (
                <div>
                  <Alert
                    type="success"
                    showIcon
                    message="生成成功"
                    description="uni-app 项目已成功生成"
                    style={{ marginBottom: 16 }}
                  />

                  {previewInfo && (
                    <Alert
                      type="info"
                      showIcon
                      message="预览就绪"
                      description="预览会话已创建，可前往预览页面查看"
                      style={{ marginBottom: 16 }}
                    />
                  )}

                  <Divider style={{ margin: '12px 0' }} />

                  <div style={{ marginBottom: 16 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>应用名称</Text>
                    <div style={{ fontSize: 16, fontWeight: 500 }}>{generateResult.appCode}</div>
                  </div>

                  {generateResult.fileCount !== undefined && (
                    <div style={{ marginBottom: 16 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>生成文件数</Text>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <FileTextOutlined style={{ color: '#52c41a' }} />
                        <span style={{ fontSize: 16, fontWeight: 500 }}>
                          {generateResult.fileCount} 个文件
                        </span>
                      </div>
                    </div>
                  )}

                  <div style={{ marginBottom: 16 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>运行平台</Text>
                    <div style={{ marginTop: 4 }}>
                      <Space wrap>
                        {generateResult.platforms.map(platform => (
                          <Tag key={platform} icon={platformIcons[platform]} color="blue">
                            {platformNames[platform]}
                          </Tag>
                        ))}
                      </Space>
                    </div>
                  </div>

                  {generateResult.targetPlatforms && generateResult.targetPlatforms.length > 0 && (
                    <div style={{ marginBottom: 16 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>发布平台</Text>
                      <div style={{ marginTop: 4 }}>
                        <Space wrap>
                          {generateResult.targetPlatforms.map(tp => (
                            <Tag key={tp} icon={targetPlatformIcons[tp]} color="green">
                              {targetPlatformNames[tp]}
                            </Tag>
                          ))}
                        </Space>
                      </div>
                    </div>
                  )}

                  <div style={{ marginBottom: 16 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>预览Token</Text>
                    <div style={{ fontFamily: 'monospace', fontSize: 12, background: '#f5f5f5', padding: 8, borderRadius: 4, marginTop: 4, wordBreak: 'break-all' }}>
                      {previewInfo?.previewToken || generateResult.previewToken || '暂无'}
                    </div>
                  </div>

                  <div style={{ marginBottom: 16 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>项目路径</Text>
                    <div style={{ fontFamily: 'monospace', fontSize: 12, background: '#f5f5f5', padding: 8, borderRadius: 4, marginTop: 4, wordBreak: 'break-all' }}>
                      {generateResult.projectPath}
                    </div>
                  </div>

                  {previewInfo?.previewUrl && (
                    <div style={{ marginBottom: 16 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>预览地址</Text>
                      <div style={{ fontFamily: 'monospace', fontSize: 11, background: '#f5f5f5', padding: 8, borderRadius: 4, marginTop: 4, wordBreak: 'break-all', color: '#1677ff' }}>
                        {previewInfo.previewUrl}
                      </div>
                    </div>
                  )}

                  <div style={{ marginBottom: 16 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>包大小</Text>
                    <div style={{ fontSize: 16, fontWeight: 500 }}>
                      {(generateResult.packageSize / 1024 / 1024).toFixed(2)} MB
                    </div>
                  </div>

                  <div style={{ marginBottom: 16 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>生成时间</Text>
                    <div>{generateResult.generatedTime}</div>
                  </div>

                  <Divider style={{ margin: '12px 0' }} />

                  <Space style={{ width: '100%' }} direction="vertical">
                    <Button
                      type="primary"
                      block
                      icon={<EyeOutlined />}
                      onClick={handleGoToPreview}
                      loading={creatingPreview}
                    >
                      前往预览
                    </Button>
                    <Button
                      block
                      icon={<DownloadOutlined />}
                      onClick={handleDownload}
                    >
                      {generateResult.downloadUrl ? '下载项目包' : '下载项目 ZIP'}
                    </Button>
                    {generateResult.downloadUrl && (
                      <div style={{ fontSize: 11, color: '#8c8c8c', padding: '0 8px', wordBreak: 'break-all' }}>
                        下载链接: {generateResult.downloadUrl}
                      </div>
                    )}
                  </Space>
                </div>
              )}
            </Card>

            <Card title="生成说明">
              <div style={{ fontSize: 13, lineHeight: 1.8 }}>
                <div style={{ marginBottom: 8 }}>
                  <strong>生成内容：</strong>
                </div>
                <ul style={{ paddingLeft: 20, margin: 0 }}>
                  <li>uni-app 项目基础结构</li>
                  <li>pages.json 页面路由配置</li>
                  <li>manifest.json 应用配置</li>
                  <li>App.vue 入口文件</li>
                  <li>各页面 Vue 组件</li>
                  <li>API 请求封装</li>
                  <li>数据模型 TypeScript 类型定义</li>
                  <li>工具函数库</li>
                </ul>
                <Divider style={{ margin: '12px 0' }} />
                <div style={{ marginBottom: 8 }}>
                  <strong>目标发布平台：</strong>
                </div>
                <ul style={{ paddingLeft: 20, margin: 0 }}>
                  <li><WechatOutlined /> 微信小程序</li>
                  <li><AlipayCircleOutlined /> 支付宝小程序</li>
                  <li><Html5Outlined /> H5 - 移动端网页</li>
                  <li><GlobalOutlined /> App - iOS / Android / HarmonyOS</li>
                </ul>
                <Divider style={{ margin: '12px 0' }} />
                <div style={{ marginBottom: 8 }}>
                  <strong>App 运行平台：</strong>
                </div>
                <ul style={{ paddingLeft: 20, margin: 0 }}>
                  <li><AppleOutlined /> iOS - App Store</li>
                  <li><AndroidOutlined /> Android - 应用商店</li>
                  <li><StarOutlined /> HarmonyOS - 华为应用市场</li>
                </ul>
              </div>
            </Card>
          </Col>
        </Row>
      </Spin>
    </div>
  )
}

export default MobileGenerator
