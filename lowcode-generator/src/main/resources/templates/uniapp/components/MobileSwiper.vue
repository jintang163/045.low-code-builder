<template>
  <view
    class="mobile-swiper"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <swiper
      class="mobile-swiper__swiper"
      :current="currentIndex"
      :autoplay="autoplay"
      :interval="interval"
      :duration="duration"
      :circular="circular"
      :vertical="vertical"
      :indicator-dots="false"
      @change="onSwiperChange"
      @animationfinish="onAnimationFinish"
    >
      <swiper-item v-for="(item, index) in items" :key="index">
        <view class="mobile-swiper__item">
          <slot name="item" :item="item" :index="index">
            <image
              v-if="item.image"
              :src="item.image"
              mode="aspectFill"
              class="mobile-swiper__image"
            />
            <view v-else class="mobile-swiper__placeholder">
              <text>{{ item.title || `Slide ${index + 1}` }}</text>
            </view>
          </slot>
        </view>
      </swiper-item>
    </swiper>

    <view v-if="showIndicators" class="mobile-swiper__indicators" :class="{ 'is-vertical': vertical }">
      <view
        v-for="(_, index) in items"
        :key="index"
        class="mobile-swiper__indicator"
        :class="{ 'is-active': currentIndex === index }"
      ></view>
    </view>

    <view v-if="showTitle && currentItem?.title" class="mobile-swiper__title">
      <text>{{ currentItem.title }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

export interface SwiperItem {
  image?: string
  title?: string
  link?: string
  [key: string]: unknown
}

interface SwiperProps {
  items: SwiperItem[]
  modelValue?: number
  autoplay?: boolean
  interval?: number
  duration?: number
  circular?: boolean
  vertical?: boolean
  showIndicators?: boolean
  showTitle?: boolean
  height?: number
}

interface SwiperEmits {
  (e: 'update:modelValue', value: number): void
  (e: 'change', index: number, item: SwiperItem): void
  (e: 'click', index: number, item: SwiperItem): void
  (e: 'longpress', index: number, item: SwiperItem): void
  (e: 'swipe', direction: 'left' | 'right' | 'up' | 'down'): void
}

const props = withDefaults(defineProps<SwiperProps>(), {
  items: () => [],
  modelValue: 0,
  autoplay: false,
  interval: 3000,
  duration: 500,
  circular: false,
  vertical: false,
  showIndicators: true,
  showTitle: false,
  height: 300
})

const emit = defineEmits<SwiperEmits>()

const currentIndex = ref(props.modelValue)

const touchState = reactive({
  startX: 0,
  startY: 0,
  moveX: 0,
  moveY: 0,
  startTime: 0,
  longPressTimer: null as ReturnType<typeof setTimeout> | null,
  isLongPressed: false
})

const currentItem = computed(() => props.items[currentIndex.value])

const onSwiperChange = (e: { detail: { current: number } }) => {
  currentIndex.value = e.detail.current
  emit('update:modelValue', e.detail.current)
  emit('change', e.detail.current, props.items[e.detail.current])
}

const onAnimationFinish = (e: { detail: { current: number } }) => {
  currentIndex.value = e.detail.current
}

const onTouchStart = (e: TouchEvent) => {
  const touch = e.touches[0]
  touchState.startX = touch.clientX
  touchState.startY = touch.clientY
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY
  touchState.startTime = Date.now()
  touchState.isLongPressed = false

  touchState.longPressTimer = setTimeout(() => {
    touchState.isLongPressed = true
    emit('longpress', currentIndex.value, currentItem.value)
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

  if (touchState.isLongPressed) return

  const deltaX = touchState.moveX - touchState.startX
  const deltaY = touchState.moveY - touchState.startY
  const deltaTime = Date.now() - touchState.startTime

  if (deltaTime < 300 && Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
    emit('click', currentIndex.value, currentItem.value)
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
.mobile-swiper {
  position: relative;
  width: 100%;
  overflow: hidden;

  &__swiper {
    width: 100%;
  }

  &__item {
    width: 100%;
    height: 100%;
  }

  &__image {
    width: 100%;
    height: 100%;
  }

  &__placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f7f8fa;
  }

  &__indicators {
    position: absolute;
    bottom: 20rpx;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 12rpx;

    &.is-vertical {
      left: auto;
      right: 20rpx;
      top: 50%;
      bottom: auto;
      transform: translateY(-50%);
      flex-direction: column;
    }
  }

  &__indicator {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background-color: rgba(255, 255, 255, 0.5);
    transition: all 0.3s ease;

    &.is-active {
      width: 32rpx;
      border-radius: 6rpx;
      background-color: #fff;
    }
  }

  &__title {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 24rpx 32rpx;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
    color: #fff;
    font-size: 28rpx;
  }
}
</style>
