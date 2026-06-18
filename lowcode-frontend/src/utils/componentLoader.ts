import React from 'react'
import { customComponentApi, CustomComponent } from '@/api/page'

interface ComponentModule {
  default: React.ComponentType<any>
  propSchema?: any
  eventSchema?: any
  exposedEvents?: string[]
  defaultProps?: any
  defaultStyle?: any
}

interface LoadedComponent {
  component: React.ComponentType<any>
  config: CustomComponent
  schema: {
    propSchema?: any
    eventSchema?: any
    exposedEvents?: string[]
    defaultProps?: any
    defaultStyle?: any
  }
}

const componentCache = new Map<string, LoadedComponent>()
const loadingPromises = new Map<string, Promise<LoadedComponent>>()

export const loadCustomComponent = async (
  componentType: string,
  version?: string
): Promise<LoadedComponent> => {
  const cacheKey = version ? `${componentType}@${version}` : componentType

  if (componentCache.has(cacheKey)) {
    return componentCache.get(cacheKey)!
  }

  if (loadingPromises.has(cacheKey)) {
    return loadingPromises.get(cacheKey)!
  }

  const loadPromise = (async () => {
    try {
      const componentRes = await customComponentApi.getByType(componentType)
      if (!componentRes.data) {
        throw new Error(`Component ${componentType} not found`)
      }

      const componentConfig = componentRes.data
      const bundleUrl = componentConfig.packageUrl

      if (!bundleUrl) {
        const urlRes = await customComponentApi.getBundleUrl(componentConfig.id!, version)
        if (!urlRes.data) {
          throw new Error(`Failed to get bundle URL for ${componentType}`)
        }
        return await loadComponentFromUrl(urlRes.data, componentConfig, cacheKey)
      }

      return await loadComponentFromUrl(bundleUrl, componentConfig, cacheKey)
    } catch (error) {
      loadingPromises.delete(cacheKey)
      throw error
    }
  })()

  loadingPromises.set(cacheKey, loadPromise)
  return loadPromise
}

const loadComponentFromUrl = async (
  url: string,
  config: CustomComponent,
  cacheKey: string
): Promise<LoadedComponent> => {
  try {
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error(`Failed to load component bundle: ${response.statusText}`)
    }

    const arrayBuffer = await response.arrayBuffer()
    const zip = await loadJSZip()
    const unzipped = await zip.loadAsync(arrayBuffer)

    const componentJsonEntry = unzipped.file(/component\.json$/)[0]
    let componentJson: any = {}
    if (componentJsonEntry) {
      const jsonStr = await componentJsonEntry.async('string')
      componentJson = JSON.parse(jsonStr)
    }

    const mainFile = componentJson.main || 'index.js'
    const mainEntry = unzipped.file(mainFile)
    if (!mainEntry) {
      throw new Error(`Main file ${mainFile} not found in component package`)
    }

    const mainCode = await mainEntry.async('string')
    const module = await executeModule(mainCode, unzipped)

    const versionInfo = config.currentVersionInfo
    const result: LoadedComponent = {
      component: module.default,
      config,
      schema: {
        propSchema: componentJson.propSchema || (versionInfo?.propSchema ? JSON.parse(versionInfo.propSchema) : undefined),
        eventSchema: componentJson.eventSchema || (versionInfo?.eventSchema ? JSON.parse(versionInfo.eventSchema) : undefined),
        exposedEvents: componentJson.exposedEvents || (versionInfo?.exposedEvents ? JSON.parse(versionInfo.exposedEvents) : []),
        defaultProps: componentJson.defaultProps || (versionInfo?.defaultProps ? JSON.parse(versionInfo.defaultProps) : {}),
        defaultStyle: componentJson.defaultStyle || (versionInfo?.defaultStyle ? JSON.parse(versionInfo.defaultStyle) : {}),
      },
    }

    componentCache.set(cacheKey, result)
    loadingPromises.delete(cacheKey)
    return result
  } catch (error) {
    loadingPromises.delete(cacheKey)
    throw error
  }
}

const executeModule = async (code: string, zip: any): Promise<ComponentModule> => {
  const require = async (path: string) => {
    let filePath = path.startsWith('.') ? path : `./${path}`
    if (!filePath.endsWith('.js') && !filePath.endsWith('.json')) {
      filePath += '.js'
    }

    const file = zip.file(filePath.replace(/^\.\//, ''))
    if (!file) {
      const jsonFile = zip.file(filePath.replace(/\.js$/, '.json').replace(/^\.\//, ''))
      if (jsonFile) {
        const content = await jsonFile.async('string')
        return { default: JSON.parse(content) }
      }
      throw new Error(`Module not found: ${path}`)
    }

    const content = await file.async('string')
    if (filePath.endsWith('.json')) {
      return { default: JSON.parse(content) }
    }

    return executeModule(content, zip)
  }

  const moduleExports: any = {}
  const module = { exports: moduleExports }

  const wrappedCode = `
    (function(module, exports, require) {
      ${code}
    })(module, module.exports, require);
  `

  const asyncRequire = (path: string) => {
    return require(path)
  }

  const func = new Function('module', 'exports', 'require', 'React', wrappedCode)
  await func(module, module.exports, asyncRequire, React)

  return module.exports as ComponentModule
}

const loadJSZip = async () => {
  if (!(window as any).JSZip) {
    const script = document.createElement('script')
    script.src = 'https://cdn.jsdelivr.net/npm/jszip@3.10.1/dist/jszip.min.js'
    await new Promise((resolve, reject) => {
      script.onload = resolve
      script.onerror = reject
      document.head.appendChild(script)
    })
  }
  return new (window as any).JSZip()
}

export const preloadCustomComponents = async (componentTypes: string[]) => {
  const promises = componentTypes.map((type) =>
    loadCustomComponent(type).catch((e) => {
      console.warn(`Failed to preload component ${type}:`, e)
      return null
    })
  )
  return Promise.all(promises)
}

export const getComponentSchema = (componentType: string) => {
  for (const [key, value] of componentCache.entries()) {
    if (key.startsWith(componentType + '@') || key === componentType) {
      return value.schema
    }
  }
  return null
}

export const clearComponentCache = () => {
  componentCache.clear()
  loadingPromises.clear()
}
