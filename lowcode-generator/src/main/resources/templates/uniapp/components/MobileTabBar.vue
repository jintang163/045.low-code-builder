<template>
  <view class="mobile-tabbar" :class="{ 'is-fixed': fixed, 'is-border': border }">
    <view class="mobile-tabbar__placeholder" v-if="fixed"></view>
    <view class="mobile-tabbar__wrapper">
      <view
        v-for="(item, index) in items"
        :key="index"
        class="mobile-tabbar__item"
        :class="{ 'is-active': modelValue === item.key }"
        @touchstart="onItemTouchStart($event, index)"
        @touchmove="onItemTouchMove"
        @touchend="onItemTouchEnd(index)"
      >
        <view class="mobile-tabbar__icon">
          <slot name="icon" :item="item" :index="index">
            <image
              v-if="item.icon"
              :src="modelValue === item.key ? item.activeIcon || item.icon : item.icon"
              mode="aspectFit"
              class="mobile-tabbar__icon-img"
            />
          </slot>
          <view v-if="item.badge" class="mobile-tabbar__badge">
            <text class="mobile-tabbar__badge-text">{{ item.badge }}</text>
          </view>
        </view>
        <view class="mobile-tabbar__title" :class="{ 'is-active': modelValue === item.key }">
          <slot name="title" :item="item" :index="index">
            {{ item.title }}
          </slot>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

export interface TabBarItem {
  key: string
  title: string
  icon?: string
  activeIcon?: string
  badge?: string | number
  disabled?: boolean
  [key: string]: unknown
}

interface TabBarProps {
  items: TabBarItem[]
  modelValue: string
  fixed?: boolean
  border?: boolean
  activeColor?: string
  inactiveColor?: string
  swipeable?: boolean
}

interface TabBarEmits {
  (e: 'update:modelValue', value: string): void
  (e: 'change', key: string, index: number): void
  (e: 'click', key: string, index: number): void
  (e: 'longpress', key: string, index: number): void
  (e: 'swipe', direction: 'left' | 'right'): void
}

const props = withDefaults(defineProps<TabBarProps>(), {
  items: () => [],
  modelValue: '',
  fixed: true,
  border: true,
  activeColor: '#1989fa',
  inactiveColor: '#646566',
  swipeable: false
})

const emit = defineEmits<TabBarEmits>()

const touchState = reactive({
  startX: 0,
  startY: 0,
  moveX: 0,
  moveY: 0,
  startTime: 0,
  isMoved: false,
  longPressTimer: null as ReturnType<typeof setTimeout> | null,
  currentIndex: -1
})

const onItemTouchStart = (e: TouchEvent, index: number) => {
  const touch = e.touches[0]
  touchState.startX = touch.clientX
  touchState.startY = touch.clientY
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY
  touchState.startTime = Date.now()
  touchState.isMoved = false
  touchState.currentIndex = index

  const item = props.items[index]
  if (item && !item.disabled) {
    touchState.longPressTimer = setTimeout(() => {
      emit('longpress', item.key, index)
    }, 500)
  }
}

const onItemTouchMove = (e: TouchEvent) => {
  const touch = e.touches[0]
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY

  const deltaX = Math.abs(touchState.moveX - touchState.startX)
  const deltaY = Math.abs(touchState.moveY - touchState.startY)

  if (deltaX > 10 || deltaY > 10) {
    touchState.isMoved = true
    if (touchState.longPressTimer) {
      clearTimeout(touchState.longPressTimer)
      touchState.longPressTimer = null
    }
  }
}

const onItemTouchEnd = (index: number) => {
  if (touchState.longPressTimer) {
    clearTimeout(touchState.longPressTimer)
    touchState.longPressTimer = null
  }

  const item = props.items[index]
  if (!item || item.disabled) return

  const deltaX = touchState.moveX - touchState.startX
  const deltaY = touchState.moveY - touchState.startY
  const deltaTime = Date.now() - touchState.startTime

  if (!touchState.isMoved && props.swipeable) {
    if (Math.abs(deltaX) > 50 && Math.abs(deltaX) > Math.abs(deltaY)) {
      const direction = deltaX > 0 ? 'right' : 'left'
      emit('swipe', direction)
      handleSwipe(direction)
      return
    }
  }

  if (deltaTime < 300 && Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
    emit('update:modelValue', item.key)
    emit('change', item.key, index)
    emit('click', item.key, index)
  }
}

const handleSwipe = (direction: 'left' | 'right') => {
  const currentIndex = props.items.findIndex(item => item.key === props.modelValue)
  let nextIndex = direction === 'left' ? currentIndex + 1 : currentIndex - 1
  nextIndex = Math.max(0, Math.min(props.items.length - 1, nextIndex))
  
  while (nextIndex >= 0 && nextIndex < props.items.length && props.items[nextIndex]?.disabled) {
    nextIndex = direction === 'left' ? nextIndex + 1 : nextIndex - 1
  }

  if (nextIndex >= 0 && nextIndex < props.items.length && nextIndex !== currentIndex) {
    const nextItem = props.items[nextIndex]
    emit('update:modelValue', nextItem.key)
    emit('change', nextItem.key, nextIndex)
  }
}
</script>

<style lang="scss" scoped>
.mobile-tabbar {
  width: 100%;
  background-color: #fff;

  &__placeholder {
    height: 100rpx;
  }

  &__wrapper {
    display: flex;
    align-items: center;
    justify-content: space-around;
    height: 100rpx;
    padding-bottom: env(safe-area-inset-bottom);
  }

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 1;
    height: 100%;
    transition: transform 0.2s ease;

    &:active {
      transform: scale(0.95);
    }

    &.is-active {
      .mobile-tabbar__title {
        color: var(--tabbar-active-color, #1989fa);
      }
    }
  }

  &__icon {
    position: relative;
    width: 48rpx;
    height: 48rpx;
    margin-bottom: 4rpx;
  }

  &__icon-img {
    width: 100%;
    height: 100%;
  }

  &__badge {
    position: absolute;
    top: -8rpx;
    right: -16rpx;
    min-width: 32rpx;
    height: 32rpx;
    padding: 0 8rpx;
    background-color: #ee0a24;
    border-radius: 32rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__badge-text {
    font-size: 20rpx;
    color: #fff;
    line-height: 1;
  }

  &__title {
    font-size: 24rpx;
    color: var(--tabbar-inactive-color, #646566);
    transition: color 0.2s ease;

    &.is-active {
      font-weight: 500;
    }
  }

  &.is-fixed {
    position: fixed;
    bottom: 0;
    left: 0;
    z-index: 100;
  }

  &.is-border {
    border-top: 1rpx solid #ebedf0;
  }
}
</style>
