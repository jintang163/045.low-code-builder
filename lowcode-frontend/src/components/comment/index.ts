import { lazy } from 'react'

export { default as CommentPanel } from './CommentPanel'
export type { CommentPanelProps } from './CommentPanel'

export { default as DesignHistoryPanel } from './DesignHistoryPanel'
export type { DesignHistoryPanelProps } from './DesignHistoryPanel'

export const LazyCommentPanel = lazy(() => import('./CommentPanel'))
export const LazyDesignHistoryPanel = lazy(() => import('./DesignHistoryPanel'))
