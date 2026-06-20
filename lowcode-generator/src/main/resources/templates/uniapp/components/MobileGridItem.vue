<template>
  <view
    class="mobile-grid-item"
    :style="itemStyle"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <view class="mobile-grid-item__content" :class="{ 'is-active': isActive }">
      <slot></slot>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

interface GridItemProps {
  span?: number
  border?: boolean
  square?: boolean
}

interface GridItemEmits {
  (e: 'click'): void
  (e: 'longpress'): void
  (e: 'swipe', direction: 'left' | 'right' | 'up' | 'down'): void
}

const props = withDefaults(defineProps<GridItemProps>(), {
  span: 1,
  border: false,
  square: false
})

const emit = defineEmits<GridItemEmits>()

const isActive = ref(false)

const touchState = reactive({
  startX: 0,
  startY: 0,
  moveX: 0,
  moveY: 0,
  startTime: 0,
  longPressTimer: null as ReturnType<typeof setTimeout> | null
})

const itemStyle = computed(() => {
  const style: Record<string, string> = {}
  if (props.span > 1) {
    style.gridColumn = `span ${props.span}`
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
  isActive.value = true

  touchState.longPressTimer = setTimeout(() => {
    isActive.value = false
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
    isActive.value = false
    if (touchState.longPressTimer) {
      clearTimeout(touchState.longPressTimer)
      touchState.longPressTimer = null
    }
  }
}

const onTouchEnd = () => {
  isActive.value = false

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
.mobile-grid-item {
  position: relative;
  box-sizing: border-box;

  &__content {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.2s ease;

    &.is-active {
      background-color: rgba(0, 0, 0, 0.05);
    }
  }

  &--border {
    border-right: 1rpx solid #ebedf0;
    border-bottom: 1rpx solid #ebedf0;
  }
}
</style>
