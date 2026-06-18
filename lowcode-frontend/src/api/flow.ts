import request from '@/utils/request'

export interface BusinessLogic {
  id?: number
  appId: number
  logicName: string
  logicCode: string
  logicType?: string
  description?: string
  triggerType?: string
  triggerConfig?: string
  status?: string
  version?: string
  nodes?: LogicNode[]
  edges?: LogicEdge[]
}

export interface LogicNode {
  id?: number
  logicId?: number
  nodeId: string
  nodeName: string
  nodeType: string
  nodeCategory: string
  nodeConfig?: string
  positionX?: number
  positionY?: number
  width?: number
  height?: number
}

export interface LogicEdge {
  id?: number
  logicId?: number
  edgeId: string
  sourceNodeId: string
  targetNodeId: string
  sourcePort?: string
  targetPort?: string
  edgeConfig?: string
  conditionExpression?: string
}

export interface WorkflowDefinition {
  id?: number
  appId: number
  workflowName: string
  workflowCode: string
  description?: string
  bpmnXml?: string
  flowableDefinitionId?: string
  flowableDeploymentId?: string
  version?: number
  status?: string
  category?: string
}

export const logicApi = {
  list: (appId: number) => request.get<BusinessLogic[]>(`/businessLogic/list/${appId}`),
  page: (appId: number, current = 1, size = 10) =>
    request.get(`/businessLogic/page`, { params: { appId, current, size } }),
  get: (id: number) => request.get<BusinessLogic>(`/businessLogic/${id}`),
  save: (data: BusinessLogic) => request.post<BusinessLogic>('/businessLogic', data),
  update: (data: BusinessLogic) => request.put<BusinessLogic>('/businessLogic', data),
  delete: (id: number) => request.delete(`/businessLogic/${id}`),
  generateCode: (id: number) => request.get<string>(`/businessLogic/${id}/generateCode`),
  publish: (id: number) => request.post<BusinessLogic>(`/businessLogic/${id}/publish`),
  nodeTypes: () => request.get('/businessLogic/nodeTypes'),
}

export const workflowApi = {
  list: (appId: number) => request.get<WorkflowDefinition[]>(`/workflow/list/${appId}`),
  page: (appId: number, current = 1, size = 10) =>
    request.get(`/workflow/page`, { params: { appId, current, size } }),
  get: (id: number) => request.get<WorkflowDefinition>(`/workflow/${id}`),
  save: (data: WorkflowDefinition) => request.post<WorkflowDefinition>('/workflow', data),
  update: (data: WorkflowDefinition) => request.put<WorkflowDefinition>('/workflow', data),
  delete: (id: number) => request.delete(`/workflow/${id}`),
  deploy: (id: number) => request.post<WorkflowDefinition>(`/workflow/${id}/deploy`),
  startProcess: (id: number, variables?: Record<string, any>) =>
    request.post(`/workflow/${id}/start`, variables),
  getTasks: (assignee: string) => request.get('/workflow/tasks', { params: { assignee } }),
  completeTask: (taskId: string, variables?: Record<string, any>, comment?: string) =>
    request.post(`/workflow/task/${taskId}/complete`, variables, { params: { comment } }),
  delegateTask: (taskId: string, assignee: string) =>
    request.post(`/workflow/task/${taskId}/delegate`, null, { params: { assignee } }),
  generateBpmn: (id: number) => request.get<string>(`/workflow/${id}/generateBpmn`),
  parseBpmn: (bpmnXml: string) => request.post('/workflow/parseBpmn', bpmnXml),
  processStatus: (processInstanceId: string) =>
    request.get(`/workflow/process/${processInstanceId}/status`),
  nodeTypes: () => request.get('/workflow/nodeTypes'),
  deployed: () => request.get('/workflow/deployed'),
}

export const debugApi = {
  start: (logicId: number, inputParams?: Record<string, any>) =>
    request.post('/logicDebug/start', inputParams, { params: { logicId } }),
  stepForward: (sessionId: string) =>
    request.post('/logicDebug/stepForward', null, { params: { sessionId } }),
  status: (sessionId: string) =>
    request.get('/logicDebug/status', { params: { sessionId } }),
  stop: (sessionId: string) =>
    request.post('/logicDebug/stop', null, { params: { sessionId } }),
  setBreakpoint: (sessionId: string, nodeId: string) =>
    request.post('/logicDebug/breakpoint/set', null, { params: { sessionId, nodeId } }),
  removeBreakpoint: (sessionId: string, nodeId: string) =>
    request.post('/logicDebug/breakpoint/remove', null, { params: { sessionId, nodeId } }),
  history: (logicId: number) => request.get(`/logicDebug/history/${logicId}`),
}
