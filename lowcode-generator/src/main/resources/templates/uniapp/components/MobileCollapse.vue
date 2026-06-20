<template>
  <view class="mobile-collapse">
    <view
      v-for="(item, index) in processedItems"
      :key="index"
      class="mobile-collapse__item"
      :class="{ 'is-active': item.isOpen }"
    >
      <view
        class="mobile-collapse__header"
        @touchstart="onHeaderTouchStart($event, index)"
        @touchmove="onHeaderTouchMove"
        @touchend="onHeaderTouchEnd(index)"
      >
        <view class="mobile-collapse__title">{{ item.title }}</view>
        <view class="mobile-collapse__icon" :class="{ 'is-rotated': item.isOpen }">
          <text class="iconfont">▾</text>
        </view>
      </view>
      <view
        class="mobile-collapse__content"
        :style="getContentStyle(index)"
      >
        <view class="mobile-collapse__content-inner">
          <slot :name="`item-${index}`" :index="index" :item="item">
            <text>{{ item.content }}</text>
          </slot>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

export interface CollapseItem {
  title: string
  content?: string
  disabled?: boolean
  [key: string]: unknown
}

interface CollapseProps {
  items: CollapseItem[]
  modelValue?: string | string[]
  accordion?: boolean
  border?: boolean
}

interface CollapseEmits {
  (e: 'update:modelValue', value: string | string[]): void
  (e: 'change', openNames: string | string[]): void
  (e: 'swipe', direction: 'left' | 'right' | 'up' | 'down'): void
}

interface ProcessedItem extends CollapseItem {
  isOpen: boolean
  name: string
}

const props = withDefaults(defineProps<CollapseProps>(), {
  items: () => [],
  modelValue: '',
  accordion: false,
  border: true
})

const emit = defineEmits<CollapseEmits>()

const contentHeights = ref<Record<number, number>>({})
const touchState = reactive({
  startX: 0,
  startY: 0,
  moveX: 0,
  moveY: 0,
  startTime: 0,
  isMoved: false
})

const processedItems = computed<ProcessedItem[]>(() => {
  return props.items.map((item, index) => ({
    ...item,
    name: item.name ?? String(index),
    isOpen: isItemOpen(String(index))
  }))
})

const isItemOpen = (name: string): boolean => {
  if (props.accordion) {
    return props.modelValue === name
  }
  return Array.isArray(props.modelValue) && props.modelValue.includes(name)
}

const toggleItem = (index: number) => {
  const item = processedItems.value[index]
  if (item.disabled) return

  const name = item.name

  if (props.accordion) {
    const newValue = item.isOpen ? '' : name
    emit('update:modelValue', newValue)
    emit('change', newValue)
  } else {
    const currentValue = Array.isArray(props.modelValue) ? [...props.modelValue] : []
    const idx = currentValue.indexOf(name)
    if (idx > -1) {
      currentValue.splice(idx, 1)
    } else {
      currentValue.push(name)
    }
    emit('update:modelValue', currentValue)
    emit('change', currentValue)
  }
}

const getContentStyle = (index: number) => {
  const isOpen = processedItems.value[index]?.isOpen
  const height = contentHeights.value[index] || 0
  return {
    height: isOpen ? `${height}px` : '0px',
    opacity: isOpen ? '1' : '0'
  }
}

const onHeaderTouchStart = (e: TouchEvent, index: number) => {
  const touch = e.touches[0]
  touchState.startX = touch.clientX
  touchState.startY = touch.clientY
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY
  touchState.startTime = Date.now()
  touchState.isMoved = false
}

const onHeaderTouchMove = (e: TouchEvent) => {
  const touch = e.touches[0]
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY

  const deltaX = Math.abs(touchState.moveX - touchState.startX)
  const deltaY = Math.abs(touchState.moveY - touchState.startY)

  if (deltaX > 10 || deltaY > 10) {
    touchState.isMoved = true
  }
}

const onHeaderTouchEnd = (index: number) => {
  const deltaX = touchState.moveX - touchState.startX
  const deltaY = touchState.moveY - touchState.startY
  const deltaTime = Date.now() - touchState.startTime

  if (!touchState.isMoved && deltaTime < 300) {
    toggleItem(index)
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
.mobile-collapse {
  background-color: #fff;

  &__item {
    &:not(:last-child) {
      border-bottom: 1rpx solid #ebedf0;
    }
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 32rpx;
    background-color: #fff;
    transition: background-color 0.2s ease;

    &:active {
      background-color: #f7f8fa;
    }
  }

  &__title {
    font-size: 32rpx;
    color: #323233;
    font-weight: 500;
  }

  &__icon {
    font-size: 24rpx;
    color: #969799;
    transition: transform 0.3s ease;

    &.is-rotated {
      transform: rotate(180deg);
    }
  }

  &__content {
    overflow: hidden;
    transition: height 0.3s ease, opacity 0.3s ease;
    background-color: #fff;
  }

  &__content-inner {
    padding: 0 32rpx 32rpx;
    font-size: 28rpx;
    color: #646566;
    line-height: 1.6;
  }

  &--border {
    border-top: 1rpx solid #ebedf0;
    border-bottom: 1rpx solid #ebedf0;
  }
}
</style>
