import {
  get,
  set,
  getAll,
  remove,
  addPendingChange,
  ResourceType,
  ChangeAction,
} from './indexedDB'
import { isOnline } from './networkDetector'
import { PageInfo, pageApi } from '@/api/page'
import { DataModel, dataModelApi } from '@/api/dataModel'
import { BusinessLogic, logicApi } from '@/api/flow'

export interface PageOfflineInfo extends PageInfo {
  isOffline?: boolean
  syncStatus?: 'synced' | 'pending' | 'failed'
}

export async function savePageOffline(page: PageInfo): Promise<void> {
  if (!page.id) {
    throw new Error('页面ID不能为空')
  }

  await set('pages', page.id, page)

  if (isOnline()) {
    try {
      if (page.id) {
        await pageApi.update(page)
      }
    } catch (e) {
      console.error('在线保存页面失败，已保存到本地:', e)
      await addPendingChange({
        resourceType: 'page',
        resourceId: page.id,
        action: 'update',
        data: page,
      })
    }
  } else {
    await addPendingChange({
      resourceType: 'page',
      resourceId: page.id,
      action: 'update',
      data: page,
    })
  }
}

export async function getPageOffline(pageId: number): Promise<PageInfo | undefined> {
  return get<PageInfo>('pages', pageId)
}

export async function getPageListOffline(appId: number): Promise<PageInfo[]> {
  const allPages = await getAll<PageInfo>('pages')
  return allPages.filter((page) => page.appId === appId)
}

export async function savePageOfflineOnly(page: PageInfo): Promise<void> {
  if (!page.id) {
    throw new Error('页面ID不能为空')
  }
  await set('pages', page.id, page)
}

export async function syncPageToServer(pageId: number): Promise<void> {
  if (!isOnline()) {
    throw new Error('网络不可用，无法同步')
  }

  const page = await get<PageInfo>('pages', pageId)
  if (!page) {
    throw new Error('本地不存在该页面')
  }

  try {
    if (page.id) {
      await pageApi.update(page)
    }
  } catch (e) {
    console.error('同步页面到服务器失败:', e)
    throw e
  }
}

export async function loadPageForEdit(pageId: number): Promise<{ page: PageInfo; fromCache: boolean }> {
  if (isOnline()) {
    try {
      const res = await pageApi.get(pageId)
      const pageData = res.data

      if (pageData) {
        await set('pages', pageId, pageData)
      }

      return { page: pageData, fromCache: false }
    } catch (e) {
      console.warn('从服务器加载页面失败，尝试从本地加载:', e)
      const localPage = await get<PageInfo>('pages', pageId)
      if (localPage) {
        return { page: localPage, fromCache: true }
      }
      throw e
    }
  } else {
    const localPage = await get<PageInfo>('pages', pageId)
    if (localPage) {
      return { page: localPage, fromCache: true }
    }
    throw new Error('离线状态下本地不存在该页面数据')
  }
}

export async function createPageOffline(page: Omit<PageInfo, 'id'> & { id?: number }): Promise<PageInfo> {
  const tempId = page.id || Date.now()
  const newPage = { ...page, id: tempId }

  await set('pages', tempId, newPage)

  if (isOnline()) {
    try {
      const res = await pageApi.save(newPage as PageInfo)
      if (res.data) {
        await set('pages', res.data.id!, res.data)
        if (tempId !== res.data.id) {
          await remove('pages', tempId)
        }
        return res.data
      }
    } catch (e) {
      console.error('在线创建页面失败，已保存到本地:', e)
      await addPendingChange({
        resourceType: 'page',
        resourceId: tempId,
        action: 'create',
        data: newPage,
      })
    }
  } else {
    await addPendingChange({
      resourceType: 'page',
      resourceId: tempId,
      action: 'create',
      data: newPage,
    })
  }

  return newPage as PageInfo
}

export async function deletePageOffline(pageId: number): Promise<void> {
  await remove('pages', pageId)

  if (isOnline()) {
    try {
      await pageApi.delete(pageId)
    } catch (e) {
      console.error('在线删除页面失败，已记录待同步:', e)
      await addPendingChange({
        resourceType: 'page',
        resourceId: pageId,
        action: 'delete',
        data: { id: pageId },
      })
    }
  } else {
    await addPendingChange({
      resourceType: 'page',
      resourceId: pageId,
      action: 'delete',
      data: { id: pageId },
    })
  }
}

