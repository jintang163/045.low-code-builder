<template>
  <view 
    class="mobile-grid"
    :style="gridStyle"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <slot></slot>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'

interface GridProps {
  columns?: number
  gap?: number
  border?: boolean
  square?: boolean
}

interface GridEmits {
  (e: 'swipe', direction: 'left' | 'right' | 'up' | 'down'): void
  (e: 'longpress'): void
  (e: 'click'): void
}

const props = withDefaults(defineProps<GridProps>(), {
  columns: 4,
  gap: 0,
  border: false,
  square: false
})

const emit = defineEmits<GridEmits>()

const touchState = reactive({
  startX: 0,
  startY: 0,
  moveX: 0,
  moveY: 0,
  startTime: 0,
  longPressTimer: null as ReturnType<typeof setTimeout> | null
})

const gridStyle = computed(() => {
  const style: Record<string, string> = {
    display: 'grid',
    gridTemplateColumns: `repeat(${props.columns}, 1fr)`,
    gap: `${props.gap}px`
  }
  if (props.square) {
    style.aspectRatio = '1'
  }
  return style
})

const onTouchStart = (e: TouchEvent) => {
  const touch = e.touches[0]
  touchState.startX = touch.clientX
  touchState.startY = touch.clientY
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY
  touchState.startTime = Date.now()

  touchState.longPressTimer = setTimeout(() => {
    emit('longpress')
  }, 500)
}

const onTouchMove = (e: TouchEvent) => {
  const touch = e.touches[0]
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY

  const deltaX = Math.abs(touchState.moveX - touchState.startX)
  const deltaY = Math.abs(touchState.moveY - touchState.startY)

  if (deltaX > 10 || deltaY > 10) {
    if (touchState.longPressTimer) {
      clearTimeout(touchState.longPressTimer)
      touchState.longPressTimer = null
    }
  }
}

const onTouchEnd = () => {
  if (touchState.longPressTimer) {
    clearTimeout(touchState.longPressTimer)
    touchState.longPressTimer = null
  }

  const deltaX = touchState.moveX - touchState.startX
  const deltaY = touchState.moveY - touchState.startY
  const deltaTime = Date.now() - touchState.startTime

  if (deltaTime < 300 && Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
    emit('click')
    return
  }

  if (Math.abs(deltaX) > Math.abs(deltaY)) {
    if (Math.abs(deltaX) > 30) {
      emit('swipe', deltaX > 0 ? 'right' : 'left')
    }
  } else {
    if (Math.abs(deltaY) > 30) {
      emit('swipe', deltaY > 0 ? 'down' : 'up')
    }
  }
}
</script>

<style lang="scss" scoped>
.mobile-grid {
  width: 100%;
  box-sizing: border-box;

  &--border {
    border-top: 1rpx solid #ebedf0;
    border-left: 1rpx solid #ebedf0;
  }
}
</style>
