import { create } from 'zustand'
import { AppInfo } from '@/api'

interface AppState {
  currentApp: AppInfo | null
  setCurrentApp: (app: AppInfo | null) => void
  collapsed: boolean
  setCollapsed: (collapsed: boolean) => void
  userInfo: any
  setUserInfo: (info: any) => void
}

export const useAppStore = create<AppState>((set) => ({
  currentApp: null,
  setCurrentApp: (app) => set({ currentApp: app }),
  collapsed: false,
  setCollapsed: (collapsed) => set({ collapsed }),
  userInfo: null,
  setUserInfo: (info) => set({ userInfo: info }),
}))
