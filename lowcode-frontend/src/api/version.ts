const versionApi = {
  list: async (pageId: number) => {
    return { data: [], code: 0 }
  },
  create: async (data: any) => {
    return { data: null, code: 0 }
  },
  restore: async (versionId: number) => {
    return { data: null, code: 0 }
  },
  delete: async (versionId: number) => {
    return { data: null, code: 0 }
  },
  release: async (data: any) => {
    return { data: null, code: 0 }
  },
  releaseList: async (pageId: number) => {
    return { data: [], code: 0 }
  },
}

export { versionApi }
