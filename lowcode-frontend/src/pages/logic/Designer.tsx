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
  ThunderboltOutlined,
  BugOutlined,
  StepForwardOutlined,
  PauseCircleOutlined,
  StopOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  BranchOutlined,
  RetweetOutlined,
  VariableOutlined,
  BellOutlined,
  SyncOutlined,
  PlaySquareOutlined,
  FilterOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import { DndProvider, useDrag, useDrop } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'
import { BusinessLogic, LogicNode, LogicEdge, logicApi, debugApi } from '@/api/flow'
import { useAppStore } from '@/store/appStore'

const { Header, Sider, Content } = Layout
const { Option } = Select
const { Panel } = Collapse

const nodeCategories = [
  { key: 'trigger', name: '触发器', icon: '⚡', color: '#faad14' },
  { key: 'control', name: '控制流程', icon: '🔀', color: '#1677ff' },
  { key: 'data', name: '数据操作', icon: '🗄️', color: '#52c41a' },
  { key: 'variable', name: '变量操作', icon: '📦', color: '#722ed1' },
  { key: 'notify', name: '通知交互', icon: '🔔', color: '#eb2f96' },
  { key: 'advanced', name: '高级操作', icon: '⚙️', color: '#13c2c2' },
]

const nodeTypes: Record<string, any[]> = {
  trigger: [
    { type: 'SCHEDULE', name: '定时任务', icon: '⏰', description: '按Cron表达式定时触发' },
    { type: 'API', name: 'API调用', icon: '🌐', description: '接收API请求触发' },
    { type: 'TABLE_EVENT', name: '数据表事件', icon: '📊', description: '数据表增删改时触发' },
    { type: 'MANUAL', name: '手动触发', icon: '👆', description: '用户手动点击触发' },
  ],
  control: [
    { type: 'CONDITION', name: '条件分支', icon: '🔀', description: '根据条件选择执行路径' },
    { type: 'LOOP', name: '循环', icon: '🔄', description: '循环执行节点' },
    { type: 'PARALLEL', name: '并行执行', icon: '⚡', description: '多个节点并行执行' },
    { type: 'DELAY', name: '延迟等待', icon: '⏳', description: '等待一段时间后执行' },
  ],
  data: [
    { type: 'QUERY', name: '查询数据', icon: '🔍', description: '从数据库查询数据' },
    { type: 'INSERT', name: '新增数据', icon: '➕', description: '向数据库插入数据' },
    { type: 'UPDATE', name: '更新数据', icon: '✏️', description: '更新数据库数据' },
    { type: 'DELETE', name: '删除数据', icon: '🗑️', description: '删除数据库数据' },
    { type: 'BATCH_INSERT', name: '批量新增', icon: '📥', description: '批量插入数据' },
    { type: 'BATCH_UPDATE', name: '批量更新', icon: '📤', description: '批量更新数据' },
  ],
  variable: [
    { type: 'ASSIGN', name: '变量赋值', icon: '📝', description: '给变量赋值' },
    { type: 'INCREMENT', name: '变量自增', icon: '➕', description: '变量值自增' },
    { type: 'DECREMENT', name: '变量自减', icon: '➖', description: '变量值自减' },
    { type: 'ARRAY_OP', name: '数组操作', icon: '📋', description: '对数组进行操作' },
    { type: 'OBJECT_OP', name: '对象操作', icon: '🎯', description: '对对象进行操作' },
  ],
  notify: [
    { type: 'EMAIL', name: '发送邮件', icon: '📧', description: '发送电子邮件' },
    { type: 'SMS', name: '发送短信', icon: '📱', description: '发送手机短信' },
    { type: 'IN_APP', name: '站内通知', icon: '🔔', description: '发送站内通知' },
    { type: 'WEBHOOK', name: 'WebHook', icon: '🌐', description: '调用外部WebHook' },
    { type: 'DINGTALK', name: '钉钉通知', icon: '💬', description: '发送钉钉消息' },
    { type: 'WECHAT', name: '企业微信', icon: '💚', description: '发送企业微信消息' },
  ],
  advanced: [
    { type: 'API_CALL', name: '调用API', icon: '🌐', description: '调用外部HTTP API' },
    { type: 'CODE', name: '自定义代码', icon: '⌨️', description: '执行自定义代码片段' },
    { type: 'SUB_LOGIC', name: '子流程', icon: '📦', description: '调用另一个业务逻辑' },
    { type: 'WORKFLOW', name: '启动工作流', icon: '🔄', description: '启动Flowable工作流' },
    { type: 'TRANSACTION', name: '事务', icon: '🔒', description: '将多个操作包裹在事务中' },
  ],
}

