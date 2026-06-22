import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './styles/global.less'
import './styles/theme.less'
import AppThemeProvider from './context/AppThemeProvider'
import { initDB } from './utils/offline/indexedDB'
import { getNetworkDetector } from './utils/offline/networkDetector'
import { getSyncManager } from './utils/offline/syncManager'
import { registerSW } from 'virtual:pwa-register'

const initOfflineSupport = async () => {
  try {
    await initDB()
    console.log('[离线支持] IndexedDB 初始化成功')
  } catch (e) {
    console.error('[离线支持] IndexedDB 初始化失败:', e)
  }

  try {
    getNetworkDetector()
    console.log('[离线支持] 网络状态检测器初始化成功')
  } catch (e) {
    console.error('[离线支持] 网络状态检测器初始化失败:', e)
  }

  try {
    getSyncManager()
    console.log('[离线支持] 同步管理器初始化成功')
  } catch (e) {
    console.error('[离线支持] 同步管理器初始化失败:', e)
  }

  if (navigator.onLine) {
    try {
      const { cacheComponentTree } = await import('./utils/offline/pageOfflineService')
      await cacheComponentTree()
      console.log('[离线支持] 组件库预缓存成功')
    } catch (e) {
      console.warn('[离线支持] 组件库预缓存失败:', e)
    }
  }
}

const registerServiceWorker = () => {
  if ('serviceWorker' in navigator) {
    const updateSW = registerSW({
      onNeedRefresh() {
        console.log('[PWA] 发现新版本，正在更新...')
      },
      onOfflineReady() {
        console.log('[PWA] 应用已准备好离线使用')
      },
      onRegistered(r) {
        console.log('[PWA] Service Worker 注册成功')
      },
      onRegisterError(error) {
        console.error('[PWA] Service Worker 注册失败:', error)
      },
    })
    return updateSW
  }
  return undefined
}

initOfflineSupport()
registerServiceWorker()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AppThemeProvider>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </AppThemeProvider>
  </React.StrictMode>,
)
