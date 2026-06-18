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
  Collapse,
  Divider,
  Radio,
  Card,
  Badge,
  Tag,
  Table,
  Tooltip,
  Popconfirm,
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
  SettingOutlined,
  ApiOutlined,
  CloudUploadOutlined,
  RocketOutlined,
  UserOutlined,
  ThunderboltOutlined,
  SafetyOutlined,
  FilterOutlined,
  TeamOutlined,
  UserSwitchOutlined,
  ScheduleOutlined,
  FileTextOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { DndProvider, useDrag, useDrop } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'
import { WorkflowDefinition, workflowApi } from '@/api/flow'
import { useAppStore } from '@/store/appStore'

const { Header, Sider, Content } = Layout
const { Option } = Select
const { Panel } = Collapse

const bpmnNodeCategories = [
  { key: 'event', name: '事件', icon: '⚡', color: '#52c41a' },
  { key: 'task', name: '任务', icon: '📋', color: '#1677ff' },
  { key: 'gateway', name: '网关', icon: '🔀', color: '#faad14' },
  { key: 'flow', name: '流程', icon: '🔄', color: '#722ed1' },
  { key: 'advanced', name: '高级', icon: '⚙️', color: '#eb2f96' },
]

const bpmnNodeTypes: Record<string, any[]> = {
  event: [
    { type: 'START_EVENT', name: '开始事件', icon: '🚀', description: '流程的起点', shape: 'circle', color: '#52c41a' },
    { type: 'END_EVENT', name: '结束事件', icon: '🏁', description: '流程的终点', shape: 'circle', color: '#ff4d4f' },
    { type: 'TIMER_START_EVENT', name: '定时启动', icon: '⏰', description: '定时触发流程', shape: 'circle', color: '#faad14' },
    { type: 'MESSAGE_START_EVENT', name: '消息启动', icon: '📧', description: '接收消息启动', shape: 'circle', color: '#1890ff' },
    { type: 'SIGNAL_START_EVENT', name: '信号启动', icon: '📡', description: '接收信号启动', shape: 'circle', color: '#722ed1' },
    { type: 'TIMER_INTERMEDIATE_EVENT', name: '定时事件', icon: '⏰', description: '中间定时事件', shape: 'circle', color: '#faad14' },
    { type: 'MESSAGE_INTERMEDIATE_EVENT', name: '消息事件', icon: '📧', description: '中间消息事件', shape: 'circle', color: '#1890ff' },
    { type: 'SIGNAL_INTERMEDIATE_EVENT', name: '信号事件', icon: '📡', description: '中间信号事件', shape: 'circle', color: '#722ed1' },
    { type: 'BOUNDARY_TIMER_EVENT', name: '边界定时', icon: '⏱️', description: '边界定时器事件', shape: 'circle', color: '#faad14' },
    { type: 'BOUNDARY_ERROR_EVENT', name: '边界错误', icon: '❌', description: '边界错误事件', shape: 'circle', color: '#ff4d4f' },
  ],
  task: [
    { type: 'USER_TASK', name: '用户任务', icon: '👤', description: '需要人工处理的任务', shape: 'rounded', color: '#1677ff' },
    { type: 'SERVICE_TASK', name: '服务任务', icon: '🔧', description: '自动调用服务', shape: 'rounded', color: '#13c2c2' },
    { type: 'SCRIPT_TASK', name: '脚本任务', icon: '⌨️', description: '执行脚本代码', shape: 'rounded', color: '#fa8c16' },
    { type: 'BUSINESS_RULE_TASK', name: '规则任务', icon: '📏', description: '执行业务规则', shape: 'rounded', color: '#a0d911' },
    { type: 'SEND_TASK', name: '发送任务', icon: '📤', description: '发送消息', shape: 'rounded', color: '#1890ff' },
    { type: 'RECEIVE_TASK', name: '接收任务', icon: '📥', description: '等待接收消息', shape: 'rounded', color: '#722ed1' },
    { type: 'MANUAL_TASK', name: '手动任务', icon: '✋', description: '无需系统参与', shape: 'rounded', color: '#faad14' },
    { type: 'CALL_ACTIVITY', name: '调用活动', icon: '🔗', description: '调用另一个流程', shape: 'rounded', color: '#eb2f96' },
  ],
  gateway: [
    { type: 'EXCLUSIVE_GATEWAY', name: '排他网关', icon: '⬡', description: '根据条件选择一条路径', shape: 'diamond', color: '#faad14' },
    { type: 'PARALLEL_GATEWAY', name: '并行网关', icon: '⬢', description: '所有路径同时执行', shape: 'diamond', color: '#52c41a' },
    { type: 'INCLUSIVE_GATEWAY', name: '包容网关', icon: '⬣', description: '满足条件的路径执行', shape: 'diamond', color: '#1890ff' },
    { type: 'EVENT_BASED_GATEWAY', name: '事件网关', icon: '⬢', description: '根据事件选择路径', shape: 'diamond', color: '#722ed1' },
    { type: 'COMPLEX_GATEWAY', name: '复杂网关', icon: '⬡', description: '复杂条件判断', shape: 'diamond', color: '#eb2f96' },
  ],
  flow: [
    { type: 'SEQUENCE_FLOW', name: '顺序流', icon: '➡️', description: '连接两个节点', shape: 'line', color: '#1677ff' },
  ],
  advanced: [
    { type: 'SUB_PROCESS', name: '子流程', icon: '📦', description: '嵌套的子流程', shape: 'rect', color: '#722ed1' },
    { type: 'TRANSACTION', name: '事务', icon: '🔒', description: '事务性子流程', shape: 'rect', color: '#faad14' },
    { type: 'AD_HOC_SUB_PROCESS', name: '即席流程', icon: '✨', description: '灵活的即席子流程', shape: 'rect', color: '#13c2c2' },
    { type: 'DATA_OBJECT', name: '数据对象', icon: '📄', description: '流程数据对象', shape: 'rect', color: '#a0d911' },
    { type: 'DATA_STORE', name: '数据存储', icon: '💾', description: '数据存储引用', shape: 'rect', color: '#fa8c16' },
    { type: 'POOL', name: '泳池', icon: '🏊', description: '流程参与者', shape: 'pool', color: '#e8e8e8' },
    { type: 'LANE', name: '泳道', icon: '📊', description: '职责划分', shape: 'lane', color: '#f5f5f5' },
  ],
}

