import { useState, useCallback, useRef, useMemo } from 'react'

export type LinkageTriggerType = 'click' | 'hover' | 'select'

export type LinkageActionType = 'filter' | 'highlight' | 'drillDown'

export interface MappingField {
  sourceField: string
  targetField: string
}

export interface LinkageTarget {
  targetComponentId: string
  mappingFields: MappingField[]
  action: LinkageActionType
}

export interface LinkageConfig {
  sourceComponentId: string
  triggerType: LinkageTriggerType
  targets: LinkageTarget[]
}

export interface LinkageFilter {
  field: string
  value: any
  operator?: 'eq' | 'ne' | 'in' | 'notIn' | 'gt' | 'lt' | 'gte' | 'lte'
}

export interface LinkageState {
  activeFilters: Record<string, LinkageFilter[]>
  highlightData: Record<string, any[]>
  activeConfigId: string | null
}

export interface TriggerParams {
  componentId: string
  triggerType: LinkageTriggerType
  data: Record<string, any>
}

export interface TargetUpdateHandler {
  (filters: LinkageFilter[], highlightData: any[]): void
}

export function useChartLinkage(initialConfigs: LinkageConfig[] = []) {
  const [linkageConfigs, setLinkageConfigs] = useState<LinkageConfig[]>(initialConfigs)

  const [activeFilters, setActiveFilters] = useState<Record<string, LinkageFilter[]>>({})

  const [highlightData, setHighlightData] = useState<Record<string, any[]>>({})

  const targetHandlers = useRef<Map<string, TargetUpdateHandler>>(new Map())

  const registerTarget = useCallback((componentId: string, handler: TargetUpdateHandler) => {
    targetHandlers.current.set(componentId, handler)
    return () => {
      targetHandlers.current.delete(componentId)
    }
  }, [])

  const notifyTarget = useCallback((targetId: string, filters: LinkageFilter[], highlight: any[]) => {
    const handler = targetHandlers.current.get(targetId)
    if (handler) {
      handler(filters, highlight)
    }
  }, [])

  const buildFiltersFromData = useCallback((
    data: Record<string, any>,
    mappingFields: MappingField[]
  ): LinkageFilter[] => {
    return mappingFields
      .filter(mf => data[mf.sourceField] !== undefined && data[mf.sourceField] !== null)
      .map(mf => ({
        field: mf.targetField,
        value: data[mf.sourceField],
        operator: 'eq' as const,
      }))
  }, [])

  const mergeFilters = useCallback((
    existing: LinkageFilter[],
    newFilters: LinkageFilter[]
  ): LinkageFilter[] => {
    const merged = [...existing]
    for (const nf of newFilters) {
      const existingIndex = merged.findIndex(f => f.field === nf.field && f.operator === nf.operator)
      if (existingIndex >= 0) {
        merged[existingIndex] = nf
      } else {
        merged.push(nf)
      }
    }
    return merged
  }, [])

  const clearTargetFilters = useCallback((
    existing: LinkageFilter[],
    sourceComponentId: string,
    configs: LinkageConfig[]
  ): LinkageFilter[] => {
    const relevantFields = new Set<string>()
    for (const config of configs) {
      if (config.sourceComponentId === sourceComponentId) {
        for (const target of config.targets) {
          for (const mf of target.mappingFields) {
            relevantFields.add(mf.targetField)
          }
        }
      }
    }
    return existing.filter(f => !relevantFields.has(f.field))
  }, [])

  const onChartClick = useCallback((params: TriggerParams) => {
    const { componentId, data } = params
    const configs = linkageConfigs.filter(
      c => c.sourceComponentId === componentId && c.triggerType === 'click'
    )

    for (const config of configs) {
      for (const target of config.targets) {
        const filters = buildFiltersFromData(data, target.mappingFields)

        setActiveFilters(prev => {
          const existing = prev[target.targetComponentId] || []
          let updated: LinkageFilter[]
          
          if (filters.length === 0) {
            updated = clearTargetFilters(existing, componentId, linkageConfigs)
          } else {
            const withoutSource = clearTargetFilters(existing, componentId, linkageConfigs)
            updated = mergeFilters(withoutSource, filters)
          }

          notifyTarget(target.targetComponentId, updated, highlightData[target.targetComponentId] || [])

          return {
            ...prev,
            [target.targetComponentId]: updated,
          }
        })

        if (target.action === 'highlight') {
          setHighlightData(prev => ({
            ...prev,
            [target.targetComponentId]: [data],
          }))
        }
      }
    }
  }, [linkageConfigs, buildFiltersFromData, mergeFilters, clearTargetFilters, notifyTarget, highlightData])

  const onChartHover = useCallback((params: TriggerParams) => {
    const { componentId, data } = params
    const configs = linkageConfigs.filter(
      c => c.sourceComponentId === componentId && c.triggerType === 'hover'
    )

    for (const config of configs) {
      for (const target of config.targets) {
        if (target.action === 'highlight') {
          setHighlightData(prev => {
            const updated = {
              ...prev,
              [target.targetComponentId]: [data],
            }
            notifyTarget(
              target.targetComponentId,
              activeFilters[target.targetComponentId] || [],
              [data]
            )
            return updated
          })
        } else if (target.action === 'filter') {
          const filters = buildFiltersFromData(data, target.mappingFields)
          setActiveFilters(prev => {
            const existing = prev[target.targetComponentId] || []
            const updated = mergeFilters(existing, filters)
            notifyTarget(target.targetComponentId, updated, prev[target.targetComponentId] || [])
            return {
              ...prev,
              [target.targetComponentId]: updated,
            }
          })
        }
      }
    }
  }, [linkageConfigs, buildFiltersFromData, mergeFilters, notifyTarget, activeFilters])

  const onChartSelect = useCallback((params: TriggerParams & { selected: boolean }) => {
    const { componentId, data, selected } = params
    const configs = linkageConfigs.filter(
      c => c.sourceComponentId === componentId && c.triggerType === 'select'
    )

    for (const config of configs) {
      for (const target of config.targets) {
        if (target.action === 'highlight') {
          setHighlightData(prev => {
            const current = prev[target.targetComponentId] || []
            let updated: any[]
            if (selected) {
              updated = [...current, data]
            } else {
              updated = current.filter(d => {
                const keys = Object.keys(data)
                return !keys.every(k => d[k] === data[k])
              })
            }
            notifyTarget(
              target.targetComponentId,
              activeFilters[target.targetComponentId] || [],
              updated
            )
            return {
              ...prev,
              [target.targetComponentId]: updated,
            }
          })
        } else if (target.action === 'filter') {
          const filters = buildFiltersFromData(data, target.mappingFields)
          setActiveFilters(prev => {
            const existing = prev[target.targetComponentId] || []
            let updated: LinkageFilter[]
            if (selected) {
              updated = mergeFilters(existing, filters)
            } else {
              const filterFields = new Set(filters.map(f => f.field))
              updated = existing.filter(f => !filterFields.has(f.field))
            }
            notifyTarget(
              target.targetComponentId,
              updated,
              highlightData[target.targetComponentId] || []
            )
            return {
              ...prev,
              [target.targetComponentId]: updated,
            }
          })
        }
      }
    }
  }, [linkageConfigs, buildFiltersFromData, mergeFilters, notifyTarget, activeFilters, highlightData])

  const clearLinkage = useCallback((componentId?: string) => {
    if (componentId) {
      setActiveFilters(prev => {
        const updated = { ...prev }
        delete updated[componentId]
        return updated
      })
      setHighlightData(prev => {
        const updated = { ...prev }
        delete updated[componentId]
        return updated
      })
      notifyTarget(componentId, [], [])
    } else {
      for (const targetId of Object.keys(activeFilters)) {
        notifyTarget(targetId, [], [])
      }
      setActiveFilters({})
      setHighlightData({})
    }
  }, [activeFilters, notifyTarget])

  const getFiltersForComponent = useCallback((componentId: string): LinkageFilter[] => {
    return activeFilters[componentId] || []
  }, [activeFilters])

  const getHighlightDataForComponent = useCallback((componentId: string): any[] => {
    return highlightData[componentId] || []
  }, [highlightData])

  const addLinkageConfig = useCallback((config: LinkageConfig) => {
    setLinkageConfigs(prev => [...prev, config])
  }, [])

  const removeLinkageConfig = useCallback((sourceComponentId: string, triggerType: LinkageTriggerType) => {
    setLinkageConfigs(prev => 
      prev.filter(c => !(c.sourceComponentId === sourceComponentId && c.triggerType === triggerType))
    )
  }, [])

  const setLinkageConfigsList = useCallback((configs: LinkageConfig[]) => {
    setLinkageConfigs(configs)
  }, [])

  const filterData = useCallback((data: Record<string, any>[], filters: LinkageFilter[]): Record<string, any>[] => {
    if (filters.length === 0) return data
    return data.filter(item => {
      return filters.every(filter => {
        const value = item[filter.field]
        const op = filter.operator || 'eq'
        switch (op) {
          case 'eq':
            return value === filter.value
          case 'ne':
            return value !== filter.value
          case 'in':
            return Array.isArray(filter.value) && filter.value.includes(value)
          case 'notIn':
            return Array.isArray(filter.value) && !filter.value.includes(value)
          case 'gt':
            return value > filter.value
          case 'lt':
            return value < filter.value
          case 'gte':
            return value >= filter.value
          case 'lte':
            return value <= filter.value
          default:
            return value === filter.value
        }
      })
    })
  }, [])

  const state = useMemo<LinkageState>(() => ({
    activeFilters,
    highlightData,
    activeConfigId: null,
  }), [activeFilters, highlightData])

  return {
    state,
    linkageConfigs,
    onChartClick,
    onChartHover,
    onChartSelect,
    registerTarget,
    clearLinkage,
    getFiltersForComponent,
    getHighlightDataForComponent,
    addLinkageConfig,
    removeLinkageConfig,
    setLinkageConfigs: setLinkageConfigsList,
    filterData,
  }
}

export default useChartLinkage
