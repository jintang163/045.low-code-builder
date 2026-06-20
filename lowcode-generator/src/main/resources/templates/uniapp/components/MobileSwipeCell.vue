<template>
  <view
    class="mobile-swipe-cell"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <view class="mobile-swipe-cell__wrapper" :style="wrapperStyle">
      <view class="mobile-swipe-cell__left" @tap.stop="onCellClick">
        <slot name="left" :open="isOpen" :close="close">
          <template v-if="leftActions.length">
            <view
              v-for="(action, index) in leftActions"
              :key="index"
              class="mobile-swipe-cell__action"
              :style="{ backgroundColor: action.color }"
              @tap.stop="onActionClick(action, 'left', index)"
            >
              <text>{{ action.text }}</text>
            </view>
          </template>
        </slot>
      </view>

      <view class="mobile-swipe-cell__content" :class="{ 'is-active': isActive }">
        <slot></slot>
      </view>

      <view class="mobile-swipe-cell__right" @tap.stop="onCellClick">
        <slot name="right" :open="isOpen" :close="close">
          <template v-if="rightActions.length">
            <view
              v-for="(action, index) in rightActions"
              :key="index"
              class="mobile-swipe-cell__action"
              :style="{ backgroundColor: action.color }"
              @tap.stop="onActionClick(action, 'right', index)"
            >
              <text>{{ action.text }}</text>
            </view>
          </template>
        </slot>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

export interface SwipeCellAction {
  text: string
  color?: string
  key?: string
  disabled?: boolean
  [key: string]: unknown
}

interface SwipeCellProps {
  leftActions?: SwipeCellAction[]
  rightActions?: SwipeCellAction[]
  disabled?: boolean
  name?: string
  threshold?: number
  closeOnClick?: boolean
  closeOnTouchOutside?: boolean
}

interface SwipeCellEmits {
  (e: 'open', position: 'left' | 'right'): void
  (e: 'close'): void
  (e: 'click'): void
  (e: 'action', action: SwipeCellAction, position: 'left' | 'right', index: number): void
  (e: 'longpress'): void
  (e: 'swipe', direction: 'left' | 'right'): void
}

const props = withDefaults(defineProps<SwipeCellProps>(), {
  leftActions: () => [],
  rightActions: () => [],
  disabled: false,
  name: '',
  threshold: 20,
  closeOnClick: true,
  closeOnTouchOutside: true
})

const emit = defineEmits<SwipeCellEmits>()

const offsetX = ref(0)
const isOpen = ref(false)
const openPosition = ref<'left' | 'right' | null>(null)
const isActive = ref(false)

const touchState = reactive({
  startX: 0,
  startY: 0,
  moveX: 0,
  moveY: 0,
  startTime: 0,
  isDragging: false,
  direction: '' as 'horizontal' | 'vertical' | '',
  longPressTimer: null as ReturnType<typeof setTimeout> | null
})

const leftWidth = computed(() => {
  const slotWidth = 80
  return props.leftActions.length * slotWidth
})

const rightWidth = computed(() => {
  const slotWidth = 80
  return props.rightActions.length * slotWidth
})

const wrapperStyle = computed(() => ({
  transform: `translateX(${offsetX.value}px)`,
  transition: touchState.isDragging ? 'none' : 'transform 0.3s ease'
}))

const onTouchStart = (e: TouchEvent) => {
  if (props.disabled) return

  const touch = e.touches[0]
  touchState.startX = touch.clientX
  touchState.startY = touch.clientY
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY
  touchState.startTime = Date.now()
  touchState.isDragging = false
  touchState.direction = ''

  isActive.value = true

  touchState.longPressTimer = setTimeout(() => {
    emit('longpress')
  }, 500)
}

