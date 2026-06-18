import { create } from 'zustand'

interface AppState {
  currentApp: { id: number; name: string } | null
  setCurrentApp: (app: { id: number; name: string } | null) => void
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
