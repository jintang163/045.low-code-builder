import { useState, useEffect, useCallback, useMemo } from 'react'
import { permissionApi, UserPermissionVO, ComponentPermissionVO, FieldPermissionVO } from '@/api/permission'
import { useAppStore } from '@/store/appStore'

export function usePermission() {
  const { currentApp } = useAppStore()
  const [permissions, setPermissions] = useState<UserPermissionVO | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadPermissions = useCallback(async () => {
    if (!currentApp?.id) return
    setLoading(true)
    setError(null)
    try {
      const res = await permissionApi.getCurrentUserPermissions(currentApp.id)
      if (res.code === 0 || res.code === 200) {
        setPermissions(res.data)
      } else {
        setError(res.message || '获取权限失败')
      }
    } catch (e: any) {
      setError(e.message || '获取权限失败')
    } finally {
      setLoading(false)
    }
  }, [currentApp?.id])

  useEffect(() => {
    loadPermissions()
  }, [loadPermissions])

  const hasRole = useCallback((role: string | string[]) => {
    if (!permissions) return false
    const roles = Array.isArray(role) ? role : [role]
    return roles.some(r => permissions.roles.includes(r))
  }, [permissions])

  const hasPermission = useCallback((permission: string | string[]) => {
    if (!permissions) return false
    const perms = Array.isArray(permission) ? permission : [permission]
    return perms.some(p => permissions.permissions.includes(p))
  }, [permissions])

  const canAccessPage = useCallback((pageId: number) => {
    if (!permissions) return true
    if (!permissions.accessiblePageIds || permissions.accessiblePageIds.length === 0) return true
    return permissions.accessiblePageIds.includes(pageId)
  }, [permissions])

  const getComponentPermission = useCallback((componentId: string): ComponentPermissionVO | null => {
    if (!permissions?.componentPermissions) return null
    return permissions.componentPermissions[componentId] || null
  }, [permissions])

  const isComponentVisible = useCallback((componentId: string) => {
    const perm = getComponentPermission(componentId)
    return perm ? perm.visible : true
  }, [getComponentPermission])

  const isComponentDisabled = useCallback((componentId: string) => {
    const perm = getComponentPermission(componentId)
    return perm ? perm.disabled : false
  }, [getComponentPermission])

  const getFieldPermission = useCallback((fieldId: number): FieldPermissionVO | null => {
    if (!permissions?.fieldPermissions) return null
    return permissions.fieldPermissions[fieldId] || null
  }, [permissions])

  const canViewField = useCallback((fieldId: number) => {
    const perm = getFieldPermission(fieldId)
    return perm ? perm.canView : true
  }, [getFieldPermission])

  const canEditField = useCallback((fieldId: number) => {
    const perm = getFieldPermission(fieldId)
    return perm ? perm.canEdit : false
  }, [getFieldPermission])

  const getRowLevelSqlFilter = useCallback(() => {
    return permissions?.rowLevelSqlFilter || null
  }, [permissions])

  const getModelRowLevelFilter = useCallback((modelId: number) => {
    if (!permissions?.modelRowLevelFilters) return null
    return permissions.modelRowLevelFilters[modelId] || null
  }, [permissions])

  const isAdmin = useMemo(() => {
    return hasRole(['admin', 'super_admin', 'SYSTEM_ADMIN', 'APP_ADMIN'])
  }, [hasRole])

  return {
    permissions,
    loading,
    error,
    reloadPermissions: loadPermissions,
    hasRole,
    hasPermission,
    canAccessPage,
    isComponentVisible,
    isComponentDisabled,
    getComponentPermission,
    canViewField,
    canEditField,
    getFieldPermission,
    getRowLevelSqlFilter,
    getModelRowLevelFilter,
    isAdmin,
  }
}
