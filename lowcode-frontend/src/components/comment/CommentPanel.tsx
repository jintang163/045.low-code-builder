import React, { useState, useEffect, useRef } from 'react'
import {
  Comment,
  Avatar,
  Button,
  Input,
  Upload,
  Tooltip,
  Tag,
  Dropdown,
  Menu,
  Modal,
  Form,
  Select,
  DatePicker,
  Badge,
  Empty,
  message,
  Popover,
  List,
  Image,
  Typography,
  Tabs,
} from 'antd'
import {
  SendOutlined,
  PictureOutlined,
  FileOutlined,
  SmileOutlined,
  MoreOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LikeOutlined,
  LikeFilled,
  EditOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  UserOutlined,
  BellOutlined,
  ClockCircleOutlined,
  FlagOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import type { UploadFile, UploadProps } from 'antd'
import type { MenuProps } from 'antd'
import dayjs from 'dayjs'
import {
  collaborationApi,
  Comment as CommentType,
  CommentAttachment,
  TaskAssignment,
  TaskAssignmentDTO,
} from '@/api/collaboration'
import { useCollaborationWs, WsMessage } from '@/hooks/useCollaborationWs'
import './CommentPanel.less'

const { TextArea } = Input
const { Option } = Select
const { Text, Paragraph } = Typography

export interface CommentPanelProps {
  appId: number
  targetType: string
  targetId: number
  targetName?: string
  userId: number
  username?: string
  avatar?: string
  userList?: Array<{ id: number; username: string; nickname?: string; avatar?: string }>
}

const TARGET_TYPE_MAP: Record<string, string> = {
  DATA_MODEL: '数据模型',
  PAGE: '页面',
  PAGE_COMPONENT: '页面组件',
  LOGIC_NODE: '逻辑节点',
  WORKFLOW: '工作流',
}

const COMMENT_TAG_OPTIONS = [
  { value: 'TODO', label: '待办', color: 'blue' },
  { value: 'QUESTION', label: '疑问', color: 'purple' },
  { value: 'IDEA', label: '想法', color: 'cyan' },
  { value: 'BUG', label: 'Bug', color: 'red' },
  { value: 'SUGGESTION', label: '建议', color: 'orange' },
]

const TASK_PRIORITY_OPTIONS = [
  { value: 'URGENT', label: '紧急', color: 'red' },
  { value: 'HIGH', label: '高', color: 'orange' },
  { value: 'MEDIUM', label: '中', color: 'blue' },
  { value: 'LOW', label: '低', color: 'green' },
]

const TASK_STATUS_OPTIONS = [
  { value: 'TODO', label: '待办', color: 'default' },
  { value: 'IN_PROGRESS', label: '进行中', color: 'processing' },
  { value: 'DONE', label: '已完成', color: 'success' },
  { value: 'CANCELLED', label: '已取消', color: 'error' },
]

const CommentPanel: React.FC<CommentPanelProps> = ({
  appId,
  targetType,
  targetId,
  targetName,
  userId,
  username,
  avatar,
  userList = [],
}) => {
  const [comments, setComments] = useState<CommentType[]>([])
  const [tasks, setTasks] = useState<TaskAssignment[]>([])
  const [loading, setLoading] = useState(false)
  const [content, setContent] = useState('')
  const [attachments, setAttachments] = useState<UploadFile[]>([])
  const [selectedTag, setSelectedTag] = useState<string | undefined>()
  const [mentionUserIds, setMentionUserIds] = useState<number[]>([])
  const [replyTo, setReplyTo] = useState<{ commentId: number; userId?: number; userName?: string } | null>(null)
  const [taskModalVisible, setTaskModalVisible] = useState(false)
  const [taskForm] = Form.useForm()
  const [unreadMentionCount, setUnreadMentionCount] = useState(0)
  const [activeTab, setActiveTab] = useState<'comments' | 'tasks' | 'history'>('comments')
  const inputRef = useRef<any>(null)

  useCollaborationWs({
    appId,
    userId,
    username,
    avatar,
    targetType,
    targetId,
    onMessage: handleWsMessage,
  })

  function handleWsMessage(msg: WsMessage) {
    if (msg.type === 'NEW_COMMENT') {
      loadComments()
    } else if (msg.type === 'TASK_UPDATE') {
      loadTasks()
    } else if (msg.type === 'NOTIFICATION') {
      message.info(`收到新通知: ${msg.data?.type}`)
    }
  }

  useEffect(() => {
    loadComments()
    loadTasks()
    loadUnreadMentionCount()
  }, [appId, targetType, targetId])

  const loadComments = async () => {
    try {
      setLoading(true)
      const res = await collaborationApi.listComments({ appId, targetType, targetId })
      if (res.code === 0 || res.code === 200) {
        setComments(res.data || [])
      }
    } catch (e) {
      console.error('加载评论失败:', e)
    } finally {
      setLoading(false)
    }
  }

  const loadTasks = async () => {
    try {
      const res = await collaborationApi.listTasksByTarget(appId, targetType, targetId)
      if (res.code === 0 || res.code === 200) {
        setTasks(res.data || [])
      }
    } catch (e) {
      console.error('加载任务失败:', e)
    }
  }

  const loadUnreadMentionCount = async () => {
    try {
      const res = await collaborationApi.countUnreadMentions()
      if (res.code === 0 || res.code === 200) {
        setUnreadMentionCount(res.data || 0)
      }
    } catch (e) {
      console.error('加载未读@数量失败:', e)
    }
  }

  const handleUploadChange: UploadProps['onChange'] = ({ fileList }) => {
    setAttachments(fileList)
  }

  const beforeUpload = (file: File) => {
    const isImage = file.type.startsWith('image/')
    const isLt10M = file.size / 1024 / 1024 < 10
    if (!isImage && !file.type.startsWith('application/')) {
      message.error('只能上传图片或文档文件!')
      return Upload.LIST_IGNORE
    }
    if (!isLt10M) {
      message.error('文件大小不能超过 10MB!')
      return Upload.LIST_IGNORE
    }
    return false
  }

  const handleSubmit = async () => {
    if (!content.trim()) {
      message.warning('请输入评论内容')
      inputRef.current?.focus()
      return
    }

    try {
      const atts: CommentAttachment[] = attachments.map((f) => ({
        fileName: f.name,
        fileUrl: f.url || f.response?.url || '#',
        fileType: f.type,
        fileSize: f.size,
      }))

      const res = await collaborationApi.createComment({
        appId,
        targetType,
        targetId,
        targetName,
        parentId: replyTo?.commentId || undefined,
        replyToUserId: replyTo?.userId,
        replyToUserName: replyTo?.userName,
        content: content.trim(),
        commentTag: selectedTag,
        attachments: atts,
        mentionUserIds: mentionUserIds.length > 0 ? mentionUserIds : undefined,
      })

      if (res.code === 0 || res.code === 200) {
        message.success('评论发送成功')
        setContent('')
        setAttachments([])
        setSelectedTag(undefined)
        setMentionUserIds([])
        setReplyTo(null)
        loadComments()
      }
    } catch (e) {
      console.error('发送评论失败:', e)
    }
  }

  const handleSubmitWithTask = async (values: any) => {
    if (!content.trim()) {
      message.warning('请输入评论内容')
      return
    }

    try {
      const atts: CommentAttachment[] = attachments.map((f) => ({
        fileName: f.name,
        fileUrl: f.url || f.response?.url || '#',
        fileType: f.type,
        fileSize: f.size,
      }))

      const taskAssignment: TaskAssignmentDTO = {
        taskTitle: values.taskTitle,
        taskDesc: values.taskDesc,
        taskPriority: values.taskPriority,
        assigneeId: values.assigneeId,
        assigneeName: userList.find((u) => u.id === values.assigneeId)?.nickname || userList.find((u) => u.id === values.assigneeId)?.username,
        dueDate: values.dueDate?.format(),
      }

      const res = await collaborationApi.createComment({
        appId,
        targetType,
        targetId,
        targetName,
        content: content.trim(),
        commentTag: 'TODO',
        attachments: atts,
        mentionUserIds: mentionUserIds.length > 0 ? mentionUserIds : undefined,
        taskAssignment,
      })

      if (res.code === 0 || res.code === 200) {
        message.success('任务已分配并发送评论')
        setContent('')
        setAttachments([])
        setSelectedTag(undefined)
        setMentionUserIds([])
        setTaskModalVisible(false)
        taskForm.resetFields()
        loadComments()
        loadTasks()
      }
    } catch (e) {
      console.error('创建任务失败:', e)
    }
  }

  const handleResolve = async (id: number) => {
    try {
      await collaborationApi.resolveComment(id)
      message.success('已标记为已解决')
      loadComments()
    } catch (e) {
      console.error('解决评论失败:', e)
    }
  }

  const handleUnresolve = async (id: number) => {
    try {
      await collaborationApi.unresolveComment(id)
      message.success('已取消解决状态')
      loadComments()
    } catch (e) {
      console.error('取消解决失败:', e)
    }
  }

  const handleDelete = async (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这条评论吗？',
      onOk: async () => {
        try {
          await collaborationApi.deleteComment(id)
          message.success('删除成功')
          loadComments()
        } catch (e) {
          console.error('删除评论失败:', e)
        }
      },
    })
  }

  const handleLike = async (id: number) => {
    try {
      await collaborationApi.likeComment(id)
      loadComments()
    } catch (e) {
      console.error('点赞失败:', e)
    }
  }

  const handleReply = (comment: CommentType) => {
    setReplyTo({
      commentId: comment.id!,
      userId: comment.createdBy,
      userName: comment.createdByName || comment.createdBy?.toString(),
    })
    inputRef.current?.focus()
  }

  const handleUpdateTaskStatus = async (taskId: number, status: string) => {
    try {
      await collaborationApi.updateTask(taskId, { taskStatus: status })
      message.success('任务状态已更新')
      loadTasks()
    } catch (e) {
      console.error('更新任务状态失败:', e)
    }
  }

  const renderCommentItem = (comment: CommentType) => {
    const tag = COMMENT_TAG_OPTIONS.find((t) => t.value === comment.commentTag)
    const commentMenuItems: MenuProps['items'] = [
      {
        key: 'reply',
        icon: <EditOutlined />,
        label: '回复',
        onClick: () => handleReply(comment),
      },
      comment.status === 2
        ? {
            key: 'unresolve',
            icon: <CloseCircleOutlined />,
            label: '标记为未解决',
            onClick: () => handleUnresolve(comment.id!),
          }
        : {
            key: 'resolve',
            icon: <CheckCircleOutlined />,
            label: '标记为已解决',
            onClick: () => handleResolve(comment.id!),
          },
      { type: 'divider' },
      {
        key: 'delete',
        icon: <DeleteOutlined />,
        label: '删除',
        danger: true,
        onClick: () => handleDelete(comment.id!),
      },
    ]

    return (
      <Comment
        key={comment.id}
        className={comment.status === 2 ? 'comment-item resolved' : 'comment-item'}
        author={
          <span className="comment-author">
            <span>{comment.createdByName || '未知用户'}</span>
            {tag && (
              <Tag color={tag.color} className="comment-tag" style={{ marginLeft: 8 }}>
                {tag.label}
              </Tag>
            )}
            {comment.status === 2 && (
              <Tag color="success" className="comment-tag" style={{ marginLeft: 8 }}>
                <CheckCircleOutlined /> 已解决
              </Tag>
            )}
          </span>
        }
        avatar={
          <Avatar
            src={comment.createdByAvatar}
            icon={<UserOutlined />}
            size={40}
          />
        }
        content={
          <div className="comment-content">
            {comment.replyToUserName && (
              <Tag color="blue" className="reply-tag">
                @{comment.replyToUserName}
              </Tag>
            )}
            <Paragraph className="comment-text">{comment.content}</Paragraph>
            {comment.attachments && comment.attachments.length > 0 && (
              <div className="comment-attachments">
                {comment.attachments.map((att, idx) =>
                  att.fileType?.startsWith('image/') ? (
                    <Image
                      key={idx}
                      width={120}
                      height={90}
                      src={att.fileUrl}
                      className="attachment-image"
                      alt={att.fileName}
                    />
                  ) : (
                    <a key={idx} href={att.fileUrl} target="_blank" className="attachment-file">
                      <FileOutlined /> {att.fileName}
                    </a>
                  )
                )}
              </div>
            )}
            {comment.mentions && comment.mentions.length > 0 && (
              <div className="comment-mentions">
                {comment.mentions.map((m, idx) => (
                  <Tag key={idx} color="purple">
                    @{m.nickname || m.username}
                  </Tag>
                ))}
              </div>
            )}
          </div>
        }
        datetime={
          <Tooltip title={dayjs(comment.createdTime).format('YYYY-MM-DD HH:mm:ss')}>
            <span>{dayjs(comment.createdTime).fromNow()}</span>
          </Tooltip>
        }
        actions={[
          <span key="comment-nested-comment-list" onClick={() => handleReply(comment)}>
            <EditOutlined /> 回复
          </span>,
          <span key="comment-list-like" onClick={() => handleLike(comment.id!)}>
            {comment.likeCount && comment.likeCount > 0 ? <LikeFilled /> : <LikeOutlined />}
            {comment.likeCount || 0}
          </span>,
          <Dropdown key="more" menu={{ items: commentMenuItems }} trigger={['click']}>
            <a onClick={(e) => e.preventDefault()}>
              <MoreOutlined />
            </a>
          </Dropdown>,
        ]}
      >
        {comment.replies && comment.replies.length > 0 && (
          <div className="comment-replies">
            {comment.replies.map((reply) => renderCommentItem(reply))}
          </div>
        )}
      </Comment>
    )
  }

  const renderTaskItem = (task: TaskAssignment) => {
    const priority = TASK_PRIORITY_OPTIONS.find((p) => p.value === task.taskPriority)
    const status = TASK_STATUS_OPTIONS.find((s) => s.value === task.taskStatus)
    const taskMenuItems: MenuProps['items'] = [
      {
        key: 'todo',
        label: '标记为待办',
        onClick: () => handleUpdateTaskStatus(task.id!, 'TODO'),
      },
      {
        key: 'in_progress',
        label: '标记为进行中',
        onClick: () => handleUpdateTaskStatus(task.id!, 'IN_PROGRESS'),
      },
      {
        key: 'done',
        label: '标记为已完成',
        onClick: () => handleUpdateTaskStatus(task.id!, 'DONE'),
      },
      { type: 'divider' },
      {
        key: 'delete',
        label: '删除任务',
        danger: true,
        onClick: () => {
          Modal.confirm({
            title: '确认删除',
            content: '确定要删除这个任务吗？',
            onOk: async () => {
              try {
                await collaborationApi.deleteTask(task.id!)
                message.success('删除成功')
                loadTasks()
              } catch (e) {
                console.error('删除任务失败:', e)
              }
            },
          })
        },
      },
    ]

    return (
      <div key={task.id} className="task-item">
        <div className="task-header">
          <div className="task-title">
            <FlagOutlined style={{ color: priority?.color }} />
            <Text strong>{task.taskTitle}</Text>
            {priority && (
              <Tag color={priority.color} style={{ marginLeft: 8 }}>
                {priority.label}
              </Tag>
            )}
            {status && <Badge status={status.color as any} text={status.label} style={{ marginLeft: 8 }} />}
          </div>
          <Dropdown menu={{ items: taskMenuItems }} trigger={['click']}>
            <Button type="text" size="small" icon={<MoreOutlined />} />
          </Dropdown>
        </div>
        {task.taskDesc && <Paragraph type="secondary" className="task-desc">{task.taskDesc}</Paragraph>}
        <div className="task-meta">
          <div className="task-assignee">
            <Avatar size={20} src={task.assigneeAvatar} icon={<UserOutlined />} />
            <span style={{ marginLeft: 4 }}>{task.assigneeName}</span>
          </div>
          {task.dueDate && (
            <div className="task-due">
              <ClockCircleOutlined />
              <span style={{ marginLeft: 4 }}>{dayjs(task.dueDate).format('YYYY-MM-DD')}</span>
            </div>
          )}
          {task.createdByName && (
            <div className="task-creator">
              <TeamOutlined />
              <span style={{ marginLeft: 4 }}>创建人: {task.createdByName}</span>
            </div>
          )}
        </div>
      </div>
    )
  }

  const MentionUserPopover = (
    <div className="mention-user-popover">
      <div style={{ marginBottom: 8, fontWeight: 'bold' }}>选择要@的成员</div>
      <Select
        mode="multiple"
        placeholder="搜索用户..."
        style={{ width: '100%' }}
        value={mentionUserIds}
        onChange={(values) => setMentionUserIds(values as number[])}
        allowClear
        showSearch
        filterOption={(input, option) =>
          (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
        }
      >
        {userList.map((u) => (
          <Option key={u.id} value={u.id} label={u.nickname || u.username}>
            <Avatar size={20} src={u.avatar} icon={<UserOutlined />} style={{ marginRight: 8 }} />
            {u.nickname || u.username}
          </Option>
        ))}
      </Select>
    </div>
  )

  return (
    <div className="comment-panel">
      <div className="panel-header">
        <Tabs
          activeKey={activeTab}
          onChange={(k) => setActiveTab(k as any)}
          size="small"
          items={[
            {
              key: 'comments',
              label: (
                <span>
                  评论 <Badge count={comments.length} size="small" style={{ marginLeft: 4 }} />
                </span>
              ),
            },
            {
              key: 'tasks',
              label: (
                <span>
                  任务 <Badge count={tasks.filter((t) => t.taskStatus !== 'DONE').length} size="small" style={{ marginLeft: 4 }} />
                </span>
              ),
            },
          ]}
        />
        <Badge count={unreadMentionCount} size="small" offset={[-2, 2]}>
          <Tooltip title="@我的未读消息">
            <Button
              type="text"
              icon={<BellOutlined />}
              size="small"
              onClick={() => collaborationApi.markAllMentionsAsRead().then(() => loadUnreadMentionCount())}
            />
          </Tooltip>
        </Badge>
      </div>

      <div className="panel-sub-header">
        <Tag color="blue">{TARGET_TYPE_MAP[targetType] || targetType}</Tag>
        {targetName && <Text ellipsis style={{ maxWidth: 200 }}>{targetName}</Text>}
      </div>

      {activeTab === 'comments' && (
        <div className="comments-section">
          <div className="comment-input-area">
            <div className="input-row">
              {replyTo && (
                <div className="reply-banner">
                  <Tag color="blue">回复 @{replyTo.userName}</Tag>
                  <Button type="text" size="small" onClick={() => setReplyTo(null)}>
                    取消
                  </Button>
                </div>
              )}
              <TextArea
                ref={inputRef}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder={replyTo ? `回复 @${replyTo.userName}...` : '发表评论，支持@提及成员...'}
                autoSize={{ minRows: 3, maxRows: 6 }}
                onPressEnter={(e) => {
                  if (e.ctrlKey || e.metaKey) {
                    handleSubmit()
                  }
                }}
              />
            </div>
            <div className="toolbar-row">
              <div className="toolbar-left">
                <Upload
                  fileList={attachments}
                  onChange={handleUploadChange}
                  beforeUpload={beforeUpload}
                  multiple
                  accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt"
                  listType="picture"
                  showUploadList={{ showPreviewIcon: true, showRemoveIcon: true }}
                >
                  <Button type="text" icon={<PictureOutlined />} size="small">
                    图片
                  </Button>
                </Upload>
                <Popover content={MentionUserPopover} title={null} trigger="click">
                  <Button type="text" icon={<UserOutlined />} size="small">
                    @成员 {mentionUserIds.length > 0 && `(${mentionUserIds.length})`}
                  </Button>
                </Popover>
                <Select
                  placeholder="添加标签"
                  allowClear
                  size="small"
                  value={selectedTag}
                  onChange={setSelectedTag}
                  style={{ width: 100 }}
                  bordered={false}
                >
                  {COMMENT_TAG_OPTIONS.map((t) => (
                    <Option key={t.value} value={t.value}>
                      <Tag color={t.color}>{t.label}</Tag>
                    </Option>
                  ))}
                </Select>
                <Button
                  type="text"
                  icon={<ExclamationCircleOutlined />}
                  size="small"
                  onClick={() => setTaskModalVisible(true)}
                >
                  分配任务
                </Button>
              </div>
              <div className="toolbar-right">
                <Button type="primary" icon={<SendOutlined />} onClick={handleSubmit}>
                  发送
                </Button>
              </div>
            </div>
          </div>

          <div className="comments-list" loading={loading}>
            {comments.length === 0 ? (
              <Empty description="暂无评论，快来发表第一条评论吧" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <List
                dataSource={comments}
                renderItem={(item) => <List.Item style={{ padding: 0 }}>{renderCommentItem(item)}</List.Item>}
              />
            )}
          </div>
        </div>
      )}

      {activeTab === 'tasks' && (
        <div className="tasks-section">
          <div className="tasks-list">
            {tasks.length === 0 ? (
              <Empty description="暂无任务，点击「分配任务」创建" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <List
                dataSource={tasks}
                renderItem={(item) => <List.Item style={{ padding: 0 }}>{renderTaskItem(item)}</List.Item>}
              />
            )}
          </div>
          <Button
            type="dashed"
            block
            icon={<ExclamationCircleOutlined />}
            onClick={() => setTaskModalVisible(true)}
            style={{ marginTop: 12 }}
          >
            分配新任务
          </Button>
        </div>
      )}

      <Modal
        title="分配任务"
        open={taskModalVisible}
        onCancel={() => setTaskModalVisible(false)}
        footer={null}
        destroyOnClose
      >
        <Form
          form={taskForm}
          layout="vertical"
          onFinish={handleSubmitWithTask}
          initialValues={{ taskPriority: 'MEDIUM' }}
        >
          <Form.Item label="任务标题" name="taskTitle" rules={[{ required: true, message: '请输入任务标题' }]}>
            <Input placeholder="例如：请完成订单表设计" />
          </Form.Item>
          <Form.Item label="任务描述" name="taskDesc">
            <TextArea rows={3} placeholder="详细描述任务内容..." />
          </Form.Item>
          <Form.Item label="优先级" name="taskPriority">
            <Select>
              {TASK_PRIORITY_OPTIONS.map((p) => (
                <Option key={p.value} value={p.value}>
                  <Tag color={p.color}>{p.label}</Tag>
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item label="指派给" name="assigneeId" rules={[{ required: true, message: '请选择指派用户' }]}>
            <Select placeholder="选择团队成员" showSearch filterOption={(input, option) =>
              (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
            }>
              {userList.map((u) => (
                <Option key={u.id} value={u.id} label={u.nickname || u.username}>
                  <Avatar size={20} src={u.avatar} icon={<UserOutlined />} style={{ marginRight: 8 }} />
                  {u.nickname || u.username}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item label="截止日期" name="dueDate">
            <DatePicker style={{ width: '100%' }} placeholder="选择截止日期" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              创建任务并发送评论
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default CommentPanel
