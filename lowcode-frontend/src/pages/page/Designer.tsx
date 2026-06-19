import React, { useState, useEffect, useCallback, useRef } from 'react'
import {
  Layout,
  Button,
  Space,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Tabs,
  Modal,
  message,
  Drawer,
  Tree,
  Collapse,
  Divider,
  Radio,
  Slider,
  ColorPicker,
  Card,
  Badge,
  Row,
  Col,
  Tag,
} from 'antd'
import {
  SaveOutlined,
  PlayCircleOutlined,
  CodeOutlined,
  PlusOutlined,
  DeleteOutlined,
  ArrowLeftOutlined,
  EyeOutlined,
  UndoOutlined,
  RedoOutlined,
  DesktopOutlined,
  MobileOutlined,
  SettingOutlined,
  CodeSandboxOutlined,
  ApiOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  FormOutlined,
  RobotOutlined,
  DatabaseOutlined,
  ArrowRightOutlined,
  ExperimentOutlined,
  SafetyOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { DndProvider, useDrag, useDrop } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'
import { createForm, Field } from '@formily/core'
import { createSchemaField, FormProvider, FormItem, Input as FormilyInput, Select as FormilySelect, NumberPicker, Switch as FormilySwitch } from '@formily/antd'
import { PageInfo, PageComponent, ComponentLibrary, pageApi, componentApi, customComponentApi, CustomComponent } from '@/api/page'
import { dataModelApi, dataSourceApi, fieldMappingApi, expressionApi, DataSource, FieldMapping, TableColumnInfo, ExpressionFunctionDef, ExpressionCategoryDef } from '@/api/dataModel'
import { useAppStore } from '@/store/appStore'
import CustomComponentWrapper, { CustomComponentPreview, useCustomComponentSchema } from '@/components/CustomComponentWrapper'
import { loadCustomComponent } from '@/utils/componentLoader'
import AiPagePanel from '@/components/AiPagePanel'
import PermissionGuard from '@/components/PermissionGuard'
import ExpressionEditor from '@/components/ExpressionEditor'

const { Header, Sider, Content } = Layout
const { Option } = Select
const { Panel } = Collapse
const { TreeNode } = Tree
const SchemaField = createSchemaField({
  components: {
    Input: FormilyInput,
    Select: FormilySelect,
    NumberPicker,
    Switch: FormilySwitch,
  },
})

const componentCategories = [
  { key: 'basic', name: '基础组件', icon: '📦' },
  { key: 'form', name: '表单组件', icon: '📝' },
  { key: 'layout', name: '布局容器', icon: '📐' },
  { key: 'data', name: '数据展示', icon: '📊' },
  { key: 'chart', name: '图表组件', icon: '📈' },
  { key: 'advanced', name: '高级组件', icon: '⚡' },
  { key: 'custom', name: '自定义组件', icon: '🔧' },
]

const componentIconMap: Record<string, string> = {
  INPUT: '📝',
  TEXTAREA: '📄',
  NUMBER: '🔢',
  SELECT: '📋',
  DATE: '📅',
  DATETIME: '⏰',
  TIME: '🕐',
  SWITCH: '🔘',
  CHECKBOX: '☑️',
  RADIO: '🔘',
  UPLOAD: '📤',
  RICHTEXT: '📑',
  TABLE: '📊',
  BUTTON: '🔲',
  LINK: '🔗',
  IMAGE: '🖼️',
  TEXT: '📄',
  TITLE: '🏷️',
  ICON: '🎯',
  DIVIDER: '➖',
  TABS: '📑',
  CARD: '🃏',
  GRID: '📐',
  FLEX: '📏',
  MODAL: '🪟',
  FORM: '📋',
  STEPS: '👣',
  TIMELINE: '⏱️',
  PROGRESS: '📊',
  RATE: '⭐',
  SLIDER: '🎚️',
  LINECHART: '📈',
  BARCHART: '📊',
  PIECHART: '🥧',
  AREACHART: '📈',
  SCATTERCHART: '🔵',
  RADARCHART: '🕸️',
}

interface ComponentItemProps {
  component: ComponentLibrary | CustomComponent
  onDragStart?: () => void
}

const ComponentItem: React.FC<ComponentItemProps> = ({ component }) => {
  const isCustom = 'currentVersion' in component
  const defaultProps = isCustom
    ? (component as CustomComponent).currentVersionInfo?.defaultProps
    : (component as ComponentLibrary).defaultProps

  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'COMPONENT',
    item: {
      componentType: component.componentType,
      componentName: component.componentName,
      defaultProps,
      isCustom,
      currentVersion: isCustom ? (component as CustomComponent).currentVersion : undefined,
    },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  }))

  return (
    <div
      ref={drag}
      className="component-item"
      style={{
        padding: '8px 12px',
        background: '#fff',
        border: isCustom ? '1px solid #9254de' : '1px solid #e8e8e8',
        borderRadius: 4,
        marginBottom: 8,
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        opacity: isDragging ? 0.5 : 1,
      }}
    >
      <span style={{ fontSize: 16 }}>
        {componentIconMap[component.componentType] || (component as any).icon || '📦'}
      </span>
      <span style={{ fontSize: 13 }}>{component.componentName}</span>
      {isCustom && (
        <Tag color="purple" style={{ marginLeft: 'auto', fontSize: 10 }}>
          自定义
        </Tag>
      )}
    </div>
  )
}

interface CanvasComponentProps {
  component: PageComponent
  index: number
  isSelected: boolean
  onSelect: () => void
  onDelete: () => void
  onDrop: (item: any) => void
}

