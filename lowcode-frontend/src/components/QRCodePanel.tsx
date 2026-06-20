import React, { useState } from 'react'
import { Card, Button, Input, Space, message, Typography } from 'antd'
import {
  QrcodeOutlined,
  DownloadOutlined,
  CopyOutlined,
  CheckOutlined,
} from '@ant-design/icons'

const { Text, Paragraph } = Typography

interface QRCodePanelProps {
  url: string
  title?: string
}

const QRCodePanel: React.FC<QRCodePanelProps> = ({
  url,
  title = '扫码预览',
}) => {
  const [copied, setCopied] = useState(false)

  const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(url)}`

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(url)
      setCopied(true)
      message.success('链接已复制')
      setTimeout(() => setCopied(false), 2000)
    } catch (e) {
      message.error('复制失败')
    }
  }

  const handleDownload = () => {
    const link = document.createElement('a')
    link.href = qrCodeUrl
    link.download = 'qrcode.png'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <Card
      title={
        <Space>
          <QrcodeOutlined />
          <span>{title}</span>
        </Space>
      }
      style={{ width: '100%' }}
    >
      <div style={{ textAlign: 'center' }}>
        <div
          style={{
            width: 200,
            height: 200,
            margin: '0 auto 16px',
            padding: 8,
            background: '#fff',
            borderRadius: 8,
            border: '1px solid #f0f0f0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <img
            src={qrCodeUrl}
            alt="QR Code"
            style={{ width: '100%', height: '100%', objectFit: 'contain' }}
          />
        </div>

        <div style={{ marginBottom: 16 }}>
          <div style={{ color: '#8c8c8c', fontSize: 12, marginBottom: 4 }}>
            预览链接
          </div>
          <Input
            value={url}
            readOnly
            style={{ marginBottom: 8 }}
            suffix={
              <Button
                type="text"
                icon={copied ? <CheckOutlined /> : <CopyOutlined />}
                onClick={handleCopy}
                size="small"
              >
                {copied ? '已复制' : '复制'}
              </Button>
            }
          />
        </div>

        <Space>
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            onClick={handleDownload}
            block
          >
            下载二维码
          </Button>
        </Space>
      </div>
    </Card>
  )
}

export default QRCodePanel
