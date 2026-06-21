import React, { createContext, useContext, useMemo, ReactNode } from 'react'
import {
  useChartLinkage,
  LinkageConfig,
  LinkageFilter,
  LinkageState,
  LinkageTriggerType,
  TriggerParams,
  TargetUpdateHandler,
} from '../hooks/useChartLinkage'

interface LinkageContextValue {
  state: LinkageState
  linkageConfigs: LinkageConfig[]
  onChartClick: (params: TriggerParams) => void
  onChartHover: (params: TriggerParams) => void
  onChartSelect: (params: TriggerParams & { selected: boolean }) => void
  registerTarget: (componentId: string, handler: TargetUpdateHandler) => () => void
  clearLinkage: (componentId?: string) => void
  getFiltersForComponent: (componentId: string) => LinkageFilter[]
  getHighlightDataForComponent: (componentId: string) => any[]
  addLinkageConfig: (config: LinkageConfig) => void
  removeLinkageConfig: (sourceComponentId: string, triggerType: LinkageTriggerType) => void
  setLinkageConfigs: (configs: LinkageConfig[]) => void
  filterData: (data: Record<string, any>[], filters: LinkageFilter[]) => Record<string, any>[]
}

const LinkageContext = createContext<LinkageContextValue | null>(null)

interface LinkageProviderProps {
  children: ReactNode
  initialConfigs?: LinkageConfig[]
}

export const LinkageProvider: React.FC<LinkageProviderProps> = ({
  children,
  initialConfigs = [],
}) => {
  const linkage = useChartLinkage(initialConfigs)

  const contextValue = useMemo<LinkageContextValue>(() => ({
    state: linkage.state,
    linkageConfigs: linkage.linkageConfigs,
    onChartClick: linkage.onChartClick,
    onChartHover: linkage.onChartHover,
    onChartSelect: linkage.onChartSelect,
    registerTarget: linkage.registerTarget,
    clearLinkage: linkage.clearLinkage,
    getFiltersForComponent: linkage.getFiltersForComponent,
    getHighlightDataForComponent: linkage.getHighlightDataForComponent,
    addLinkageConfig: linkage.addLinkageConfig,
    removeLinkageConfig: linkage.removeLinkageConfig,
    setLinkageConfigs: linkage.setLinkageConfigs,
    filterData: linkage.filterData,
  }), [linkage])

  return (
    <LinkageContext.Provider value={contextValue}>
      {children}
    </LinkageContext.Provider>
  )
}

export function useLinkage(): LinkageContextValue {
  const context = useContext(LinkageContext)
  if (!context) {
    throw new Error('useLinkage must be used within a LinkageProvider')
  }
  return context
}

export default LinkageContext
