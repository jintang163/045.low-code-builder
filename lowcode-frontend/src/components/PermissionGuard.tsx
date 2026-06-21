import React from 'react'

interface PermissionGuardProps {
  permission?: string
  children: React.ReactNode
}

const PermissionGuard: React.FC<PermissionGuardProps> = ({ children }) => {
  return <>{children}</>
}

export default PermissionGuard
