import React, { useState } from 'react'
import { Space, Button, Input, Slider, Tooltip, Dropdown, MenuProps } from 'antd'
import {
  ReloadOutlined,
  ArrowLeftOutlined,
  RotateLeftOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  GlobalOutlined,
  AppleOutlined,
  AndroidOutlined,
  StarOutlined,
  MobileOutlined,
  CaretDownOutlined,
} from '@ant-design/icons'
import { DeviceConfig, Platform } from '@/hooks/useMobileSimulator'
import DeviceSelector from './DeviceSelector'
import TouchEventHandler, { GestureEvent, TouchPoint } from './TouchEventHandler'

interface MobileSimulatorProps {
  device: DeviceConfig
  rotation: 'portrait' | 'landscape'
  scale: number
  url: string
  touchEventsEnabled: boolean
  gesturesEnabled: boolean
  deviceList: DeviceConfig[]
  onDeviceChange: (device: DeviceConfig) => void
  onRotationChange: (rotation: 'portrait' | 'landscape') => void
  onScaleChange: (scale: number) => void
  onUrlChange: (url: string) => void
  onRefresh: () => void
  onBack: () => void
  onTouchStart?: (touches: TouchPoint[]) => void
  onTouchMove?: (touches: TouchPoint[]) => void
  onTouchEnd?: (touches: TouchPoint[]) => void
  onGesture?: (gesture: GestureEvent) => void
}

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

