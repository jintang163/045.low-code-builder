export const generateColor = (index: number) => {
  const colors = ['#1677ff', '#52c41a', '#faad14', '#eb2f96', '#722ed1', '#13c2c2']
  return colors[index % colors.length]
}

export type CRDTOperation = {
  type: string
  componentId: string
  data: any
}

export type ConflictInfo = {
  id: string
  type: string
  localData: any
  remoteData: any
}
