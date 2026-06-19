import React from 'react'
import { usePermission } from '@/hooks/usePermission'
import { Spin, Alert } from 'antd'

interface PermissionGuardProps {
  permission?: string | string[]
  role?: string | string[]
  children: React.ReactNode
  fallback?: React.ReactNode
  showLoading?: boolean
  showError?: boolean
}

export const PermissionGuard: React.FC<PermissionGuardProps> = ({
  permission,
  role,
  children,
  fallback = null,
  showLoading = true,
  showError = true,
}) => {
  const { loading, error, hasPermission, hasRole } = usePermission()

  if (loading && showLoading) {
    return (
      <div style={{ padding: 20, textAlign: 'center' }}>
        <Spin size="small" />
      </div>
    )
  }

  if (error && showError) {
    return <Alert type="error" message={error} showIcon />
  }

  if (permission && !hasPermission(permission)) {
    return <>{fallback}</>
  }

  if (role && !hasRole(role)) {
    return <>{fallback}</>
  }

  return <>{children}</>
}

export default PermissionGuard