const MobileSimulator: React.FC<MobileSimulatorProps> = ({
  device,
  rotation,
  scale,
  url,
  touchEventsEnabled,
  gesturesEnabled,
  deviceList,
  onDeviceChange,
  onRotationChange,
  onScaleChange,
  onUrlChange,
  onRefresh,
  onBack,
  onTouchStart,
  onTouchMove,
  onTouchEnd,
  onGesture,
}) => {
  const [inputUrl, setInputUrl] = useState(url)
  const [showDeviceSelector, setShowDeviceSelector] = useState(false)

  const viewportWidth = rotation === 'portrait' ? device.width : device.height
  const viewportHeight = rotation === 'portrait' ? device.height : device.width

  const frameWidth = viewportWidth * scale + 60
  const frameHeight = viewportHeight * scale + 100

  const platformMenuItems: MenuProps['items'] = (['ios', 'android', 'harmony'] as Platform[]).map(platform => ({
    key: platform,
    icon: platformIcons[platform],
    label: platformNames[platform],
    onClick: () => {
      const samePlatformDevices = deviceList.filter(d => d.platform === platform)
      if (samePlatformDevices.length > 0) {
        onDeviceChange(samePlatformDevices[0])
      }
    },
  }))

  const handleUrlSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onUrlChange(inputUrl)
  }

  const handleZoomIn = () => {
    onScaleChange(Math.min(2, scale + 0.1))
  }

  const handleZoomOut = () => {
    onScaleChange(Math.max(0.5, scale - 0.1))
  }

  const handleToggleRotation = () => {
    onRotationChange(rotation === 'portrait' ? 'landscape' : 'portrait')
  }

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 16,
        padding: 24,
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: frameWidth + 40,
          display: 'flex',
          flexDirection: 'column',
          gap: 12,
        }}
      >
        <Space.Compact style={{ width: '100%' }}>
          <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
            后退
          </Button>
          <form onSubmit={handleUrlSubmit} style={{ flex: 1, display: 'flex' }}>
            <Input
              value={inputUrl}
              onChange={(e) => setInputUrl(e.target.value)}
              prefix={<GlobalOutlined style={{ color: '#8c8c8c' }} />}
              placeholder="输入预览URL"
              style={{ flex: 1, borderRight: 'none', borderLeft: 'none', borderRadius: 0 }}
            />
          </form>
          <Button icon={<ReloadOutlined />} onClick={onRefresh}>
            刷新
          </Button>
        </Space.Compact>

        <Space style={{ flexWrap: 'wrap' }}>
          <Dropdown
            open={showDeviceSelector}
            onOpenChange={setShowDeviceSelector}
            dropdownRender={() => (
              <div
                style={{
                  background: '#fff',
                  borderRadius: 8,
                  boxShadow: '0 6px 16px rgba(0,0,0,0.12)',
                  padding: 8,
                  width: 300,
                  maxHeight: 400,
                  overflowY: 'auto',
                }}
              >
                <DeviceSelector
                  deviceList={deviceList}
                  selectedDevice={device}
                  onSelect={(d) => {
                    onDeviceChange(d)
                    setShowDeviceSelector(false)
                  }}
                />
              </div>
            )}
          >
            <Button>
              <Space>
                <MobileOutlined />
                <span>{device.name}</span>
                <CaretDownOutlined />
              </Space>
            </Button>
          </Dropdown>

          <Dropdown menu={{ items: platformMenuItems }}>
            <Button>
              <Space>
                {platformIcons[device.platform]}
                <span>{platformNames[device.platform]}</span>
                <CaretDownOutlined />
              </Space>
            </Button>
          </Dropdown>

          <Tooltip title="旋转">
            <Button icon={<RotateLeftOutlined />} onClick={handleToggleRotation}>
              {rotation === 'portrait' ? '竖屏' : '横屏'}
            </Button>
          </Tooltip>

          <Space size={8} align="center" style={{ marginLeft: 'auto' }}>
            <Tooltip title="缩小">
              <Button
                icon={<ZoomOutOutlined />}
                onClick={handleZoomOut}
                disabled={scale <= 0.5}
              />
            </Tooltip>
            <div style={{ width: 120 }}>
              <Slider
                min={0.5}
                max={2}
                step={0.1}
                value={scale}
                onChange={onScaleChange}
                tooltip={{
                  formatter: (value) => `${Math.round(value * 100)}%`,
                }}
              />
            </div>
            <Tooltip title="放大">
              <Button
                icon={<ZoomInOutlined />}
                onClick={handleZoomIn}
                disabled={scale >= 2}
              />
            </Tooltip>
            <span style={{ color: '#8c8c8c', fontSize: 12, minWidth: 45 }}>
              {Math.round(scale * 100)}%
            </span>
          </Space>
        </Space>
      </div>

      <div
        style={{
          position: 'relative',
          width: frameWidth,
          height: frameHeight,
          background: '#1a1a1a',
          borderRadius: device.borderRadius * scale + 30,
          padding: '50px 30px',
          boxShadow: '0 25px 50px rgba(0,0,0,0.25)',
          transition: 'all 0.3s ease',
        }}
      >
        <div
          style={{
            position: 'absolute',
            top: 15,
            left: '50%',
            transform: 'translateX(-50%)',
            width: device.hasNotch ? 120 * scale : 80 * scale,
            height: device.hasNotch ? 30 * scale : 6 * scale,
            background: '#000',
            borderRadius: device.hasNotch ? 15 * scale : 3 * scale,
            zIndex: 10,
          }}
        />

        <div
          style={{
            position: 'absolute',
            right: 5,
            top: '50%',
            transform: 'translateY(-50%)',
            width: 3,
            height: 60,
            background: '#333',
            borderRadius: 2,
          }}
        />

        <div
          style={{
            position: 'absolute',
            left: 5,
            top: '35%',
            transform: 'translateY(-50%)',
            width: 3,
            height: 40,
            background: '#333',
            borderRadius: 2,
          }}
        />

        <div
          style={{
            position: 'absolute',
            left: 5,
            top: '50%',
            transform: 'translateY(-50%)',
            width: 3,
            height: 40,
            background: '#333',
            borderRadius: 2,
          }}
        />

        <div
          style={{
            position: 'relative',
            width: viewportWidth * scale,
            height: viewportHeight * scale,
            background: '#fff',
            borderRadius: device.borderRadius * scale,
            overflow: 'hidden',
          }}
        >
          {device.hasNotch && (
            <div
              style={{
                position: 'absolute',
                top: 0,
                left: '50%',
                transform: 'translateX(-50%)',
                width: 120 * scale,
                height: device.notchHeight * scale,
                background: '#000',
                borderRadius: `0 0 ${16 * scale}px ${16 * scale}px`,
                zIndex: 100,
                pointerEvents: 'none',
              }}
            />
          )}

          {!device.hasNotch && device.notchHeight > 0 && (
            <div
              style={{
                position: 'absolute',
                top: 4 * scale,
                left: '50%',
                transform: 'translateX(-50%)',
                width: 14 * scale,
                height: 14 * scale,
                background: '#000',
                borderRadius: '50%',
                zIndex: 100,
                pointerEvents: 'none',
              }}
            />
          )}

          <TouchEventHandler
            enabled={touchEventsEnabled}
            gesturesEnabled={gesturesEnabled}
            onTouchStart={onTouchStart}
            onTouchMove={onTouchMove}
            onTouchEnd={onTouchEnd}
            onGesture={onGesture}
            style={{
              width: '100%',
              height: '100%',
              position: 'relative',
            }}
          >
            <iframe
              id="mobile-simulator-iframe"
              src={url}
              style={{
                width: '100%',
                height: '100%',
                border: 'none',
                pointerEvents: touchEventsEnabled ? 'none' : 'auto',
              }}
              title="Mobile Preview"
            />
          </TouchEventHandler>

          {device.hasSafeArea && device.safeAreaBottom > 0 && (
            <div
              style={{
                position: 'absolute',
                bottom: 0,
                left: 0,
                right: 0,
                height: device.safeAreaBottom * scale,
                background: '#000',
                zIndex: 100,
                pointerEvents: 'none',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <div
                style={{
                  width: 120 * scale,
                  height: 5 * scale,
                  background: '#fff',
                  borderRadius: 3 * scale,
                  opacity: 0.3,
                }}
              />
            </div>
          )}
        </div>

        <div
          style={{
            position: 'absolute',
            bottom: 15,
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            alignItems: 'center',
            gap: 20,
            color: '#666',
            fontSize: 11,
          }}
        >
          <span>{viewportWidth}×{viewportHeight}</span>
          <span>·</span>
          <span>{device.pixelRatio}x</span>
        </div>
      </div>
    </div>
  )
}

export default MobileSimulator