const getBpmnNodeColor = (nodeType: string): string => {
  for (const [cat, nodes] of Object.entries(bpmnNodeTypes)) {
    if (nodes.some(n => n.type === nodeType)) {
      return bpmnNodeCategories.find(c => c.key === cat)?.color || '#1677ff'
    }
  }
  return '#1677ff'
}

const getBpmnNodeIcon = (nodeType: string): string => {
  for (const nodes of Object.values(bpmnNodeTypes)) {
    const node = nodes.find(n => n.type === nodeType)
    if (node) return node.icon
  }
  return '📦'
}

const getBpmnNodeShape = (nodeType: string): string => {
  for (const nodes of Object.values(bpmnNodeTypes)) {
    const node = nodes.find(n => n.type === nodeType)
    if (node) return node.shape
  }
  return 'rounded'
}

interface BpmnNodeItemProps {
  nodeType: any
}

const BpmnNodeItem: React.FC<BpmnNodeItemProps> = ({ nodeType }) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'BPMN_NODE',
    item: { nodeType: nodeType.type, nodeName: nodeType.name, shape: nodeType.shape, color: nodeType.color },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  }))

  return (
    <Tooltip title={nodeType.description} placement="right">
      <div
        ref={drag}
        className="component-item"
        style={{
          padding: '8px 12px',
          background: '#fff',
          border: `1px solid ${nodeType.color}40`,
          borderRadius: 4,
          marginBottom: 8,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          opacity: isDragging ? 0.5 : 1,
        }}
      >
        <span style={{ fontSize: 16 }}>{nodeType.icon}</span>
        <span style={{ fontSize: 13 }}>{nodeType.name}</span>
      </div>
    </Tooltip>
  )
}

interface BpmnCanvasNodeProps {
  node: any
  isSelected: boolean
  onSelect: () => void
  onDelete: () => void
  onDragEnd: (x: number, y: number) => void
  onStartConnect: () => void
}

