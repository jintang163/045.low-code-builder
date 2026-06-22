import React, { useMemo } from 'react'
import { ConfigProvider, theme as antTheme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { ThemeProvider, useTheme } from './ThemeContext'

const ThemeConfigWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { theme, themeMode } = useTheme()

  const themeConfig = useMemo(() => {
    const algorithm = themeMode === 'dark' ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm

    return {
      algorithm,
      token: {
        colorPrimary: theme?.primaryColor || '#1677ff',
        colorSuccess: theme?.successColor || '#52c41a',
        colorWarning: theme?.warningColor || '#faad14',
        colorError: theme?.errorColor || '#ff4d4f',
        colorInfo: theme?.infoColor || '#1677ff',
        borderRadius: parseInt(theme?.borderRadius || '6px') || 6,
        fontFamily: theme?.fontFamily || "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
        fontSize: parseInt(theme?.fontSize || '14px') || 14,
      },
      components: {
        Button: {
          borderRadius: parseInt(theme?.borderRadius || '6px') || 6,
        },
        Input: {
          borderRadius: parseInt(theme?.borderRadius || '6px') || 6,
        },
        Card: {
          borderRadius: parseInt(theme?.borderRadius || '6px') || 6,
        },
        Modal: {
          borderRadius: (parseInt(theme?.borderRadius || '6px') || 6) * 1.5,
        },
        Select: {
          borderRadius: parseInt(theme?.borderRadius || '6px') || 6,
        },
      },
    }
  }, [theme, themeMode])

  return (
    <ConfigProvider locale={zhCN} theme={themeConfig}>
      {children}
    </ConfigProvider>
  )
}

const AppThemeProvider: React.FC<{ children: React.ReactNode; appId?: number }> = ({ children, appId }) => {
  return (
    <ThemeProvider appId={appId}>
      <ThemeConfigWrapper>{children}</ThemeConfigWrapper>
    </ThemeProvider>
  )
}

export default AppThemeProvider
