import React, { useState, useEffect } from 'react'
import { Input, Button, Modal, Space, Tag, InputNumber } from 'antd'
import { EnvironmentOutlined, SearchOutlined } from '@ant-design/icons'

export interface LocationValue {
  address?: string
  longitude?: number
  latitude?: number
  province?: string
  city?: string
  district?: string
}

export interface LocationPickerProps {
  value?: LocationValue | string
  onChange?: (value: LocationValue) => void
  placeholder?: string
  showCoordinate?: boolean
  mapType?: 'amap' | 'baidu'
  disabled?: boolean
  width?: number | string
  style?: React.CSSProperties
  className?: string
  amapKey?: string
  baiduKey?: string
}

const LocationPicker: React.FC<LocationPickerProps> = ({
  value,
  onChange,
  placeholder = '请选择位置',
  showCoordinate = true,
  mapType = 'amap',
  disabled = false,
  width = '100%',
  style,
  className,
  amapKey,
  baiduKey,
}) => {
  const [modalVisible, setModalVisible] = useState(false)
  const [location, setLocation] = useState<LocationValue>({})
  const [searchText, setSearchText] = useState('')
  const [searchResults, setSearchResults] = useState<LocationValue[]>([])
  const [isMapLoaded, setIsMapLoaded] = useState(false)

  useEffect(() => {
    if (value) {
      if (typeof value === 'string') {
        try {
          setLocation(JSON.parse(value))
        } catch {
          setLocation({ address: value })
        }
      } else {
        setLocation(value)
      }
    }
  }, [value])

  const handleSearch = () => {
    const mockResults: LocationValue[] = [
      {
        address: searchText + ' - 示例位置1',
        longitude: 116.397,
        latitude: 39.908,
        province: '北京市',
        city: '北京市',
        district: '东城区',
      },
      {
        address: searchText + ' - 示例位置2',
        longitude: 116.407,
        latitude: 39.918,
        province: '北京市',
        city: '北京市',
        district: '朝阳区',
      },
    ]
    setSearchResults(mockResults)
  }

  const handleSelect = (item: LocationValue) => {
    setLocation(item)
    onChange?.(item)
    setModalVisible(false)
  }

  const handleLngChange = (val: number | null) => {
    setLocation(prev => ({ ...prev, longitude: val || undefined }))
  }

  const handleLatChange = (val: number | null) => {
    setLocation(prev => ({ ...prev, latitude: val || undefined }))
  }

  const handleConfirm = () => {
    onChange?.(location)
    setModalVisible(false)
  }

  const displayText = location?.address || placeholder

  return (
    <div className={className} style={{ width, ...style }}>
      <Input
        readOnly
        value={location?.address || ''}
        placeholder={placeholder}
        prefix={<EnvironmentOutlined />}
        suffix={
          <Button
            type="link"
            size="small"
            disabled={disabled}
            onClick={() => setModalVisible(true)}
          >
            选择
          </Button>
        }
        disabled={disabled}
        onClick={() => !disabled && setModalVisible(true)}
        style={{ cursor: disabled ? 'not-allowed' : 'pointer' }}
      />
      {showCoordinate && location?.longitude && location?.latitude && (
        <div style={{ marginTop: 4, fontSize: 12, color: '#999' }}>
          经度: {location.longitude.toFixed(6)}，纬度: {location.latitude.toFixed(6)}
        </div>
      )}

      <Modal
        title="选择地理位置"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleConfirm}
        width={720}
      >
        <Space.Compact style={{ width: '100%', marginBottom: 16 }}>
          <Input
            placeholder="搜索地点"
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
            onPressEnter={handleSearch}
            allowClear
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
        </Space.Compact>

        <div style={{ marginBottom: 16 }}>
          <Tag color="blue">{mapType === 'amap' ? '高德地图' : '百度地图'}</Tag>
          {!amapKey && !baiduKey && (
            <Tag color="orange">演示模式（请配置地图Key）</Tag>
          )}
        </div>

        <div
          style={{
            height: 200,
            background: '#f5f5f5',
            border: '1px solid #e8e8e8',
            borderRadius: 4,
            marginBottom: 16,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#999',
          }}
        >
          <div style={{ textAlign: 'center' }}>
            <EnvironmentOutlined style={{ fontSize: 48, marginBottom: 8 }} />
            <div>地图区域（配置地图Key后显示真实地图）</div>
            {location?.longitude && location?.latitude && (
              <div style={{ marginTop: 8, fontSize: 12 }}>
                当前位置: {location.longitude.toFixed(6)}, {location.latitude.toFixed(6)}
              </div>
            )}
          </div>
        </div>

        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>手动输入坐标</div>
          <Space>
            <span>经度:</span>
            <InputNumber
              value={location?.longitude}
              onChange={handleLngChange}
              step={0.000001}
              precision={6}
              min={-180}
              max={180}
              style={{ width: 180 }}
            />
            <span>纬度:</span>
            <InputNumber
              value={location?.latitude}
              onChange={handleLatChange}
              step={0.000001}
              precision={6}
              min={-90}
              max={90}
              style={{ width: 180 }}
            />
          </Space>
        </div>

        {searchResults.length > 0 && (
          <div>
            <div style={{ marginBottom: 8, fontWeight: 500 }}>搜索结果</div>
            <div style={{ maxHeight: 200, overflowY: 'auto' }}>
              {searchResults.map((item, index) => (
                <div
                  key={index}
                  style={{
                    padding: '8px 12px',
                    cursor: 'pointer',
                    borderBottom: '1px solid #f0f0f0',
                    background: location?.address === item.address ? '#e6f7ff' : 'transparent',
                  }}
                  onClick={() => handleSelect(item)}
                  onMouseEnter={e => (e.currentTarget.style.background = '#f5f5f5')}
                  onMouseLeave={e => (e.currentTarget.style.background = location?.address === item.address ? '#e6f7ff' : 'transparent')}
                >
                  <div style={{ fontWeight: 500 }}>{item.address}</div>
                  {item.province && (
                    <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
                      {item.province} {item.city} {item.district}
                    </div>
                  )}
                  {item.longitude && item.latitude && (
                    <div style={{ fontSize: 12, color: '#999' }}>
                      {item.longitude.toFixed(6)}, {item.latitude.toFixed(6)}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default LocationPicker
