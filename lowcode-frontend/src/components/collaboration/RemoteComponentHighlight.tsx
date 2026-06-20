import React from 'react'
import { Collaborator } from '@/utils/collaboration'
import './RemoteComponentHighlight.less'

interface RemoteComponentHighlightProps {
  componentId: string
  collaborator: Collaborator
  isActive?: boolean
  style?: React.CSSProperties
  onClose?: () => void
}

export const RemoteComponentHighlight: React.FC<RemoteComponentHighlightProps> = ({
  componentId,
  collaborator,
  isActive = true,
  style,
  onClose,
}) => {
  const { color, username } = collaborator

  return (
    <div
      className={`remote-component-highlight ${isActive ? 'active' : 'fading'}`}
      style={{
        borderColor: color,
        ...style,
      }}
      data-component-id={componentId}
      data-user-id={collaborator.userId}
    >
      <div
        className="highlight-label"
        style={{
          backgroundColor: color,
        }}
      >
        <span className="highlight-username">{username}</span>
        <span className="highlight-status">正在编辑</span>
      </div>
      <div
        className="highlight-border"
        style={{
          borderColor: color,
          boxShadow: `0 0 0 2px ${color}33`,
        }}
      />
    </div>
  )
}

interface RemoteComponentHighlightsProps {
  components: Array<{
    componentId: string
    collaborator: Collaborator
    isActive?: boolean
  }>
}

export const RemoteComponentHighlights: React.FC<RemoteComponentHighlightsProps> = ({
  components,
}) => {
  return (
    <>
      {components.map((item) => (
        <RemoteComponentHighlight
          key={`${item.collaborator.userId}-${item.componentId}`}
          componentId={item.componentId}
          collaborator={item.collaborator}
          isActive={item.isActive}
        />
      ))}
    </>
  )
}

export default RemoteComponentHighlight