export async function saveDataModelOffline(model: DataModel): Promise<void> {
  if (!model.id) {
    throw new Error('数据模型ID不能为空')
  }

  await set('dataModels', model.id, model)

  if (isOnline()) {
    try {
      if (model.id) {
        await dataModelApi.update(model)
      }
    } catch (e) {
      console.error('在线保存数据模型失败，已保存到本地:', e)
      await addPendingChange({
        resourceType: 'dataModel',
        resourceId: model.id,
        action: 'update',
        data: model,
      })
    }
  } else {
    await addPendingChange({
      resourceType: 'dataModel',
      resourceId: model.id,
      action: 'update',
      data: model,
    })
  }
}

export async function getDataModelOffline(modelId: number): Promise<DataModel | undefined> {
  return get<DataModel>('dataModels', modelId)
}

export async function getDataModelListOffline(appId: number): Promise<DataModel[]> {
  const allModels = await getAll<DataModel>('dataModels')
  return allModels.filter((model) => model.appId === appId)
}

export async function loadDataModelForEdit(modelId: number): Promise<{ model: DataModel; fromCache: boolean }> {
  if (isOnline()) {
    try {
      const res = await dataModelApi.get(modelId)
      const modelData = res.data

      if (modelData) {
        await set('dataModels', modelId, modelData)
      }

      return { model: modelData, fromCache: false }
    } catch (e) {
      console.warn('从服务器加载数据模型失败，尝试从本地加载:', e)
      const localModel = await get<DataModel>('dataModels', modelId)
      if (localModel) {
        return { model: localModel, fromCache: true }
      }
      throw e
    }
  } else {
    const localModel = await get<DataModel>('dataModels', modelId)
    if (localModel) {
      return { model: localModel, fromCache: true }
    }
    throw new Error('离线状态下本地不存在该数据模型')
  }
}

export async function saveBusinessLogicOffline(logic: BusinessLogic): Promise<void> {
  if (!logic.id) {
    throw new Error('业务逻辑ID不能为空')
  }

  await set('businessLogics', logic.id, logic)

  if (isOnline()) {
    try {
      if (logic.id) {
        await logicApi.update(logic)
      }
    } catch (e) {
      console.error('在线保存业务逻辑失败，已保存到本地:', e)
      await addPendingChange({
        resourceType: 'businessLogic',
        resourceId: logic.id,
        action: 'update',
        data: logic,
      })
    }
  } else {
    await addPendingChange({
      resourceType: 'businessLogic',
      resourceId: logic.id,
      action: 'update',
      data: logic,
    })
  }
}

export async function getBusinessLogicOffline(logicId: number): Promise<BusinessLogic | undefined> {
  return get<BusinessLogic>('businessLogics', logicId)
}

export async function getBusinessLogicListOffline(appId: number): Promise<BusinessLogic[]> {
  const allLogics = await getAll<BusinessLogic>('businessLogics')
  return allLogics.filter((logic) => logic.appId === appId)
}

export async function loadBusinessLogicForEdit(logicId: number): Promise<{ logic: BusinessLogic; fromCache: boolean }> {
  if (isOnline()) {
    try {
      const res = await logicApi.get(logicId)
      const logicData = res.data

      if (logicData) {
        await set('businessLogics', logicId, logicData)
      }

      return { logic: logicData, fromCache: false }
    } catch (e) {
      console.warn('从服务器加载业务逻辑失败，尝试从本地加载:', e)
      const localLogic = await get<BusinessLogic>('businessLogics', logicId)
      if (localLogic) {
        return { logic: localLogic, fromCache: true }
      }
      throw e
    }
  } else {
    const localLogic = await get<BusinessLogic>('businessLogics', logicId)
    if (localLogic) {
      return { logic: localLogic, fromCache: true }
    }
    throw new Error('离线状态下本地不存在该业务逻辑')
  }
}

export async function cachePageList(appId: number): Promise<void> {
  try {
    const res = await pageApi.list(appId)
    if (res.data) {
      const { bulkPut } = await import('./indexedDB')
      await bulkPut('pages', res.data)
    }
  } catch (e) {
    console.error('缓存页面列表失败:', e)
  }
}

export async function cacheDataModelList(appId: number): Promise<void> {
  try {
    const res = await dataModelApi.list(appId)
    if (res.data) {
      const { bulkPut } = await import('./indexedDB')
      await bulkPut('dataModels', res.data)
    }
  } catch (e) {
    console.error('缓存数据模型列表失败:', e)
  }
}

export async function cacheBusinessLogicList(appId: number): Promise<void> {
  try {
    const res = await logicApi.list(appId)
    if (res.data) {
      const { bulkPut } = await import('./indexedDB')
      await bulkPut('businessLogics', res.data)
    }
  } catch (e) {
    console.error('缓存业务逻辑列表失败:', e)
  }
}
