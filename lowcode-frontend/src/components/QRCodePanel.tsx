import React, { useState, useMemo } from 'react'
import { Card, Button, Input, Space, message, Typography, Spin } from 'antd'
import {
  QrcodeOutlined,
  DownloadOutlined,
  CopyOutlined,
  CheckOutlined,
} from '@ant-design/icons'

const { Text } = Typography

interface QRCodePanelProps {
  url: string
  title?: string
  qrCodeBase64?: string
  previewToken?: string
}

const QRCodePanel: React.FC<QRCodePanelProps> = ({
  url,
  title = '扫码预览',
  qrCodeBase64,
  previewToken,
}) => {
  const [copied, setCopied] = useState(false)
  const [imageError, setImageError] = useState(false)

  const qrCodeImageSrc = useMemo(() => {
    if (qrCodeBase64 && !imageError) {
      return qrCodeBase64.startsWith('data:') ? qrCodeBase64 : `data:image/png;base64,${qrCodeBase64}`
    }
    if (previewToken && !imageError) {
      return `/api/uniapp/preview/${previewToken}/qrcode?size=300`
    }
    if (url) {
      return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(url)}`
    }
    return ''
  }, [qrCodeBase64, previewToken, url, imageError])

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

  const handleDownload = async () => {
    try {
      let href = qrCodeImageSrc
      let downloadName = 'qrcode.png'

      if (qrCodeBase64) {
        href = qrCodeBase64.startsWith('data:') ? qrCodeBase64 : `data:image/png;base64,${qrCodeBase64}`
      } else if (previewToken) {
        const apiUrl = `/api/uniapp/preview/${previewToken}/qrcode?size=300`
        const response = await fetch(apiUrl)
        const blob = await response.blob()
        href = URL.createObjectURL(blob)
      }

      const link = document.createElement('a')
      link.href = href
      link.download = downloadName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      if (previewToken && !qrCodeBase64) {
        setTimeout(() => URL.revokeObjectURL(href), 1000)
      }
    } catch (e) {
      console.error('下载二维码失败:', e)
      message.error('下载二维码失败')
    }
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
          {qrCodeImageSrc ? (
            <img
              src={qrCodeImageSrc}
              alt="QR Code"
              style={{ width: '100%', height: '100%', objectFit: 'contain' }}
              onError={() => setImageError(true)}
            />
          ) : (
            <Spin />
          )}
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
            disabled={!qrCodeImageSrc}
          >
            下载二维码
          </Button>
        </Space>
      </div>
    </Card>
  )
}

export default QRCodePanel
