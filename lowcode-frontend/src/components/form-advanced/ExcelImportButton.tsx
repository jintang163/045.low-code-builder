import React, { useState } from 'react'
import { Button, Upload, message, Modal, Progress, Space, Alert } from 'antd'
import { ImportOutlined, FileExcelOutlined, DownloadOutlined } from '@ant-design/icons'
import type { UploadProps } from 'antd/es/upload/interface'
import { excelApi } from '@/api/dataModel'

export interface ExcelImportButtonProps {
  modelId?: number | string
  onSuccess?: (data: any) => void
  onError?: (error: any) => void
  type?: 'primary' | 'default' | 'dashed' | 'link' | 'text'
  size?: 'small' | 'middle' | 'large'
  disabled?: boolean
  buttonText?: string
  showTemplateDownload?: boolean
  style?: React.CSSProperties
  className?: string
  sheetIndex?: number
  startRow?: number
}

const ExcelImportButton: React.FC<ExcelImportButtonProps> = ({
  modelId,
  onSuccess,
  onError,
  type = 'default',
  size = 'middle',
  disabled = false,
  buttonText = 'Excel导入',
  showTemplateDownload = true,
  style,
  className,
  sheetIndex = 0,
  startRow = 1,
}) => {
  const [uploading, setUploading] = useState(false)
  const [importResult, setImportResult] = useState<any>(null)
  const [resultModalVisible, setResultModalVisible] = useState(false)

  const uploadProps: UploadProps = {
    name: 'file',
    accept: '.xlsx,.xls',
    showUploadList: false,
    disabled: disabled || uploading,
    customRequest: async options => {
      const { file, onSuccess: onUploadSuccess, onError: onUploadError } = options

      if (!modelId) {
        message.error('请配置关联模型ID')
        onUploadError(new Error('缺少 modelId'))
        return
      }

      setUploading(true)
      try {
        const res = await excelApi.importData(
          Number(modelId),
          file as File,
          sheetIndex,
          startRow
        )
        const result = (res as any)?.data || res
        setImportResult(result)
        setResultModalVisible(true)
        onUploadSuccess(result, new XMLHttpRequest())
        if (result.failCount > 0) {
          message.warning(`导入完成，成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
        } else {
          message.success(`导入成功，共 ${result.successCount} 条数据`)
        }
        onSuccess?.(result)
      } catch (error: any) {
        const errMsg = error?.response?.data?.message || error?.message || '导入失败'
        onUploadError(new Error(errMsg))
        onError?.(error)
        message.error(errMsg)
      } finally {
        setUploading(false)
      }
    },
  }

  const handleDownloadTemplate = async () => {
    if (!modelId) {
      message.info('请配置关联模型ID')
      return
    }
    try {
      const res = await excelApi.downloadTemplate(Number(modelId))
      const blob = (res as any)?.data || res
      const url = window.URL.createObjectURL(
        blob instanceof Blob ? blob : new Blob([blob as any])
      )
      const link = document.createElement('a')
      link.href = url
      link.download = `导入模板.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      message.success('模板下载成功')
    } catch (e) {
      message.error('模板下载失败')
    }
  }

  return (
    <>
      <Space style={style} className={className}>
        <Upload {...uploadProps}>
          <Button
            type={type}
            size={size}
            icon={<ImportOutlined />}
            disabled={disabled}
            loading={uploading}
          >
            {uploading ? '导入中...' : buttonText}
          </Button>
        </Upload>

        {showTemplateDownload && (
          <Button
            size={size}
            icon={<DownloadOutlined />}
            onClick={handleDownloadTemplate}
            disabled={disabled}
          >
            下载模板
          </Button>
        )}
      </Space>

      <Modal
        title="导入结果"
        open={resultModalVisible}
        onCancel={() => setResultModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setResultModalVisible(false)}>
            关闭
          </Button>,
        ]}
      >
        {importResult && (
          <div>
            <div style={{ marginBottom: 16 }}>
              <Space>
                <span>共 {importResult.totalCount} 条数据</span>
                <span style={{ color: '#52c41a' }}>成功 {importResult.successCount} 条</span>
                {importResult.failCount > 0 && (
                  <span style={{ color: '#ff4d4f' }}>失败 {importResult.failCount} 条</span>
                )}
              </Space>
            </div>

            {importResult.failCount > 0 && (
              <Alert
                type="warning"
                showIcon
                message={`有 ${importResult.failCount} 条数据导入失败`}
                style={{ marginBottom: 12 }}
              />
            )}

            {importResult.errors && importResult.errors.length > 0 && (
              <div>
                <div style={{ marginBottom: 8, fontWeight: 500 }}>错误详情：</div>
                <div
                  style={{
                    maxHeight: 200,
                    overflowY: 'auto',
                    background: '#fafafa',
                    padding: '8px 12px',
                    borderRadius: 4,
                  }}
                >
                  {importResult.errors.map((error: any, index: number) => (
                    <div key={index} style={{ color: '#ff4d4f', fontSize: 12, lineHeight: 1.8 }}>
                      {typeof error === 'string'
                        ? error
                        : `第${error.row}行: ${error.message}`}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </Modal>
    </>
  )
}

export default ExcelImportButton
