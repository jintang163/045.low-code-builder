import request from '@/utils/request'

export interface ABTestInfo {
  id?: number
  appId: number
  testName: string
  testCode: string
  description?: string
  resourceType: 'page' | 'component' | 'logic'
  resourceId: number
  controlVariantId?: number
  status?: number
  trafficAllocationType: 'percentage' | 'custom'
  sampleSize?: number
  confidenceLevel?: number
  startTime?: string
  endTime?: string
  winnerVariantId?: number
  conclusion?: string
  variants?: ABTestVariant[]
  createdTime?: string
  updatedTime?: string
}

export interface ABTestVariant {
  id?: number
  testId?: number
  variantName: string
  variantType: 'control' | 'experimental'
  snapshotId?: number
  version?: string
  trafficWeight: number
  description?: string
  pageViews?: number
  uniqueVisitors?: number
  conversions?: number
  conversionRate?: number
  createdTime?: string
}

export interface ABTestMetric {
  id?: number
  testId?: number
  variantId?: number
  metricName: string
  metricType: 'conversion' | 'revenue' | 'engagement'
  metricKey: string
  totalValue?: number
  count?: number
  avgValue?: number
  uniqueCount?: number
  confidenceInterval?: number
  statisticalSignificance?: number
}

export interface ABTestEvent {
  id?: number
  testId: number
  variantId?: number
  eventType: 'page_view' | 'click' | 'conversion' | 'custom'
  eventKey: string
  userId?: string
  sessionId?: string
  pageUrl?: string
  componentId?: string
  eventValue?: number
  timestamp?: string
}

export interface ABTestStats {
  testId: number
  totalPageViews: number
  totalUniqueVisitors: number
  totalConversions: number
  overallConversionRate: number
  variants: ABTestVariantStats[]
  metrics: ABTestMetric[]
  winnerVariantId?: number
  confidenceLevel: number
  isStatisticallySignificant: boolean
}

export interface ABTestVariantStats {
  variantId: number
  variantName: string
  variantType: string
  pageViews: number
  uniqueVisitors: number
  conversions: number
  conversionRate: number
  confidenceInterval: number
  statisticalSignificance: number
  improvementVsControl?: number
}

export interface ABTestPageParams {
  appId: number
  current?: number
  size?: number
  keyword?: string
  status?: number
  resourceType?: string
}

export const abtestApi = {
  list: (appId: number) => request.get<ABTestInfo[]>(`/abtest/list/${appId}`),

  page: (params: ABTestPageParams) =>
    request.get(`/abtest/page`, { params }),

  get: (id: number) => request.get<ABTestInfo>(`/abtest/${id}`),

  save: (data: ABTestInfo) => request.post<ABTestInfo>('/abtest', data),

  update: (data: ABTestInfo) => request.put<ABTestInfo>('/abtest', data),

  delete: (id: number) => request.delete(`/abtest/${id}`),

  start: (id: number) => request.post(`/abtest/${id}/start`),

  pause: (id: number) => request.post(`/abtest/${id}/pause`),

  stop: (id: number, winnerVariantId?: number) =>
    request.post(`/abtest/${id}/stop`, { winnerVariantId }),

  stats: (id: number) => request.get<ABTestStats>(`/abtest/${id}/stats`),

  allocate: (testId: number) => request.get<ABTestVariant>(`/abtest/${testId}/allocate`),

  recordEvent: (data: ABTestEvent) => request.post('/abtest/event', data),

  getVariants: (testId: number) =>
    request.get<ABTestVariant[]>(`/abtest/${testId}/variants`),

  saveVariant: (testId: number, variant: ABTestVariant) =>
    request.post<ABTestVariant>(`/abtest/${testId}/variant`, variant),

  updateVariant: (testId: number, variant: ABTestVariant) =>
    request.put<ABTestVariant>(`/abtest/${testId}/variant`, variant),

  deleteVariant: (testId: number, variantId: number) =>
    request.delete(`/abtest/${testId}/variant/${variantId}`),

  promoteWinner: (id: number, variantId: number) =>
    request.post(`/abtest/${id}/promote/${variantId}`),
}

