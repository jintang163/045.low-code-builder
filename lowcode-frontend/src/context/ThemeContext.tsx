import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react'
import { themeApi, AppTheme } from '@/api'

interface ThemeContextType {
  theme: AppTheme | null
  themeMode: 'light' | 'dark'
  setTheme: (theme: AppTheme) => void
  setThemeMode: (mode: 'light' | 'dark') => void
  loadTheme: (appId: number) => Promise<void>
  applyTheme: (theme: AppTheme) => void
  toggleThemeMode: () => void
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

const defaultTheme: AppTheme = {
  appId: 0,
  themeName: '默认主题',
  themeMode: 'light',
  primaryColor: '#1677ff',
  successColor: '#52c41a',
  warningColor: '#faad14',
  errorColor: '#ff4d4f',
  infoColor: '#1677ff',
  borderRadius: '6px',
  fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
  fontSize: '14px',
  layoutMode: 'side',
  sidebarTheme: 'dark',
  headerTheme: 'light',
  customCss: '',
  themeConfig: '',
  isDefault: 1,
  status: 1,
}

export const ThemeProvider: React.FC<{ children: ReactNode; appId?: number }> = ({ children, appId }) => {
  const [theme, setThemeState] = useState<AppTheme | null>(null)
  const [themeMode, setThemeMode] = useState<'light' | 'dark'>('light')

  const applyTheme = useCallback((themeData: AppTheme) => {
    const root = document.documentElement

    if (themeData.primaryColor) {
      root.style.setProperty('--primary-color', themeData.primaryColor)
      root.style.setProperty('--ant-primary-color', themeData.primaryColor)
    }
    if (themeData.successColor) {
      root.style.setProperty('--success-color', themeData.successColor)
    }
    if (themeData.warningColor) {
      root.style.setProperty('--warning-color', themeData.warningColor)
    }
    if (themeData.errorColor) {
      root.style.setProperty('--error-color', themeData.errorColor)
    }
    if (themeData.infoColor) {
      root.style.setProperty('--info-color', themeData.infoColor)
    }
    if (themeData.borderRadius) {
      root.style.setProperty('--border-radius', themeData.borderRadius)
      root.style.setProperty('--ant-border-radius', themeData.borderRadius)
    }
    if (themeData.fontFamily) {
      root.style.setProperty('--font-family', themeData.fontFamily)
      root.style.fontFamily = themeData.fontFamily
    }
    if (themeData.fontSize) {
      root.style.setProperty('--font-size', themeData.fontSize)
      root.style.fontSize = themeData.fontSize
    }

    const mode = themeData.themeMode || 'light'
    document.body.setAttribute('data-theme', mode)
    document.documentElement.setAttribute('data-theme', mode)

    const customStyleId = 'app-theme-custom-css'
    let styleEl = document.getElementById(customStyleId) as HTMLStyleElement
    if (!styleEl) {
      styleEl = document.createElement('style')
      styleEl.id = customStyleId
      document.head.appendChild(styleEl)
    }
    styleEl.textContent = themeData.customCss || ''

    setThemeState(themeData)
    setThemeMode(mode as 'light' | 'dark')
  }, [])

  const loadTheme = useCallback(async (appId: number) => {
    try {
      const res = await themeApi.getDefault(appId)
      if (res) {
        applyTheme(res)
      } else {
        applyTheme({ ...defaultTheme, appId })
      }
    } catch (error) {
      console.error('加载主题失败:', error)
      applyTheme({ ...defaultTheme, appId })
    }
  }, [applyTheme])

  const setTheme = useCallback((themeData: AppTheme) => {
    applyTheme(themeData)
  }, [applyTheme])

  const toggleThemeMode = useCallback(() => {
    if (theme) {
      const newMode = themeMode === 'light' ? 'dark' : 'light'
      const newTheme = { ...theme, themeMode: newMode }
      applyTheme(newTheme)
    }
  }, [theme, themeMode, applyTheme])

  useEffect(() => {
    if (appId) {
      loadTheme(appId)
    } else {
      applyTheme(defaultTheme)
    }
  }, [appId, loadTheme, applyTheme])

  return (
    <ThemeContext.Provider
      value={{
        theme,
        themeMode,
        setTheme,
        setThemeMode,
        loadTheme,
        applyTheme,
        toggleThemeMode,
      }}
    >
      {children}
    </ThemeContext.Provider>
  )
}

export const useTheme = (): ThemeContextType => {
  const context = useContext(ThemeContext)
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider')
  }
  return context
}

export { defaultTheme }
