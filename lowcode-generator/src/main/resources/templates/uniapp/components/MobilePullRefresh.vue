<template>
  <view
    class="mobile-pull-refresh"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <view class="mobile-pull-refresh__header" :style="headerStyle">
      <view v-if="pullDistance > 0" class="mobile-pull-refresh__indicator">
        <view v-if="!isLoading" class="mobile-pull-refresh__spinner" :class="{ 'is-rotated': pullDistance >= threshold }">
          <text>↓</text>
        </view>
        <view v-else class="mobile-pull-refresh__loading">
          <text class="mobile-pull-refresh__loading-icon">⟳</text>
        </view>
        <text class="mobile-pull-refresh__text">{{ statusText }}</text>
      </view>
    </view>

    <scroll-view
      class="mobile-pull-refresh__scroll"
      :scroll-y="true"
      :scroll-top="scrollTop"
      :refresher-enabled="false"
      @scroll="onScroll"
      @scrolltoupper="onScrollToUpper"
      @scrolltolower="onScrollToLower"
    >
      <view class="mobile-pull-refresh__content" :style="contentStyle">
        <slot></slot>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

interface PullRefreshProps {
  threshold?: number
  maxDistance?: number
  disabled?: boolean
  successText?: string
  pullingText?: string
  loosingText?: string
  loadingText?: string
  duration?: number
}

interface PullRefreshEmits {
  (e: 'refresh'): void
  (e: 'loadmore'): void
  (e: 'scroll', e: Event): void
  (e: 'pulling', distance: number): void
  (e: 'release'): void
  (e: 'longpress'): void
  (e: 'swipe', direction: 'left' | 'right' | 'up' | 'down'): void
}

const props = withDefaults(defineProps<PullRefreshProps>(), {
  threshold: 80,
  maxDistance: 120,
  disabled: false,
  successText: '刷新成功',
  pullingText: '下拉即可刷新',
  loosingText: '释放即可刷新',
  loadingText: '加载中...',
  duration: 300
})

const emit = defineEmits<PullRefreshEmits>()

const scrollTop = ref(0)
const pullDistance = ref(0)
const isLoading = ref(false)
const isPulling = ref(false)
const showSuccess = ref(false)

const touchState = reactive({
  startY: 0,
  startX: 0,
  moveY: 0,
  moveX: 0,
  startTime: 0,
  longPressTimer: null as ReturnType<typeof setTimeout> | null,
  isDragging: false
})

const statusText = computed(() => {
  if (isLoading.value) return props.loadingText
  if (showSuccess.value) return props.successText
  if (pullDistance.value >= props.threshold) return props.loosingText
  return props.pullingText
})

const headerStyle = computed(() => ({
  height: `${Math.max(0, pullDistance.value)}px`,
  transition: isPulling.value ? 'none' : `height ${props.duration}ms ease`
}))

const contentStyle = computed(() => ({
  transform: `translateY(${pullDistance.value}px)`,
  transition: isPulling.value ? 'none' : `transform ${props.duration}ms ease`
}))

const onTouchStart = (e: TouchEvent) => {
  if (props.disabled || isLoading.value) return

  const touch = e.touches[0]
  touchState.startX = touch.clientX
  touchState.startY = touch.clientY
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY
  touchState.startTime = Date.now()
  touchState.isDragging = false

  touchState.longPressTimer = setTimeout(() => {
    emit('longpress')
  }, 500)
}

const onTouchMove = (e: TouchEvent) => {
  if (props.disabled || isLoading.value) return

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

  if (deltaY > deltaX && touchState.moveY > touchState.startY) {
    e.preventDefault && e.preventDefault()
    touchState.isDragging = true
    isPulling.value = true

    let distance = touchState.moveY - touchState.startY
    if (distance > props.maxDistance) {
      distance = props.maxDistance + (distance - props.maxDistance) * 0.3
    }

    pullDistance.value = distance
    emit('pulling', distance)
  }
}

const onTouchEnd = () => {
  if (touchState.longPressTimer) {
    clearTimeout(touchState.longPressTimer)
    touchState.longPressTimer = null
  }

  if (props.disabled || isLoading.value) {
    pullDistance.value = 0
    isPulling.value = false
    return
  }

  const deltaX = touchState.moveX - touchState.startX
  const deltaY = touchState.moveY - touchState.startY
  const deltaTime = Date.now() - touchState.startTime

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

  isPulling.value = false

  if (pullDistance.value >= props.threshold) {
    emit('release')
    startRefresh()
  } else {
    pullDistance.value = 0
  }
}

const startRefresh = () => {
  isLoading.value = true
  pullDistance.value = props.threshold
  emit('refresh')
}

const completeRefresh = (showSuccessText = false) => {
  if (showSuccessText) {
    showSuccess.value = true
    setTimeout(() => {
      showSuccess.value = false
      isLoading.value = false
      pullDistance.value = 0
    }, 500)
  } else {
    isLoading.value = false
    pullDistance.value = 0
  }
}

const onScroll = (e: Event) => {
  emit('scroll', e)
}

const onScrollToUpper = () => {
  scrollTop.value = 0
}

const onScrollToLower = () => {
  emit('loadmore')
}

defineExpose({
  startRefresh,
  completeRefresh
})
</script>

<style lang="scss" scoped>
.mobile-pull-refresh {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;

  &__header {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    z-index: 1;
  }

  &__indicator {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8rpx;
    padding: 20rpx;
  }

  &__spinner {
    font-size: 40rpx;
    color: #969799;
    transition: transform 0.3s ease;

    &.is-rotated {
      transform: rotate(180deg);
    }
  }

  &__loading {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__loading-icon {
    font-size: 40rpx;
    color: #1989fa;
    animation: rotate 1s linear infinite;
  }

  &__text {
    font-size: 24rpx;
    color: #969799;
  }

  &__scroll {
    width: 100%;
    height: 100%;
  }

  &__content {
    width: 100%;
    min-height: 100%;
  }

  @keyframes rotate {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }
}
</style>
