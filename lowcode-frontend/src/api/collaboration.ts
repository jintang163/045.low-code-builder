import request from '@/utils/request'

export interface CommentAttachment {
  id?: number
  commentId?: number
  fileName: string
  fileUrl: string
  fileType: string
  fileSize?: number
  width?: number
  height?: number
}

export interface CommentMention {
  id?: number
  commentId?: number
  userId: number
  username?: string
  nickname?: string
  isRead?: number
  readTime?: string
}

export interface Comment {
  id?: number
  appId: number
  targetType: string
  targetId: number
  targetName?: string
  parentId?: number
  replyToUserId?: number
  replyToUserName?: string
  content: string
  status?: number
  likeCount?: number
  resolvedBy?: number
  resolvedTime?: string
  commentTag?: string
  createdBy?: number
  createdByName?: string
  createdByAvatar?: string
  createdTime?: string
  updatedTime?: string
  attachments?: CommentAttachment[]
  mentions?: CommentMention[]
  replies?: Comment[]
}

export interface CommentCreateDTO {
  appId: number
  targetType: string
  targetId: number
  targetName?: string
  parentId?: number
  replyToUserId?: number
  replyToUserName?: string
  content: string
  commentTag?: string
  attachments?: CommentAttachment[]
  mentionUserIds?: number[]
  taskAssignment?: TaskAssignmentDTO
}

export interface TaskAssignmentDTO {
  taskTitle: string
  taskDesc?: string
  taskPriority?: 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW'
  assigneeId: number
  assigneeName?: string
  dueDate?: string
}

export interface TaskAssignment {
  id?: number
  appId: number
  taskTitle: string
  taskDesc?: string
  taskPriority?: string
  taskStatus?: string
  targetType?: string
  targetId?: number
  targetName?: string
  assigneeId: number
  assigneeName?: string
  assigneeAvatar?: string
  commentId?: number
  dueDate?: string
  completedTime?: string
  completedNote?: string
  createdBy?: number
  createdByName?: string
  createdByAvatar?: string
  createdTime?: string
  updatedTime?: string
}

export interface DesignHistory {
  id?: number
  appId: number
  targetType: string
  targetId: number
  targetName?: string
  operationType: string
  operationDesc?: string
  beforeSnapshot?: string
  afterSnapshot?: string
  diffJson?: string
  ipAddress?: string
  createdBy?: number
  createdByName?: string
  createdByAvatar?: string
  createdTime?: string
}

export interface DesignHistoryCreateDTO {
  appId: number
  targetType: string
  targetId: number
  targetName?: string
  operationType: string
  operationDesc?: string
  beforeSnapshot?: string
  afterSnapshot?: string
  diffJson?: string
}

export const collaborationApi = {
  createComment: (data: CommentCreateDTO) =>
    request.post<Comment>('/collaboration/comment', data),

  getComment: (id: number) =>
    request.get<Comment>(`/collaboration/comment/${id}`),

  listComments: (params: {
    appId: number
    targetType: string
    targetId: number
    createdBy?: number
    commentTag?: string
    status?: number
  }) =>
    request.get<Comment[]>('/collaboration/comment/list', { params }),

  deleteComment: (id: number) =>
    request.delete(`/collaboration/comment/${id}`),

  resolveComment: (id: number) =>
    request.post(`/collaboration/comment/${id}/resolve`),

  unresolveComment: (id: number) =>
    request.post(`/collaboration/comment/${id}/unresolve`),

  likeComment: (id: number) =>
    request.post(`/collaboration/comment/${id}/like`),

  countUnreadMentions: () =>
    request.get<number>('/collaboration/comment/mentions/unread/count'),

  getUnreadMentions: () =>
    request.get<Comment[]>('/collaboration/comment/mentions/unread'),

  markMentionAsRead: (commentId: number) =>
    request.post(`/collaboration/comment/mentions/${commentId}/read`),

  markAllMentionsAsRead: () =>
    request.post('/collaboration/comment/mentions/read-all'),

  createHistory: (data: DesignHistoryCreateDTO) =>
    request.post<DesignHistory>('/collaboration/history', data),

  listHistory: (appId: number, targetType: string, targetId: number, limit = 100) =>
    request.get<DesignHistory[]>('/collaboration/history/list', {
      params: { appId, targetType, targetId, limit },
    }),

  createTask: (
    data: TaskAssignmentDTO,
    params: {
      appId: number
      targetType: string
      targetId: number
      targetName?: string
      commentId?: number
    }
  ) =>
    request.post<TaskAssignment>('/collaboration/task', data, { params }),

  getTask: (id: number) =>
    request.get<TaskAssignment>(`/collaboration/task/${id}`),

  listTasksByTarget: (appId: number, targetType: string, targetId: number) =>
    request.get<TaskAssignment[]>('/collaboration/task/list', {
      params: { appId, targetType, targetId },
    }),

  listMyTasks: (assigneeId: number) =>
    request.get<TaskAssignment[]>('/collaboration/task/mine', {
      params: { assigneeId },
    }),

  updateTask: (id: number, data: {
    taskStatus?: string
    completedNote?: string
    taskPriority?: string
    assigneeId?: number
    assigneeName?: string
    dueDate?: string
  }) =>
    request.put<TaskAssignment>(`/collaboration/task/${id}`, data),

  deleteTask: (id: number) =>
    request.delete(`/collaboration/task/${id}`),
}

export default collaborationApi
