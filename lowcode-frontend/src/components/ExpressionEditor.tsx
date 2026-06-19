import React, { useState, useEffect, useRef, useCallback } from 'react'
import { Input, Tag, Collapse, Tooltip, Alert, Spin, Empty, Button, Space, Popover, Tabs, Badge, Card } from 'antd'
import { FunctionOutlined, PlusOutlined, CheckCircleOutlined, CloseCircleOutlined, WarningOutlined, FieldNumberOutlined, FieldStringOutlined, FieldTimeOutlined, BugOutlined, CalculatorOutlined, PlayCircleOutlined } from '@ant-design/icons'

const { Panel } = Collapse

export interface FunctionDef {
  name: string
  displayName: string
  description: string
  syntax: string
  returnType: string
  category: string
}

export interface CategoryDef {
  name: string
  displayName: string
}

export interface ValidationResult {
  valid: boolean
  errors: string[]
  warnings: string[]
}

export interface FieldDef {
  name: string
  type: string
  table?: string
  dataSourceId?: number
}

export interface ExpressionRuntimeResult {
  success: boolean
  result: any
  resultType?: string
  error?: string
  duration: number
}

interface ExpressionEditorProps {
  value?: string
  onChange?: (value: string) => void
  fields?: FieldDef[]
  functions?: FunctionDef[]
  categories?: CategoryDef[]
  onValidate?: (expression: string) => Promise<ValidationResult>
  onExecute?: (expression: string, context: Record<string, any>) => Promise<ExpressionRuntimeResult>
  placeholder?: string
  mode?: 'condition' | 'mapping'
  height?: number
}

const CATEGORY_ICONS: Record<string, React.ReactNode> = {
  AGGREGATE: <FieldNumberOutlined />,
  CONDITIONAL: <FunctionOutlined />,
  STRING_MATCH: <FieldStringOutlined />,
  STRING_PROCESS: <FieldStringOutlined />,
  DATE: <FieldTimeOutlined />,
  MATH: <FieldNumberOutlined />,
  NULL_HANDLER: <WarningOutlined />,
}

const TYPE_COLORS: Record<string, string> = {
  number: 'blue',
  string: 'green',
  boolean: 'orange',
  any: 'purple',
  date: 'cyan',
}

const HIGHLIGHT_STYLES: Record<string, React.CSSProperties> = {
  keyword: { color: '#c678dd', fontWeight: 600 },
  string: { color: '#98c379' },
  number: { color: '#d19a66' },
  variable: { color: '#e06c75', backgroundColor: 'rgba(224,108,117,0.1)', padding: '0 2px', borderRadius: 2 },
  function: { color: '#61afef', fontWeight: 600 },
  operator: { color: '#56b6c2' },
  bracket: { color: '#abb2bf' },
  default: { color: '#abb2bf' },
}

const tokenizeExpression = (expr: string): { type: string; text: string }[] => {
  if (!expr) return []
  const tokens: { type: string; text: string }[] = []

  const patterns: [RegExp, string][] = [
    [/^\$\{[^}]+\}/, 'variable'],
    [/^(?:true|false|null|undefined)\b/, 'keyword'],
    [/^\d+\.?\d*/, 'number'],
    [/^[A-Z_]+(?=\s*\()/, 'function'],
    [/^"[^"]*"/, 'string'],
    [/^'[^']*'/, 'string'],
    [/^(==|!=|>=|<=|>|<|&&|\|\||!)/, 'operator'],
    [/^[+\-*/%]/, 'operator'],
    [/^[()[\]{},.:]/, 'bracket'],
    [/^\s+/, 'default'],
    [/^[a-zA-Z_]\w*/, 'default'],
  ]

  let remaining = expr
  while (remaining.length > 0) {
    let matched = false
    for (const [pattern, type] of patterns) {
      const match = remaining.match(pattern)
      if (match) {
        tokens.push({ type, text: match[0] })
        remaining = remaining.slice(match[0].length)
        matched = true
        break
      }
    }
    if (!matched) {
      tokens.push({ type: 'default', text: remaining[0] })
      remaining = remaining.slice(1)
    }
  }
  return tokens
}