const getNodeColor = (nodeType: string): string => {
  for (const [cat, nodes] of Object.entries(nodeTypes)) {
    if (nodes.some(n => n.type === nodeType)) {
      return nodeCategories.find(c => c.key === cat)?.color || '#1677ff'
    }
  }
  return '#1677ff'
}

const getNodeIcon = (nodeType: string): string => {
  for (const nodes of Object.values(nodeTypes)) {
    const node = nodes.find(n => n.type === nodeType)
    if (node) return node.icon
  }
  return '📦'
}

interface NodeItemProps {
  nodeType: any
  onDragStart?: () => void
}

const NodeItem: React.FC<NodeItemProps> = ({ nodeType }) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'LOGIC_NODE',
    item: { nodeType: nodeType.type, nodeName: nodeType.name, nodeCategory: nodeType.icon },
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
          border: '1px solid #e8e8e8',
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

interface CanvasNodeProps {
  node: LogicNode
  isSelected: boolean
  onSelect: () => void
  onDelete: () => void
  onDragEnd: (x: number, y: number) => void
  onStartConnect: () => void
}

const CanvasNode: React.FC<CanvasNodeProps> = ({ node, isSelected, onSelect, onDelete, onDragEnd, onStartConnect }) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'EXISTING_NODE',
    item: { nodeId: node.nodeId },
    end: (_item, monitor) => {
      const offset = monitor.getClientOffset()
      if (offset) {
        const canvas = document.getElementById('logic-canvas')
        if (canvas) {
          const rect = canvas.getBoundingClientRect()
          onDragEnd(offset.x - rect.left - 80, offset.y - rect.top - 30)
        }
      }
    },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  }))

  const color = getNodeColor(node.nodeType)
  const icon = getNodeIcon(node.nodeType)

  return (
    <div
      ref={drag}
      className={`node-wrapper ${isSelected ? 'selected' : ''}`}
      style={{
        left: node.positionX || 100,
        top: node.positionY || 100,
        width: node.width || 160,
        opacity: isDragging ? 0.5 : 1,
      }}
      onClick={(e) => { e.stopPropagation(); onSelect() }}
    >
      <div
        style={{
          padding: '12px 16px',
          background: '#fff',
          border: `2px solid ${isSelected ? '#1677ff' : color}`,
          borderRadius: 8,
          boxShadow: isSelected ? '0 4px 12px rgba(22, 119, 255, 0.25)' : '0 2px 8px rgba(0, 0, 0, 0.1)',
          position: 'relative',
        }}
      >
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
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
          <span style={{ fontSize: 18 }}>{icon}</span>
          <strong style={{ fontSize: 13 }}>{node.nodeName}</strong>
        </div>
        <div style={{ fontSize: 11, color: '#999' }}>{node.nodeType}</div>
        {node.nodeConfig && (
          <div style={{ fontSize: 10, color: '#666', marginTop: 4, background: '#f5f5f5', padding: 4, borderRadius: 2 }}>
            已配置
          </div>
        )}
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
          }}
          onClick={(e) => { e.stopPropagation(); onStartConnect() }}
        />
      </div>
    </div>
  )
}

