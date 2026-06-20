import React, { useState } from 'react'
import { Popover, List, Badge } from 'antd'
import { TeamOutlined } from '@ant-design/icons'
import { Collaborator, formatJoinTime } from '@/utils/collaboration'
import { CollaboratorAvatar } from './CollaboratorAvatar'
import './CollaboratorList.less'

interface CollaboratorListProps {
  collaborators: Collaborator[]
  maxVisible?: number
}

export const CollaboratorList: React.FC<CollaboratorListProps> = ({
  collaborators,
  maxVisible = 5,
}) => {
  const [visible, setVisible] = useState(false)

  const onlineCollaborators = collaborators.filter((c) => c.isOnline)
  const visibleCollaborators = onlineCollaborators.slice(0, maxVisible)
  const hiddenCount = Math.max(0, onlineCollaborators.length - maxVisible)

  const content = (
    <div className="collaborator-list-popover">
      <div className="collaborator-list-header">
        <TeamOutlined />
        <span style={{ marginLeft: 8 }}>协作者 ({onlineCollaborators.length}人在线)</span>
      </div>
      <List
        size="small"
        dataSource={onlineCollaborators}
        renderItem={(item) => (
          <List.Item key={item.userId}>
            <div style={{ display: 'flex', alignItems: 'center', width: '100%' }}>
              <CollaboratorAvatar collaborator={item} size="small" showStatus={false} />
              <div style={{ marginLeft: 12, flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 500 }}>{item.username}</div>
                <div style={{ fontSize: 11, color: '#999' }}>
                  加入于 {formatJoinTime(item.joinTime)}
                </div>
              </div>
              <Badge
                status={item.isOnline ? 'success' : 'default'}
                text={item.isOnline ? '在线' : '离线'}
              />
            </div>
          </List.Item>
        )}
      />
    </div>
  )

  return (
    <Popover
      content={content}
      trigger="click"
      placement="bottomRight"
      open={visible}
      onOpenChange={setVisible}
      overlayClassName="collaborator-list-overlay"
    >
      <div className="collaborator-avatar-stack">
        {visibleCollaborators.map((collaborator, index) => (
          <div
            key={collaborator.userId}
            className="collaborator-avatar-item"
            style={{
              zIndex: visibleCollaborators.length - index,
              marginLeft: index === 0 ? 0 : -8,
            }}
          >
            <CollaboratorAvatar collaborator={collaborator} size="default" showStatus={false} />
          </div>
        ))}
        {hiddenCount > 0 && (
          <div
            className="collaborator-avatar-item collaborator-avatar-more"
            style={{ marginLeft: -8 }}
          >
            <div className="collaborator-avatar-more-text">+{hiddenCount}</div>
          </div>
        )}
        <div className="collaborator-online-count">
          <span className="online-dot" />
          {onlineCollaborators.length}人在线
        </div>
      </div>
    </Popover>
  )
}

export default CollaboratorList