export const mockABTests: ABTestInfo[] = [
  {
    id: 1,
    appId: 1,
    testName: '首页按钮颜色测试',
    testCode: 'home_button_color_test',
    description: '测试不同颜色的CTA按钮对转化率的影响',
    resourceType: 'page',
    resourceId: 1,
    controlVariantId: 1,
    status: 1,
    trafficAllocationType: 'percentage',
    sampleSize: 10000,
    confidenceLevel: 95,
    startTime: '2024-01-15 10:00:00',
    endTime: '2024-01-30 10:00:00',
    createdTime: '2024-01-10 09:00:00',
    updatedTime: '2024-01-20 14:30:00',
    variants: [
      {
        id: 1,
        testId: 1,
        variantName: '原始版本（蓝色按钮）',
        variantType: 'control',
        trafficWeight: 50,
        pageViews: 5234,
        uniqueVisitors: 4521,
        conversions: 234,
        conversionRate: 5.18,
      },
      {
        id: 2,
        testId: 1,
        variantName: '实验版本（绿色按钮）',
        variantType: 'experimental',
        trafficWeight: 50,
        pageViews: 5198,
        uniqueVisitors: 4489,
        conversions: 312,
        conversionRate: 6.95,
      },
    ],
  },
  {
    id: 2,
    appId: 1,
    testName: '登录页布局测试',
    testCode: 'login_layout_test',
    description: '测试不同登录页面布局对用户注册转化率的影响',
    resourceType: 'page',
    resourceId: 2,
    controlVariantId: 3,
    status: 2,
    trafficAllocationType: 'percentage',
    sampleSize: 5000,
    confidenceLevel: 95,
    startTime: '2024-01-05 08:00:00',
    endTime: '2024-01-15 08:00:00',
    winnerVariantId: 4,
    conclusion: '简化版登录表单显著提升了注册转化率，提升幅度达23.5%',
    createdTime: '2024-01-01 10:00:00',
    updatedTime: '2024-01-16 09:00:00',
    variants: [
      {
        id: 3,
        testId: 2,
        variantName: '原始版本（完整表单）',
        variantType: 'control',
        trafficWeight: 50,
        pageViews: 2876,
        uniqueVisitors: 2654,
        conversions: 156,
        conversionRate: 5.88,
      },
      {
        id: 4,
        testId: 2,
        variantName: '实验版本（简化表单）',
        variantType: 'experimental',
        trafficWeight: 50,
        pageViews: 2901,
        uniqueVisitors: 2689,
        conversions: 193,
        conversionRate: 7.18,
      },
    ],
  },
  {
    id: 3,
    appId: 1,
    testName: '商品详情页标题测试',
    testCode: 'product_title_test',
    description: '测试不同标题风格对商品点击率的影响',
    resourceType: 'page',
    resourceId: 3,
    controlVariantId: 5,
    status: 0,
    trafficAllocationType: 'percentage',
    sampleSize: 8000,
    confidenceLevel: 90,
    createdTime: '2024-01-18 11:00:00',
    updatedTime: '2024-01-19 16:45:00',
    variants: [
      {
        id: 5,
        testId: 3,
        variantName: '原始版本（功能型标题）',
        variantType: 'control',
        trafficWeight: 50,
      },
      {
        id: 6,
        testId: 3,
        variantName: '实验版本（情感型标题）',
        variantType: 'experimental',
        trafficWeight: 50,
      },
    ],
  },
  {
    id: 4,
    appId: 1,
    testName: '支付流程优化测试',
    testCode: 'payment_flow_test',
    description: '优化支付流程，减少支付环节的用户流失',
    resourceType: 'logic',
    resourceId: 1,
    controlVariantId: 7,
    status: 3,
    trafficAllocationType: 'percentage',
    sampleSize: 3000,
    confidenceLevel: 95,
    startTime: '2024-01-08 10:00:00',
    endTime: '2024-01-12 10:00:00',
    createdTime: '2024-01-05 14:00:00',
    updatedTime: '2024-01-13 10:30:00',
    variants: [
      {
        id: 7,
        testId: 4,
        variantName: '原始流程',
        variantType: 'control',
        trafficWeight: 50,
        pageViews: 1523,
        uniqueVisitors: 1456,
        conversions: 892,
        conversionRate: 61.26,
      },
      {
        id: 8,
        testId: 4,
        variantName: '优化流程（一步支付）',
        variantType: 'experimental',
        trafficWeight: 50,
        pageViews: 1498,
        uniqueVisitors: 1432,
        conversions: 987,
        conversionRate: 68.92,
      },
    ],
  },
]

export const mockStats: ABTestStats = {
  testId: 1,
  totalPageViews: 10432,
  totalUniqueVisitors: 9010,
  totalConversions: 546,
  overallConversionRate: 6.06,
  winnerVariantId: 2,
  confidenceLevel: 95,
  isStatisticallySignificant: true,
  variants: [
    {
      variantId: 1,
      variantName: '原始版本（蓝色按钮）',
      variantType: 'control',
      pageViews: 5234,
      uniqueVisitors: 4521,
      conversions: 234,
      conversionRate: 5.18,
      confidenceInterval: 0.45,
      statisticalSignificance: 0,
    },
    {
      variantId: 2,
      variantName: '实验版本（绿色按钮）',
      variantType: 'experimental',
      pageViews: 5198,
      uniqueVisitors: 4489,
      conversions: 312,
      conversionRate: 6.95,
      confidenceInterval: 0.52,
      statisticalSignificance: 0.98,
      improvementVsControl: 34.17,
    },
  ],
  metrics: [
    {
      id: 1,
      testId: 1,
      variantId: 1,
      metricName: '按钮点击率',
      metricType: 'conversion',
      metricKey: 'button_click',
      totalValue: 234,
      count: 5234,
      avgValue: 0.0447,
      uniqueCount: 234,
      confidenceInterval: 0.0045,
      statisticalSignificance: 0,
    },
    {
      id: 2,
      testId: 1,
      variantId: 2,
      metricName: '按钮点击率',
      metricType: 'conversion',
      metricKey: 'button_click',
      totalValue: 312,
      count: 5198,
      avgValue: 0.0600,
      uniqueCount: 312,
      confidenceInterval: 0.0052,
      statisticalSignificance: 0.98,
    },
  ],
}