const LogicDesigner: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { currentApp } = useAppStore()
  const [logic, setLogic] = useState<BusinessLogic | null>(null)
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [connecting, setConnecting] = useState(false)
  const [connectStart, setConnectStart] = useState<string | null>(null)
  const [form] = Form.useForm()
  const [nodeForm] = Form.useForm()
  const [codeModalVisible, setCodeModalVisible] = useState(false)
  const [codeContent, setCodeContent] = useState('')
  const [expandedCategory, setExpandedCategory] = useState<string[]>(['trigger', 'control'])
  const [history, setHistory] = useState<any[]>([])
  const [historyIndex, setHistoryIndex] = useState(-1)
  const [debugVisible, setDebugVisible] = useState(false)
  const [debugSessionId, setDebugSessionId] = useState<string | null>(null)
  const [debugStatus, setDebugStatus] = useState<any>(null)
  const [variables, setVariables] = useState<any[]>([])
  const [breakpoints, setBreakpoints] = useState<string[]>([])

  const canvasRef = useRef<HTMLDivElement>(null)
  const svgRef = useRef<SVGSVGElement>(null)

  const loadLogic = useCallback(async () => {
    if (!id || id === 'undefined') {
      setLogic({
        appId: currentApp?.id || 1,
        logicName: '',
        logicCode: '',
        triggerType: 'MANUAL',
        status: 'DRAFT',
        nodes: [],
        edges: [],
      })
      form.setFieldsValue({
        logicName: '',
        logicCode: '',
        triggerType: 'MANUAL',
        description: '',
      })
      return
    }
    try {
      const res = await logicApi.get(Number(id))
      setLogic(res.data || null)
      form.setFieldsValue(res.data)
    } catch (e) {
      console.error(e)
    }
  }, [id, currentApp])

  useEffect(() => {
    loadLogic()
  }, [loadLogic])

  const saveHistory = useCallback(() => {
    const newHistory = history.slice(0, historyIndex + 1)
    newHistory.push({
      nodes: JSON.parse(JSON.stringify(logic?.nodes || [])),
      edges: JSON.parse(JSON.stringify(logic?.edges || [])),
    })
    setHistory(newHistory)
    setHistoryIndex(newHistory.length - 1)
  }, [logic, history, historyIndex])

  const handleUndo = () => {
    if (historyIndex > 0) {
      const newIndex = historyIndex - 1
      setHistoryIndex(newIndex)
      if (logic) {
        setLogic({
          ...logic,
          nodes: history[newIndex].nodes,
          edges: history[newIndex].edges,
        })
      }
    }
  }

  const handleRedo = () => {
    if (historyIndex < history.length - 1) {
      const newIndex = historyIndex + 1
      setHistoryIndex(newIndex)
      if (logic) {
        setLogic({
          ...logic,
          nodes: history[newIndex].nodes,
          edges: history[newIndex].edges,
        })
      }
    }
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      if (!logic) return
      const data = {
        ...logic,
        ...values,
      }
      if (logic.id) {
        await logicApi.update(data)
        message.success('保存成功')
      } else {
        const res = await logicApi.save(data)
        setLogic(res.data)
        message.success('创建成功')
        navigate(`/logic/designer/${res.data.id}`, { replace: true })
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handlePublish = async () => {
    if (!logic?.id) return
    try {
      await logicApi.publish(logic.id)
      message.success('发布成功')
      loadLogic()
    } catch (e) {
      console.error(e)
    }
  }

  const handleViewCode = async () => {
    if (!logic?.id) return
    try {
      const res = await logicApi.generateCode(logic.id)
      setCodeContent(res.data || '')
      setCodeModalVisible(true)
    } catch (e) {
      console.error(e)
    }
  }

  const handleDropNode = (item: any, offsetX: number, offsetY: number) => {
    if (!logic) return
    saveHistory()

    const newNode: LogicNode = {
      nodeId: `node_${Date.now()}`,
      nodeName: item.nodeName,
      nodeType: item.nodeType,
      nodeCategory: item.nodeCategory,
      positionX: offsetX,
      positionY: offsetY,
      width: 160,
      height: 80,
      nodeConfig: '{}',
    }

    const newNodes = [...(logic.nodes || []), newNode]
    setLogic({ ...logic, nodes: newNodes })
    setSelectedNodeId(newNode.nodeId)
    loadNodeForm(newNode)
  }

  const [{ isOver }, canvasDrop] = useDrop(() => ({
    accept: 'LOGIC_NODE',
    drop: (item: any, monitor) => {
      const offset = monitor.getClientOffset()
      const canvas = document.getElementById('logic-canvas')
      if (canvas && offset) {
        const rect = canvas.getBoundingClientRect()
        handleDropNode(item, offset.x - rect.left - 80, offset.y - rect.top - 40)
      }
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  }))

  const handleNodeDragEnd = (nodeId: string, x: number, y: number) => {
    if (!logic) return
    saveHistory()

    const newNodes = logic.nodes?.map(n =>
      n.nodeId === nodeId ? { ...n, positionX: Math.max(0, x), positionY: Math.max(0, y) } : n
    ) || []
    setLogic({ ...logic, nodes: newNodes })
  }

  const handleDeleteNode = (nodeId: string) => {
    if (!logic) return
    saveHistory()

    const newNodes = logic.nodes?.filter(n => n.nodeId !== nodeId) || []
    const newEdges = logic.edges?.filter(e => e.sourceNodeId !== nodeId && e.targetNodeId !== nodeId) || []
    setLogic({ ...logic, nodes: newNodes, edges: newEdges })
    if (selectedNodeId === nodeId) {
      setSelectedNodeId(null)
    }
  }

  const handleSelectNode = (nodeId: string) => {
    setSelectedNodeId(nodeId)
    const node = logic?.nodes?.find(n => n.nodeId === nodeId)
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

      const targetNode = logic?.nodes?.find(n => {
        const nx = n.positionX || 0
        const ny = n.positionY || 0
        const nw = n.width || 160
        const nh = n.height || 80
        return x >= nx && x <= nx + nw && y >= ny && y <= ny + nh
      })

      if (targetNode && targetNode.nodeId !== connectStart) {
        saveHistory()
        const newEdge: LogicEdge = {
          edgeId: `edge_${Date.now()}`,
          sourceNodeId: connectStart,
          targetNodeId: targetNode.nodeId,
          edgeConfig: '{}',
        }
        const newEdges = [...(logic?.edges || []), newEdge]
        setLogic({ ...logic!, edges: newEdges })
      }

      setConnecting(false)
      setConnectStart(null)
    } else {
      setSelectedNodeId(null)
    }
  }

  const loadNodeForm = (node: LogicNode) => {
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
      if (!logic || !selectedNodeId) return
      saveHistory()

      const { nodeName, ...config } = values
      const newNodes = logic.nodes?.map(n =>
        n.nodeId === selectedNodeId
          ? { ...n, nodeName, nodeConfig: JSON.stringify(config) }
          : n
      ) || []
      setLogic({ ...logic, nodes: newNodes })
      message.success('节点配置保存成功')
    } catch (e) {
      console.error(e)
    }
  }

  const handleDeleteEdge = (edgeId: string) => {
    if (!logic) return
    saveHistory()
    const newEdges = logic.edges?.filter(e => e.edgeId !== edgeId) || []
    setLogic({ ...logic, edges: newEdges })
  }

  const selectedNode = logic?.nodes?.find(n => n.nodeId === selectedNodeId)

  const renderEdges = () => {
    return logic?.edges?.map(edge => {
      const sourceNode = logic.nodes?.find(n => n.nodeId === edge.sourceNodeId)
      const targetNode = logic.nodes?.find(n => n.nodeId === edge.targetNodeId)
      if (!sourceNode || !targetNode) return null

      const sx = (sourceNode.positionX || 0) + (sourceNode.width || 160)
      const sy = (sourceNode.positionY || 0) + 40
      const tx = targetNode.positionX || 0
      const ty = (targetNode.positionY || 0) + 40

      const midX = (sx + tx) / 2

      return (
        <g key={edge.edgeId}>
          <path
            d={`M ${sx} ${sy} C ${midX} ${sy}, ${midX} ${ty}, ${tx} ${ty}`}
            stroke="#1677ff"
            strokeWidth={2}
            fill="none"
            markerEnd="url(#arrowhead)"
            style={{ cursor: 'pointer' }}
            onClick={(e) => {
              e.stopPropagation()
              Modal.confirm({
                title: '确认删除连线？',
                onOk: () => handleDeleteEdge(edge.edgeId),
              })
            }}
          />
          {edge.conditionExpression && (
            <text
              x={midX}
              y={(sy + ty) / 2 - 8}
              textAnchor="middle"
              fill="#ff4d4f"
              fontSize={12}
            >
              {edge.conditionExpression}
            </text>
          )}
        </g>
      )
    })
  }

  const renderNodeConfigPanel = () => {
    if (!selectedNode) return null

    const type = selectedNode.nodeType

    const getNodeConfigFields = () => {
      switch (type) {
        case 'SCHEDULE':
          return (
            <>
              <Form.Item name="cronExpression" label="Cron表达式" rules={[{ required: true }]}>
                <Input placeholder="如: 0 0 12 * * ?" />
              </Form.Item>
              <Form.Item name="timezone" label="时区">
                <Select defaultValue="Asia/Shanghai">
                  <Option value="Asia/Shanghai">Asia/Shanghai</Option>
                  <Option value="UTC">UTC</Option>
                </Select>
              </Form.Item>
            </>
          )
        case 'API':
          return (
            <>
              <Form.Item name="method" label="请求方法" rules={[{ required: true }]}>
                <Select>
                  <Option value="GET">GET</Option>
                  <Option value="POST">POST</Option>
                  <Option value="PUT">PUT</Option>
                  <Option value="DELETE">DELETE</Option>
                </Select>
              </Form.Item>
              <Form.Item name="path" label="API路径" rules={[{ required: true }]}>
                <Input placeholder="/api/trigger" />
              </Form.Item>
            </>
          )
        case 'TABLE_EVENT':
          return (
            <>
              <Form.Item name="tableName" label="监听表名" rules={[{ required: true }]}>
                <Input placeholder="如: sys_user" />
              </Form.Item>
              <Form.Item name="eventType" label="监听事件" rules={[{ required: true }]}>
                <Select mode="multiple">
                  <Option value="INSERT">新增</Option>
                  <Option value="UPDATE">更新</Option>
                  <Option value="DELETE">删除</Option>
                </Select>
              </Form.Item>
            </>
          )
        case 'CONDITION':
          return (
            <Form.Item name="expression" label="条件表达式" rules={[{ required: true }]}>
              <Input.TextArea rows={3} placeholder='如: ${age} > 18 && ${status} == "active"' />
            </Form.Item>
          )
        case 'LOOP':
          return (
            <>
              <Form.Item name="loopType" label="循环类型" rules={[{ required: true }]}>
                <Select>
                  <Option value="FOR">For循环</Option>
                  <Option value="WHILE">While循环</Option>
                  <Option value="FOREACH">遍历集合</Option>
                </Select>
              </Form.Item>
              <Form.Item name="condition" label="循环条件">
                <Input.TextArea rows={3} placeholder='如: ${i} < 10' />
              </Form.Item>
            </>
          )
        case 'DELAY':
          return (
            <>
              <Form.Item name="delayType" label="延迟类型" rules={[{ required: true }]}>
                <Select>
                  <Option value="SECONDS">秒</Option>
                  <Option value="MINUTES">分钟</Option>
                  <Option value="HOURS">小时</Option>
                  <Option value="MILLISECONDS">毫秒</Option>
                </Select>
              </Form.Item>
              <Form.Item name="delayValue" label="延迟值" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
            </>
          )
        case 'QUERY':
        case 'INSERT':
        case 'UPDATE':
        case 'DELETE':
          return (
            <>
              <Form.Item name="modelId" label="数据模型" rules={[{ required: true }]}>
                <Select placeholder="选择数据模型">
                  <Option value="1">用户表</Option>
                  <Option value="2">订单表</Option>
                  <Option value="3">产品表</Option>
                </Select>
              </Form.Item>
              <Form.Item name="condition" label="查询条件">
                <Input.TextArea rows={3} placeholder='如: {"status": 1}' />
              </Form.Item>
              <Form.Item name="resultVariable" label="结果变量名">
                <Input placeholder="如: resultList" />
              </Form.Item>
            </>
          )
        case 'ASSIGN':
          return (
            <>
              <Form.Item name="variableName" label="变量名" rules={[{ required: true }]}>
                <Input placeholder="如: userName" />
              </Form.Item>
              <Form.Item name="value" label="变量值" rules={[{ required: true }]}>
                <Input.TextArea rows={3} placeholder='如: ${user.name}' />
              </Form.Item>
            </>
          )
        case 'INCREMENT':
        case 'DECREMENT':
          return (
            <>
              <Form.Item name="variableName" label="变量名" rules={[{ required: true }]}>
                <Input placeholder="如: count" />
              </Form.Item>
              <Form.Item name="step" label="步长">
                <InputNumber style={{ width: '100%' }} defaultValue={1} />
              </Form.Item>
            </>
          )
        case 'EMAIL':
          return (
            <>
              <Form.Item name="to" label="收件人" rules={[{ required: true }]}>
                <Input placeholder="多个邮箱用逗号分隔" />
              </Form.Item>
              <Form.Item name="subject" label="邮件主题" rules={[{ required: true }]}>
                <Input />
              </Form.Item>
              <Form.Item name="content" label="邮件内容" rules={[{ required: true }]}>
                <Input.TextArea rows={4} />
              </Form.Item>
            </>
          )
        case 'SMS':
          return (
            <>
              <Form.Item name="phone" label="手机号码" rules={[{ required: true }]}>
                <Input placeholder="多个号码用逗号分隔" />
              </Form.Item>
              <Form.Item name="templateCode" label="模板编码" rules={[{ required: true }]}>
                <Input />
              </Form.Item>
              <Form.Item name="params" label="模板参数">
                <Input.TextArea rows={3} placeholder='{"code": "1234"}' />
              </Form.Item>
            </>
          )
        case 'API_CALL':
          return (
            <>
              <Form.Item name="url" label="API地址" rules={[{ required: true }]}>
                <Input placeholder="https://api.example.com/data" />
              </Form.Item>
              <Form.Item name="method" label="请求方法" rules={[{ required: true }]}>
                <Select>
                  <Option value="GET">GET</Option>
                  <Option value="POST">POST</Option>
                  <Option value="PUT">PUT</Option>
                  <Option value="DELETE">DELETE</Option>
                </Select>
              </Form.Item>
              <Form.Item name="headers" label="请求头">
                <Input.TextArea rows={3} placeholder='{"Authorization": "Bearer xxx"}' />
              </Form.Item>
              <Form.Item name="body" label="请求体">
                <Input.TextArea rows={4} />
              </Form.Item>
              <Form.Item name="resultVariable" label="结果变量名">
                <Input placeholder="如: apiResult" />
              </Form.Item>
            </>
          )
        case 'CODE':
          return (
            <Form.Item name="code" label="代码片段" rules={[{ required: true }]}>
              <Input.TextArea rows={12} placeholder="// 支持Java/Groovy代码片段&#10;return userService.findById(userId);" />
            </Form.Item>
          )
        default:
          return (
            <Form.Item name="description" label="节点描述">
              <Input.TextArea rows={3} />
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

  const handleDebugStart = async () => {
    if (!logic?.id) {
      message.warning('请先保存逻辑')
      return
    }
    try {
      const res = await debugApi.start(logic.id)
      setDebugSessionId(res.data.sessionId)
      setDebugStatus(res.data)
      setVariables(res.data.variables || [])
      setDebugVisible(true)
      message.success('调试已启动')
    } catch (e) {
      console.error(e)
    }
  }

  const handleDebugStep = async () => {
    if (!debugSessionId) return
    try {
      const res = await debugApi.stepForward(debugSessionId)
      setDebugStatus(res.data)
      setVariables(res.data.variables || [])
      if (res.data.finished) {
        message.success('执行完成')
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handleDebugStop = async () => {
    if (!debugSessionId) return
    try {
      await debugApi.stop(debugSessionId)
      setDebugSessionId(null)
      setDebugVisible(false)
      message.success('调试已停止')
    } catch (e) {
      console.error(e)
    }
  }

  const handleToggleBreakpoint = (nodeId: string) => {
    if (breakpoints.includes(nodeId)) {
      setBreakpoints(breakpoints.filter(b => b !== nodeId))
    } else {
      setBreakpoints([...breakpoints, nodeId])
    }
  }

  const varColumns = [
    { title: '变量名', dataIndex: 'name', key: 'name', width: 150 },
    { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
    { title: '值', dataIndex: 'value', key: 'value' },
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
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/logic')}>
              返回
            </Button>
            <Form form={form} layout="inline">
              <Form.Item name="logicName" label="逻辑名称" rules={[{ required: true }]}>
                <Input placeholder="请输入逻辑名称" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="logicCode" label="逻辑编码" rules={[{ required: true }]}>
                <Input placeholder="请输入逻辑编码" style={{ width: 150 }} />
              </Form.Item>
              <Form.Item name="triggerType" label="触发类型">
                <Select style={{ width: 120 }}>
                  <Option value="MANUAL">手动触发</Option>
                  <Option value="SCHEDULE">定时任务</Option>
                  <Option value="API">API调用</Option>
                  <Option value="EVENT">事件触发</Option>
                </Select>
              </Form.Item>
              <Form.Item name="description" label="描述">
                <Input placeholder="逻辑描述" style={{ width: 200 }} />
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
            <Button icon={<BugOutlined />} onClick={handleDebugStart}>
              调试
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
            <div style={{ padding: 12 }}>
              <Collapse
                activeKey={expandedCategory}
                onChange={(keys) => setExpandedCategory(keys as string[])}
                ghost
              >
                {nodeCategories.map(cat => (
                  <Panel
                    key={cat.key}
                    header={
                      <span>
                        {cat.icon} {cat.name}
                        <Badge
                          count={nodeTypes[cat.key]?.length || 0}
                          size="small"
                          style={{ marginLeft: 8, backgroundColor: cat.color }}
                        />
                      </span>
                    }
                  >
                    {nodeTypes[cat.key]?.map(nodeType => (
                      <NodeItem key={nodeType.type} nodeType={nodeType} />
                    ))}
                  </Panel>
                ))}
              </Collapse>
            </div>
          </Sider>
          <Content style={{ background: '#f5f5f5', position: 'relative' }}>
            <div
              id="logic-canvas"
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
                    id="arrowhead"
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
              {(!logic?.nodes || logic.nodes.length === 0) && (
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
                  <ThunderboltOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />
                  <div style={{ marginTop: 16, fontSize: 16 }}>从左侧拖拽节点到此处</div>
                  <div style={{ fontSize: 12, marginTop: 8 }}>开始设计你的业务逻辑</div>
                </div>
              )}
              {logic?.nodes?.map(node => (
                <CanvasNode
                  key={node.nodeId}
                  node={node}
                  isSelected={selectedNodeId === node.nodeId}
                  onSelect={() => handleSelectNode(node.nodeId)}
                  onDelete={() => handleDeleteNode(node.nodeId)}
                  onDragEnd={(x, y) => handleNodeDragEnd(node.nodeId, x, y)}
                  onStartConnect={() => handleStartConnect(node.nodeId)}
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
            width={340}
            className="property-panel"
            style={{ overflow: 'auto' }}
          >
            {selectedNode ? (
              <div style={{ padding: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 18 }}>{getNodeIcon(selectedNode.nodeType)}</span>
                    <strong>{selectedNode.nodeName}</strong>
                  </div>
                  <Tag color={getNodeColor(selectedNode.nodeType)}>{selectedNode.nodeType}</Tag>
                </div>
                {renderNodeConfigPanel()}
                <Divider />
                <Button
                  block
                  type={breakpoints.includes(selectedNodeId) ? 'primary' : 'default'}
                  icon={<FilterOutlined />}
                  onClick={() => handleToggleBreakpoint(selectedNodeId)}
                >
                  {breakpoints.includes(selectedNodeId) ? '取消断点' : '设置断点'}
                </Button>
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '60px 20px', color: '#999' }}>
                <SettingOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />
                <div style={{ marginTop: 16 }}>请选择节点</div>
                <div style={{ fontSize: 12, marginTop: 8 }}>点击画布中的节点进行配置</div>
              </div>
            )}
          </Sider>
        </Layout>
        {debugVisible && (
          <Drawer
            title={
              <Space>
                <BugOutlined />
                调试面板
                {debugStatus?.currentNode && (
                  <Tag color="processing">执行中: {debugStatus.currentNode}</Tag>
                )}
              </Space>
            }
            placement="bottom"
            open={debugVisible}
            onClose={handleDebugStop}
            height={400}
            extra={
              <Space>
                <Button icon={<StepForwardOutlined />} onClick={handleDebugStep} disabled={debugStatus?.finished}>
                  单步执行
                </Button>
                <Button icon={<PlayCircleOutlined />} disabled>
                  继续执行
                </Button>
                <Button icon={<PauseCircleOutlined />} disabled>
                  暂停
                </Button>
                <Button icon={<StopOutlined />} danger onClick={handleDebugStop}>
                  停止
                </Button>
              </Space>
            }
          >
            <Tabs defaultActiveKey="variables">
              <Tabs.TabPane tab="变量快照" key="variables">
                <Table
                  dataSource={variables}
                  columns={varColumns}
                  rowKey="name"
                  size="small"
                />
              </Tabs.TabPane>
              <Tabs.TabPane tab="执行日志" key="logs">
                <div style={{
                  background: '#1e1e1e',
                  color: '#d4d4d4',
                  padding: 16,
                  borderRadius: 4,
                  fontFamily: 'Consolas, monospace',
                  fontSize: 12,
                  height: 300,
                  overflow: 'auto',
                }}>
                  {debugStatus?.logs?.map((log: string, i: number) => (
                    <div key={i}>[{new Date().toLocaleTimeString()}] {log}</div>
                  ))}
                  {(!debugStatus?.logs || debugStatus.logs.length === 0) && (
                    <div style={{ color: '#666' }}>暂无执行日志...</div>
                  )}
                </div>
              </Tabs.TabPane>
              <Tabs.TabPane tab="断点" key="breakpoints">
                {breakpoints.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                    暂无断点，点击节点属性面板中的"设置断点"添加
                  </div>
                ) : (
                  <Table
                    dataSource={breakpoints.map(b => {
                      const node = logic?.nodes?.find(n => n.nodeId === b)
                      return { nodeId: b, nodeName: node?.nodeName || b, nodeType: node?.nodeType }
                    })}
                    columns={[
                      { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName' },
                      { title: '节点类型', dataIndex: 'nodeType', key: 'nodeType' },
                      {
                        title: '操作',
                        key: 'action',
                        render: (_: any, record: any) => (
                          <Button
                            type="link"
                            danger
                            size="small"
                            onClick={() => handleToggleBreakpoint(record.nodeId)}
                          >
                            删除
                          </Button>
                        ),
                      },
                    ]}
                    rowKey="nodeId"
                    size="small"
                  />
                )}
              </Tabs.TabPane>
            </Tabs>
          </Drawer>
        )}

        <Modal
          title="生成的代码"
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

export default LogicDesigner
