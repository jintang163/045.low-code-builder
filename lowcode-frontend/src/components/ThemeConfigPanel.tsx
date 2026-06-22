import React, { useState, useEffect } from 'react'
import {
  Drawer,
  Form,
  Input,
  ColorPicker,
  Select,
  Slider,
  Button,
  Space,
  Divider,
  InputNumber,
  Radio,
  Tabs,
  Card,
  Row,
  Col,
  Tag,
  message,
  Popconfirm,
  Upload,
  Switch,
  Typography,
} from 'antd'
import {
  SaveOutlined,
  PlusOutlined,
  CopyOutlined,
  DeleteOutlined,
  UploadOutlined,
  ReloadOutlined,
  CheckOutlined,
} from '@ant-design/icons'
import type { UploadProps } from 'antd'
import { AppTheme, themeApi } from '@/api'
import { useTheme } from '@/context/ThemeContext'

const { TextArea } = Input
const { Option } = Select
const { TabPane } = Tabs
const { Title, Text } = Typography

interface ThemeConfigPanelProps {
  visible: boolean
  onClose: () => void
  appId: number
}

const presetThemes = [
  {
    name: '科技蓝',
    primary: '#1677ff',
    success: '#52c41a',
    warning: '#faad14',
    error: '#ff4d4f',
    mode: 'light',
  },
  {
    name: '活力橙',
    primary: '#fa8c16',
    success: '#52c41a',
    warning: '#faad14',
    error: '#ff4d4f',
    mode: 'light',
  },
  {
    name: '自然绿',
    primary: '#52c41a',
    success: '#52c41a',
    warning: '#faad14',
    error: '#ff4d4f',
    mode: 'light',
  },
  {
    name: '优雅紫',
    primary: '#722ed1',
    success: '#52c41a',
    warning: '#faad14',
    error: '#ff4d4f',
    mode: 'light',
  },
  {
    name: '深邃蓝',
    primary: '#1677ff',
    success: '#52c41a',
    warning: '#faad14',
    error: '#ff4d4f',
    mode: 'dark',
  },
  {
    name: '暗夜模式',
    primary: '#177ddc',
    success: '#49aa19',
    warning: '#d89614',
    error: '#d32029',
    mode: 'dark',
  },
]

