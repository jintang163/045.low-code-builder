import React from 'react'
import { Avatar, Tooltip } from 'antd'
import { UserOutlined } from '@ant-design/icons'
import { Collaborator, formatJoinTime } from '@/utils/collaboration'

export type AvatarSize = 'small' | 'default' | 'large'

const sizeMap: Record<AvatarSize, number> = {
  small: 24,
  default: 32,
  large: 48,
}

const dotSizeMap: Record<AvatarSize, number> = {
  small: 6,
  default: 8,
  large: 10,
}

interface CollaboratorAvatarProps {
  collaborator: Collaborator
  size?: AvatarSize
  showStatus?: boolean
}

export const CollaboratorAvatar: React.FC<CollaboratorAvatarProps> = ({
  collaborator,
  size = 'default',
  showStatus = true,
}) => {
  const avatarSize = sizeMap[size]
  const dotSize = dotSizeMap[size]

  return (
    <Tooltip
      title={
        <div style={{ padding: '4px 0' }}>
          <div style={{ fontWeight: 500, marginBottom: 4 }}>{collaborator.username}</div>
          <div style={{ fontSize: 12, opacity: 0.8 }}>
            加入时间：{formatJoinTime(collaborator.joinTime)}
          </div>
          <div style={{ fontSize: 12, opacity: 0.8 }}>
            状态：{collaborator.isOnline ? '在线' : '离线'}
          </div>
        </div>
      }
    >
      <div style={{ position: 'relative', display: 'inline-block' }}>
        <Avatar
          size={avatarSize}
          src={collaborator.avatar}
          icon={!collaborator.avatar && <UserOutlined />}
          style={{
            border: `2px solid ${collaborator.color}`,
            backgroundColor: '#fff',
          }}
        />
        {showStatus && (
          <span
            style={{
              position: 'absolute',
              bottom: 0,
              right: 0,
              width: dotSize,
              height: dotSize,
              borderRadius: '50%',
              backgroundColor: collaborator.isOnline ? '#52c41a' : '#bfbfbf',
              border: '2px solid #fff',
              boxSizing: 'content-box',
            }}
          />
        )}
      </div>
    </Tooltip>
  )
}

export default CollaboratorAvatar
