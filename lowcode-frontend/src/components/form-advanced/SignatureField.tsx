import React, { useRef, useEffect, useState, useCallback } from 'react'
import { Button, Space } from 'antd'
import { UndoOutlined, ClearOutlined, CheckOutlined } from '@ant-design/icons'

export interface SignatureFieldProps {
  value?: string
  onChange?: (value: string) => void
  width?: number
  height?: number
  penColor?: string
  backgroundColor?: string
  penWidth?: number
  disabled?: boolean
  placeholder?: string
  showToolbar?: boolean
  style?: React.CSSProperties
  className?: string
}

const SignatureField: React.FC<SignatureFieldProps> = ({
  value,
  onChange,
  width = 400,
  height = 200,
  penColor = '#000000',
  backgroundColor = '#ffffff',
  penWidth = 2,
  disabled = false,
  placeholder = '请在此处签名',
  showToolbar = true,
  style,
  className,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [isDrawing, setIsDrawing] = useState(false)
  const [hasContent, setHasContent] = useState(false)
  const [history, setHistory] = useState<string[]>([])

  const initCanvas = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    ctx.fillStyle = backgroundColor
    ctx.fillRect(0, 0, width, height)

    if (!hasContent) {
      ctx.font = '16px Arial'
      ctx.fillStyle = '#cccccc'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(placeholder, width / 2, height / 2)
    }
  }, [width, height, backgroundColor, placeholder, hasContent])

  useEffect(() => {
    initCanvas()
    if (value) {
      const img = new Image()
      img.onload = () => {
        const canvas = canvasRef.current
        const ctx = canvas?.getContext('2d')
        if (ctx) {
          ctx.drawImage(img, 0, 0)
          setHasContent(true)
        }
      }
      img.src = value
    }
  }, [value])

  useEffect(() => {
    initCanvas()
  }, [initCanvas])

  const getPosition = (e: React.MouseEvent | React.TouchEvent) => {
    const canvas = canvasRef.current
    if (!canvas) return { x: 0, y: 0 }

    const rect = canvas.getBoundingClientRect()
    const scaleX = canvas.width / rect.width
    const scaleY = canvas.height / rect.height

    if ('touches' in e) {
      return {
        x: (e.touches[0].clientX - rect.left) * scaleX,
        y: (e.touches[0].clientY - rect.top) * scaleY,
      }
    }

    return {
      x: (e.clientX - rect.left) * scaleX,
      y: (e.clientY - rect.top) * scaleY,
    }
  }

  const saveState = () => {
    const canvas = canvasRef.current
    if (canvas) {
      const dataUrl = canvas.toDataURL()
      setHistory(prev => [...prev, dataUrl])
    }
  }

  const startDrawing = (e: React.MouseEvent | React.TouchEvent) => {
    if (disabled) return

    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!ctx) return

    saveState()
    setIsDrawing(true)

    const pos = getPosition(e)
    ctx.beginPath()
    ctx.moveTo(pos.x, pos.y)
    ctx.strokeStyle = penColor
    ctx.lineWidth = penWidth
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'

    if (!hasContent) {
      ctx.fillStyle = backgroundColor
      ctx.fillRect(0, 0, width, height)
      ctx.strokeStyle = penColor
      setHasContent(true)
    }
  }

  const draw = (e: React.MouseEvent | React.TouchEvent) => {
    if (!isDrawing || disabled) return

    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!ctx) return

    const pos = getPosition(e)
    ctx.lineTo(pos.x, pos.y)
    ctx.stroke()
  }

  const stopDrawing = () => {
    if (!isDrawing) return
    setIsDrawing(false)

    const canvas = canvasRef.current
    if (canvas && hasContent) {
      const dataUrl = canvas.toDataURL()
      onChange?.(dataUrl)
    }
  }

  const handleClear = () => {
    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!ctx) return

    saveState()
    ctx.fillStyle = backgroundColor
    ctx.fillRect(0, 0, width, height)

    if (!hasContent) {
      ctx.font = '16px Arial'
      ctx.fillStyle = '#cccccc'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(placeholder, width / 2, height / 2)
    }

    setHasContent(false)
    onChange?.('')
  }

  const handleUndo = () => {
    if (history.length === 0) return

    const prevState = history[history.length - 1]
    setHistory(prev => prev.slice(0, -1))

    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!ctx) return

    const img = new Image()
    img.onload = () => {
      ctx.clearRect(0, 0, width, height)
      ctx.drawImage(img, 0, 0)
    }
    img.src = prevState

    onChange?.(prevState)
  }

  const handleConfirm = () => {
    const canvas = canvasRef.current
    if (canvas && hasContent) {
      const dataUrl = canvas.toDataURL()
      onChange?.(dataUrl)
    }
  }

  return (
    <div className={className} style={{ display: 'inline-block', ...style }}>
      <canvas
        ref={canvasRef}
        width={width}
        height={height}
        style={{
          border: '1px solid #d9d9d9',
          borderRadius: 4,
          cursor: disabled ? 'not-allowed' : 'crosshair',
          touchAction: 'none',
        }}
        onMouseDown={startDrawing}
        onMouseMove={draw}
        onMouseUp={stopDrawing}
        onMouseLeave={stopDrawing}
        onTouchStart={startDrawing}
        onTouchMove={draw}
        onTouchEnd={stopDrawing}
      />
      {showToolbar && !disabled && (
        <div style={{ marginTop: 8, textAlign: 'right' }}>
          <Space size="small">
            <Button size="small" icon={<UndoOutlined />} onClick={handleUndo} disabled={history.length === 0}>
              撤销
            </Button>
            <Button size="small" icon={<ClearOutlined />} onClick={handleClear}>
              清除
            </Button>
            <Button size="small" type="primary" icon={<CheckOutlined />} onClick={handleConfirm} disabled={!hasContent}>
              确认
            </Button>
          </Space>
        </div>
      )}
    </div>
  )
}

export default SignatureField
