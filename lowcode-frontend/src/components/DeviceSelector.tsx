import React from 'react'
import { List, Tag, Select, Space } from 'antd'
import {
  MobileOutlined,
  TabletOutlined,
  AppleOutlined,
  AndroidOutlined,
  StarOutlined,
} from '@ant-design/icons'
import { DeviceConfig, Platform } from '@/hooks/useMobileSimulator'

interface DeviceSelectorProps {
  deviceList: DeviceConfig[]
  selectedDevice: DeviceConfig
  onSelect: (device: DeviceConfig) => void
  mode?: 'list' | 'dropdown'
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

const platformColors: Record<Platform, string> = {
  ios: '#000000',
  android: '#3DDC84',
  harmony: '#FF0000',
}

const getDeviceIcon = (device: DeviceConfig) => {
  const isTablet = device.width >= 700 || device.height >= 700
  return isTablet ? <TabletOutlined /> : <MobileOutlined />
}

const DeviceSelector: React.FC<DeviceSelectorProps> = ({
  deviceList,
  selectedDevice,
  onSelect,
  mode = 'list',
}) => {
  if (mode === 'dropdown') {
    return (
      <Select
        value={selectedDevice.id}
        onChange={(value) => {
          const device = deviceList.find(d => d.id === value)
          if (device) onSelect(device)
        }}
        style={{ width: '100%' }}
        options={deviceList.map(device => ({
          value: device.id,
          label: (
            <Space>
              <span style={{ color: platformColors[device.platform] }}>
                {platformIcons[device.platform]}
              </span>
              <span>{device.name}</span>
              <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                {device.width}×{device.height}
              </span>
            </Space>
          ),
        }))}
      />
    )
  }

  return (
    <div>
      <List
        dataSource={deviceList}
        renderItem={(device) => (
          <List.Item
            key={device.id}
            onClick={() => onSelect(device)}
            style={{
              cursor: 'pointer',
              borderRadius: 8,
              marginBottom: 8,
              padding: 12,
              background: selectedDevice.id === device.id ? '#e6f4ff' : 'transparent',
              border: selectedDevice.id === device.id ? '1px solid #1677ff' : '1px solid transparent',
              transition: 'all 0.2s',
            }}
          >
            <List.Item.Meta
              avatar={
                <div
                  style={{
                    width: 40,
                    height: 40,
                    borderRadius: 8,
                    background: selectedDevice.id === device.id ? '#1677ff' : '#f0f0f0',
                    color: selectedDevice.id === device.id ? '#fff' : '#666',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 20,
                  }}
                >
                  {getDeviceIcon(device)}
                </div>
              }
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontWeight: 500 }}>{device.name}</span>
                  <Tag
                    icon={platformIcons[device.platform]}
                    style={{
                      margin: 0,
                      fontSize: 11,
                      padding: '0 6px',
                      height: 20,
                      lineHeight: '18px',
                      borderColor: platformColors[device.platform],
                      color: platformColors[device.platform],
                    }}
                  >
                    {platformNames[device.platform]}
                  </Tag>
                </div>
              }
              description={
                <Space size={12} style={{ fontSize: 12, color: '#8c8c8c' }}>
                  <span>分辨率: {device.width}×{device.height}</span>
                  <span>像素比: {device.pixelRatio}x</span>
                  {device.hasNotch && <span>刘海屏</span>}
                </Space>
              }
            />
          </List.Item>
        )}
      />
    </div>
  )
}

export default DeviceSelector
