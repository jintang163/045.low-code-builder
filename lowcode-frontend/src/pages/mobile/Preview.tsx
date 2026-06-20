import React, { useState } from 'react'
import { Layout, Space, Button, Card, Switch, Form, Select, Tag, message, Divider, Typography } from 'antd'
import {
  CodeOutlined,
  DownloadOutlined,
  QrcodeOutlined,
  SettingOutlined,
  MobileOutlined,
  TouchOutlined,
  HandOutlined,
  AppleOutlined,
  AndroidOutlined,
  StarOutlined,
} from '@ant-design/icons'
import { useMobileSimulator, Platform, DeviceConfig } from '@/hooks/useMobileSimulator'
import MobileSimulator from '@/components/MobileSimulator'
import DeviceSelector from '@/components/DeviceSelector'
import QRCodePanel from '@/components/QRCodePanel'
import { TouchPoint, GestureEvent } from '@/components/TouchEventHandler'

const { Sider, Content, Header } = Layout
const { Title, Text } = Typography

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

const MobilePreview: React.FC = () => {
  const {
    state,
    deviceList,
    viewportSize,
    setDevice,
    setRotation,
    setScale,
    setUrl,
    setTouchEventsEnabled,
    setGesturesEnabled,
    refresh,
    goBack,
  } = useMobileSimulator()

  const [showQRCode, setShowQRCode] = useState(false)
  const [touchLog, setTouchLog] = useState<string[]>([])

  const handleTouchStart = (touches: TouchPoint[]) => {
    const log = `[touchstart] ${touches.map(t => `(${t.x.toFixed(0)}, ${t.y.toFixed(0)})`).join(', ')}`
    setTouchLog(prev => [log, ...prev].slice(0, 10))
  }

  const handleTouchMove = (touches: TouchPoint[]) => {
    const log = `[touchmove] ${touches.map(t => `(${t.x.toFixed(0)}, ${t.y.toFixed(0)})`).join(', ')}`
    setTouchLog(prev => [log, ...prev].slice(0, 10))
  }

  const handleTouchEnd = (touches: TouchPoint[]) => {
    const log = `[touchend] ${touches.map(t => `(${t.x.toFixed(0)}, ${t.y.toFixed(0)})`).join(', ')}`
    setTouchLog(prev => [log, ...prev].slice(0, 10))
  }

  const handleGesture = (gesture: GestureEvent) => {
    const parts = [`[${gesture.type}]`]
    if (gesture.direction) parts.push(`方向: ${gesture.direction}`)
    if (gesture.scale) parts.push(`缩放: ${gesture.scale.toFixed(2)}x`)
    if (gesture.velocity) parts.push(`速度: ${gesture.velocity.toFixed(2)}`)
    setTouchLog(prev => [parts.join(' '), ...prev].slice(0, 10))
  }

  const handleGenerateCode = () => {
    const code = `// 移动端预览配置
const deviceConfig = {
  device: '${state.device.name}',
  platform: '${state.device.platform}',
  width: ${viewportSize.width},
  height: ${viewportSize.height},
  pixelRatio: ${state.device.pixelRatio},
  scale: ${state.scale},
  rotation: '${state.rotation}',
  url: '${state.url}',
  touchEvents: ${state.touchEventsEnabled},
  gestures: ${state.gesturesEnabled},
};`
    navigator.clipboard.writeText(code)
    message.success('代码已复制到剪贴板')
  }

  const handleDownload = () => {
    const config = {
      device: state.device.id,
      platform: state.device.platform,
      width: viewportSize.width,
      height: viewportSize.height,
      scale: state.scale,
      rotation: state.rotation,
      url: state.url,
      timestamp: Date.now(),
    }
    const blob = new Blob([JSON.stringify(config, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'mobile-preview-config.json'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    message.success('配置已下载')
  }

  const filterDevicesByPlatform = (platform: Platform | 'all') => {
    if (platform === 'all') return deviceList
    return deviceList.filter(d => d.platform === platform)
  }

  const [platformFilter, setPlatformFilter] = useState<Platform | 'all'>('all')
  const filteredDevices = filterDevicesByPlatform(platformFilter)

  return (
    <Layout style={{ height: '100vh', background: '#f5f5f5' }}>
      <Header
        style={{
          background: '#fff',
          padding: '0 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #f0f0f0',
        }}
      >
        <Space>
          <MobileOutlined style={{ fontSize: 20, color: '#1677ff' }} />
          <Title level={4} style={{ margin: 0 }}>
            移动端模拟器
          </Title>
          <Tag color="blue">{viewportSize.width} × {viewportSize.height}</Tag>
          <Tag icon={platformIcons[state.device.platform]}>
            {platformNames[state.device.platform]}
          </Tag>
        </Space>

        <Space>
          <Button
            icon={<QrcodeOutlined />}
            onClick={() => setShowQRCode(!showQRCode)}
            type={showQRCode ? 'primary' : 'default'}
          >
            扫码预览
          </Button>
          <Button icon={<DownloadOutlined />} onClick={handleDownload}>
            下载配置
          </Button>
          <Button type="primary" icon={<CodeOutlined />} onClick={handleGenerateCode}>
            生成代码
          </Button>
        </Space>
      </Header>

      <Layout>
        <Sider
          width={280}
          style={{
            background: '#fff',
            borderRight: '1px solid #f0f0f0',
            overflowY: 'auto',
          }}
        >
          <div style={{ padding: 16 }}>
            <Space style={{ marginBottom: 16, width: '100%' }}>
              <Text strong style={{ fontSize: 14 }}>设备列表</Text>
            </Space>

            <Space style={{ marginBottom: 16, width: '100%' }} wrap>
              <Button
                size="small"
                type={platformFilter === 'all' ? 'primary' : 'default'}
                onClick={() => setPlatformFilter('all')}
              >
                全部
              </Button>
              {(Object.keys(platformNames) as Platform[]).map(platform => (
                <Button
                  key={platform}
                  size="small"
                  type={platformFilter === platform ? 'primary' : 'default'}
                  icon={platformIcons[platform]}
                  onClick={() => setPlatformFilter(platform)}
                >
                  {platformNames[platform]}
                </Button>
              ))}
            </Space>

            <DeviceSelector
              deviceList={filteredDevices}
              selectedDevice={state.device}
              onSelect={setDevice}
            />
          </div>
        </Sider>

        <Content
          style={{
            overflow: 'auto',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'flex-start',
            background: '#f5f5f5',
          }}
        >
          {showQRCode && (
            <div style={{ margin: 24, width: 280 }}>
              <QRCodePanel url={state.url} />
            </div>
          )}

          <MobileSimulator
            device={state.device}
            rotation={state.rotation}
            scale={state.scale}
            url={state.url}
            touchEventsEnabled={state.touchEventsEnabled}
            gesturesEnabled={state.gesturesEnabled}
            deviceList={deviceList}
            onDeviceChange={setDevice}
            onRotationChange={setRotation}
            onScaleChange={setScale}
            onUrlChange={setUrl}
            onRefresh={refresh}
            onBack={goBack}
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
            onGesture={handleGesture}
          />
        </Content>

        <Sider
          width={280}
          style={{
            background: '#fff',
            borderLeft: '1px solid #f0f0f0',
            overflowY: 'auto',
          }}
        >
          <div style={{ padding: 16 }}>
            <Space style={{ marginBottom: 16, width: '100%' }}>
              <SettingOutlined />
              <Text strong style={{ fontSize: 14 }}>属性配置</Text>
            </Space>

            <Card size="small" style={{ marginBottom: 16 }}>
              <Form layout="vertical">
                <Form.Item label="平台配置">
                  <Select
                    value={state.device.platform}
                    onChange={(value: Platform) => {
                      const samePlatformDevices = deviceList.filter(d => d.platform === value)
                      if (samePlatformDevices.length > 0) {
                        setDevice(samePlatformDevices[0])
                      }
                    }}
                    options={(Object.keys(platformNames) as Platform[]).map(platform => ({
                      value: platform,
                      label: (
                        <Space>
                          {platformIcons[platform]}
                          <span>{platformNames[platform]}</span>
                        </Space>
                      ),
                    }))}
                  />
                </Form.Item>

                <Form.Item label="当前设备">
                  <Select
                    value={state.device.id}
                    onChange={(value) => {
                      const device = deviceList.find(d => d.id === value)
                      if (device) setDevice(device)
                    }}
                    options={deviceList.map(device => ({
                      value: device.id,
                      label: `${device.name} (${device.width}×${device.height})`,
                    }))}
                  />
                </Form.Item>
              </Form>
            </Card>

            <Card size="small" style={{ marginBottom: 16 }}>
              <Space style={{ marginBottom: 12 }}>
                <TouchOutlined />
                <Text strong>触屏事件</Text>
              </Space>
              <Form layout="vertical">
                <Form.Item
                  label={
                    <Space>
                      <span>启用触屏模拟</span>
                      <Switch
                        checked={state.touchEventsEnabled}
                        onChange={setTouchEventsEnabled}
                        size="small"
                      />
                    </Space>
                  }
                >
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    使用鼠标模拟 touch 事件
                  </Text>
                </Form.Item>

                <Form.Item
                  label={
                    <Space>
                      <HandOutlined />
                      <span>启用手势识别</span>
                      <Switch
                        checked={state.gesturesEnabled}
                        onChange={setGesturesEnabled}
                        size="small"
                        disabled={!state.touchEventsEnabled}
                      />
                    </Space>
                  }
                >
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    识别滑动、点击、长按等手势
                  </Text>
                </Form.Item>
              </Form>
            </Card>

            <Card size="small" style={{ marginBottom: 16 }}>
              <Space style={{ marginBottom: 12 }}>
                <MobileOutlined />
                <Text strong>设备信息</Text>
              </Space>
              <div style={{ fontSize: 12, color: '#595959' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>分辨率</span>
                  <span style={{ color: '#000' }}>{state.device.width} × {state.device.height}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>像素比</span>
                  <span style={{ color: '#000' }}>{state.device.pixelRatio}x</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>视口尺寸</span>
                  <span style={{ color: '#000' }}>{viewportSize.width} × {viewportSize.height}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>缩放比例</span>
                  <span style={{ color: '#000' }}>{Math.round(state.scale * 100)}%</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>刘海屏</span>
                  <span style={{ color: state.device.hasNotch ? '#52c41a' : '#8c8c8c' }}>
                    {state.device.hasNotch ? '是' : '否'}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>安全区域</span>
                  <span style={{ color: state.device.hasSafeArea ? '#52c41a' : '#8c8c8c' }}>
                    {state.device.hasSafeArea ? '是' : '否'}
                  </span>
                </div>
              </div>
            </Card>

            {state.touchEventsEnabled && touchLog.length > 0 && (
              <Card size="small">
                <Space style={{ marginBottom: 12 }}>
                  <TouchOutlined />
                  <Text strong>事件日志</Text>
                </Space>
                <div
                  style={{
                    maxHeight: 200,
                    overflowY: 'auto',
                    fontFamily: 'monospace',
                    fontSize: 11,
                    color: '#52c41a',
                    background: '#000',
                    padding: 8,
                    borderRadius: 4,
                  }}
                >
                  {touchLog.map((log, idx) => (
                    <div key={idx} style={{ marginBottom: 2 }}>
                      {log}
                    </div>
                  ))}
                </div>
              </Card>
            )}
          </div>
        </Sider>
      </Layout>
    </Layout>
  )
}

export default MobilePreview