const BpmnCanvasNode: React.FC<BpmnCanvasNodeProps> = ({ node, isSelected, onSelect, onDelete, onDragEnd, onStartConnect }) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'EXISTING_BPMN_NODE',
    item: { nodeId: node.id },
    end: (_item, monitor) => {
      const offset = monitor.getClientOffset()
      if (offset) {
        const canvas = document.getElementById('bpmn-canvas')
        if (canvas) {
          const rect = canvas.getBoundingClientRect()
          onDragEnd(offset.x - rect.left - 50, offset.y - rect.top - 30)
        }
      }
    },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  }))

  const color = getBpmnNodeColor(node.nodeType)
  const icon = getBpmnNodeIcon(node.nodeType)
  const shape = getBpmnNodeShape(node.nodeType)

  const getNodeStyle = () => {
    const baseStyle: React.CSSProperties = {
      position: 'relative',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      flexDirection: 'column',
      gap: 4,
      cursor: 'move',
      userSelect: 'none',
      transition: 'all 0.2s',
    }

    switch (shape) {
      case 'circle':
        return {
          ...baseStyle,
          width: node.width || 50,
          height: node.height || 50,
          borderRadius: '50%',
          background: '#fff',
          border: `3px solid ${isSelected ? '#1677ff' : color}`,
          boxShadow: isSelected ? '0 4px 12px rgba(22, 119, 255, 0.25)' : '0 2px 8px rgba(0, 0, 0, 0.1)',
        }
      case 'diamond':
        return {
          ...baseStyle,
          width: node.width || 60,
          height: node.height || 60,
          background: '#fff',
          border: `3px solid ${isSelected ? '#1677ff' : color}`,
          boxShadow: isSelected ? '0 4px 12px rgba(22, 119, 255, 0.25)' : '0 2px 8px rgba(0, 0, 0, 0.1)',
          transform: 'rotate(45deg)',
        }
      case 'rect':
        return {
          ...baseStyle,
          width: node.width || 120,
          height: node.height || 80,
          borderRadius: 4,
          background: '#fff',
          border: `3px solid ${isSelected ? '#1677ff' : color}`,
          boxShadow: isSelected ? '0 4px 12px rgba(22, 119, 255, 0.25)' : '0 2px 8px rgba(0, 0, 0, 0.1)',
        }
      case 'pool':
      case 'lane':
        return {
          ...baseStyle,
          width: node.width || 300,
          height: node.height || 200,
          borderRadius: 4,
          background: shape === 'pool' ? '#fafafa' : '#fff',
          border: `2px solid ${isSelected ? '#1677ff' : '#d9d9d9'}`,
          boxShadow: isSelected ? '0 4px 12px rgba(22, 119, 255, 0.25)' : 'none',
        }
      case 'rounded':
      default:
        return {
          ...baseStyle,
          width: node.width || 120,
          height: node.height || 60,
          borderRadius: 8,
          background: '#fff',
          border: `3px solid ${isSelected ? '#1677ff' : color}`,
          boxShadow: isSelected ? '0 4px 12px rgba(22, 119, 255, 0.25)' : '0 2px 8px rgba(0, 0, 0, 0.1)',
        }
    }
  }

  const nodeStyle = getNodeStyle()

  return (
    <div
      ref={drag}
      className={`node-wrapper ${isSelected ? 'selected' : ''}`}
      style={{
        left: node.positionX || 100,
        top: node.positionY || 100,
        opacity: isDragging ? 0.5 : 1,
      }}
      onClick={(e) => { e.stopPropagation(); onSelect() }}
    >
      <div style={nodeStyle}>
        {isSelected && (
          <div style={{ position: 'absolute', top: -12, right: -8, zIndex: 10, display: 'flex', gap: 4 }}>
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              onClick={(e) => { e.stopPropagation(); onStartConnect() }}
            />
            <Button
              type="primary"
              danger
              size="small"
              icon={<DeleteOutlined />}
              onClick={(e) => { e.stopPropagation(); onDelete() }}
            />
          </div>
        )}
        <div style={shape === 'diamond' ? { transform: 'rotate(-45deg)', textAlign: 'center' } : { textAlign: 'center' }}>
          <span style={{ fontSize: shape === 'circle' ? 16 : 20 }}>{icon}</span>
          <div style={{ fontSize: shape === 'circle' ? 9 : 11, color: '#666', marginTop: 2, maxWidth: shape === 'circle' ? 40 : 'none', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {node.nodeName}
          </div>
        </div>
        <div
          style={{
            position: 'absolute',
            left: -6,
            top: '50%',
            transform: 'translateY(-50%)',
            width: 12,
            height: 12,
            background: '#fff',
            border: '2px solid #1677ff',
            borderRadius: '50%',
            cursor: 'crosshair',
            zIndex: 5,
          }}
          onClick={(e) => { e.stopPropagation(); onStartConnect() }}
        />
        <div
          style={{
            position: 'absolute',
            right: -6,
            top: '50%',
            transform: 'translateY(-50%)',
            width: 12,
            height: 12,
            background: '#fff',
            border: '2px solid #1677ff',
            borderRadius: '50%',
            cursor: 'crosshair',
            zIndex: 5,
          }}
          onClick={(e) => { e.stopPropagation(); onStartConnect() }}
        />
      </div>
    </div>
  )
}

const WorkflowDesigner: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [workflow, setWorkflow] = useState<WorkflowDefinition | null>(null)
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [connecting, setConnecting] = useState(false)
  const [connectStart, setConnectStart] = useState<string | null>(null)
  const [form] = Form.useForm()
  const [nodeForm] = Form.useForm()
  const [codeModalVisible, setCodeModalVisible] = useState(false)
  const [codeContent, setCodeContent] = useState('')
  const [expandedCategory, setExpandedCategory] = useState<string[]>(['event', 'task', 'gateway'])
  const [history, setHistory] = useState<any[]>([])
  const [historyIndex, setHistoryIndex] = useState(-1)
  const [nodes, setNodes] = useState<any[]>([])
  const [edges, setEdges] = useState<any[]>([])
  const [deployVisible, setDeployVisible] = useState(false)
  const [processList, setProcessList] = useState<any[]>([])

  const canvasRef = useRef<HTMLDivElement>(null)
  const svgRef = useRef<SVGSVGElement>(null)

  const loadWorkflow = useCallback(async () => {
    if (!id || id === 'undefined') {
      setWorkflow({
        appId: currentApp?.id || 1,
        workflowName: '',
        workflowCode: '',
        description: '',
        status: 'DRAFT',
        version: 1,
        category: 'default',
      })
      form.setFieldsValue({
        workflowName: '',
        workflowCode: '',
        description: '',
        category: 'default',
      })
      setNodes([])
      setEdges([])
      return
    }
    try {
      const res = await workflowApi.get(Number(id))
      setWorkflow(res.data || null)
      form.setFieldsValue(res.data)
      if (res.data?.bpmnXml) {
        try {
          const parsed = JSON.parse(res.data.bpmnXml)
          setNodes(parsed.nodes || [])
          setEdges(parsed.edges || [])
        } catch {
          setNodes([])
          setEdges([])
        }
      }
    } catch (e) {
      console.error(e)
    }
  }, [id, currentApp])

  useEffect(() => {
    loadWorkflow()
  }, [loadWorkflow])

  const saveHistory = useCallback(() => {
    const newHistory = history.slice(0, historyIndex + 1)
    newHistory.push({
      nodes: JSON.parse(JSON.stringify(nodes || [])),
      edges: JSON.parse(JSON.stringify(edges || [])),
    })
    setHistory(newHistory)
    setHistoryIndex(newHistory.length - 1)
  }, [nodes, edges, history, historyIndex])

  const handleUndo = () => {
    if (historyIndex > 0) {
      const newIndex = historyIndex - 1
      setHistoryIndex(newIndex)
      setNodes(history[newIndex].nodes)
      setEdges(history[newIndex].edges)
    }
  }

  const handleRedo = () => {
    if (historyIndex < history.length - 1) {
      const newIndex = historyIndex + 1
      setHistoryIndex(newIndex)
      setNodes(history[newIndex].nodes)
      setEdges(history[newIndex].edges)
    }
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      if (!workflow) return
      const bpmnData = { nodes, edges }
      const data = {
        ...workflow,
        ...values,
        bpmnXml: JSON.stringify(bpmnData),
      }
      if (workflow.id) {
        await workflowApi.update(data)
        message.success('保存成功')
      } else {
        const res = await workflowApi.save(data)
        setWorkflow(res.data)
        message.success('创建成功')
        navigate(`/workflow/designer/${res.data.id}`, { replace: true })
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handleDeploy = async () => {
    if (!workflow?.id) {
      message.warning('请先保存流程')
      return
    }
    try {
      await workflowApi.deploy(workflow.id)
      message.success('部署成功')
      loadWorkflow()
      setDeployVisible(false)
      loadProcessList()
    } catch (e) {
      console.error(e)
    }
  }

  const handleViewBpmn = async () => {
    if (!workflow?.id) {
      message.warning('请先保存流程')
      return
    }
    try {
      const res = await workflowApi.generateBpmn(workflow.id)
      setCodeContent(res.data || '')
      setCodeModalVisible(true)
    } catch (e) {
      console.error(e)
    }
  }

  const loadProcessList = async () => {
    try {
      const res = await workflowApi.deployed()
      setProcessList(res.data || [])
    } catch (e) {
      console.error(e)
    }
  }

  const handleDropNode = (item: any, offsetX: number, offsetY: number) => {
    saveHistory()

    const width = item.shape === 'circle' ? 50 : item.shape === 'diamond' ? 60 : item.shape === 'pool' ? 300 : 120
    const height = item.shape === 'circle' ? 50 : item.shape === 'diamond' ? 60 : item.shape === 'pool' ? 200 : 60

    const newNode = {
      id: `node_${Date.now()}`,
      nodeName: item.nodeName,
      nodeType: item.nodeType,
      positionX: Math.max(0, offsetX),
      positionY: Math.max(0, offsetY),
      width,
      height,
      nodeConfig: '{}',
    }

    const newNodes = [...nodes, newNode]
    setNodes(newNodes)
    setSelectedNodeId(newNode.id)
    loadNodeForm(newNode)
  }

  const [{ isOver }, canvasDrop] = useDrop(() => ({
    accept: 'BPMN_NODE',
    drop: (item: any, monitor) => {
      const offset = monitor.getClientOffset()
      const canvas = document.getElementById('bpmn-canvas')
      if (canvas && offset) {
        const rect = canvas.getBoundingClientRect()
        handleDropNode(item, offset.x - rect.left - 50, offset.y - rect.top - 30)
      }
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  }))

  const handleNodeDragEnd = (nodeId: string, x: number, y: number) => {
    saveHistory()
    const newNodes = nodes.map(n =>
      n.id === nodeId ? { ...n, positionX: Math.max(0, x), positionY: Math.max(0, y) } : n
    )
    setNodes(newNodes)
  }

  const handleDeleteNode = (nodeId: string) => {
    saveHistory()
    const newNodes = nodes.filter(n => n.id !== nodeId)
    const newEdges = edges.filter(e => e.source !== nodeId && e.target !== nodeId)
    setNodes(newNodes)
    setEdges(newEdges)
    if (selectedNodeId === nodeId) {
      setSelectedNodeId(null)
    }
  }

  const handleSelectNode = (nodeId: string) => {
    setSelectedNodeId(nodeId)
    const node = nodes.find(n => n.id === nodeId)
    if (node) {
      loadNodeForm(node)
    }
  }

  const handleStartConnect = (nodeId: string) => {
    setConnecting(true)
    setConnectStart(nodeId)
  }

  const handleCanvasClick = (e: React.MouseEvent) => {
    if (connecting && connectStart && canvasRef.current) {
      const rect = canvasRef.current.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top

      const targetNode = nodes.find(n => {
        const nx = n.positionX || 0
        const ny = n.positionY || 0
        const nw = n.width || 120
        const nh = n.height || 60
        return x >= nx && x <= nx + nw && y >= ny && y <= ny + nh
      })

      if (targetNode && targetNode.id !== connectStart) {
        saveHistory()
        const newEdge = {
          id: `edge_${Date.now()}`,
          source: connectStart,
          target: targetNode.id,
          conditionExpression: '',
          flowName: '',
        }
        setEdges([...edges, newEdge])
      }

      setConnecting(false)
      setConnectStart(null)
    } else {
      setSelectedNodeId(null)
    }
  }

  const loadNodeForm = (node: any) => {
    const config = node.nodeConfig ? JSON.parse(node.nodeConfig) : {}
    nodeForm.setFieldsValue({
      nodeName: node.nodeName,
      nodeType: node.nodeType,
      ...config,
    })
  }

  const handleNodeConfigSave = async () => {
    try {
      const values = await nodeForm.validateFields()
      if (!selectedNodeId) return
      saveHistory()

      const { nodeName, ...config } = values
      const newNodes = nodes.map(n =>
        n.id === selectedNodeId
          ? { ...n, nodeName, nodeConfig: JSON.stringify(config) }
          : n
      )
      setNodes(newNodes)
      message.success('节点配置保存成功')
    } catch (e) {
      console.error(e)
    }
  }

  const handleDeleteEdge = (edgeId: string) => {
    saveHistory()
    const newEdges = edges.filter(e => e.id !== edgeId)
    setEdges(newEdges)
  }

  const selectedNode = nodes.find(n => n.id === selectedNodeId)

  const renderEdges = () => {
    return edges.map(edge => {
      const sourceNode = nodes.find(n => n.id === edge.source)
      const targetNode = nodes.find(n => n.id === edge.target)
      if (!sourceNode || !targetNode) return null

      const sx = (sourceNode.positionX || 0) + (sourceNode.width || 120)
      const sy = (sourceNode.positionY || 0) + (sourceNode.height || 60) / 2
      const tx = targetNode.positionX || 0
      const ty = (targetNode.positionY || 0) + (targetNode.height || 60) / 2

      const midX = (sx + tx) / 2

      return (
        <g key={edge.id}>
          <path
            d={`M ${sx} ${sy} C ${midX} ${sy}, ${midX} ${ty}, ${tx} ${ty}`}
            stroke="#1677ff"
            strokeWidth={2}
            fill="none"
            markerEnd="url(#bpmn-arrowhead)"
            style={{ cursor: 'pointer' }}
            onClick={(e) => {
              e.stopPropagation()
              Modal.confirm({
                title: '确认删除连线？',
                onOk: () => handleDeleteEdge(edge.id),
              })
            }}
          />
          {edge.flowName && (
            <text
              x={midX}
              y={(sy + ty) / 2 - 12}
              textAnchor="middle"
              fill="#1677ff"
              fontSize={12}
              fontWeight="bold"
            >
              {edge.flowName}
            </text>
          )}
          {edge.conditionExpression && (
            <text
              x={midX}
              y={(sy + ty) / 2 + 8}
              textAnchor="middle"
              fill="#ff4d4f"
              fontSize={11}
            >
              [{edge.conditionExpression}]
            </text>
          )}
        </g>
      )
    })
  }

  const renderBpmnNodeConfigPanel = () => {
    if (!selectedNode) return null

    const type = selectedNode.nodeType

    const getNodeConfigFields = () => {
      switch (type) {
        case 'USER_TASK':
          return (
            <>
              <Form.Item name="assignee" label="处理人">
                <Input placeholder="指定处理人，如: ${initiator}" />
              </Form.Item>
              <Form.Item name="candidateUsers" label="候选用户">
                <Input placeholder="多个用户用逗号分隔" />
              </Form.Item>
              <Form.Item name="candidateGroups" label="候选组">
                <Input placeholder="多个组用逗号分隔，如: dept_leader, hr" />
              </Form.Item>
              <Divider>任务属性</Divider>
              <Form.Item name="dueDate" label="截止日期">
                <Input placeholder="如: ${dueDate} 或 ISO日期格式" />
              </Form.Item>
              <Form.Item name="priority" label="优先级">
                <InputNumber style={{ width: '100%' }} min={0} max={100} defaultValue={50} />
              </Form.Item>
              <Form.Item name="formKey" label="表单Key">
                <Input placeholder="绑定的表单标识" />
              </Form.Item>
              <Divider>会签设置</Divider>
              <Form.Item name="multiInstance" label="会签类型">
                <Select allowClear>
                  <Option value="parallel">并行会签</Option>
                  <Option value="sequential">串行会签</Option>
                </Select>
              </Form.Item>
              <Form.Item name="multiInstanceCollection" label="会签集合">
                <Input placeholder="如: ${approvers}" />
              </Form.Item>
              <Form.Item name="multiInstanceElementVariable" label="元素变量">
                <Input placeholder="如: approver" />
              </Form.Item>
              <Form.Item name="completionCondition" label="完成条件">
                <Input placeholder="如: ${nrOfCompletedInstances >= nrOfInstances}" />
              </Form.Item>
              <Divider>监听器</Divider>
              <Form.Item name="taskListeners" label="任务监听器">
                <Input.TextArea rows={3} placeholder='[{"event": "create", "class": "com.example.TaskCreateListener"}]' />
              </Form.Item>
              <Form.Item name="executionListeners" label="执行监听器">
                <Input.TextArea rows={3} placeholder='[{"event": "start", "class": "com.example.ExecutionListener"}]' />
              </Form.Item>
            </>
          )
        case 'SERVICE_TASK':
          return (
            <>
              <Form.Item name="implementation" label="实现类型" rules={[{ required: true }]}>
                <Select>
                  <Option value="class">Java类</Option>
                  <Option value="delegateExpression">代理表达式</Option>
                  <Option value="expression">表达式</Option>
                  <Option value="webService">WebService</Option>
                  <Option value="http">HTTP请求</Option>
                </Select>
              </Form.Item>
              <Form.Item name="javaClass" label="Java类名">
                <Input placeholder="如: com.example.MyServiceTask" />
              </Form.Item>
              <Form.Item name="expression" label="表达式">
                <Input placeholder="如: ${myService.doSomething()}" />
              </Form.Item>
              <Form.Item name="delegateExpression" label="代理表达式">
                <Input placeholder="如: ${myDelegateBean}" />
              </Form.Item>
              <Form.Item name="resultVariable" label="结果变量">
                <Input placeholder="执行结果保存的变量名" />
              </Form.Item>
              <Divider>HTTP配置</Divider>
              <Form.Item name="httpUrl" label="请求URL">
                <Input placeholder="如: https://api.example.com/data" />
              </Form.Item>
              <Form.Item name="httpMethod" label="请求方法">
                <Select>
                  <Option value="GET">GET</Option>
                  <Option value="POST">POST</Option>
                  <Option value="PUT">PUT</Option>
                  <Option value="DELETE">DELETE</Option>
                </Select>
              </Form.Item>
              <Form.Item name="httpHeaders" label="请求头">
                <Input.TextArea rows={3} placeholder='{"Authorization": "Bearer ${token}"}' />
              </Form.Item>
              <Form.Item name="httpBody" label="请求体">
                <Input.TextArea rows={4} />
              </Form.Item>
            </>
          )
        case 'SCRIPT_TASK':
          return (
            <>
              <Form.Item name="scriptFormat" label="脚本格式" rules={[{ required: true }]}>
                <Select defaultValue="groovy">
                  <Option value="groovy">Groovy</Option>
                  <Option value="javascript">JavaScript</Option>
                  <Option value="juel">JUEL</Option>
                  <Option value="python">Python</Option>
                  <Option value="ruby">Ruby</Option>
                </Select>
              </Form.Item>
              <Form.Item name="script" label="脚本内容" rules={[{ required: true }]}>
                <Input.TextArea rows={12} placeholder="// Groovy脚本示例&#10;def user = execution.getVariable('user')&#10;execution.setVariable('result', userService.validate(user))" />
              </Form.Item>
              <Form.Item name="resultVariable" label="结果变量">
                <Input placeholder="脚本返回值保存的变量名" />
              </Form.Item>
              <Form.Item name="autoStoreVariables" label="自动存储变量" valuePropName="checked">
                <Switch defaultChecked />
              </Form.Item>
            </>
          )
        case 'BUSINESS_RULE_TASK':
          return (
            <>
              <Form.Item name="ruleVariablesInput" label="输入规则变量">
                <Input placeholder="多个变量用逗号分隔" />
              </Form.Item>
              <Form.Item name="ruleVariablesOutput" label="输出规则变量">
                <Input placeholder="多个变量用逗号分隔" />
              </Form.Item>
              <Form.Item name="rules" label="规则">
                <Input.TextArea rows={4} placeholder='["rule1", "rule2"]' />
              </Form.Item>
              <Form.Item name="ruleGroup" label="规则组">
                <Input placeholder="规则组名称" />
              </Form.Item>
              <Form.Item name="resultVariable" label="结果变量">
                <Input placeholder="规则执行结果保存的变量名" />
              </Form.Item>
              <Form.Item name="exclude" label="排除规则" valuePropName="checked">
                <Switch />
              </Form.Item>
            </>
          )
        case 'EXCLUSIVE_GATEWAY':
        case 'INCLUSIVE_GATEWAY':
          return (
            <>
              <Form.Item name="defaultFlow" label="默认流转">
                <Select placeholder="条件都不满足时的默认流向">
                  {edges.filter(e => e.source === selectedNodeId).map(e => {
                    const target = nodes.find(n => n.id === e.target)
                    return <Option key={e.id} value={e.id}>{target?.nodeName || e.target}</Option>
                  })}
                </Select>
              </Form.Item>
              <Divider>出线条件</Divider>
              {edges.filter(e => e.source === selectedNodeId).map((e, idx) => {
                const target = nodes.find(n => n.id === e.target)
                return (
                  <Form.Item key={e.id} label={`流向: ${target?.nodeName || e.target}`}>
                    <Input.TextArea
                      rows={2}
                      placeholder='如: ${amount > 1000}'
                      value={e.conditionExpression}
                      onChange={(ev) => {
                        const newEdges = edges.map(edge =>
                          edge.id === e.id ? { ...edge, conditionExpression: ev.target.value } : edge
                        )
                        setEdges(newEdges)
                      }}
                    />
                  </Form.Item>
                )
              })}
            </>
          )
        case 'PARALLEL_GATEWAY':
          return (
            <Form.Item name="async" label="异步执行" valuePropName="checked">
              <Switch />
            </Form.Item>
          )
        case 'TIMER_START_EVENT':
        case 'TIMER_INTERMEDIATE_EVENT':
        case 'BOUNDARY_TIMER_EVENT':
          return (
            <>
              <Form.Item name="timerType" label="定时器类型" rules={[{ required: true }]}>
                <Select>
                  <Option value="date">指定日期</Option>
                  <Option value="duration">持续时间</Option>
                  <Option value="cycle">循环执行</Option>
                </Select>
              </Form.Item>
              <Form.Item name="timerDate" label="指定日期">
                <Input placeholder="ISO8601格式: 2024-01-15T12:00:00" />
              </Form.Item>
              <Form.Item name="timerDuration" label="持续时间">
                <Input placeholder="ISO8601格式: PT5M (5分钟), P1D (1天)" />
              </Form.Item>
              <Form.Item name="timerCycle" label="循环周期">
                <Input placeholder="Cron表达式: 0 0 12 * * ? 或 R3/PT10M" />
              </Form.Item>
              {type === 'BOUNDARY_TIMER_EVENT' && (
                <Form.Item name="cancelActivity" label="取消活动" valuePropName="checked">
                  <Switch defaultChecked />
                </Form.Item>
              )}
            </>
          )
        case 'MESSAGE_START_EVENT':
        case 'MESSAGE_INTERMEDIATE_EVENT':
        case 'RECEIVE_TASK':
          return (
            <>
              <Form.Item name="messageRef" label="消息引用" rules={[{ required: true }]}>
                <Input placeholder="消息定义的ID" />
              </Form.Item>
              <Form.Item name="messageName" label="消息名称">
                <Input placeholder="消息的名称" />
              </Form.Item>
              <Form.Item name="correlationKey" label="关联键">
                <Input placeholder="用于匹配流程实例的表达式，如: ${orderId}" />
              </Form.Item>
            </>
          )
        case 'SIGNAL_START_EVENT':
        case 'SIGNAL_INTERMEDIATE_EVENT':
          return (
            <>
              <Form.Item name="signalRef" label="信号引用" rules={[{ required: true }]}>
                <Input placeholder="信号定义的ID" />
              </Form.Item>
              <Form.Item name="signalName" label="信号名称">
                <Input placeholder="信号的名称" />
              </Form.Item>
              <Form.Item name="async" label="异步触发" valuePropName="checked">
                <Switch />
              </Form.Item>
            </>
          )
        case 'BOUNDARY_ERROR_EVENT':
          return (
            <>
              <Form.Item name="errorCode" label="错误码">
                <Input placeholder="匹配的错误代码" />
              </Form.Item>
              <Form.Item name="cancelActivity" label="取消活动" valuePropName="checked">
                <Switch defaultChecked />
              </Form.Item>
            </>
          )
        case 'CALL_ACTIVITY':
          return (
            <>
              <Form.Item name="calledElement" label="被调用流程" rules={[{ required: true }]}>
                <Select placeholder="选择要调用的流程">
                  {processList.map(p => (
                    <Option key={p.id} value={p.key}>{p.name} (v{p.version})</Option>
                  ))}
                </Select>
              </Form.Item>
              <Divider>变量传递</Divider>
              <Form.Item name="inVariables" label="输入变量">
                <Input.TextArea rows={3} placeholder='{"localVar1": "processVar1", "localVar2": "processVar2"}' />
              </Form.Item>
              <Form.Item name="outVariables" label="输出变量">
                <Input.TextArea rows={3} placeholder='{"processVar3": "localVar3"}' />
              </Form.Item>
              <Form.Item name="inheritVariables" label="继承所有变量" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item name="businessKey" label="业务Key">
                <Input placeholder="如: ${businessKey}" />
              </Form.Item>
            </>
          )
        case 'SUB_PROCESS':
        case 'TRANSACTION':
        case 'AD_HOC_SUB_PROCESS':
          return (
            <>
              <Form.Item name="triggeredByEvent" label="事件触发" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item name="async" label="异步执行" valuePropName="checked">
                <Switch />
              </Form.Item>
              {type === 'TRANSACTION' && (
                <Form.Item name="transactionMethod" label="事务方法">
                  <Select defaultValue="STANDARD">
                    <Option value="STANDARD">标准事务</Option>
                  </Select>
                </Form.Item>
              )}
              {type === 'AD_HOC_SUB_PROCESS' && (
                <>
                  <Form.Item name="ordering" label="执行顺序">
                    <Select>
                      <Option value="parallel">并行</Option>
                      <Option value="sequential">串行</Option>
                    </Select>
                  </Form.Item>
                  <Form.Item name="completionCondition" label="完成条件">
                    <Input placeholder='如: ${nrOfCompletedInstances >= 2}' />
                  </Form.Item>
                </>
              )}
            </>
          )
        case 'SEND_TASK':
          return (
            <>
              <Form.Item name="implementation" label="实现类型" rules={[{ required: true }]}>
                <Select>
                  <Option value="class">Java类</Option>
                  <Option value="expression">表达式</Option>
                  <Option value="delegateExpression">代理表达式</Option>
                  <Option value="webService">WebService</Option>
                </Select>
              </Form.Item>
              <Form.Item name="javaClass" label="Java类名">
                <Input placeholder="如: com.example.SendTask" />
              </Form.Item>
              <Form.Item name="expression" label="表达式">
                <Input placeholder="如: ${notificationService.send(message)}" />
              </Form.Item>
            </>
          )
        case 'MANUAL_TASK':
          return (
            <Form.Item name="priority" label="优先级">
              <InputNumber style={{ width: '100%' }} min={0} max={100} defaultValue={50} />
            </Form.Item>
          )
        case 'DATA_OBJECT':
          return (
            <>
              <Form.Item name="dataObjectId" label="数据对象ID" rules={[{ required: true }]}>
                <Input placeholder="数据对象的唯一标识" />
              </Form.Item>
              <Form.Item name="dataObjectName" label="数据对象名称">
                <Input />
              </Form.Item>
              <Form.Item name="itemSubjectRef" label="数据类型">
                <Select>
                  <Option value="string">字符串</Option>
                  <Option value="integer">整数</Option>
                  <Option value="boolean">布尔</Option>
                  <Option value="date">日期</Option>
                  <Option value="object">对象</Option>
                </Select>
              </Form.Item>
              <Form.Item name="defaultValue" label="默认值">
                <Input.TextArea rows={2} />
              </Form.Item>
            </>
          )
        case 'POOL':
        case 'LANE':
          return (
            <>
              <Form.Item name="processRef" label="流程引用">
                <Input placeholder="关联的流程ID" />
              </Form.Item>
              <Form.Item name="name" label="名称">
                <Input placeholder="泳池/泳道名称" />
              </Form.Item>
            </>
          )
        default:
          return (
            <Form.Item name="documentation" label="说明文档">
              <Input.TextArea rows={3} placeholder="节点的说明文档" />
            </Form.Item>
          )
      }
    }

    return (
      <Form form={nodeForm} layout="vertical" onFinish={handleNodeConfigSave}>
        <Form.Item name="nodeName" label="节点名称" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="nodeType" label="节点类型">
          <Input disabled />
        </Form.Item>
        <Form.Item name="documentation" label="节点描述">
          <Input.TextArea rows={2} />
        </Form.Item>
        <Divider />
        {getNodeConfigFields()}
        <Form.Item style={{ marginTop: 16 }}>
          <Button type="primary" htmlType="submit" block>
            保存配置
          </Button>
        </Form.Item>
      </Form>
    )
  }

  const handleStartProcess = async (processKey: string) => {
    try {
      await workflowApi.startProcess(0, { processKey })
      message.success('流程启动成功')
    } catch (e) {
      console.error(e)
    }
  }

  const deployedColumns = [
    { title: '流程名称', dataIndex: 'name', key: 'name' },
    { title: '流程Key', dataIndex: 'key', key: 'key' },
    { title: '版本', dataIndex: 'version', key: 'version' },
    { title: '部署时间', dataIndex: 'deploymentTime', key: 'deploymentTime' },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleStartProcess(record.key)}>
            启动
          </Button>
        </Space>
      ),
    },
  ]

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
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/workflow')}>
              返回
            </Button>
            <Form form={form} layout="inline">
              <Form.Item name="workflowName" label="流程名称" rules={[{ required: true }]}>
                <Input placeholder="请输入流程名称" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="workflowCode" label="流程编码" rules={[{ required: true }]}>
                <Input placeholder="请输入流程编码" style={{ width: 150 }} />
              </Form.Item>
              <Form.Item name="category" label="流程分类">
                <Select style={{ width: 120 }}>
                  <Option value="default">默认分类</Option>
                  <Option value="approval">审批流程</Option>
                  <Option value="business">业务流程</Option>
                  <Option value="system">系统流程</Option>
                </Select>
              </Form.Item>
              <Form.Item name="description" label="描述">
                <Input placeholder="流程描述" style={{ width: 200 }} />
              </Form.Item>
            </Form>
          </Space>
          <Space>
            <Button icon={<UndoOutlined />} onClick={handleUndo} disabled={historyIndex <= 0}>
              撤销
            </Button>
            <Button icon={<RedoOutlined />} onClick={handleRedo} disabled={historyIndex >= history.length - 1}>
              重做
            </Button>
            <Divider type="vertical" />
            <Button icon={<EyeOutlined />} onClick={handleViewBpmn}>
              BPMN XML
            </Button>
            <Button icon={<CloudUploadOutlined />} onClick={() => { setDeployVisible(true); loadProcessList(); }}>
              已部署流程
            </Button>
            <Button icon={<SaveOutlined />} type="primary" onClick={handleSave}>
              保存
            </Button>
            <Button icon={<RocketOutlined />} type="primary" onClick={handleDeploy}>
              部署
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
            <div style={{ padding: 12 }}>
              <Collapse
                activeKey={expandedCategory}
                onChange={(keys) => setExpandedCategory(keys as string[])}
                ghost
              >
                {bpmnNodeCategories.map(cat => (
                  <Panel
                    key={cat.key}
                    header={
                      <span>
                        {cat.icon} {cat.name}
                        <Badge
                          count={bpmnNodeTypes[cat.key]?.length || 0}
                          size="small"
                          style={{ marginLeft: 8, backgroundColor: cat.color }}
                        />
                      </span>
                    }
                  >
                    {bpmnNodeTypes[cat.key]?.map(nodeType => (
                      <BpmnNodeItem key={nodeType.type} nodeType={nodeType} />
                    ))}
                  </Panel>
                ))}
              </Collapse>
            </div>
          </Sider>
          <Content style={{ background: '#f5f5f5', position: 'relative' }}>
            <div
              id="bpmn-canvas"
              ref={(node) => canvasDrop(node)}
              className="designer-canvas"
              style={{
                width: '100%',
                height: '100%',
                overflow: 'auto',
                position: 'relative',
              }}
              onClick={handleCanvasClick}
            >
              <svg
                ref={svgRef}
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  height: '100%',
                  pointerEvents: 'none',
                  zIndex: 1,
                }}
              >
                <defs>
                  <marker
                    id="bpmn-arrowhead"
                    markerWidth="10"
                    markerHeight="7"
                    refX="9"
                    refY="3.5"
                    orient="auto"
                  >
                    <polygon points="0 0, 10 3.5, 0 7" fill="#1677ff" />
                  </marker>
                </defs>
                <g style={{ pointerEvents: 'auto' }}>
                  {renderEdges()}
                </g>
              </svg>
              {nodes.length === 0 && (
                <div
                  style={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    textAlign: 'center',
                    color: '#999',
                  }}
                >
                  <FileTextOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />
                  <div style={{ marginTop: 16, fontSize: 16 }}>从左侧拖拽BPMN节点到此处</div>
                  <div style={{ fontSize: 12, marginTop: 8 }}>开始设计你的工作流</div>
                </div>
              )}
              {nodes.map(node => (
                <BpmnCanvasNode
                  key={node.id}
                  node={node}
                  isSelected={selectedNodeId === node.id}
                  onSelect={() => handleSelectNode(node.id)}
                  onDelete={() => handleDeleteNode(node.id)}
                  onDragEnd={(x, y) => handleNodeDragEnd(node.id, x, y)}
                  onStartConnect={() => handleStartConnect(node.id)}
                />
              ))}
              {connecting && (
                <div style={{
                  position: 'fixed',
                  top: 16,
                  left: '50%',
                  transform: 'translateX(-50%)',
                  background: '#1677ff',
                  color: '#fff',
                  padding: '8px 16px',
                  borderRadius: 4,
                  zIndex: 1000,
                }}>
                  点击目标节点完成连线，或点击空白处取消
                </div>
              )}
            </div>
          </Content>
          <Sider
            width={360}
            className="property-panel"
            style={{ overflow: 'auto' }}
          >
            {selectedNode ? (
              <div style={{ padding: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 18 }}>{getBpmnNodeIcon(selectedNode.nodeType)}</span>
                    <strong>{selectedNode.nodeName}</strong>
                  </div>
                  <Tag color={getBpmnNodeColor(selectedNode.nodeType)}>{selectedNode.nodeType}</Tag>
                </div>
                <Tabs
                  defaultActiveKey="basic"
                  size="small"
                  tabBarStyle={{ margin: '0 -16px 16px', padding: '0 16px', borderBottom: '1px solid #e8e8e8' }}
                  items={[
                    {
                      key: 'basic',
                      label: (
                        <span>
                          <SettingOutlined /> 基本属性
                        </span>
                      ),
                      children: renderBpmnNodeConfigPanel(),
                    },
                    {
                      key: 'flow',
                      label: (
                        <span>
                          <FilterOutlined /> 流转条件
                        </span>
                      ),
                      children: (
                        <div>
                          <h4 style={{ marginBottom: 12 }}>出线配置</h4>
                          {edges.filter(e => e.source === selectedNodeId).length === 0 ? (
                            <div style={{ color: '#999', textAlign: 'center', padding: '20px 0' }}>
                              暂无出线，请先连接其他节点
                            </div>
                          ) : (
                            edges.filter(e => e.source === selectedNodeId).map(e => {
                              const target = nodes.find(n => n.id === e.target)
                              return (
                                <Card key={e.id} size="small" style={{ marginBottom: 8 }}>
                                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                                    <span style={{ fontWeight: 'bold' }}>流向: {target?.nodeName || e.target}</span>
                                    <Popconfirm title="删除此连线？" onConfirm={() => handleDeleteEdge(e.id)}>
                                      <Button type="text" danger size="small" icon={<DeleteOutlined />} />
                                    </Popconfirm>
                                  </div>
                                  <Form layout="vertical">
                                    <Form.Item label="流转名称">
                                      <Input
                                        placeholder="给这条线起个名字"
                                        value={e.flowName || ''}
                                        onChange={(ev) => {
                                          const newEdges = edges.map(edge =>
                                            edge.id === e.id ? { ...edge, flowName: ev.target.value } : edge
                                          )
                                          setEdges(newEdges)
                                        }}
                                      />
                                    </Form.Item>
                                    <Form.Item label="条件表达式">
                                      <Input.TextArea
                                        rows={2}
                                        placeholder='如: ${amount > 1000}'
                                        value={e.conditionExpression || ''}
                                        onChange={(ev) => {
                                          const newEdges = edges.map(edge =>
                                            edge.id === e.id ? { ...edge, conditionExpression: ev.target.value } : edge
                                          )
                                          setEdges(newEdges)
                                        }}
                                      />
                                    </Form.Item>
                                  </Form>
                                </Card>
                              )
                            })
                          )}
                        </div>
                      ),
                    },
                  ]}
                />
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '60px 20px', color: '#999' }}>
                <SafetyOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />
                <div style={{ marginTop: 16 }}>请选择节点</div>
                <div style={{ fontSize: 12, marginTop: 8 }}>点击画布中的节点进行配置</div>
              </div>
            )}
          </Sider>
        </Layout>

        <Modal
          title="已部署的流程"
          open={deployVisible}
          onCancel={() => setDeployVisible(false)}
          footer={null}
          width={900}
        >
          <Table
            dataSource={processList}
            columns={deployedColumns}
            rowKey="id"
            size="small"
          />
        </Modal>

        <Modal
          title="BPMN 2.0 XML"
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
      </Layout>
    </DndProvider>
  )
}

export default WorkflowDesigner
