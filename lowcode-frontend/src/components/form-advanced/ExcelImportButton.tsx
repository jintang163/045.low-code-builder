import React, { useState } from 'react'
import { Button, Upload, message, Modal, Progress, Space } from 'antd'
import { ImportOutlined, FileExcelOutlined } from '@ant-design/icons'
import type { UploadProps } from 'antd/es/upload/interface'

export interface ExcelImportButtonProps {
  modelId?: number | string
  onSuccess?: (data: any) => void
  onError?: (error: any) => void
  type?: 'primary' | 'default' | 'dashed' | 'link' | 'text'
  size?: 'small' | 'middle' | 'large'
  disabled?: boolean
  buttonText?: string
  showTemplateDownload?: boolean
  templateUrl?: string
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
  templateUrl,
  style,
  className,
  sheetIndex = 0,
  startRow = 1,
}) => {
  const [progress, setProgress] = useState(0)
  const [importResult, setImportResult] = useState<any>(null)
  const [resultModalVisible, setResultModalVisible] = useState(false)

  const uploadProps: UploadProps = {
    name: 'file',
    accept: '.xlsx,.xls',
    showUploadList: false,
    disabled,
    customRequest: options => {
      const { file, onSuccess, onError } = options

      const reader = new FileReader()
      reader.onload = () => {
        let progressVal = 0
        const interval = setInterval(() => {
          progressVal += 20
          setProgress(progressVal)
          if (progressVal >= 100) {
            clearInterval(interval)
            setProgress(0)

            const mockResult = {
              successCount: 95,
              failCount: 5,
              totalCount: 100,
              errors: [
                '第3行: 字段【姓名】不能为空',
                '第15行: 字段【年龄】格式错误',
                '第28行: 字段【日期】格式错误',
                '第67行: 字段【邮箱】格式错误',
                '第89行: 字段【手机号】格式错误',
              ],
            }
            setImportResult(mockResult)
            setResultModalVisible(true)
            onSuccess?.(mockResult as any)
            message.success(`导入完成，成功 ${mockResult.successCount} 条，失败 ${mockResult.failCount} 条`)
          }
        }, 200)
      }

      reader.onerror = () => {
        onError?.(new Error('文件读取失败'))
        message.error('文件读取失败')
      }

      reader.readAsArrayBuffer(file as Blob)
    },
  }

  const handleDownloadTemplate = () => {
    if (templateUrl) {
      window.open(templateUrl, '_blank')
    } else {
      message.info('请配置模板下载地址')
    }
  }

  return (
    <>
      <Space style={style} className={className}>
        <Upload {...uploadProps}>
          <Button type={type} size={size} icon={<ImportOutlined />} disabled={disabled}>
            {buttonText}
          </Button>
        </Upload>

        {showTemplateDownload && (
          <Button
            size={size}
            icon={<FileExcelOutlined />}
            onClick={handleDownloadTemplate}
            disabled={disabled}
          >
            下载模板
          </Button>
        )}
      </Space>

      {progress > 0 && (
        <div style={{ marginTop: 8 }}>
          <Progress percent={progress} size="small" />
        </div>
      )}

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
                <span style={{ color: '#ff4d4f' }}>失败 {importResult.failCount} 条</span>
              </Space>
            </div>

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
                  {importResult.errors.map((error: string, index: number) => (
                    <div key={index} style={{ color: '#ff4d4f', fontSize: 12, lineHeight: 1.8 }}>
                      {error}
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