const CanvasComponent: React.FC<CanvasComponentProps> = ({ component, isSelected, onSelect, onDelete, onDrop }) => {
  const [{ isOver }, drop] = useDrop(() => ({
    accept: 'COMPONENT',
    drop: (item: any) => onDrop(item),
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  }))

  const renderPreview = () => {
    const props = component.propsConfig ? JSON.parse(component.propsConfig) : {}
    const style = component.styleConfig ? JSON.parse(component.styleConfig) : {}

    const baseStyle = {
      padding: 8,
      margin: 4,
      border: isSelected ? '2px solid #1677ff' : '1px dashed #d9d9d9',
      borderRadius: 4,
      background: isSelected ? '#e6f7ff' : '#fff',
      minHeight: 40,
      ...style,
    }

    const systemTypes = ['INPUT', 'TEXTAREA', 'NUMBER', 'SELECT', 'DATE', 'DATETIME', 'TIME', 'SWITCH', 'CHECKBOX', 'RADIO', 'UPLOAD', 'RICHTEXT', 'TABLE', 'BUTTON', 'LINK', 'IMAGE', 'TEXT', 'TITLE', 'ICON', 'DIVIDER', 'TABS', 'CARD', 'GRID', 'FLEX', 'MODAL', 'FORM', 'STEPS', 'TIMELINE', 'PROGRESS', 'RATE', 'SLIDER', 'LINECHART', 'BARCHART', 'PIECHART', 'AREACHART', 'SCATTERCHART', 'RADARCHART']

    if (!systemTypes.includes(component.componentType)) {
      return (
        <div style={baseStyle}>
          <CustomComponentPreview componentType={component.componentType} defaultProps={props} />
        </div>
      )
    }

    switch (component.componentType) {
      case 'INPUT':
        return <Input placeholder={props.placeholder || '请输入'} style={{ width: '100%' }} />
      case 'TEXTAREA':
        return <Input.TextArea placeholder={props.placeholder || '请输入'} rows={props.rows || 3} />
      case 'NUMBER':
        return <InputNumber placeholder={props.placeholder || '请输入数字'} style={{ width: '100%' }} />
      case 'SELECT':
        return (
          <Select placeholder={props.placeholder || '请选择'} style={{ width: '100%' }}>
            <Option value="1">选项1</Option>
            <Option value="2">选项2</Option>
          </Select>
        )
      case 'DATE':
        return <Input placeholder="选择日期" style={{ width: '100%' }} />
      case 'BUTTON':
        return <Button type={props.type || 'primary'}>{props.text || '按钮'}</Button>
      case 'TITLE':
        return <div style={{ fontSize: props.level ? 24 - (props.level - 1) * 4 : 20, fontWeight: 'bold' }}>{props.text || '标题'}</div>
      case 'TEXT':
        return <div style={{ color: props.color || '#333' }}>{props.text || '文本内容'}</div>
      case 'IMAGE':
        return <div style={{ width: props.width || 200, height: props.height || 150, background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>🖼️ 图片</div>
      case 'TABLE':
        return (
          <div style={{ background: '#fafafa', padding: 16, borderRadius: 4 }}>
            <div style={{ color: '#999', textAlign: 'center' }}>📊 表格组件</div>
          </div>
        )
      case 'FORM':
        return (
          <div style={{ background: '#fafafa', padding: 16, borderRadius: 4 }}>
            <FormOutlined style={{ fontSize: 24, color: '#999', display: 'block', textAlign: 'center' }} />
            <div style={{ color: '#999', textAlign: 'center', marginTop: 8 }}>📋 表单容器</div>
          </div>
        )
      case 'CARD':
        return (
          <Card title={props.title || '卡片标题'} size="small">
            <div style={{ color: '#999' }}>卡片内容区域</div>
          </Card>
        )
      case 'TABS':
        return (
          <Tabs defaultActiveKey="1" size="small">
            <Tabs.TabPane tab="标签1" key="1">内容1</Tabs.TabPane>
            <Tabs.TabPane tab="标签2" key="2">内容2</Tabs.TabPane>
          </Tabs>
        )
      case 'DIVIDER':
        return <Divider>{props.text || ''}</Divider>
      case 'GRID':
        return (
          <div style={{ display: 'grid', gridTemplateColumns: `repeat(${props.columns || 2}, 1fr)`, gap: 8, padding: 8, border: '1px dashed #e8e8e8' }}>
            {Array.from({ length: props.columns || 2 }).map((_, i) => (
              <div key={i} style={{ height: 60, background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>
                列{i + 1}
              </div>
            ))}
          </div>
        )
      case 'FLEX':
        return (
          <div style={{ display: 'flex', gap: 8, padding: 8, border: '1px dashed #e8e8e8' }}>
            <div style={{ flex: 1, height: 60, background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>子元素1</div>
            <div style={{ flex: 1, height: 60, background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>子元素2</div>
          </div>
        )
      case 'LINECHART':
      case 'BARCHART':
      case 'PIECHART':
        return (
          <div style={{ height: 200, background: '#fafafa', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 4 }}>
            <span style={{ color: '#999' }}>📈 {component.componentName}</span>
          </div>
        )
      default:
        return (
          <div style={{ color: '#999', textAlign: 'center', padding: 16 }}>
            {componentIconMap[component.componentType] || '📦'} {component.componentName}
          </div>
        )
    }
  }

  return (
    <div
      ref={drop}
      style={{
        position: 'relative',
        ...(isOver ? { outline: '2px dashed #1677ff', outlineOffset: 2 } : {}),
      }}
      onClick={(e) => { e.stopPropagation(); onSelect() }}
    >
      {isSelected && (
        <div style={{ position: 'absolute', top: -12, right: -8, zIndex: 10 }}>
          <Button
            type="primary"
            danger
            size="small"
            icon={<DeleteOutlined />}
            onClick={(e) => { e.stopPropagation(); onDelete() }}
          />
        </div>
      )}
      <div style={isOver ? { background: 'rgba(22, 119, 255, 0.1)' } : {}}>
        {renderPreview()}
      </div>
    </div>
  )
}

const PageDesigner: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [page, setPage] = useState<PageInfo | null>(null)
  const [selectedComponentId, setSelectedComponentId] = useState<string | null>(null)
  const [form] = Form.useForm()
  const [propsForm] = Form.useForm()
  const [styleForm] = Form.useForm()
  const [eventForm] = Form.useForm()
  const [dataSourceForm] = Form.useForm()
  const [validationForm] = Form.useForm()
  const [permissionForm] = Form.useForm()
  const [componentTree, setComponentTree] = useState<any[]>([])
  const [activeTab, setActiveTab] = useState('props')
  const [previewVisible, setPreviewVisible] = useState(false)
  const [previewMode, setPreviewMode] = useState<'pc' | 'mobile'>('pc')
  const [codeModalVisible, setCodeModalVisible] = useState(false)
  const [codeContent, setCodeContent] = useState('')
  const [componentLibrary, setComponentLibrary] = useState<Record<string, ComponentLibrary[]>>({})
  const [customComponents, setCustomComponents] = useState<Record<string, CustomComponent[]>>({})
  const [customComponentSchemas, setCustomComponentSchemas] = useState<Record<string, any>>({})
  const [mergedComponentLibrary, setMergedComponentLibrary] = useState<Record<string, (ComponentLibrary | CustomComponent)[]>>({})
  const [expandedCategory, setExpandedCategory] = useState<string[]>(['basic', 'form', 'custom'])
  const [dataModels, setDataModels] = useState<any[]>([])
  const [history, setHistory] = useState<any[]>([])
  const [historyIndex, setHistoryIndex] = useState(-1)
  const [aiPanelVisible, setAiPanelVisible] = useState(false)
  const [aiSessionId, setAiSessionId] = useState('')

  const [extDataSources, setExtDataSources] = useState<DataSource[]>([])
  const [selectedExtDataSourceId, setSelectedExtDataSourceId] = useState<number | null>(null)
  const [extTables, setExtTables] = useState<string[]>([])
  const [selectedExtTable, setSelectedExtTable] = useState<string | null>(null)
  const [extTableColumns, setExtTableColumns] = useState<TableColumnInfo[]>([])
  const [extMappings, setExtMappings] = useState<FieldMapping[]>([])
  const [extMappingLoading, setExtMappingLoading] = useState(false)
  const [extTablesLoading, setExtTablesLoading] = useState(false)
  const [extColumnsLoading, setExtColumnsLoading] = useState(false)

  const [exprFunctions, setExprFunctions] = useState<ExpressionFunctionDef[]>([])
  const [exprCategories, setExprCategories] = useState<ExpressionCategoryDef[]>([])
  const [conditionExpr, setConditionExpr] = useState('')
  const [mappingExpr, setMappingExpr] = useState('')

  const canvasRef = useRef<HTMLDivElement>(null)

  const loadPage = useCallback(async () => {
      setPage({
        appId: currentApp?.id || 1,
        pageName: '',
        pageCode: '',
        pagePath: '',
        layoutType: 'FREE',
        isHome: 0,
        status: 0,
        components: [],
      })
      form.setFieldsValue({
        pageName: '',
        pageCode: '',
        pagePath: '',
        layoutType: 'FREE',
        isHome: false,
      })
      return
    }
    try {
      const res = await pageApi.get(Number(id))
      setPage(res.data || null)
      form.setFieldsValue({
        ...res.data,
        isHome: res.data?.isHome === 1,
      })
      if (res.data?.components && res.data.components.length > 0) {
        const tree = buildComponentTree(res.data.components)
        setComponentTree(tree)
      }
    } catch (e) {
      console.error(e)
    }
  }, [id, currentApp])

  const loadComponentLibrary = useCallback(async () => {
    try {
      const [systemRes, customRes] = await Promise.all([
        componentApi.tree(),
        customComponentApi.tree(),
      ])
      setComponentLibrary(systemRes.data || {})
      setCustomComponents(customRes.data || {})

      const merged: Record<string, (ComponentLibrary | CustomComponent)[]> = {}
      componentCategories.forEach((cat) => {
        const systemComponents = systemRes.data?.[cat.key] || []
        const customComponentsList = customRes.data?.[cat.key] || []
        if (systemComponents.length > 0 || customComponentsList.length > 0) {
          merged[cat.key] = [...systemComponents, ...customComponentsList]
        }
      })
      setMergedComponentLibrary(merged)

      const schemas: Record<string, any> = {}
      const allCustomComponents = Object.values(customRes.data || {}).flat()
      await Promise.all(
        allCustomComponents.map(async (comp: any) => {
          try {
            const result = await loadCustomComponent(comp.componentType, comp.currentVersion)
            schemas[comp.componentType] = result.schema
          } catch (e) {
            console.error(`Failed to load schema for ${comp.componentType}:`, e)
          }
        })
      )
      setCustomComponentSchemas(schemas)
    } catch (e) {
      console.error(e)
    }
  }, [])

  const loadDataModels = useCallback(async () => {
    if (!currentApp) return
    try {
      const res = await dataModelApi.list(currentApp.id)
      setDataModels(res.data || [])
    } catch (e) {
      console.error(e)
    }
  }, [currentApp])

  const loadExtDataSources = useCallback(async () => {
    if (!currentApp) return
    try {
      const res: any = await dataSourceApi.list(currentApp.id)
      if (res.code === 0 || res.code === 200) {
        setExtDataSources((res.data || []).filter((ds: DataSource) => ds.status === 1))
      }
    } catch (e) {
      console.error('加载外部数据源失败:', e)
    }
  }, [currentApp])

  const loadExprFunctions = useCallback(async () => {
    try {
      const [funcRes, catRes]: any[] = await Promise.all([
        expressionApi.getFunctions(),
        expressionApi.getCategories(),
      ])
      if (funcRes.code === 0 || funcRes.code === 200) {
        setExprFunctions(funcRes.data || [])
      }
      if (catRes.code === 0 || catRes.code === 200) {
        setExprCategories(catRes.data || [])
      }
    } catch (e) {
      console.error('加载表达式函数失败:', e)
    }
  }, [])

  const loadExtTables = useCallback(async (dsId: number) => {
    try {
      setExtTablesLoading(true)
      const res: any = await dataSourceApi.getTables(dsId)
      if (res.code === 0 || res.code === 200) {
        setExtTables(res.data || [])
      }
    } catch (e) {
      console.error('加载表列表失败:', e)
      message.error('加载表列表失败')
    } finally {
      setExtTablesLoading(false)
    }
  }, [])

  const loadExtTableColumns = useCallback(async (dsId: number, tableName: string) => {
    try {
      setExtColumnsLoading(true)
      const res: any = await dataSourceApi.getTableColumns(dsId, tableName)
      if (res.code === 0 || res.code === 200) {
        setExtTableColumns(res.data || [])
      }
    } catch (e) {
      console.error('加载表字段失败:', e)
      message.error('加载表字段失败')
    } finally {
      setExtColumnsLoading(false)
    }
  }, [])

  const loadExtMappings = useCallback(async () => {
    if (!id || id === 'undefined') return
    try {
      setExtMappingLoading(true)
      const res: any = await fieldMappingApi.getByPage(Number(id))
      if (res.code === 0 || res.code === 200) {
        setExtMappings(res.data || [])
      }
    } catch (e) {
      console.error('加载字段映射失败:', e)
    } finally {
      setExtMappingLoading(false)
    }
  }, [id])

  const handleExtDataSourceChange = async (dsId: number) => {
    setSelectedExtDataSourceId(dsId)
    setSelectedExtTable(null)
    setExtTables([])
    setExtTableColumns([])
    await loadExtTables(dsId)
  }

  const handleExtTableChange = async (tableName: string) => {
    setSelectedExtTable(tableName)
    if (selectedExtDataSourceId) {
      await loadExtTableColumns(selectedExtDataSourceId, tableName)
    }
  }

  const handleDragFieldToComponent = (column: TableColumnInfo) => {
    if (!selectedComponent) {
      message.warning('请先选择目标组件')
      return
    }
    const newMapping: FieldMapping = {
      appId: currentApp?.id || 0,
      dataSourceId: selectedExtDataSourceId || 0,
      pageId: Number(id),
      sourceTable: selectedExtTable || '',
      sourceField: column.columnName,
      sourceType: column.typeName,
      targetComponent: selectedComponent.componentType,
      targetComponentId: selectedComponent.componentId,
      targetProperty: 'value',
      mappingType: 'DIRECT',
      sortOrder: extMappings.length,
    }
    setExtMappings(prev => [...prev, newMapping])
    message.success(`已将字段 ${column.columnName} 映射到 ${selectedComponent.componentName}`)
  }

  const handleSaveExtMappings = async () => {
    if (!id || id === 'undefined') return
    try {
      setExtMappingLoading(true)
      const res: any = await fieldMappingApi.saveBatchByPage(Number(id), extMappings)
      if (res.code === 0 || res.code === 200) {
        message.success('字段映射保存成功')
      }
    } catch (e: any) {
      message.error('保存失败: ' + e.message)
    } finally {
      setExtMappingLoading(false)
    }
  }

  const handleRemoveExtMapping = (index: number) => {
    setExtMappings(prev => prev.filter((_, i) => i !== index))
  }

  const handleApplyExtMappings = async () => {
    if (!page) return
    const newComponents = page.components?.map(comp => {
      const compMappings = extMappings.filter(m => m.targetComponentId === comp.componentId)
      if (compMappings.length === 0) return comp
      const dataSourceConfig = compMappings.reduce((acc, m) => {
        acc[m.targetProperty] = {
          dataSourceId: m.dataSourceId,
          sourceTable: m.sourceTable,
          sourceField: m.sourceField,
          mappingType: m.mappingType,
        }
        return acc
      }, {} as Record<string, any>)
      return {
        ...comp,
        dataSourceConfig: JSON.stringify({
          ...(comp.dataSourceConfig ? JSON.parse(comp.dataSourceConfig) : {}),
          ...dataSourceConfig,
          conditionExpression: conditionExpr || undefined,
        }),
      }
    }) || []
    setPage({ ...page, components: newComponents })
    message.success('字段映射已应用到组件配置')
    if (conditionExpr) {
      try {
        const res: any = await expressionApi.runtimeEvaluate(conditionExpr)
        if ((res.code === 0 || res.code === 200) && res.data?.success) {
          message.info(`条件表达式执行结果: ${JSON.stringify(res.data.result)} (${res.data.resultType})`)
        }
      } catch {}
    }
  }

  useEffect(() => {
    loadPage()
    loadComponentLibrary()
    loadDataModels()
    loadExtDataSources()
    loadExtMappings()
    loadExprFunctions()
    const createAiSession = async () => {
      try {
        const { aiPageApi } = await import('@/api/page')
        const res = await aiPageApi.createSession()
        setAiSessionId(res.data || '')
      } catch (e) {
        console.error('Create AI session failed:', e)
      }
    }
    createAiSession()
  }, [loadPage, loadComponentLibrary, loadDataModels, loadExtDataSources, loadExtMappings, loadExprFunctions])

  const buildComponentTree = (components: PageComponent[]): any[] => {
    const map = new Map()
    const roots: any[] = []

    components.forEach(comp => {
      map.set(comp.componentId, { ...comp, children: [], title: comp.componentName, key: comp.componentId })
    })

    components.forEach(comp => {
      const node = map.get(comp.componentId)
      if (comp.parentId && map.has(comp.parentId)) {
        map.get(comp.parentId).children.push(node)
      } else {
        roots.push(node)
      }
    })

    return roots
  }

  const saveHistory = useCallback(() => {
    const newHistory = history.slice(0, historyIndex + 1)
    newHistory.push(JSON.parse(JSON.stringify(page?.components || [])))
    setHistory(newHistory)
    setHistoryIndex(newHistory.length - 1)
  }, [page, history, historyIndex])

  const handleUndo = () => {
    if (historyIndex > 0) {
      const newIndex = historyIndex - 1
      setHistoryIndex(newIndex)
      if (page) {
        setPage({ ...page, components: history[newIndex] })
        const tree = buildComponentTree(history[newIndex])
        setComponentTree(tree)
      }
    }
  }

  const handleRedo = () => {
    if (historyIndex < history.length - 1) {
      const newIndex = historyIndex + 1
      setHistoryIndex(newIndex)
      if (page) {
        setPage({ ...page, components: history[newIndex] })
        const tree = buildComponentTree(history[newIndex])
        setComponentTree(tree)
      }
    }
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      if (!page) return
      const data = {
        ...page,
        ...values,
        isHome: values.isHome ? 1 : 0,
      }
      if (page.id) {
        await pageApi.update(data)
        message.success('保存成功')
      } else {
        const res = await pageApi.save(data)
        setPage(res.data)
        message.success('创建成功')
        navigate(`/page/designer/${res.data.id}`, { replace: true })
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handlePublish = async () => {
    if (!page?.id) return
    try {
      await pageApi.publish(page.id)
      message.success('发布成功')
      loadPage()
    } catch (e) {
      console.error(e)
    }
  }

  const [exprEvalResults, setExprEvalResults] = useState<Record<string, { visible: boolean; mappingValue: any }>>({})

  const evaluateComponentExpressions = useCallback(async () => {
    if (!page?.components) return
    const items: { id: string; expression: string; context: Record<string, any> }[] = []
    for (const comp of page.components) {
      if (!comp.dataSourceConfig) continue
      try {
        const config = JSON.parse(comp.dataSourceConfig)
        if (config.conditionExpression) {
          items.push({
            id: `${comp.componentId}__condition`,
            expression: config.conditionExpression,
            context: config.context || {},
          })
        }
        if (config.mappingExpression && config.type === 'expression') {
          items.push({
            id: `${comp.componentId}__mapping`,
            expression: config.mappingExpression,
            context: config.context || {},
          })
        }
      } catch {}
    }
    if (items.length === 0) return
    try {
      const res: any = await expressionApi.executeBatch(items)
      if (res.code === 0 || res.code === 200) {
        const newResults: Record<string, { visible: boolean; mappingValue: any }> = {}
        const results = res.data?.results || []
        for (const r of results) {
          const [compId, type] = r.id.split('__')
          if (!newResults[compId]) newResults[compId] = { visible: true, mappingValue: undefined }
          if (type === 'condition') {
            newResults[compId].visible = r.success ? !!r.result : true
          } else if (type === 'mapping') {
            newResults[compId].mappingValue = r.success ? r.result : undefined
          }
        }
        setExprEvalResults(newResults)
      }
    } catch (e) {
      console.error('表达式批量执行失败:', e)
    }
  }, [page])

  const handlePreview = async () => {
    if (!page?.id) {
      message.warning('请先保存页面')
      return
    }
    setPreviewVisible(true)
    evaluateComponentExpressions()
  }

  const handleViewCode = async () => {
    if (!page?.id) return
    try {
      const res = await pageApi.generateCode(page.id)
      setCodeContent(res.data || '')
      setCodeModalVisible(true)
    } catch (e) {
      console.error(e)
    }
  }

  const handlePageGenerated = useCallback((pageData: { pageName: string; components: any[] }) => {
    if (!page) return
    saveHistory()

    const validComponents = pageData.components.map((comp: any, index: number) => ({
      ...comp,
      componentId: comp.componentId || `comp_${Date.now()}_${index}`,
      sortOrder: comp.sortOrder || index + 1,
      propsConfig: typeof comp.propsConfig === 'string' ? comp.propsConfig : JSON.stringify(comp.propsConfig || {}),
      styleConfig: typeof comp.styleConfig === 'string' ? comp.styleConfig : JSON.stringify(comp.styleConfig || {}),
      eventConfig: typeof comp.eventConfig === 'string' ? comp.eventConfig : JSON.stringify(comp.eventConfig || []),
      dataSourceConfig: typeof comp.dataSourceConfig === 'string' ? comp.dataSourceConfig : JSON.stringify(comp.dataSourceConfig || {}),
      validationConfig: typeof comp.validationConfig === 'string' ? comp.validationConfig : JSON.stringify(comp.validationConfig || []),
    }))

    setPage({
      ...page,
      pageName: pageData.pageName || page.pageName,
      components: validComponents,
    })

    form.setFieldsValue({
      pageName: pageData.pageName || page.pageName,
    })

    const tree = buildComponentTree(validComponents)
    setComponentTree(tree)
    setSelectedComponentId(null)
    message.success(`页面生成成功，共 ${validComponents.length} 个组件`)
  }, [page, form])

  const handleDropComponent = (item: any, parentId?: string) => {
    if (!page) return
    saveHistory()

    const newComponent: PageComponent = {
      componentId: `comp_${Date.now()}`,
      componentName: item.componentName,
      componentType: item.componentType,
      componentVersion: item.isCustom ? item.currentVersion : undefined,
      parentId: parentId,
      sortOrder: (page.components?.length || 0) + 1,
      propsConfig: item.defaultProps || '{}',
      styleConfig: '{}',
      eventConfig: '[]',
      dataSourceConfig: '{}',
      validationConfig: '[]',
    }

    const newComponents = [...(page.components || []), newComponent]
    setPage({ ...page, components: newComponents })
    const tree = buildComponentTree(newComponents)
    setComponentTree(tree)
    setSelectedComponentId(newComponent.componentId)
    loadComponentForms(newComponent)
  }

  const handleCanvasDrop = useCallback((item: any) => {
    handleDropComponent(item)
  }, [page])

  const [{ isOver }, canvasDrop] = useDrop(() => ({
    accept: 'COMPONENT',
    drop: (item: any) => handleCanvasDrop(item),
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  }))

  const handleDeleteComponent = (componentId: string) => {
    if (!page) return
    saveHistory()

    const deleteRecursive = (id: string, components: PageComponent[]): PageComponent[] => {
      return components.filter(comp => {
        if (comp.componentId === id) return false
        if (comp.parentId === id) return false
        return true
      })
    }

    const newComponents = deleteRecursive(componentId, page.components || [])
    setPage({ ...page, components: newComponents })
    const tree = buildComponentTree(newComponents)
    setComponentTree(tree)
    if (selectedComponentId === componentId) {
      setSelectedComponentId(null)
    }
  }

  const handleSelectComponent = (componentId: string) => {
    setSelectedComponentId(componentId)
    const component = page?.components?.find(c => c.componentId === componentId)
    if (component) {
      loadComponentForms(component)
    }
  }

  const loadComponentForms = (component: PageComponent) => {
    const props = component.propsConfig ? JSON.parse(component.propsConfig) : {}
    const style = component.styleConfig ? JSON.parse(component.styleConfig) : {}
    const events = component.eventConfig ? JSON.parse(component.eventConfig) : []
    const dataSource = component.dataSourceConfig ? JSON.parse(component.dataSourceConfig) : {}
    const validation = component.validationConfig ? JSON.parse(component.validationConfig) : []
    const permission = component.permissionConfig ? JSON.parse(component.permissionConfig) : {}

    propsForm.setFieldsValue(props)
    styleForm.setFieldsValue(style)
    eventForm.setFieldsValue({ events })
    dataSourceForm.setFieldsValue(dataSource)
    validationForm.setFieldsValue({ validation })
    permissionForm.setFieldsValue(permission)
  }

  const handlePropsChange = async () => {
    try {
      const values = await propsForm.validateFields()
      updateSelectedComponent({ propsConfig: JSON.stringify(values) })
      message.success('属性更新成功')
    } catch (e) {
      console.error(e)
    }
  }

  const handleStyleChange = async () => {
    try {
      const values = await styleForm.validateFields()
      updateSelectedComponent({ styleConfig: JSON.stringify(values) })
      message.success('样式更新成功')
    } catch (e) {
      console.error(e)
    }
  }

  const handleEventChange = async () => {
    try {
      const values = await eventForm.validateFields()
      updateSelectedComponent({ eventConfig: JSON.stringify(values.events || []) })
      message.success('事件更新成功')
    } catch (e) {
      console.error(e)
    }
  }

  const handleDataSourceChange = async () => {
    try {
      const values = await dataSourceForm.validateFields()
      const config = {
        ...values,
        mappingExpression: mappingExpr,
        conditionExpression: conditionExpr,
      }
      updateSelectedComponent({ dataSourceConfig: JSON.stringify(config) })
      message.success('数据源更新成功')
    } catch (e) {
      console.error(e)
    }
  }

  const handleValidationChange = async () => {
    try {
      const values = await validationForm.validateFields()
      updateSelectedComponent({ validationConfig: JSON.stringify(values.validation || []) })
      message.success('校验规则更新成功')
    } catch (e) {
      console.error(e)
    }
  }

  const handlePermissionChange = async () => {
    try {
      const values = await permissionForm.validateFields()
      updateSelectedComponent({ permissionConfig: JSON.stringify(values) })
      message.success('权限配置更新成功')
    } catch (e) {
      console.error(e)
    }
  }

  const updateSelectedComponent = (updates: Partial<PageComponent>) => {
    if (!page || !selectedComponentId) return
    saveHistory()

    const newComponents = page.components?.map(comp =>
      comp.componentId === selectedComponentId ? { ...comp, ...updates } : comp
    ) || []
    setPage({ ...page, components: newComponents })
    const tree = buildComponentTree(newComponents)
    setComponentTree(tree)
  }

  const selectedComponent = page?.components?.find(c => c.componentId === selectedComponentId)

  useEffect(() => {
    if (selectedComponent?.dataSourceConfig) {
      try {
        const config = JSON.parse(selectedComponent.dataSourceConfig)
        setMappingExpr(config.mappingExpression || '')
        setConditionExpr(config.conditionExpression || '')
      } catch {
        setMappingExpr('')
        setConditionExpr('')
      }
    } else {
      setMappingExpr('')
      setConditionExpr('')
    }
  }, [selectedComponent?.componentId, selectedComponent?.dataSourceConfig])

  const CustomComponentPropsPanel: React.FC<{ componentType: string }> = ({ componentType }) => {
    const { schema, loading, error } = useCustomComponentSchema(componentType)
    const [formilyForm] = useState(() => createForm())

    useEffect(() => {
      if (selectedComponent && schema?.propSchema) {
        const props = selectedComponent.propsConfig ? JSON.parse(selectedComponent.propsConfig) : {}
        formilyForm.setInitialValues(props)
        formilyForm.reset()
      }
    }, [selectedComponent, schema, formilyForm])

    const handleCustomPropsSave = async () => {
      try {
        await formilyForm.submit()
        const values = formilyForm.getValues()
        updateSelectedComponent({ propsConfig: JSON.stringify(values) })
        message.success('属性更新成功')
      } catch (e: any) {
        console.error('Save props error:', e)
        message.error('属性保存失败: ' + (e.message || e))
      }
    }

    if (loading) {
      return <div style={{ padding: 16, textAlign: 'center' }}><Spin size="small" /> 加载属性配置中...</div>
    }

    if (error) {
      return <Alert type="error" message="加载属性配置失败" description={error} showIcon />
    }

    if (!schema?.propSchema) {
      return <Alert type="info" message="该组件没有定义属性配置" showIcon />
    }

    const renderSchemaField = () => {
      try {
        return <SchemaField schema={schema.propSchema} />
      } catch (e) {
        console.error('Schema render error:', e)
        return <Alert type="error" message="属性Schema格式错误" showIcon />
      }
    }

    return (
      <FormProvider form={formilyForm}>
        {renderSchemaField()}
        <Button type="primary" block onClick={handleCustomPropsSave} style={{ marginTop: 16 }}>
          保存属性
        </Button>
      </FormProvider>
    )
  }

  const getPropsSchema = () => {
    const type = selectedComponent?.componentType

    const systemTypes = ['INPUT', 'TEXTAREA', 'NUMBER', 'SELECT', 'DATE', 'DATETIME', 'TIME', 'SWITCH', 'CHECKBOX', 'RADIO', 'UPLOAD', 'RICHTEXT', 'TABLE', 'BUTTON', 'LINK', 'IMAGE', 'TEXT', 'TITLE', 'ICON', 'DIVIDER', 'TABS', 'CARD', 'GRID', 'FLEX', 'MODAL', 'FORM', 'STEPS', 'TIMELINE', 'PROGRESS', 'RATE', 'SLIDER', 'LINECHART', 'BARCHART', 'PIECHART', 'AREACHART', 'SCATTERCHART', 'RADARCHART']

    if (type && !systemTypes.includes(type)) {
      return {
        type: 'object',
        properties: {
          __customComponent: { type: 'string', title: '自定义组件', 'x-decorator': 'FormItem', 'x-component': 'Input' },
        },
        __isCustom: true,
      }
    }

    const baseProps: any = {
      type: 'object',
      properties: {},
    }

    const commonProps = {
      visible: { type: 'boolean', title: '是否显示', default: true, 'x-decorator': 'FormItem', 'x-component': 'Switch' },
      disabled: { type: 'boolean', title: '是否禁用', default: false, 'x-decorator': 'FormItem', 'x-component': 'Switch' },
    }

    switch (type) {
      case 'INPUT':
      case 'TEXTAREA':
        baseProps.properties = {
          placeholder: { type: 'string', title: '占位符', 'x-decorator': 'FormItem', 'x-component': 'Input' },
          defaultValue: { type: 'string', title: '默认值', 'x-decorator': 'FormItem', 'x-component': 'Input' },
          maxLength: { type: 'number', title: '最大长度', 'x-decorator': 'FormItem', 'x-component': 'NumberPicker' },
          ...commonProps,
        }
        if (type === 'TEXTAREA') {
          baseProps.properties.rows = { type: 'number', title: '行数', default: 3, 'x-decorator': 'FormItem', 'x-component': 'NumberPicker' }
        }
        break
      case 'NUMBER':
        baseProps.properties = {
          placeholder: { type: 'string', title: '占位符', 'x-decorator': 'FormItem', 'x-component': 'Input' },
          defaultValue: { type: 'number', title: '默认值', 'x-decorator': 'FormItem', 'x-component': 'NumberPicker' },
          min: { type: 'number', title: '最小值', 'x-decorator': 'FormItem', 'x-component': 'NumberPicker' },
          max: { type: 'number', title: '最大值', 'x-decorator': 'FormItem', 'x-component': 'NumberPicker' },
          step: { type: 'number', title: '步长', default: 1, 'x-decorator': 'FormItem', 'x-component': 'NumberPicker' },
          ...commonProps,
        }
        break
      case 'SELECT':
        baseProps.properties = {
          placeholder: { type: 'string', title: '占位符', 'x-decorator': 'FormItem', 'x-component': 'Input' },
          multiple: { type: 'boolean', title: '多选', default: false, 'x-decorator': 'FormItem', 'x-component': 'Switch' },
          ...commonProps,
        }
        break
      case 'BUTTON':
        baseProps.properties = {
          text: { type: 'string', title: '按钮文本', default: '按钮', 'x-decorator': 'FormItem', 'x-component': 'Input' },
          type: {
            type: 'string',
            title: '按钮类型',
            default: 'primary',
            'x-decorator': 'FormItem',
            'x-component': 'Select',
            enum: [
              { label: '主要', value: 'primary' },
              { label: '默认', value: 'default' },
              { label: '虚线', value: 'dashed' },
              { label: '链接', value: 'link' },
              { label: '文本', value: 'text' },
            ],
          },
          ...commonProps,
        }
        break
      case 'TITLE':
        baseProps.properties = {
          text: { type: 'string', title: '标题文本', default: '标题', 'x-decorator': 'FormItem', 'x-component': 'Input' },
          level: {
            type: 'number',
            title: '标题级别',
            default: 1,
            'x-decorator': 'FormItem',
            'x-component': 'NumberPicker',
            'x-component-props': { min: 1, max: 6 },
          },
          ...commonProps,
        }
        break
      case 'TEXT':
        baseProps.properties = {
          text: { type: 'string', title: '文本内容', default: '文本内容', 'x-decorator': 'FormItem', 'x-component': 'Input.TextArea' },
          ...commonProps,
        }
        break
      case 'IMAGE':
        baseProps.properties = {
          src: { type: 'string', title: '图片地址', 'x-decorator': 'FormItem', 'x-component': 'Input' },
          alt: { type: 'string', title: '替代文本', 'x-decorator': 'FormItem', 'x-component': 'Input' },
          width: { type: 'number', title: '宽度', default: 200, 'x-decorator': 'FormItem', 'x-component': 'NumberPicker' },
          height: { type: 'number', title: '高度', default: 150, 'x-decorator': 'FormItem', 'x-component': 'NumberPicker' },
          ...commonProps,
        }
        break
      case 'CARD':
        baseProps.properties = {
          title: { type: 'string', title: '卡片标题', default: '卡片标题', 'x-decorator': 'FormItem', 'x-component': 'Input' },
          ...commonProps,
        }
        break
      case 'GRID':
        baseProps.properties = {
          columns: { type: 'number', title: '列数', default: 2, 'x-decorator': 'FormItem', 'x-component': 'NumberPicker', 'x-component-props': { min: 1, max: 12 } },
          gap: { type: 'number', title: '间距', default: 8, 'x-decorator': 'FormItem', 'x-component': 'NumberPicker' },
          ...commonProps,
        }
        break
      default:
        baseProps.properties = {
          ...commonProps,
        }
    }

    return baseProps
  }

  const renderPropsPanel = () => {
    if (!selectedComponent) return null

    const schema = getPropsSchema()

    if ((schema as any).__isCustom) {
      return <CustomComponentPropsPanel componentType={selectedComponent.componentType} />
    }

    const form = createForm()

    return (
      <FormProvider form={form}>
        <SchemaField schema={schema} />
        <Button type="primary" block onClick={handlePropsChange} style={{ marginTop: 16 }}>
          保存属性
        </Button>
      </FormProvider>
    )
  }

  const renderStylePanel = () => {
    if (!selectedComponent) return null

    return (
      <Form form={styleForm} layout="vertical" onFinish={handleStyleChange}>
        <Collapse defaultActiveKey={['1', '2', '3']} ghost>
          <Panel header="尺寸" key="1">
            <Form.Item name="width" label="宽度">
              <InputNumber style={{ width: '100%' }} placeholder="auto" addonAfter="px" />
            </Form.Item>
            <Form.Item name="height" label="高度">
              <InputNumber style={{ width: '100%' }} placeholder="auto" addonAfter="px" />
            </Form.Item>
            <Form.Item name="minWidth" label="最小宽度">
              <InputNumber style={{ width: '100%' }} placeholder="auto" addonAfter="px" />
            </Form.Item>
            <Form.Item name="maxWidth" label="最大宽度">
              <InputNumber style={{ width: '100%' }} placeholder="auto" addonAfter="px" />
            </Form.Item>
          </Panel>
          <Panel header="外边距" key="2">
            <Row gutter={8}>
              <Col span={12}>
                <Form.Item name="marginTop" label="上">
                  <InputNumber style={{ width: '100%' }} addonAfter="px" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="marginBottom" label="下">
                  <InputNumber style={{ width: '100%' }} addonAfter="px" />
                </Form.Item>
              </Col>
            </Row>
            <Row gutter={8}>
              <Col span={12}>
                <Form.Item name="marginLeft" label="左">
                  <InputNumber style={{ width: '100%' }} addonAfter="px" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="marginRight" label="右">
                  <InputNumber style={{ width: '100%' }} addonAfter="px" />
                </Form.Item>
              </Col>
            </Row>
          </Panel>
          <Panel header="内边距" key="3">
            <Row gutter={8}>
              <Col span={12}>
                <Form.Item name="paddingTop" label="上">
                  <InputNumber style={{ width: '100%' }} addonAfter="px" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="paddingBottom" label="下">
                  <InputNumber style={{ width: '100%' }} addonAfter="px" />
                </Form.Item>
              </Col>
            </Row>
            <Row gutter={8}>
              <Col span={12}>
                <Form.Item name="paddingLeft" label="左">
                  <InputNumber style={{ width: '100%' }} addonAfter="px" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="paddingRight" label="右">
                  <InputNumber style={{ width: '100%' }} addonAfter="px" />
                </Form.Item>
              </Col>
            </Row>
          </Panel>
          <Panel header="背景与边框" key="4">
            <Form.Item name="backgroundColor" label="背景颜色">
              <ColorPicker showText />
            </Form.Item>
            <Form.Item name="borderRadius" label="圆角">
              <Slider min={0} max={50} />
            </Form.Item>
            <Form.Item name="borderStyle" label="边框样式">
              <Select>
                <Option value="none">无</Option>
                <Option value="solid">实线</Option>
                <Option value="dashed">虚线</Option>
                <Option value="dotted">点线</Option>
              </Select>
            </Form.Item>
            <Form.Item name="borderColor" label="边框颜色">
              <ColorPicker showText />
            </Form.Item>
          </Panel>
          <Panel header="排版" key="5">
            <Form.Item name="fontSize" label="字体大小">
              <Slider min={12} max={72} />
            </Form.Item>
            <Form.Item name="fontWeight" label="字体粗细">
              <Select>
                <Option value="normal">常规</Option>
                <Option value="bold">粗体</Option>
                <Option value="100">100</Option>
                <Option value="300">300</Option>
                <Option value="500">500</Option>
                <Option value="700">700</Option>
                <Option value="900">900</Option>
              </Select>
            </Form.Item>
            <Form.Item name="textAlign" label="文字对齐">
              <Radio.Group>
                <Radio value="left">左</Radio>
                <Radio value="center">中</Radio>
                <Radio value="right">右</Radio>
              </Radio.Group>
            </Form.Item>
            <Form.Item name="color" label="文字颜色">
              <ColorPicker showText />
            </Form.Item>
          </Panel>
        </Collapse>
        <Form.Item style={{ marginTop: 16 }}>
          <Button type="primary" htmlType="submit" block>
            保存样式
          </Button>
        </Form.Item>
      </Form>
    )
  }

  const renderPermissionPanel = () => {
    if (!selectedComponent) return null

    return (
      <Form form={permissionForm} layout="vertical" onFinish={handlePermissionChange}>
        <Alert
          type="info"
          message="组件权限配置"
          description="配置组件的可见性和操作权限。不配置表示不限制。"
          showIcon
          style={{ marginBottom: 16 }}
        />
        <Card size="small" title="可见性控制" style={{ marginBottom: 12 }}>
          <Form.Item name="enablePermission" label="启用权限控制" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="permissionCode" label="权限编码" tooltip="对应后端的权限编码，如 system:user:add">
            <Input placeholder="例如：system:user:list" />
          </Form.Item>
          <Form.Item name="requiredRoles" label="需要角色" tooltip="多个角色用逗号分隔，满足任意一个即可">
            <Input placeholder="例如：admin, editor" />
          </Form.Item>
          <Form.Item name="visibilityMode" label="控制方式">
            <Select>
              <Option value="hide">无权限时隐藏</Option>
              <Option value="disable">无权限时禁用</Option>
            </Select>
          </Form.Item>
        </Card>
        <Card size="small" title="字段权限（表单字段）">
          <Form.Item name="enableFieldPermission" label="启用字段级权限" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="fieldId" label="绑定字段ID" tooltip="绑定到数据模型字段，从权限配置读取">
            <Input placeholder="数据模型字段ID" />
          </Form.Item>
          <Form.Item name="fieldAction" label="字段操作">
            <Select>
              <Option value="view">查看权限</Option>
              <Option value="edit">编辑权限</Option>
            </Select>
          </Form.Item>
        </Card>
        <Form.Item style={{ marginTop: 16 }}>
          <Button type="primary" htmlType="submit" block>
            保存权限配置
          </Button>
        </Form.Item>
      </Form>
    )
  }

  const getAvailableEvents = () => {
    if (!selectedComponent) return []

    const type = selectedComponent.componentType
    const systemTypes = ['INPUT', 'TEXTAREA', 'NUMBER', 'SELECT', 'DATE', 'DATETIME', 'TIME', 'SWITCH', 'CHECKBOX', 'RADIO', 'UPLOAD', 'RICHTEXT', 'TABLE', 'BUTTON', 'LINK', 'IMAGE', 'TEXT', 'TITLE', 'ICON', 'DIVIDER', 'TABS', 'CARD', 'GRID', 'FLEX', 'MODAL', 'FORM', 'STEPS', 'TIMELINE', 'PROGRESS', 'RATE', 'SLIDER', 'LINECHART', 'BARCHART', 'PIECHART', 'AREACHART', 'SCATTERCHART', 'RADARCHART']

    if (type && !systemTypes.includes(type)) {
      const customSchema = customComponentSchemas[type]
      if (customSchema?.exposedEvents && customSchema.exposedEvents.length > 0) {
        return customSchema.exposedEvents.map((eventName: string) => ({
          value: eventName,
          label: customSchema.eventSchema?.[eventName]?.title || eventName,
          icon: customSchema.eventSchema?.[eventName] ? '⚡' : '📢',
        }))
      }
      return [
        { value: 'onClick', label: '点击事件', icon: '👆' },
        { value: 'onChange', label: '值改变', icon: '🔄' },
      ]
    }

    return [
      { value: 'onClick', label: '点击事件', icon: '👆' },
      { value: 'onChange', label: '值改变', icon: '🔄' },
      { value: 'onLoad', label: '加载完成', icon: '📥' },
      { value: 'onFocus', label: '获得焦点', icon: '🎯' },
      { value: 'onBlur', label: '失去焦点', icon: '💨' },
      { value: 'onMouseEnter', label: '鼠标移入', icon: '🖱️' },
      { value: 'onMouseLeave', label: '鼠标移出', icon: '🖱️' },
    ]
  }

  const renderEventPanel = () => {
    if (!selectedComponent) return null

    const eventTypes = getAvailableEvents()
    const actionTypes = [
      { value: 'navigate', label: '页面跳转', icon: '🔗' },
      { value: 'api', label: 'API调用', icon: '🌐' },
      { value: 'submit', label: '提交表单', icon: '📤' },
      { value: 'reset', label: '重置表单', icon: '🔄' },
      { value: 'modal', label: '打开弹窗', icon: '🪟' },
      { value: 'message', label: '消息提示', icon: '💬' },
      { value: 'confirm', label: '确认对话框', icon: '❓' },
      { value: 'download', label: '下载文件', icon: '📥' },
      { value: 'print', label: '打印页面', icon: '🖨️' },
      { value: 'custom', label: '自定义代码', icon: '⌨️' },
    ]

    return (
      <Form form={eventForm} layout="vertical" onFinish={handleEventChange}>
        {selectedComponent.componentVersion && (
          <Alert 
            type="info" 
            message={`组件版本: ${selectedComponent.componentVersion}`} 
            showIcon 
            style={{ marginBottom: 12 }} 
          />
        )}
        <Form.List name="events">
          {(fields, { add, remove }) => (
            <>
              {fields.map(({ key, name, ...restField }) => (
                <Card
                  key={key}
                  size="small"
                  style={{ marginBottom: 12 }}
                  extra={
                    <Button type="text" danger size="small" onClick={() => remove(name)}>
                      删除
                    </Button>
                  }
                >
                  <Form.Item
                    {...restField}
                    name={[name, 'eventType']}
                    label="触发事件"
                    rules={[{ required: true, message: '请选择事件类型' }]}
                  >
                    <Select placeholder="选择事件类型">
                      {eventTypes.map(e => (
                        <Option key={e.value} value={e.value}>
                          {e.icon} {e.label}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                  <Form.Item
                    {...restField}
                    name={[name, 'actionType']}
                    label="执行动作"
                    rules={[{ required: true, message: '请选择动作类型' }]}
                  >
                    <Select placeholder="选择动作类型">
                      {actionTypes.map(a => (
                        <Option key={a.value} value={a.value}>
                          {a.icon} {a.label}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                  <Form.Item
                    {...restField}
                    name={[name, 'actionConfig']}
                    label="动作配置"
                  >
                    <Input.TextArea rows={3} placeholder="例如：{\"url\": \"/api/list\", \"method\": \"GET\"}" />
                  </Form.Item>
                  <Form.Item
                    {...restField}
                    name={[name, 'condition']}
                    label="执行条件"
                    tooltip="满足条件时才执行此事件动作，留空则无条件执行"
                  >
                    <ExpressionEditor
                      fields={extTableColumns.map(col => ({
                        name: col.columnName,
                        type: col.typeName,
                        table: selectedExtTable || undefined,
                      }))}
                      functions={exprFunctions}
                      categories={exprCategories}
                      onValidate={async (expr) => {
                        try {
                          const res: any = await expressionApi.validate(expr)
                          if (res.code === 0 || res.code === 200) return res.data
                          return { valid: false, errors: ['校验失败'], warnings: [] }
                        } catch { return { valid: false, errors: ['校验失败'], warnings: [] } }
                      }}
                      onExecute={async (expr, ctx) => {
                        try {
                          const res: any = await expressionApi.runtimeEvaluate(expr, ctx)
                          if (res.code === 0 || res.code === 200) return res.data
                          return { success: false, result: null, error: '执行失败', duration: 0, resultType: 'null' }
                        } catch { return { success: false, result: null, error: '执行失败', duration: 0, resultType: 'null' } }
                      }}
                      mode="condition"
                      placeholder='如: ${status} == 1 && ${age} >= 18'
                    />
                  </Form.Item>
                </Card>
              ))}
              <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add()}>
                添加事件
              </Button>
            </>
          )}
        </Form.List>
        <Form.Item style={{ marginTop: 16 }}>
          <Button type="primary" htmlType="submit" block>
            保存事件
          </Button>
        </Form.Item>
      </Form>
    )
  }

  const renderDataSourcePanel = () => {
    if (!selectedComponent) return null

    const dataSourceTypes = [
      { value: 'model', label: '数据模型', icon: '🗄️' },
      { value: 'api', label: '自定义API', icon: '🌐' },
      { value: 'static', label: '静态数据', icon: '📋' },
      { value: 'variable', label: '页面变量', icon: '📦' },
      { value: 'expression', label: '表达式', icon: '⚙️' },
    ]

    const exprFields = extTableColumns.map(col => ({
      name: col.columnName,
      type: col.typeName,
      table: selectedExtTable || undefined,
      dataSourceId: selectedExtDataSourceId || undefined,
    }))

    const handleExprValidate = async (expr: string) => {
      try {
        const res: any = await expressionApi.validate(expr)
        if (res.code === 0 || res.code === 200) return res.data
        return { valid: false, errors: ['校验请求失败'], warnings: [] }
      } catch {
        return { valid: false, errors: ['校验请求失败'], warnings: [] }
      }
    }

    const handleExprExecute = async (expr: string, context: Record<string, any>) => {
      try {
        const res: any = await expressionApi.runtimeEvaluate(expr, context)
        if (res.code === 0 || res.code === 200) return res.data
        return { success: false, result: null, error: '执行请求失败', duration: 0, resultType: 'null' }
      } catch {
        return { success: false, result: null, error: '执行请求失败', duration: 0, resultType: 'null' }
      }
    }

    return (
      <Form form={dataSourceForm} layout="vertical" onFinish={handleDataSourceChange}>
        <Form.Item name="type" label="数据源类型" rules={[{ required: true, message: '请选择数据源类型' }]}>
          <Select placeholder="选择数据源类型">
            {dataSourceTypes.map(d => (
              <Option key={d.value} value={d.value}>
                {d.icon} {d.label}
              </Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item noStyle shouldUpdate={(prev, curr) => prev.type !== curr.type}>
          {({ getFieldValue }) => {
            const type = getFieldValue('type')
            if (type === 'model') {
              return (
                <>
                  <Form.Item name="modelId" label="选择数据模型" rules={[{ required: true }]}>
                    <Select placeholder="选择数据模型">
                      {dataModels.map(m => (
                        <Option key={m.id} value={m.id}>{m.modelName} ({m.tableName})</Option>
                      ))}
                    </Select>
                  </Form.Item>
                  <Form.Item name="operation" label="操作类型" rules={[{ required: true }]}>
                    <Select>
                      <Option value="list">查询列表</Option>
                      <Option value="page">分页查询</Option>
                      <Option value="get">查询详情</Option>
                      <Option value="save">新增</Option>
                      <Option value="update">更新</Option>
                      <Option value="delete">删除</Option>
                    </Select>
                  </Form.Item>
                  <Form.Item name="fieldMapping" label="字段映射">
                    <ExpressionEditor
                      value={mappingExpr}
                      onChange={(val) => setMappingExpr(val)}
                      fields={exprFields}
                      functions={exprFunctions}
                      categories={exprCategories}
                      onValidate={handleExprValidate}
                      onExecute={handleExprExecute}
                      mode="mapping"
                      placeholder='如: ${userName} 或 CONCAT(${firstName}, " ", ${lastName})'
                    />
                  </Form.Item>
                </>
              )
            }
            if (type === 'api') {
              return (
                <>
                  <Form.Item name="apiUrl" label="API地址" rules={[{ required: true }]}>
                    <Input placeholder="例如：/api/user/list" />
                  </Form.Item>
                  <Form.Item name="method" label="请求方法" rules={[{ required: true }]}>
                    <Select>
                      <Option value="GET">GET</Option>
                      <Option value="POST">POST</Option>
                      <Option value="PUT">PUT</Option>
                      <Option value="DELETE">DELETE</Option>
                    </Select>
                  </Form.Item>
                  <Form.Item name="params" label="请求参数">
                    <Input.TextArea rows={4} placeholder='例如：{"page": 1, "size": 10}' />
                  </Form.Item>
                </>
              )
            }
            if (type === 'static') {
              return (
                <Form.Item name="staticData" label="静态数据" rules={[{ required: true }]}>
                  <Input.TextArea rows={6} placeholder='例如：[{"label": "选项1", "value": "1"}, {"label": "选项2", "value": "2"}]' />
                </Form.Item>
              )
            }
            if (type === 'expression') {
              return (
                <>
                  <Form.Item name="expression" label="数据表达式">
                    <ExpressionEditor
                      value={mappingExpr}
                      onChange={(val) => setMappingExpr(val)}
                      fields={exprFields}
                      functions={exprFunctions}
                      categories={exprCategories}
                      onValidate={handleExprValidate}
                      onExecute={handleExprExecute}
                      mode="mapping"
                      placeholder='如: ${price} * ${quantity} 或 IF(${discount} > 0, ${price} * ${discount}, ${price})'
                    />
                  </Form.Item>
                  <Form.Item name="conditionExpression" label="条件表达式（可选）">
                    <ExpressionEditor
                      value={conditionExpr}
                      onChange={(val) => setConditionExpr(val)}
                      fields={exprFields}
                      functions={exprFunctions}
                      categories={exprCategories}
                      onValidate={handleExprValidate}
                      onExecute={handleExprExecute}
                      mode="condition"
                      placeholder='如: ${status} == 1 && ${age} >= 18'
                    />
                  </Form.Item>
                </>
              )
            }
            return null
          }}
        </Form.Item>
        <Form.Item style={{ marginTop: 16 }}>
          <Button type="primary" htmlType="submit" block>
            保存数据源
          </Button>
        </Form.Item>
      </Form>
    )
  }

  const renderValidationPanel = () => {
    if (!selectedComponent) return null

    const validationTypes = [
      { value: 'required', label: '必填', icon: '✅' },
      { value: 'email', label: '邮箱格式', icon: '📧' },
      { value: 'phone', label: '手机号格式', icon: '📱' },
      { value: 'idCard', label: '身份证号', icon: '🆔' },
      { value: 'url', label: 'URL格式', icon: '🔗' },
      { value: 'number', label: '数字格式', icon: '🔢' },
      { value: 'integer', label: '整数格式', icon: '🔟' },
      { value: 'pattern', label: '正则表达式', icon: '🔍' },
      { value: 'minLength', label: '最小长度', icon: '📏' },
      { value: 'maxLength', label: '最大长度', icon: '📏' },
      { value: 'min', label: '最小值', icon: '⬇️' },
      { value: 'max', label: '最大值', icon: '⬆️' },
    ]

    return (
      <Form form={validationForm} layout="vertical" onFinish={handleValidationChange}>
        <Form.List name="validation">
          {(fields, { add, remove }) => (
            <>
              {fields.map(({ key, name, ...restField }) => (
                <Card
                  key={key}
                  size="small"
                  style={{ marginBottom: 12 }}
                  extra={
                    <Button type="text" danger size="small" onClick={() => remove(name)}>
                      删除
                    </Button>
                  }
                >
                  <Form.Item
                    {...restField}
                    name={[name, 'type']}
                    label="校验类型"
                    rules={[{ required: true, message: '请选择校验类型' }]}
                  >
                    <Select placeholder="选择校验类型">
                      {validationTypes.map(v => (
                        <Option key={v.value} value={v.value}>
                          {v.icon} {v.label}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                  <Form.Item
                    {...restField}
                    name={[name, 'value']}
                    label="校验值"
                  >
                    <Input placeholder="例如：正则表达式、长度值等" />
                  </Form.Item>
                  <Form.Item
                    {...restField}
                    name={[name, 'message']}
                    label="错误提示"
                  >
                    <Input placeholder="例如：请输入正确的邮箱格式" />
                  </Form.Item>
                </Card>
              ))}
              <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add()}>
                添加校验规则
              </Button>
            </>
          )}
        </Form.List>
        <Form.Item style={{ marginTop: 16 }}>
          <Button type="primary" htmlType="submit" block>
            保存校验规则
          </Button>
        </Form.Item>
      </Form>
    )
  }

  const renderFieldMappingPanel = () => {
    if (!selectedComponent) return null

    const currentCompMappings = extMappings.filter(m => m.targetComponentId === selectedComponent.componentId)
    const dsNameMap: Record<number, string> = {}
    extDataSources.forEach(ds => { if (ds.id) dsNameMap[ds.id] = ds.sourceName })

    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <Alert
          type="info"
          message="外部数据源映射"
          description="选择数据源和表，将字段拖拽或点击映射到当前组件。"
          showIcon
        />

        <Select
          placeholder="选择外部数据源"
          loading={extMappingLoading}
          value={selectedExtDataSourceId || undefined}
          onChange={handleExtDataSourceChange}
          style={{ width: '100%' }}
          allowClear
        >
          {extDataSources.map(ds => (
            <Option key={ds.id} value={ds.id}>
              {ds.sourceName} ({ds.dbType})
            </Option>
          ))}
        </Select>

        {selectedExtDataSourceId && (
          <Select
            placeholder="选择表"
            loading={extTablesLoading}
            value={selectedExtTable || undefined}
            onChange={handleExtTableChange}
            style={{ width: '100%' }}
            allowClear
          >
            {extTables.map(table => (
              <Option key={table} value={table}>
                {table}
              </Option>
            ))}
          </Select>
        )}

        {selectedExtTable && (
          <Card
            size="small"
            title={
              <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <DatabaseOutlined />
                可用字段（点击映射）
              </span>
            }
            style={{ maxHeight: 260, overflow: 'auto' }}
            loading={extColumnsLoading}
          >
            {extTableColumns.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                {extTableColumns.map((col, idx) => (
                  <div
                    key={idx}
                    style={{
                      padding: '6px 10px',
                      border: '1px solid #e8e8e8',
                      borderRadius: 4,
                      cursor: 'pointer',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      background: '#fafafa',
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
                    onClick={() => handleDragFieldToComponent(col)}
                  >
                    <span style={{ fontWeight: 500 }}>{col.columnName}</span>
                    <Tag color="blue" style={{ margin: 0 }}>{col.typeName}</Tag>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ textAlign: 'center', color: '#999', padding: 16 }}>
                该表没有字段
              </div>
            )}
          </Card>
        )}

        <Divider style={{ margin: '8px 0' }} />

        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <span style={{ fontWeight: 500 }}>
              已配置映射 ({currentCompMappings.length})
            </span>
          </div>
          <div style={{ maxHeight: 200, overflow: 'auto' }}>
            {currentCompMappings.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {currentCompMappings.map((mapping, idx) => (
                  <Card key={idx} size="small" style={{ background: '#f0f5ff' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginBottom: 4 }}>
                          <span style={{ color: '#666', fontSize: 12 }}>数据源：</span>
                          <Tag color="green">{dsNameMap[mapping.dataSourceId] || '未知'}</Tag>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginBottom: 4 }}>
                          <span style={{ color: '#666', fontSize: 12 }}>源字段：</span>
                          <span style={{ fontWeight: 500 }}>{mapping.sourceTable}.{mapping.sourceField}</span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                          <ArrowRightOutlined style={{ color: '#1890ff' }} />
                          <span style={{ color: '#666', fontSize: 12 }}>目标属性：</span>
                          <span style={{ fontWeight: 500, color: '#1890ff' }}>{mapping.targetProperty}</span>
                        </div>
                      </div>
                      <Button
                        type="text"
                        danger
                        size="small"
                        icon={<DeleteOutlined />}
                        onClick={() => handleRemoveExtMapping(extMappings.findIndex(m => m === mapping))}
                      />
                    </div>
                  </Card>
                ))}
              </div>
            ) : (
              <div style={{ textAlign: 'center', color: '#999', padding: 20, border: '1px dashed #d9d9d9', borderRadius: 4 }}>
                暂无映射配置，请从上方字段列表选择
              </div>
            )}
          </div>
        </div>

        <Divider style={{ margin: '8px 0' }} />

        <div style={{ display: 'flex', gap: 8 }}>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={handleSaveExtMappings}
            loading={extMappingLoading}
            block
          >
            保存映射
          </Button>
          <Button
            icon={<ThunderboltOutlined />}
            onClick={handleApplyExtMappings}
            block
          >
            应用到组件
          </Button>
        </div>

        <Divider style={{ margin: '8px 0' }} />

        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
            <ExperimentOutlined style={{ color: '#722ed1' }} />
            <span style={{ fontWeight: 500 }}>条件渲染表达式</span>
          </div>
          <ExpressionEditor
            value={conditionExpr}
            onChange={(val) => setConditionExpr(val)}
            fields={extTableColumns.map(col => ({
              name: col.columnName,
              type: col.typeName,
              table: selectedExtTable || undefined,
            }))}
            functions={exprFunctions}
            categories={exprCategories}
            onValidate={async (expr) => {
              try {
                const res: any = await expressionApi.validate(expr)
                if (res.code === 0 || res.code === 200) return res.data
                return { valid: false, errors: ['校验失败'], warnings: [] }
              } catch { return { valid: false, errors: ['校验失败'], warnings: [] } }
            }}
            onExecute={async (expr, ctx) => {
              try {
                const res: any = await expressionApi.runtimeEvaluate(expr, ctx)
                if (res.code === 0 || res.code === 200) return res.data
                return { success: false, result: null, error: '执行失败', duration: 0, resultType: 'null' }
              } catch { return { success: false, result: null, error: '执行失败', duration: 0, resultType: 'null' } }
            }}
            mode="condition"
            placeholder='如: ${status} == 1 && CONTAINS(${name}, "admin")'
          />
        </div>
      </div>
    )
  }

  const renderCanvas = () => {
    const width = previewMode === 'mobile' ? 375 : '100%'
    const maxWidth = previewMode === 'mobile' ? 375 : 'none'
    const margin = previewMode === 'mobile' ? '0 auto' : '0'

    return (
      <div
        ref={(node) => canvasDrop(node)}
        className="designer-canvas"
        style={{
          width: '100%',
          height: '100%',
          padding: 24,
          overflow: 'auto',
          background: isOver ? 'rgba(22, 119, 255, 0.05)' : undefined,
        }}
        onClick={() => setSelectedComponentId(null)}
      >
        <div
          style={{
            width,
            maxWidth,
            margin,
            minHeight: '100%',
            background: '#fff',
            borderRadius: 8,
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
            padding: 24,
            position: 'relative',
          }}
        >
          {previewMode === 'mobile' && (
            <div style={{ textAlign: 'center', color: '#999', marginBottom: 16, fontSize: 12 }}>
              📱 移动端预览 (375px)
            </div>
          )}
          {(!page?.components || page.components.length === 0) && (
            <div style={{
              textAlign: 'center',
              padding: '60px 20px',
              color: '#999',
              border: '2px dashed #d9d9d9',
              borderRadius: 8,
              background: '#fafafa',
            }}>
              <CodeSandboxOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />
              <div style={{ marginTop: 16, fontSize: 16 }}>从左侧拖拽组件到此处</div>
              <div style={{ fontSize: 12, marginTop: 8 }}>开始设计你的页面</div>
            </div>
          )}
          {page?.components?.filter(c => !c.parentId).map((component, index) => (
            <CanvasComponent
              key={component.componentId}
              component={component}
              index={index}
              isSelected={selectedComponentId === component.componentId}
              onSelect={() => handleSelectComponent(component.componentId)}
              onDelete={() => handleDeleteComponent(component.componentId)}
              onDrop={(item) => handleDropComponent(item, component.componentId)}
            />
          ))}
        </div>
      </div>
    )
  }

  return (
    <DndProvider backend={HTML5Backend}>
      <Layout style={{ height: '100%' }}>
        <Header
          style={{
            background: '#fff',
            borderBottom: '1px solid #e8e8e8',
            padding: '0 16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            height: 56,
            minHeight: 56,
          }}
        >
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/page')}>
              返回
            </Button>
            <Form form={form} layout="inline">
              <Form.Item name="pageName" label="页面名称" rules={[{ required: true }]}>
                <Input placeholder="请输入页面名称" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="pageCode" label="页面编码" rules={[{ required: true }]}>
                <Input placeholder="请输入页面编码" style={{ width: 150 }} />
              </Form.Item>
              <Form.Item name="pagePath" label="页面路径" rules={[{ required: true }]}>
                <Input placeholder="/user/list" style={{ width: 150 }} />
              </Form.Item>
              <Form.Item name="layoutType" label="布局类型">
                <Select style={{ width: 100 }}>
                  <Option value="FULL">全屏</Option>
                  <Option value="SIDEBAR">侧边栏</Option>
                  <Option value="TOP">顶部导航</Option>
                  <Option value="FREE">自由布局</Option>
                </Select>
              </Form.Item>
              <Form.Item name="isHome" label="首页" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Form>
          </Space>
          <Space>
            <Radio.Group value={previewMode} onChange={(e) => setPreviewMode(e.target.value)} size="small">
              <Radio.Button value="pc"><DesktopOutlined /> PC</Radio.Button>
              <Radio.Button value="mobile"><MobileOutlined /> 移动端</Radio.Button>
            </Radio.Group>
            <Divider type="vertical" />
            <Button icon={<UndoOutlined />} onClick={handleUndo} disabled={historyIndex <= 0}>
              撤销
            </Button>
            <Button icon={<RedoOutlined />} onClick={handleRedo} disabled={historyIndex >= history.length - 1}>
              重做
            </Button>
            <Divider type="vertical" />
            <Button 
              icon={<RobotOutlined />} 
              onClick={() => setAiPanelVisible(true)}
              type="dashed"
            >
              AI助手
            </Button>
            <Button icon={<EyeOutlined />} onClick={handlePreview}>
              预览
            </Button>
            <Button icon={<CodeOutlined />} onClick={handleViewCode}>
              代码
            </Button>
            <Button icon={<SaveOutlined />} type="primary" onClick={handleSave}>
              保存
            </Button>
            <Button icon={<PlayCircleOutlined />} type="primary" onClick={handlePublish}>
              发布
            </Button>
          </Space>
        </Header>
        <Layout>
          <Sider
            width={260}
            style={{
              background: '#fff',
              borderRight: '1px solid #e8e8e8',
              overflow: 'auto',
            }}
          >
            <Tabs
              defaultActiveKey="components"
              size="small"
              tabBarStyle={{ margin: 0, padding: '0 8px', borderBottom: '1px solid #e8e8e8' }}
              items={[
                {
                  key: 'components',
                  label: (
                    <span>
                      <CodeSandboxOutlined /> 组件库
                    </span>
                  ),
                  children: (
                    <div style={{ padding: 12 }}>
                      <Collapse
                        activeKey={expandedCategory}
                        onChange={(keys) => setExpandedCategory(keys as string[])}
                        ghost
                      >
                        {componentCategories.map(cat => (
                          <Panel
                            key={cat.key}
                            header={
                              <span>
                                {cat.icon} {cat.name}
                                <Badge
                                  count={mergedComponentLibrary[cat.key]?.length || 0}
                                  size="small"
                                  style={{ marginLeft: 8 }}
                                />
                              </span>
                            }
                          >
                            {mergedComponentLibrary[cat.key]?.map(comp => (
                              <ComponentItem key={comp.componentType} component={comp} />
                            ))}
                            {(!mergedComponentLibrary[cat.key] || mergedComponentLibrary[cat.key].length === 0) && (
                              <div style={{ textAlign: 'center', padding: '20px 0', color: '#999', fontSize: 12 }}>
                                暂无组件
                              </div>
                            )}
                          </Panel>
                        ))}
                      </Collapse>
                    </div>
                  ),
                },
                {
                  key: 'tree',
                  label: (
                    <span>
                      <SettingOutlined /> 组件树
                    </span>
                  ),
                  children: (
                    <div style={{ padding: 12, height: 'calc(100vh - 150px)', overflow: 'auto' }}>
                      <Tree
                        showLine
                        blockNode
                        selectedKeys={selectedComponentId ? [selectedComponentId] : []}
                        onSelect={(keys) => {
                          if (keys.length > 0) {
                            handleSelectComponent(keys[0] as string)
                          }
                        }}
                        treeData={componentTree}
                        fieldNames={{ title: 'title', key: 'key', children: 'children' }}
                      />
                      {componentTree.length === 0 && (
                        <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                          暂无组件
                        </div>
                      )}
                    </div>
                  ),
                },
              ]}
            />
          </Sider>
          <Content style={{ background: '#f5f5f5' }}>
            {renderCanvas()}
          </Content>
          <Sider
            width={340}
            className="property-panel"
            style={{ overflow: 'auto' }}
          >
            {selectedComponent ? (
              <div style={{ padding: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 18 }}>{componentIconMap[selectedComponent.componentType] || '📦'}</span>
                    <strong>{selectedComponent.componentName}</strong>
                  </div>
                  <Badge status="processing" text={selectedComponent.componentType} />
                </div>
                <Tabs
                  activeKey={activeTab}
                  onChange={setActiveTab}
                  size="small"
                  tabBarStyle={{ margin: '0 -16px 16px', padding: '0 16px', borderBottom: '1px solid #e8e8e8' }}
                  items={[
                    {
                      key: 'props',
                      label: (
                        <span>
                          <SettingOutlined /> 属性
                        </span>
                      ),
                      children: renderPropsPanel(),
                    },
                    {
                      key: 'style',
                      label: (
                        <span>
                          🎨 样式
                        </span>
                      ),
                      children: renderStylePanel(),
                    },
                    {
                      key: 'event',
                      label: (
                        <span>
                          <ThunderboltOutlined /> 事件
                        </span>
                      ),
                      children: renderEventPanel(),
                    },
                    {
                      key: 'data',
                      label: (
                        <span>
                          <ApiOutlined /> 数据
                        </span>
                      ),
                      children: renderDataSourcePanel(),
                    },
                    {
                      key: 'validation',
                      label: (
                        <span>
                          <CheckCircleOutlined /> 校验
                        </span>
                      ),
                      children: renderValidationPanel(),
                    },
                    {
                      key: 'fieldMapping',
                      label: (
                        <span>
                          <DatabaseOutlined /> 字段映射
                        </span>
                      ),
                      children: renderFieldMappingPanel(),
                    },
                    {
                      key: 'permission',
                      label: (
                        <span>
                          <SafetyOutlined /> 权限
                        </span>
                      ),
                      children: renderPermissionPanel(),
                    },
                  ]}
                />
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '60px 20px', color: '#999' }}>
                <FormOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />
                <div style={{ marginTop: 16 }}>请选择组件</div>
                <div style={{ fontSize: 12, marginTop: 8 }}>点击画布中的组件进行配置</div>
              </div>
            )}
          </Sider>
        </Layout>
      </Layout>

      <Modal
        title="页面预览"
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        footer={null}
        width={previewMode === 'mobile' ? 420 : 1000}
        bodyStyle={{ padding: 0 }}
      >
        <div style={{
          width: previewMode === 'mobile' ? 375 : '100%',
          height: 600,
          margin: '0 auto',
          background: '#fff',
          overflow: 'auto',
          border: '1px solid #e8e8e8',
        }}>
          {previewMode === 'mobile' && (
            <div style={{ background: '#000', color: '#fff', padding: '8px 16px', fontSize: 12, textAlign: 'center' }}>
              9:41
            </div>
          )}
          <div style={{ padding: 24 }}>
            {page?.components?.filter(c => !c.parentId).map((component) => {
              const evalResult = exprEvalResults[component.componentId]
              if (evalResult && !evalResult.visible) return null
              const props = component.propsConfig ? JSON.parse(component.propsConfig) : {}
              const mappingValue = evalResult?.mappingValue
              const resolvedProps = mappingValue !== undefined ? { ...props, value: mappingValue, text: mappingValue } : props
              const permissionConfig = component.permissionConfig ? JSON.parse(component.permissionConfig) : {}
              const { enablePermission, permissionCode, requiredRoles, visibilityMode } = permissionConfig

              const renderComponent = () => {
                const isDisabled = enablePermission && visibilityMode === 'disable'
                return (
                  <div key={component.componentId} style={{ marginBottom: 16 }}>
                    {(() => {
                      switch (component.componentType) {
                        case 'INPUT':
                          return <Input placeholder={resolvedProps.placeholder || '请输入'} value={resolvedProps.value} disabled={isDisabled} style={{ width: '100%' }} />
                        case 'BUTTON':
                          return <Button type={resolvedProps.type || 'primary'} disabled={isDisabled}>{resolvedProps.text || '按钮'}</Button>
                        case 'TITLE':
                          return <h3>{resolvedProps.text || '标题'}</h3>
                        case 'TEXT':
                          return <p>{resolvedProps.text || '文本内容'}</p>
                        case 'TABLE':
                          return (
                            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                              <thead>
                                <tr style={{ background: '#f5f5f5' }}>
                                  <th style={{ border: '1px solid #e8e8e8', padding: 8 }}>列1</th>
                                  <th style={{ border: '1px solid #e8e8e8', padding: 8 }}>列2</th>
                                </tr>
                              </thead>
                              <tbody>
                                <tr>
                                  <td style={{ border: '1px solid #e8e8e8', padding: 8 }}>数据1</td>
                                  <td style={{ border: '1px solid #e8e8e8', padding: 8 }}>数据2</td>
                                </tr>
                              </tbody>
                            </table>
                          )
                        default: {
                          const systemTypes = ['INPUT', 'TEXTAREA', 'NUMBER', 'SELECT', 'DATE', 'DATETIME', 'TIME', 'SWITCH', 'CHECKBOX', 'RADIO', 'UPLOAD', 'RICHTEXT', 'TABLE', 'BUTTON', 'LINK', 'IMAGE', 'TEXT', 'TITLE', 'ICON', 'DIVIDER', 'TABS', 'CARD', 'GRID', 'FLEX', 'MODAL', 'FORM', 'STEPS', 'TIMELINE', 'PROGRESS', 'RATE', 'SLIDER', 'LINECHART', 'BARCHART', 'PIECHART', 'AREACHART', 'SCATTERCHART', 'RADARCHART']
                          if (!systemTypes.includes(component.componentType)) {
                            return (
                              <CustomComponentWrapper
                                component={component}
                                executeActions={true}
                                eventContext={{ navigate }}
                              />
                            )
                          }
                          return <div style={{ padding: 16, background: '#f5f5f5', borderRadius: 4, textAlign: 'center', color: '#999' }}>{component.componentName}</div>
                        }
                      }
                    })()}
                  </div>
                )
              }

              if (enablePermission && (permissionCode || requiredRoles)) {
                const roles = requiredRoles ? requiredRoles.split(',').map((r: string) => r.trim()).filter(Boolean) : undefined
                return (
                  <PermissionGuard
                    key={component.componentId}
                    permission={permissionCode || undefined}
                    role={roles && roles.length > 0 ? roles : undefined}
                  >
                    {renderComponent()}
                  </PermissionGuard>
                )
              }

              return renderComponent()
            })}
          </div>
        </div>
      </Modal>

      <Drawer
        title="AI 页面助手"
        placement="right"
        width={420}
        open={aiPanelVisible}
        onClose={() => setAiPanelVisible(false)}
        mask={false}
        bodyStyle={{ padding: 0 }}
      >
        <AiPagePanel
          sessionId={aiSessionId}
          onSessionIdChange={setAiSessionId}
          onPageGenerated={handlePageGenerated}
          currentPage={page}
          appId={currentApp?.id}
        />
      </Drawer>

      <Modal
        title="页面代码"
        open={codeModalVisible}
        onCancel={() => setCodeModalVisible(false)}
        footer={null}
        width={900}
      >
        <pre style={{
          background: '#f5f5f5',
          padding: 16,
          borderRadius: 4,
          maxHeight: 500,
          overflow: 'auto',
          fontSize: 12,
          lineHeight: 1.5,
        }}>
          {codeContent}
        </pre>
      </Modal>
    </DndProvider>
  )
}

export default PageDesigner
