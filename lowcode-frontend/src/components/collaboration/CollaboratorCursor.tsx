import React from 'react'
import { Collaborator, CursorPosition } from '@/utils/collaboration'
import './CollaboratorCursor.less'

interface CollaboratorCursorProps {
  collaborator: Collaborator
  position: CursorPosition
  containerRef?: React.RefObject<HTMLElement>
}

export const CollaboratorCursor: React.FC<CollaboratorCursorProps> = ({
  collaborator,
  position,
  containerRef,
}) => {
  const { color, username } = collaborator

  return (
    <div
      className="collaborator-cursor"
      style={{
        left: position.x,
        top: position.y,
        color: color,
      }}
    >
      <svg
        className="cursor-pointer"
        width="18"
        height="22"
        viewBox="0 0 18 22"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M1 1L10.5 20.5L13.5 12.5L17 9.5L1 1Z"
          fill={color}
          stroke="white"
          strokeWidth="1.5"
          strokeLinejoin="round"
        />
      </svg>
      <div
        className="cursor-label"
        style={{
          backgroundColor: color,
        }}
      >
        {username}
      </div>
      {position.selection && (
        <div
          className="cursor-selection"
          style={{
            borderColor: color,
            backgroundColor: `${color}20`,
          }}
        />
      )}
    </div>
  )
}

interface CollaboratorCursorsProps {
  collaborators: Collaborator[]
  currentUserId?: string
  containerRef?: React.RefObject<HTMLElement>
}

export const CollaboratorCursors: React.FC<CollaboratorCursorsProps> = ({
  collaborators,
  currentUserId,
  containerRef,
}) => {
  const visibleCollaborators = collaborators.filter(
    (c) => c.isOnline && c.cursorPosition && c.userId !== currentUserId
  )

  return (
    <>
      {visibleCollaborators.map((collaborator) => (
        <CollaboratorCursor
          key={collaborator.userId}
          collaborator={collaborator}
          position={collaborator.cursorPosition!}
          containerRef={containerRef}
        />
      ))}
    </>
  )
}

export default CollaboratorCursor
