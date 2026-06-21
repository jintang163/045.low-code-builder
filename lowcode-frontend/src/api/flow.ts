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
  processName: string
  processKey: string
  processDesc?: string
  bpmnXml?: string
  flowGraph?: string
  flowableProcessDefId?: string
  flowableDeploymentId?: string
  version?: number
  status?: number
  deployTime?: string
  nodes?: WorkflowNode[]
  edges?: WorkflowEdge[]
}

export interface WorkflowNode {
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

export interface WorkflowEdge {
  edgeId: string
  sourceNodeId: string
  targetNodeId: string
  sourcePort?: string
  targetPort?: string
  conditionExpression?: string
}

export const logicApi = {
  list: (appId: number) => request.get<BusinessLogic[]>(`/logic/list/${appId}`),
  page: (appId: number, current = 1, size = 10) =>
    request.get(`/logic/page`, { params: { appId, current, size } }),
  get: (id: number) => request.get<BusinessLogic>(`/logic/${id}`),
  save: (data: BusinessLogic) => request.post<BusinessLogic>('/logic', data),
  update: (data: BusinessLogic) => request.put<BusinessLogic>('/logic', data),
  delete: (id: number) => request.delete(`/logic/${id}`),
  generateCode: (id: number) => request.get<string>(`/logic/${id}/generateCode`),
  publish: (id: number) => request.post<BusinessLogic>(`/logic/${id}/publish`),
  nodeTypes: () => request.get('/logic/nodeTypes'),
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
    request.post('/debug/start', inputParams, { params: { logicId } }),
  stepForward: (sessionId: string) =>
    request.post('/debug/stepForward', null, { params: { sessionId } }),
  status: (sessionId: string) =>
    request.get('/debug/status', { params: { sessionId } }),
  stop: (sessionId: string) =>
    request.post('/debug/stop', null, { params: { sessionId } }),
  setBreakpoint: (sessionId: string, nodeId: string) =>
    request.post('/debug/breakpoint/set', null, { params: { sessionId, nodeId } }),
  removeBreakpoint: (sessionId: string, nodeId: string) =>
    request.post('/debug/breakpoint/remove', null, { params: { sessionId, nodeId } }),
  history: (logicId: number) => request.get(`/debug/history/${logicId}`),
}

export interface RpaScript {
  id?: number
  appId: number
  scriptName: string
  scriptCode: string
  description?: string
  scriptContent?: string
  scriptType?: string
  targetUrl?: string
  timeout?: number
  status?: string
  version?: string
  scheduleEnabled?: number
  cronExpression?: string
  scheduleParams?: string
  lastExecuteTime?: string
  nextExecuteTime?: string
  executeCount?: number
  successCount?: number
  failCount?: number
  createdTime?: string
  updatedTime?: string
}

export interface RpaExecution {
  id?: number
  scriptId: number
  executionNo?: string
  triggerType?: string
  triggerLogicId?: number
  triggerNodeId?: string
  inputParams?: string
  outputResult?: string
  status?: string
  errorMessage?: string
  startTime?: string
  endTime?: string
  duration?: number
  executionLog?: string
}

export interface RpaStep {
  action: string
  selector?: string
  value?: string
  url?: string
  waitTime?: number
  timeout?: number
  fieldName?: string
  extractType?: string
  attribute?: string
  direction?: string
  pixels?: number
  seconds?: number
  name?: string
  fullPage?: boolean
}

export const rpaApi = {
  listScripts: (appId: number) => request.get<RpaScript[]>(`/rpa/script/list/${appId}`),
  pageScripts: (appId: number, current = 1, size = 10) =>
    request.get(`/rpa/script/page`, { params: { appId, current, size } }),
  getScript: (id: number) => request.get<RpaScript>(`/rpa/script/${id}`),
  createScript: (data: RpaScript) => request.post<RpaScript>('/rpa/script', data),
  updateScript: (id: number, data: RpaScript) => request.put<RpaScript>(`/rpa/script/${id}`, data),
  deleteScript: (id: number) => request.delete(`/rpa/script/${id}`),
  validateScript: (id: number) => request.post(`/rpa/script/${id}/validate`),
  publishScript: (id: number) => request.post<RpaScript>(`/rpa/script/${id}/publish`),
  executeScript: (scriptId: number, inputParams?: Record<string, any>, triggerType = 'MANUAL') =>
    request.post<RpaExecution>('/rpa/execute', { scriptId, inputParams, triggerType }),
  getExecution: (id: number) => request.get<RpaExecution>(`/rpa/execution/${id}`),
  pageExecutions: (current = 1, size = 10, scriptId?: number) =>
    request.get(`/rpa/execution/page`, { params: { current, size, scriptId } }),
  checkExecutorHealth: () => request.get('/rpa/executor/health'),
  enableSchedule: (id: number, cronExpression: string, scheduleParams?: string) =>
    request.post<RpaScript>(`/rpa/script/${id}/schedule/enable`, { cronExpression, scheduleParams }),
  disableSchedule: (id: number) => request.post<RpaScript>(`/rpa/script/${id}/schedule/disable`),
  getScheduledScripts: () => request.get<RpaScript[]>('/rpa/schedule/list'),
  calculateNextExecution: (cronExpression: string) =>
    request.get('/rpa/schedule/next-execution', { params: { cronExpression } }),
}

export interface AppExposedApi {
  id?: number
  appId: number
  appCode?: string
  apiName: string
  apiCode: string
  apiType?: string
  httpMethod?: string
  apiPath?: string
  requestSchema?: string
  responseSchema?: string
  description?: string
  authType?: string
  isTransactional?: number
  timeoutMs?: number
  status?: number
  createdTime?: string
  updatedTime?: string
}

export interface AppExposedEvent {
  id?: number
  appId: number
  appCode?: string
  eventName: string
  eventCode: string
  payloadSchema?: string
  description?: string
  status?: number
  createdTime?: string
  updatedTime?: string
}

export interface CrossAppCallDTO {
  targetAppCode: string
  callType?: 'API' | 'EVENT'
  targetCode: string
  params?: Record<string, any>
  timeoutMs?: number
  callerAppId?: number
  callerLogicId?: number
}

export const crossAppApi = {
  executeCall: (dto: CrossAppCallDTO) =>
    request.post<Record<string, any>>('/cross-app/call', dto),
  publishEvent: (payload: Record<string, any>) =>
    request.post<Record<string, any>>('/cross-app/event/publish', payload),

  registerApi: (data: AppExposedApi) =>
    request.post<AppExposedApi>('/cross-app/api/register', data),
  updateApi: (data: AppExposedApi) =>
    request.put<AppExposedApi>('/cross-app/api', data),
  deleteApi: (id: number) => request.delete(`/cross-app/api/${id}`),
  getApi: (id: number) => request.get<AppExposedApi>(`/cross-app/api/${id}`),
  getApiByCode: (code: string) => request.get<AppExposedApi>(`/cross-app/api/code/${code}`),
  listApisByApp: (appId: number) =>
    request.get<AppExposedApi[]>(`/cross-app/api/list/app/${appId}`),
  listApisByAppCode: (appCode: string) =>
    request.get<AppExposedApi[]>(`/cross-app/api/list/app-code/${appCode}`),
  pageApis: (current = 1, size = 10, appId?: number, keyword?: string) =>
    request.get('/cross-app/api/page', { params: { current, size, appId, keyword } }),

  registerEvent: (data: AppExposedEvent) =>
    request.post<AppExposedEvent>('/cross-app/event/register', data),
  updateEvent: (data: AppExposedEvent) =>
    request.put<AppExposedEvent>('/cross-app/event', data),
  deleteEvent: (id: number) => request.delete(`/cross-app/event/${id}`),
  getEvent: (id: number) => request.get<AppExposedEvent>(`/cross-app/event/${id}`),
  getEventByCode: (code: string) => request.get<AppExposedEvent>(`/cross-app/event/code/${code}`),
  listEventsByApp: (appId: number) =>
    request.get<AppExposedEvent[]>(`/cross-app/event/list/app/${appId}`),
  listEventsByAppCode: (appCode: string) =>
    request.get<AppExposedEvent[]>(`/cross-app/event/list/app-code/${appCode}`),
  pageEvents: (current = 1, size = 10, appId?: number, keyword?: string) =>
    request.get('/cross-app/event/page', { params: { current, size, appId, keyword } }),

  discoverServices: (appCode?: string) =>
    request.get<Record<string, any>>('/cross-app/discover', { params: { appCode } }),
}