const fontFamilies = [
  { label: '系统默认', value: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif" },
  { label: '思源黑体', value: "'Noto Sans SC', 'Source Han Sans CN', sans-serif" },
  { label: '微软雅黑', value: "'Microsoft YaHei', '微软雅黑', sans-serif" },
  { label: 'PingFang SC', value: "'PingFang SC', 'Helvetica Neue', sans-serif" },
  { label: '等宽字体', value: "'SF Mono', 'Fira Code', 'Consolas', monospace" },
]

const layoutModes = [
  { label: '侧边菜单', value: 'side' },
  { label: '顶部菜单', value: 'top' },
  { label: '混合布局', value: 'mix' },
]

const ThemeConfigPanel: React.FC<ThemeConfigPanelProps> = ({ visible, onClose, appId }) => {
  const [form] = Form.useForm<AppTheme>()
  const { applyTheme, theme: currentTheme, themeMode } = useTheme()
  const [themes, setThemes] = useState<AppTheme[]>([])
  const [activeTab, setActiveTab] = useState('basic')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (visible && appId) {
      loadThemes()
    }
  }, [visible, appId])

  const loadThemes = async () => {
    try {
      const list = await themeApi.list(appId)
      setThemes(list)
      if (list.length > 0) {
        const defaultTheme = list.find(t => t.isDefault === 1) || list[0]
        form.setFieldsValue(defaultTheme)
        applyTheme(defaultTheme)
      }
    } catch (error) {
      message.error('加载主题列表失败')
    }
  }

  const handleValuesChange = (_: any, allValues: AppTheme) => {
    applyTheme(allValues)
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      if (values.id) {
        await themeApi.update(values)
        message.success('主题更新成功')
      } else {
        const newTheme = await themeApi.create({ ...values, appId })
        form.setFieldsValue(newTheme)
        message.success('主题创建成功')
      }
      await loadThemes()
    } catch (error: any) {
      message.error(error.message || '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const handleNewTheme = () => {
    form.resetFields()
    form.setFieldsValue({
      appId,
      themeName: '新主题',
      themeMode: 'light',
      primaryColor: '#1677ff',
      successColor: '#52c41a',
      warningColor: '#faad14',
      errorColor: '#ff4d4f',
      infoColor: '#1677ff',
      borderRadius: '6px',
      fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
      fontSize: '14px',
      layoutMode: 'side',
      sidebarTheme: 'dark',
      headerTheme: 'light',
      customCss: '',
      isDefault: 0,
      status: 1,
    })
  }

  const handleDuplicate = async () => {
    const values = form.getFieldsValue()
    if (!values.id) {
      message.warning('请先保存当前主题')
      return
    }
    try {
      const newTheme = await themeApi.duplicate(values.id, values.themeName + ' (副本)')
      form.setFieldsValue(newTheme)
      await loadThemes()
      message.success('主题复制成功')
    } catch (error) {
      message.error('复制失败')
    }
  }

  const handleDelete = async () => {
    const values = form.getFieldsValue()
    if (!values.id) return
    try {
      await themeApi.delete(values.id)
      message.success('删除成功')
      await loadThemes()
      if (themes.length > 1) {
        const remaining = themes.find(t => t.id !== values.id)
        if (remaining) {
          form.setFieldsValue(remaining)
          applyTheme(remaining)
        }
      } else {
        handleNewTheme()
      }
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleSetDefault = async () => {
    const values = form.getFieldsValue()
    if (!values.id) return
    try {
      await themeApi.setDefault(appId, values.id)
      message.success('已设为默认主题')
      await loadThemes()
    } catch (error) {
      message.error('设置失败')
    }
  }

  const handleSelectTheme = (theme: AppTheme) => {
    form.setFieldsValue(theme)
    applyTheme(theme)
  }

  const applyPresetTheme = (preset: typeof presetThemes[0]) => {
    form.setFieldsValue({
      ...form.getFieldsValue(),
      themeMode: preset.mode,
      primaryColor: preset.primary,
      successColor: preset.success,
      warningColor: preset.warning,
      errorColor: preset.error,
      infoColor: preset.primary,
    })
    applyTheme({
      ...form.getFieldsValue(),
      themeMode: preset.mode as 'light' | 'dark',
      primaryColor: preset.primary,
      successColor: preset.success,
      warningColor: preset.warning,
      errorColor: preset.error,
      infoColor: preset.primary,
    })
  }

  const customCssUploadProps: UploadProps = {
    accept: '.css',
    showUploadList: false,
    beforeUpload: (file) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        const cssContent = e.target?.result as string
        form.setFieldsValue({ customCss: cssContent })
        applyTheme({
          ...form.getFieldsValue(),
          customCss: cssContent,
        })
      }
      reader.readAsText(file)
      return false
    },
  }

  const handleReset = () => {
    if (currentTheme?.id) {
      form.setFieldsValue(currentTheme)
      applyTheme(currentTheme)
    }
  }

  return (
    <Drawer
      title="主题配置"
      placement="right"
      width={420}
      open={visible}
      onClose={onClose}
      extra={
        <Space>
          <Button onClick={handleReset} icon={<ReloadOutlined />}>
            重置
          </Button>
          <Button type="primary" onClick={handleSave} loading={saving} icon={<SaveOutlined />}>
            保存
          </Button>
        </Space>
      }
    >
      <div style={{ marginBottom: 16 }}>
        <Space wrap>
          <Button type="dashed" icon={<PlusOutlined />} onClick={handleNewTheme}>
            新建主题
          </Button>
          <Button icon={<CopyOutlined />} onClick={handleDuplicate}>
            复制
          </Button>
          <Popconfirm title="确定删除该主题吗？" onConfirm={handleDelete}>
            <Button danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
          <Button icon={<CheckOutlined />} onClick={handleSetDefault}>
            设为默认
          </Button>
        </Space>
      </div>

      {themes.length > 0 && (
        <div style={{ marginBottom: 16 }}>
          <Text type="secondary" style={{ marginBottom: 8, display: 'block' }}>
            主题列表
          </Text>
          <Space wrap size={[8, 8]}>
            {themes.map(theme => (
              <Tag
                key={theme.id}
                color={theme.id === form.getFieldValue('id') ? 'blue' : 'default'}
                style={{ cursor: 'pointer', padding: '4px 12px' }}
                onClick={() => handleSelectTheme(theme)}
              >
                <span
                  style={{
                    display: 'inline-block',
                    width: 12,
                    height: 12,
                    borderRadius: '50%',
                    backgroundColor: theme.primaryColor,
                    marginRight: 6,
                    verticalAlign: 'middle',
                  }}
                />
                {theme.themeName}
                {theme.isDefault === 1 && ' (默认)'}
              </Tag>
            ))}
          </Space>
        </div>
      )}

      <Divider />

      <Form
        form={form}
        layout="vertical"
        onValuesChange={handleValuesChange}
        initialValues={{
          themeMode: 'light',
          primaryColor: '#1677ff',
          successColor: '#52c41a',
          warningColor: '#faad14',
          errorColor: '#ff4d4f',
          infoColor: '#1677ff',
          borderRadius: '6px',
          fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
          fontSize: '14px',
          layoutMode: 'side',
          sidebarTheme: 'dark',
          headerTheme: 'light',
          customCss: '',
        }}
      >
        <Form.Item name="id" hidden>
          <Input />
        </Form.Item>

        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="基础配置" key="basic">
            <Form.Item name="themeName" label="主题名称" rules={[{ required: true, message: '请输入主题名称' }]}>
              <Input placeholder="请输入主题名称" />
            </Form.Item>

            <Form.Item name="themeMode" label="主题模式">
              <Radio.Group optionType="button" buttonStyle="solid">
                <Radio.Button value="light">浅色模式</Radio.Button>
                <Radio.Button value="dark">深色模式</Radio.Button>
              </Radio.Group>
            </Form.Item>

            <Divider orientation="left">预设主题</Divider>
            <Row gutter={[8, 8]}>
              {presetThemes.map((preset, index) => (
                <Col span={8} key={index}>
                  <Card
                    hoverable
                    size="small"
                    onClick={() => applyPresetTheme(preset)}
                    style={{
                      backgroundColor: preset.mode === 'dark' ? '#1f1f1f' : '#fff',
                      borderColor: preset.primary,
                      cursor: 'pointer',
                    }}
                    bodyStyle={{ padding: 12, textAlign: 'center' }}
                  >
                    <div
                      style={{
                        width: '100%',
                        height: 32,
                        backgroundColor: preset.primary,
                        borderRadius: 4,
                        marginBottom: 8,
                      }}
                    />
                    <Text style={{ color: preset.mode === 'dark' ? '#fff' : '#333' }}>
                      {preset.name}
                    </Text>
                  </Card>
                </Col>
              ))}
            </Row>
          </TabPane>

          <TabPane tab="色彩配置" key="color">
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="primaryColor" label="主色">
                  <ColorPicker showText />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="successColor" label="成功色">
                  <ColorPicker showText />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="warningColor" label="警告色">
                  <ColorPicker showText />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="errorColor" label="错误色">
                  <ColorPicker showText />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="infoColor" label="信息色">
                  <ColorPicker showText />
                </Form.Item>
              </Col>
            </Row>
          </TabPane>

          <TabPane tab="样式配置" key="style">
            <Form.Item name="borderRadius" label="圆角大小">
              <Slider
                min={0}
                max={24}
                step={1}
                marks={{
                  0: '0px',
                  6: '6px',
                  12: '12px',
                  18: '18px',
                  24: '24px',
                }}
                tooltip={{ formatter: (value) => `${value}px` }}
              />
            </Form.Item>

            <Form.Item name="fontFamily" label="字体">
              <Select>
                {fontFamilies.map(font => (
                  <Option key={font.value} value={font.value}>
                    {font.label}
                  </Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item name="fontSize" label="字号">
              <Radio.Group optionType="button">
                <Radio.Button value="12px">小</Radio.Button>
                <Radio.Button value="14px">中</Radio.Button>
                <Radio.Button value="16px">大</Radio.Button>
                <Radio.Button value="18px">特大</Radio.Button>
              </Radio.Group>
            </Form.Item>

            <Divider />

            <Form.Item name="layoutMode" label="布局模式">
              <Radio.Group optionType="button" buttonStyle="solid">
                {layoutModes.map(mode => (
                  <Radio.Button key={mode.value} value={mode.value}>
                    {mode.label}
                  </Radio.Button>
                ))}
              </Radio.Group>
            </Form.Item>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="sidebarTheme" label="侧边栏主题">
                  <Radio.Group optionType="button">
                    <Radio.Button value="light">浅色</Radio.Button>
                    <Radio.Button value="dark">深色</Radio.Button>
                  </Radio.Group>
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="headerTheme" label="顶栏主题">
                  <Radio.Group optionType="button">
                    <Radio.Button value="light">浅色</Radio.Button>
                    <Radio.Button value="dark">深色</Radio.Button>
                  </Radio.Group>
                </Form.Item>
              </Col>
            </Row>
          </TabPane>

          <TabPane tab="自定义CSS" key="custom">
            <Form.Item label="上传CSS文件">
              <Upload {...customCssUploadProps}>
                <Button icon={<UploadOutlined />}>上传CSS文件</Button>
              </Upload>
            </Form.Item>

            <Form.Item name="customCss" label="自定义CSS">
              <TextArea
                rows={12}
                placeholder="输入自定义CSS，例如：
.my-class {
  color: red;
}
"
                style={{ fontFamily: 'monospace' }}
              />
            </Form.Item>

            <Text type="secondary">
              自定义CSS将在主题加载时应用，可用于覆盖默认样式实现品牌定制。
            </Text>
          </TabPane>
        </Tabs>
      </Form>
    </Drawer>
  )
}

export default ThemeConfigPanel