const onTouchMove = (e: TouchEvent) => {
  if (props.disabled) return

  const touch = e.touches[0]
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY

  const deltaX = touchState.moveX - touchState.startX
  const deltaY = touchState.moveY - touchState.startY

  const absX = Math.abs(deltaX)
  const absY = Math.abs(deltaY)

  if (!touchState.direction) {
    if (absX > absY && absX > 10) {
      touchState.direction = 'horizontal'
    } else if (absY > absX && absY > 10) {
      touchState.direction = 'vertical'
    }
  }

  if (touchState.direction === 'horizontal') {
    isActive.value = false
    if (touchState.longPressTimer) {
      clearTimeout(touchState.longPressTimer)
      touchState.longPressTimer = null
    }
  }

  if (touchState.direction === 'horizontal') {
    e.preventDefault?.()
    touchState.isDragging = true

    let newOffset = deltaX

    if (isOpen.value) {
      if (openPosition.value === 'right') {
        newOffset -= rightWidth.value + deltaX
      } else if (openPosition.value === 'left') {
        newOffset += leftWidth.value + deltaX
      }
    }

    if (newOffset > 0 && props.leftActions.length === 0) {
      newOffset = newOffset * 0.3
    } else if (newOffset < 0 && props.rightActions.length === 0) {
      newOffset = newOffset * 0.3
    }

    if (newOffset > leftWidth.value) {
      newOffset = leftWidth.value + (newOffset - leftWidth.value) * 0.3
    } else if (newOffset < -rightWidth.value) {
      newOffset = -rightWidth.value + (newOffset + rightWidth.value) * 0.3
    }

    offsetX.value = newOffset
  }
}

const onTouchEnd = () => {
  isActive.value = false

  if (touchState.longPressTimer) {
    clearTimeout(touchState.longPressTimer)
    touchState.longPressTimer = null
  }

  if (props.disabled) {
    resetPosition()
    return
  }

  const deltaX = touchState.moveX - touchState.startX
  const deltaY = touchState.moveY - touchState.startY
  const deltaTime = Date.now() - touchState.startTime

  if (!touchState.isDragging && deltaTime < 300 && Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
    if (isOpen.value && props.closeOnClick) {
      close()
    }
    emit('click')
    return
  }

  if (!touchState.isDragging) {
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

  const threshold = 0.3

  if (offsetX.value > leftWidth.value * threshold) {
    open('left')
  } else if (offsetX.value < -rightWidth.value * threshold) {
    open('right')
  } else {
    resetPosition()
  }
}

const open = (position: 'left' | 'right') => {
  if (position === 'left' && props.leftActions.length === 0) return
  if (position === 'right' && props.rightActions.length === 0) return

  isOpen.value = true
  openPosition.value = position
  offsetX.value = position === 'left' ? leftWidth.value : -rightWidth.value
  emit('open', position)
}

const close = () => {
  resetPosition()
  emit('close')
}

const resetPosition = () => {
  offsetX.value = 0
  isOpen.value = false
  openPosition.value = null
}

const onCellClick = () => {
  if (isOpen.value && props.closeOnClick) {
    close()
  }
}

const onActionClick = (action: SwipeCellAction, position: 'left' | 'right', index: number) => {
  if (action.disabled) return
  emit('action', action, position, index)
  if (props.closeOnClick) {
    close()
  }
}

defineExpose({
  open,
  close
})
</script>

<style lang="scss" scoped>
.mobile-swipe-cell {
  position: relative;
  width: 100%;
  overflow: hidden;

  &__wrapper {
    position: relative;
    display: flex;
    width: 100%;
  }

  &__left,
  &__right {
    position: absolute;
    top: 0;
    height: 100%;
    display: flex;
  }

  &__left {
    left: 0;
    transform: translateX(-100%);
  }

  &__right {
    right: 0;
    transform: translateX(100%);
  }

  &__content {
    width: 100%;
    background-color: #fff;
    transition: background-color 0.2s ease;

    &.is-active {
      background-color: #f7f8fa;
    }
  }

  &__action {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 160rpx;
    height: 100%;
    padding: 0 32rpx;
    color: #fff;
    font-size: 28rpx;
  }
}
</style>