const extractVariables = (expr: string): string[] => {
  const matches = expr.matchAll(/\$\{([^}]+)\}/g)
  return [...new Set([...matches].map(m => m[1]))]
}

const ExpressionEditor: React.FC<ExpressionEditorProps> = ({
  value = '',
  onChange,
  fields = [],
  functions = [],
  categories = [],
  onValidate,
  onExecute,
  placeholder = '输入表达式，如: ${price} * ${quantity}',
  mode = 'condition',
  height = 120,
}) => {
  const [expression, setExpression] = useState(value)
  const [validation, setValidation] = useState<ValidationResult | null>(null)
  const [validating, setValidating] = useState(false)
  const [cursorPosition, setCursorPosition] = useState(0)
  const [suggestionVisible, setSuggestionVisible] = useState(false)
  const [suggestions, setSuggestions] = useState<{ type: 'function' | 'field'; item: FunctionDef | FieldDef }[]>([])
  const [selectedSuggestion, setSelectedSuggestion] = useState(0)
  const textareaRef = useRef<any>(null)
  const debounceRef = useRef<any>(null)
  const highlightRef = useRef<HTMLDivElement>(null)

  const [trialVisible, setTrialVisible] = useState(false)
  const [trialContext, setTrialContext] = useState<Record<string, string>>({})
  const [trialResult, setTrialResult] = useState<ExpressionRuntimeResult | null>(null)
  const [trialLoading, setTrialLoading] = useState(false)

  useEffect(() => {
    setExpression(value)
  }, [value])

  useEffect(() => {
    if (trialVisible) {
      const vars = extractVariables(expression)
      const newContext: Record<string, string> = {}
      for (const v of vars) {
        newContext[v] = trialContext[v] ?? ''
      }
      setTrialContext(newContext)
    }
  }, [expression, trialVisible])

  const triggerValidate = useCallback(async (expr: string) => {
    if (!onValidate || !expr.trim()) {
      setValidation(null)
      return
    }
    setValidating(true)
    try {
      const result = await onValidate(expr)
      setValidation(result)
    } catch {
      setValidation({ valid: false, errors: ['校验请求失败'], warnings: [] })
    } finally {
      setValidating(false)
    }
  }, [onValidate])

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newVal = e.target.value
    setExpression(newVal)
    onChange?.(newVal)
    setCursorPosition(e.target.selectionStart)

    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      triggerValidate(newVal)
    }, 500)

    updateSuggestions(newVal, e.target.selectionStart)
  }

  const updateSuggestions = (expr: string, cursor: number) => {
    const textBeforeCursor = expr.slice(0, cursor)
    const lastWordMatch = textBeforeCursor.match(/([A-Z_]*|[a-z_]*)$/)
    if (!lastWordMatch || !lastWordMatch[1]) {
      setSuggestionVisible(false)
      return
    }
    const prefix = lastWordMatch[1].toUpperCase()
    if (prefix.length === 0) {
      setSuggestionVisible(false)
      return
    }

    const funcSuggestions = functions
      .filter(f => f.name.startsWith(prefix))
      .map(f => ({ type: 'function' as const, item: f }))

    const fieldSuggestions = fields
      .filter(f => f.name.toUpperCase().startsWith(prefix))
      .map(f => ({ type: 'field' as const, item: f }))

    const allSuggestions = [...funcSuggestions, ...fieldSuggestions]
    setSuggestions(allSuggestions)
    setSelectedSuggestion(0)
    setSuggestionVisible(allSuggestions.length > 0)
  }

  const applySuggestion = (suggestion: { type: 'function' | 'field'; item: FunctionDef | FieldDef }) => {
    const textBeforeCursor = expression.slice(0, cursorPosition)
    const lastWordMatch = textBeforeCursor.match(/([A-Z_]*|[a-z_]*)$/)
    if (!lastWordMatch) return

    const replaceStart = cursorPosition - lastWordMatch[1].length
    let insertText: string
    if (suggestion.type === 'function') {
      const fn = suggestion.item as FunctionDef
      insertText = fn.name + '('
    } else {
      const field = suggestion.item as FieldDef
      insertText = '${' + (field.table ? `${field.table}.${field.name}` : field.name) + '}'
    }

    const newExpr = expression.slice(0, replaceStart) + insertText + expression.slice(cursorPosition)
    setExpression(newExpr)
    onChange?.(newExpr)
    setSuggestionVisible(false)
    textareaRef.current?.focus()
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (suggestionVisible && suggestions.length > 0) {
      if (e.key === 'ArrowDown') {
        e.preventDefault()
        setSelectedSuggestion(prev => (prev + 1) % suggestions.length)
        return
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault()
        setSelectedSuggestion(prev => (prev - 1 + suggestions.length) % suggestions.length)
        return
      }
      if (e.key === 'Enter' || e.key === 'Tab') {
        e.preventDefault()
        applySuggestion(suggestions[selectedSuggestion])
        return
      }
      if (e.key === 'Escape') {
        setSuggestionVisible(false)
        return
      }
    }
  }

  const handleFieldClick = (field: FieldDef) => {
    const varRef = '${' + (field.table ? `${field.table}.${field.name}` : field.name) + '}'
    const newExpr = expression + (expression && !expression.endsWith(' ') ? ' ' : '') + varRef
    setExpression(newExpr)
    onChange?.(newExpr)
  }

  const handleFunctionClick = (fn: FunctionDef) => {
    const newExpr = expression + (expression && !expression.endsWith(' ') ? ' ' : '') + fn.name + '('
    setExpression(newExpr)
    onChange?.(newExpr)
  }

  const handleFieldDragStart = (e: React.DragEvent, field: FieldDef) => {
    const varRef = '${' + (field.table ? `${field.table}.${field.name}` : field.name) + '}'
    e.dataTransfer.setData('text/plain', varRef)
    e.dataTransfer.effectAllowed = 'copy'
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    const data = e.dataTransfer.getData('text/plain')
    if (data) {
      const newExpr = expression + (expression && !expression.endsWith(' ') ? ' ' : '') + data
      setExpression(newExpr)
      onChange?.(newExpr)
    }
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'copy'
  }

  const renderHighlighted = () => {
    const tokens = tokenizeExpression(expression)
    return tokens.map((token, i) => (
      <span key={i} style={HIGHLIGHT_STYLES[token.type] || HIGHLIGHT_STYLES.default}>
        {token.text}
      </span>
    ))
  }

  const syncScroll = () => {
    if (highlightRef.current && textareaRef.current) {
      highlightRef.current.scrollTop = textareaRef.current.scrollTop
      highlightRef.current.scrollLeft = textareaRef.current.scrollLeft
    }
  }

  const functionsByCategory = categories.reduce((acc, cat) => {
    acc[cat.name] = functions.filter(f => f.category === cat.name)
    return acc
  }, {} as Record<string, FunctionDef[]>)

  const fieldsByTable = fields.reduce((acc, f) => {
    const table = f.table || '默认'
    if (!acc[table]) acc[table] = []
    acc[table].push(f)
    return acc
  }, {} as Record<string, FieldDef[]>)

  const renderValidationStatus = () => {
    if (validating) {
      return <Spin size="small" />
    }
    if (!validation) {
      return null
    }
    if (validation.valid) {
      return (
        <Tooltip title={validation.warnings.length > 0 ? validation.warnings.join('\n') : '语法正确'}>
          <CheckCircleOutlined style={{ color: '#52c41a' }} />
        </Tooltip>
      )
    }
    return (
      <Tooltip title={validation.errors.join('\n')}>
        <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
      </Tooltip>
    )
  }

  const handleTrialExecute = async () => {
    if (!onExecute) return
    setTrialLoading(true)
    setTrialResult(null)
    try {
      const contextValue: Record<string, any> = {}
      for (const [k, v] of Object.entries(trialContext)) {
        contextValue[k] = v
      }
      const result = await onExecute(expression, contextValue)
      setTrialResult(result)
    } catch (err: any) {
      setTrialResult({ success: false, result: null, error: err?.message || '执行失败', duration: 0 })
    } finally {
      setTrialLoading(false)
    }
  }

  const trialVariables = trialVisible ? extractVariables(expression) : []

  const resultColor = trialResult
    ? trialResult.success
      ? (trialResult.result === null || trialResult.result === undefined ? '#d9d9d9' : '#52c41a')
      : '#ff4d4f'
    : '#d9d9d9'

  return (
    <div className="expression-editor" style={{ border: '1px solid #d9d9d9', borderRadius: 6, overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', padding: '4px 8px', background: '#fafafa', borderBottom: '1px solid #f0f0f0', justifyContent: 'space-between' }}>
        <Space size={4}>
          <FunctionOutlined style={{ color: '#1890ff' }} />
          <span style={{ fontSize: 12, fontWeight: 500 }}>
            {mode === 'condition' ? '条件表达式' : '映射表达式'}
          </span>
        </Space>
        <Space size={4}>
          {renderValidationStatus()}
          <Button
            type="link"
            size="small"
            icon={<BugOutlined />}
            onClick={() => triggerValidate(expression)}
            disabled={!expression.trim()}
          >
            校验
          </Button>
        </Space>
      </div>

      <div style={{ position: 'relative' }}>
        <div
          ref={highlightRef}
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            padding: '8px 11px',
            fontFamily: "'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace",
            fontSize: 13,
            lineHeight: 1.5,
            whiteSpace: 'pre-wrap',
            wordWrap: 'break-word',
            overflow: 'auto',
            pointerEvents: 'none',
            background: '#1e1e1e',
            color: '#abb2bf',
            zIndex: 0,
          }}
        >
          {renderHighlighted()}
          <span style={{ borderRight: '2px solid #528bff', animation: 'blink 1s infinite' }}> </span>
        </div>
        <Input.TextArea
          ref={textareaRef}
          value={expression}
          onChange={handleChange}
          onScroll={syncScroll}
          onKeyDown={handleKeyDown}
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          placeholder={placeholder}
          autoSize={{ minRows: 3, maxRows: 6 }}
          style={{
            position: 'relative',
            fontFamily: "'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace",
            fontSize: 13,
            lineHeight: 1.5,
            background: 'transparent',
            color: 'transparent',
            caretColor: '#528bff',
            border: 'none',
            resize: 'none',
            zIndex: 1,
            padding: '8px 11px',
          }}
        />
      </div>

      {suggestionVisible && suggestions.length > 0 && (
        <div style={{
          position: 'absolute',
          background: '#fff',
          border: '1px solid #d9d9d9',
          borderRadius: 4,
          boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
          maxHeight: 200,
          overflow: 'auto',
          zIndex: 1000,
          width: '100%',
        }}>
          {suggestions.map((s, i) => (
            <div
              key={i}
              style={{
                padding: '6px 12px',
                cursor: 'pointer',
                background: i === selectedSuggestion ? '#e6f7ff' : '#fff',
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
              onMouseEnter={() => setSelectedSuggestion(i)}
              onClick={() => applySuggestion(s)}
            >
              {s.type === 'function' ? (
                <>
                  <FunctionOutlined style={{ color: '#1890ff' }} />
                  <span style={{ fontWeight: 500 }}>{(s.item as FunctionDef).name}</span>
                  <span style={{ color: '#999', fontSize: 12 }}>{(s.item as FunctionDef).description}</span>
                </>
              ) : (
                <>
                  <FieldNumberOutlined style={{ color: '#52c41a' }} />
                  <span style={{ fontWeight: 500 }}>{(s.item as FieldDef).name}</span>
                  <Tag color={TYPE_COLORS[(s.item as FieldDef).type] || 'default'} style={{ marginLeft: 'auto', margin: 0 }}>
                    {(s.item as FieldDef).type}
                  </Tag>
                </>
              )}
            </div>
          ))}
        </div>
      )}

      {validation && !validation.valid && validation.errors.length > 0 && (
        <div style={{ padding: '4px 8px' }}>
          {validation.errors.map((err, i) => (
            <Alert key={i} type="error" message={err} showIcon style={{ padding: '2px 8px', marginBottom: 2, fontSize: 12 }} />
          ))}
        </div>
      )}
      {validation && validation.warnings.length > 0 && (
        <div style={{ padding: '4px 8px' }}>
          {validation.warnings.map((warn, i) => (
            <Alert key={i} type="warning" message={warn} showIcon style={{ padding: '2px 8px', marginBottom: 2, fontSize: 12 }} />
          ))}
        </div>
      )}

      <Tabs
        size="small"
        tabBarStyle={{ padding: '0 8px', margin: 0, background: '#fafafa', borderTop: '1px solid #f0f0f0' }}
        items={[
          {
            key: 'functions',
            label: <span><FunctionOutlined /> 函数</span>,
            children: (
              <div style={{ maxHeight: 220, overflow: 'auto', padding: '0 4px' }}>
                <Collapse ghost size="small">
                  {categories.map(cat => {
                    const fns = functionsByCategory[cat.name] || []
                    if (fns.length === 0) return null
                    return (
                      <Panel
                        header={
                          <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            {CATEGORY_ICONS[cat.name] || <FunctionOutlined />}
                            {cat.displayName}
                            <Badge count={fns.length} style={{ backgroundColor: '#1890ff' }} size="small" />
                          </span>
                        }
                        key={cat.name}
                      >
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                          {fns.map(fn => (
                            <Tooltip key={fn.name} title={`${fn.syntax} — ${fn.description}`}>
                              <Tag
                                color={TYPE_COLORS[fn.returnType] || 'default'}
                                style={{ cursor: 'pointer', margin: 0 }}
                                onClick={() => handleFunctionClick(fn)}
                              >
                                {fn.name}
                              </Tag>
                            </Tooltip>
                          ))}
                        </div>
                      </Panel>
                    )
                  })}
                </Collapse>
              </div>
            ),
          },
          {
            key: 'fields',
            label: <span><FieldNumberOutlined /> 字段</span>,
            children: (
              <div style={{ maxHeight: 220, overflow: 'auto', padding: '0 4px' }}>
                {Object.keys(fieldsByTable).length > 0 ? (
                  <Collapse ghost size="small">
                    {Object.entries(fieldsByTable).map(([table, tableFields]) => (
                      <Panel header={<span style={{ fontWeight: 500 }}>{table}</span>} key={table}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                          {tableFields.map((field, i) => (
                            <div
                              key={i}
                              draggable
                              onDragStart={(e) => handleFieldDragStart(e, field)}
                              onClick={() => handleFieldClick(field)}
                              style={{
                                padding: '4px 8px',
                                border: '1px solid #e8e8e8',
                                borderRadius: 4,
                                cursor: 'grab',
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'center',
                                background: '#fafafa',
                                fontSize: 12,
                                transition: 'all 0.2s',
                              }}
                              onMouseEnter={(e) => {
                                e.currentTarget.style.background = '#e6f7ff'
                                e.currentTarget.style.borderColor = '#1890ff'
                              }}
                              onMouseLeave={(e) => {
                                e.currentTarget.style.background = '#fafafa'
                                e.currentTarget.style.borderColor = '#e8e8e8'
                              }}
                            >
                              <span style={{ fontWeight: 500 }}>{field.name}</span>
                              <Tag color={TYPE_COLORS[field.type] || 'default'} style={{ margin: 0 }}>
                                {field.type}
                              </Tag>
                            </div>
                          ))}
                        </div>
                      </Panel>
                    ))}
                  </Collapse>
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无可用字段" />
                )}
              </div>
            ),
          },
        ]}
      />

      <div style={{ padding: '4px 8px', background: '#fafafa', borderTop: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 11, color: '#999' }}>
          变量: $&#123;field&#125; | 函数: SUM, IF, CONTAINS... | 运算: +, -, *, /, ==, &&, ||
        </span>
        <Button
          type="link"
          size="small"
          icon={<CalculatorOutlined />}
          onClick={() => {
            setTrialVisible(!trialVisible)
            setTrialResult(null)
          }}
          disabled={!onExecute}
        >
          试算
        </Button>
      </div>

      {trialVisible && (
        <Collapse
          ghost
          activeKey={['trial']}
          style={{ borderTop: '1px solid #f0f0f0' }}
        >
          <Panel header="🧮 试算面板" key="trial">
            {trialVariables.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 12 }}>
                {trialVariables.map(v => (
                  <div key={v} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ minWidth: 120, fontSize: 13, fontWeight: 500, color: '#e06c75' }}>${'{'}{v}{'}'}</span>
                    <Input
                      size="small"
                      placeholder={`输入 ${v} 的测试值`}
                      value={trialContext[v] ?? ''}
                      onChange={e => setTrialContext(prev => ({ ...prev, [v]: e.target.value }))}
                      style={{ flex: 1 }}
                    />
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ color: '#999', fontSize: 12, marginBottom: 12 }}>表达式中未检测到变量引用</div>
            )}
            <Button
              type="primary"
              size="small"
              icon={<PlayCircleOutlined />}
              loading={trialLoading}
              onClick={handleTrialExecute}
              disabled={!expression.trim() || !onExecute}
              style={{ marginBottom: 12 }}
            >
              执行
            </Button>
            {trialResult && (
              trialResult.success ? (
                <Card
                  title="执行结果"
                  size="small"
                  style={{ borderColor: resultColor }}
                  headStyle={{ color: resultColor, fontSize: 13 }}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                    <div>
                      <span style={{ color: '#999', marginRight: 8 }}>结果:</span>
                      <span style={{ color: resultColor, fontWeight: 600 }}>
                        {trialResult.result === null || trialResult.result === undefined ? '(空)' : String(trialResult.result)}
                      </span>
                    </div>
                    {trialResult.resultType && (
                      <div>
                        <span style={{ color: '#999', marginRight: 8 }}>类型:</span>
                        <Tag color={TYPE_COLORS[trialResult.resultType] || 'default'}>{trialResult.resultType}</Tag>
                      </div>
                    )}
                    <div>
                      <span style={{ color: '#999', marginRight: 8 }}>耗时:</span>
                      <span>{trialResult.duration}ms</span>
                    </div>
                  </div>
                </Card>
              ) : (
                <>
                  <Card
                    title="执行结果"
                    size="small"
                    style={{ borderColor: '#ff4d4f' }}
                    headStyle={{ color: '#ff4d4f', fontSize: 13 }}
                  >
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      <div>
                        <span style={{ color: '#999', marginRight: 8 }}>结果:</span>
                        <span style={{ color: '#ff4d4f' }}>{String(trialResult.result)}</span>
                      </div>
                      {trialResult.resultType && (
                        <div>
                          <span style={{ color: '#999', marginRight: 8 }}>类型:</span>
                          <Tag color={TYPE_COLORS[trialResult.resultType] || 'default'}>{trialResult.resultType}</Tag>
                        </div>
                      )}
                      <div>
                        <span style={{ color: '#999', marginRight: 8 }}>耗时:</span>
                        <span>{trialResult.duration}ms</span>
                      </div>
                    </div>
                  </Card>
                  {trialResult.error && (
                    <Alert type="error" message={trialResult.error} showIcon style={{ marginTop: 8, fontSize: 12 }} />
                  )}
                </>
              )
            )}
            {!trialResult && !trialLoading && (
              <Card title="执行结果" size="small" style={{ borderColor: '#d9d9d9' }} headStyle={{ color: '#999', fontSize: 13 }}>
                <span style={{ color: '#d9d9d9' }}>尚未执行</span>
              </Card>
            )}
          </Panel>
        </Collapse>
      )}
    </div>
  )
}

export default ExpressionEditor
