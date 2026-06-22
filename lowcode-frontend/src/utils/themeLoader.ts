import { themeApi, AppTheme } from '@/api'

let currentTheme: AppTheme | null = null

const THEME_STORAGE_KEY = 'app_theme'
const THEME_MODE_STORAGE_KEY = 'app_theme_mode'

export const loadThemeByAppId = async (appId: number): Promise<AppTheme | null> => {
  try {
    const theme = await themeApi.getDefault(appId)
    if (theme) {
      currentTheme = theme
      applyTheme(theme)
      saveThemeToStorage(theme)
    }
    return theme
  } catch (error) {
    console.error('加载主题失败:', error)
    const cachedTheme = getThemeFromStorage()
    if (cachedTheme) {
      applyTheme(cachedTheme)
      return cachedTheme
    }
    return null
  }
}

export const applyTheme = (theme: AppTheme): void => {
  const root = document.documentElement
  const body = document.body

  if (theme.primaryColor) {
    root.style.setProperty('--primary-color', theme.primaryColor)
    root.style.setProperty('--ant-primary-color', theme.primaryColor)
  }
  if (theme.successColor) {
    root.style.setProperty('--success-color', theme.successColor)
  }
  if (theme.warningColor) {
    root.style.setProperty('--warning-color', theme.warningColor)
  }
  if (theme.errorColor) {
    root.style.setProperty('--error-color', theme.errorColor)
  }
  if (theme.infoColor) {
    root.style.setProperty('--info-color', theme.infoColor)
  }
  if (theme.borderRadius) {
    root.style.setProperty('--border-radius', theme.borderRadius)
    root.style.setProperty('--ant-border-radius', theme.borderRadius)
  }
  if (theme.fontFamily) {
    root.style.setProperty('--font-family', theme.fontFamily)
    root.style.fontFamily = theme.fontFamily
  }
  if (theme.fontSize) {
    root.style.setProperty('--font-size', theme.fontSize)
    root.style.fontSize = theme.fontSize
  }

  const mode = theme.themeMode || 'light'
  body.setAttribute('data-theme', mode)
  root.setAttribute('data-theme', mode)
  localStorage.setItem(THEME_MODE_STORAGE_KEY, mode)

  const customStyleId = 'app-theme-custom-css'
  let styleEl = document.getElementById(customStyleId) as HTMLStyleElement
  if (!styleEl) {
    styleEl = document.createElement('style')
    styleEl.id = customStyleId
    document.head.appendChild(styleEl)
  }
  styleEl.textContent = theme.customCss || ''

  currentTheme = theme
}

export const applyThemeFromCSS = (css: string): void => {
  const styleId = 'app-dynamic-theme'
  let styleEl = document.getElementById(styleId) as HTMLStyleElement
  if (!styleEl) {
    styleEl = document.createElement('style')
    styleEl.id = styleId
    document.head.appendChild(styleEl)
  }
  styleEl.textContent = css
}

export const getCurrentTheme = (): AppTheme | null => {
  return currentTheme
}

export const setThemeMode = (mode: 'light' | 'dark'): void => {
  if (currentTheme) {
    applyTheme({ ...currentTheme, themeMode: mode })
  } else {
    document.body.setAttribute('data-theme', mode)
    document.documentElement.setAttribute('data-theme', mode)
    localStorage.setItem(THEME_MODE_STORAGE_KEY, mode)
  }
}

export const toggleThemeMode = (): void => {
  const currentMode = currentTheme?.themeMode || localStorage.getItem(THEME_MODE_STORAGE_KEY) || 'light'
  const newMode = currentMode === 'light' ? 'dark' : 'light'
  setThemeMode(newMode as 'light' | 'dark')
}

export const getThemeMode = (): 'light' | 'dark' => {
  const stored = localStorage.getItem(THEME_MODE_STORAGE_KEY)
  if (stored === 'dark' || stored === 'light') {
    return stored
  }
  return currentTheme?.themeMode as 'light' | 'dark' || 'light'
}

const saveThemeToStorage = (theme: AppTheme): void => {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify(theme))
  } catch (e) {
    console.warn('保存主题到本地存储失败:', e)
  }
}

const getThemeFromStorage = (): AppTheme | null => {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY)
    if (stored) {
      return JSON.parse(stored)
    }
  } catch (e) {
    console.warn('从本地存储加载主题失败:', e)
  }
  return null
}

export const clearThemeStorage = (): void => {
  localStorage.removeItem(THEME_STORAGE_KEY)
  localStorage.removeItem(THEME_MODE_STORAGE_KEY)
}

export const generateThemeCSS = (theme: AppTheme): string => {
  let css = ':root {\n'

  if (theme.primaryColor) {
    css += `  --primary-color: ${theme.primaryColor};\n`
    css += `  --ant-primary-color: ${theme.primaryColor};\n`
  }
  if (theme.successColor) {
    css += `  --success-color: ${theme.successColor};\n`
  }
  if (theme.warningColor) {
    css += `  --warning-color: ${theme.warningColor};\n`
  }
  if (theme.errorColor) {
    css += `  --error-color: ${theme.errorColor};\n`
  }
  if (theme.infoColor) {
    css += `  --info-color: ${theme.infoColor};\n`
  }
  if (theme.borderRadius) {
    css += `  --border-radius: ${theme.borderRadius};\n`
    css += `  --ant-border-radius: ${theme.borderRadius};\n`
  }
  if (theme.fontFamily) {
    css += `  --font-family: ${theme.fontFamily};\n`
  }
  if (theme.fontSize) {
    css += `  --font-size: ${theme.fontSize};\n`
  }

  css += '}\n'

  if (theme.themeMode === 'dark') {
    css += `
[data-theme='dark'] {
  --bg-color: #141414;
  --bg-color-secondary: #1f1f1f;
  --text-color: #ffffffd9;
  --text-color-secondary: #ffffffa6;
  --text-color-tertiary: #ffffff73;
  --border-color: #424242;
  --border-color-secondary: #303030;
  --shadow-color: rgba(0, 0, 0, 0.45);
}
`
  }

  if (theme.customCss) {
    css += '\n/* Custom CSS */\n'
    css += theme.customCss + '\n'
  }

  return css
}

export default {
  loadThemeByAppId,
  applyTheme,
  applyThemeFromCSS,
  getCurrentTheme,
  setThemeMode,
  toggleThemeMode,
  getThemeMode,
  generateThemeCSS,
  clearThemeStorage,
}
