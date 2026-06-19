import React from 'react'
import { Button, ButtonProps } from 'antd'
import { usePermission } from '@/hooks/usePermission'

interface PermissionButtonProps extends ButtonProps {
  permission?: string | string[]
  role?: string | string[]
  componentId?: string
  hideWhenNoPermission?: boolean
}

export const PermissionButton: React.FC<PermissionButtonProps> = ({
  permission,
  role,
  componentId,
  hideWhenNoPermission = true,
  children,
  disabled,
  ...restProps
}) => {
  const { hasPermission, hasRole, isComponentDisabled, isComponentVisible, loading } = usePermission()

  const hasPerm = !permission || hasPermission(permission)
  const hasRolePerm = !role || hasRole(role)
  const visible = !componentId || isComponentVisible(componentId)
  const compDisabled = componentId ? isComponentDisabled(componentId) : false

  if (!visible && hideWhenNoPermission) {
    return null
  }

  if ((!hasPerm || !hasRolePerm) && hideWhenNoPermission) {
    return null
  }

  return (
    <Button
      disabled={disabled || compDisabled || !hasPerm || !hasRolePerm || loading}
      {...restProps}
    >
      {children}
    </Button>
  )
}

export default PermissionButton
