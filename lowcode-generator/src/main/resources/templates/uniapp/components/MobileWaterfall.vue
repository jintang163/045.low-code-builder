<template>
  <view
    class="mobile-waterfall"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <scroll-view
      class="mobile-waterfall__scroll"
      :scroll-y="true"
      :scroll-top="scrollTop"
      @scroll="onScroll"
      @scrolltolower="onScrollToLower"
    >
      <view class="mobile-waterfall__columns">
        <view
          v-for="(column, colIndex) in columns"
          :key="colIndex"
          class="mobile-waterfall__column"
          :style="{ width: columnWidth + 'px' }"
        >
          <view
            v-for="(item, itemIndex) in column"
            :key="item.id || itemIndex"
            class="mobile-waterfall__item"
            :style="getItemStyle(item)"
            @tap="onItemTap(item, colIndex, itemIndex)"
            @longpress="onItemLongPress(item, colIndex, itemIndex)"
          >
            <slot name="item" :item="item" :index="itemIndex" :column="colIndex">
              <image
                v-if="item.image"
                :src="item.image"
                mode="widthFix"
                class="mobile-waterfall__image"
                @load="onImageLoad($event, item, colIndex, itemIndex)"
              />
              <view v-else class="mobile-waterfall__placeholder" :style="{ height: (item.height || 200) + 'px' }">
                <text>{{ item.title || `Item ${itemIndex + 1}` }}</text>
              </view>
              <view v-if="item.title" class="mobile-waterfall__title">
                <text>{{ item.title }}</text>
              </view>
            </slot>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'

export interface WaterfallItem {
  id?: string | number
  image?: string
  title?: string
  height?: number
  width?: number
  [key: string]: unknown
}

interface WaterfallProps {
  items: WaterfallItem[]
  columnCount?: number
  gap?: number
  itemHeight?: number
  loadMoreThreshold?: number
  lazyLoad?: boolean
}

interface WaterfallEmits {
  (e: 'click', item: WaterfallItem, column: number, index: number): void
  (e: 'longpress', item: WaterfallItem, column: number, index: number): void
  (e: 'loadmore'): void
  (e: 'scroll', e: Event): void
  (e: 'image-load', item: WaterfallItem, column: number, index: number): void
  (e: 'swipe', direction: 'left' | 'right' | 'up' | 'down'): void
}

const props = withDefaults(defineProps<WaterfallProps>(), {
  items: () => [],
  columnCount: 2,
  gap: 10,
  itemHeight: 0,
  loadMoreThreshold: 50,
  lazyLoad: false
})

const emit = defineEmits<WaterfallEmits>()

const scrollTop = ref(0)
const containerWidth = ref(0)
const columnHeights = ref<number[]>([])
const itemHeights = ref<Map<string, number>>(new Map())

const touchState = reactive({
  startX: 0,
  startY: 0,
  moveX: 0,
  moveY: 0,
  startTime: 0,
  longPressTimer: null as ReturnType<typeof setTimeout> | null
})

const columnWidth = computed(() => {
  const totalGap = props.gap * (props.columnCount + 1)
  return (containerWidth.value - totalGap) / props.columnCount
})

const columns = computed<WaterfallItem[][]>(() => {
  const result: WaterfallItem[][] = Array.from({ length: props.columnCount }, () => [])
  const heights: number[] = Array(props.columnCount).fill(0)

  props.items.forEach((item) => {
    const shortestCol = heights.indexOf(Math.min(...heights))
    result[shortestCol].push(item)

    const itemKey = item.id?.toString() || `${shortestCol}-${result[shortestCol].length - 1}`
    const height = itemHeights.value.get(itemKey) || item.height || props.itemHeight || 200
    heights[shortestCol] += height + props.gap
  })

  columnHeights.value = heights
  return result
})

const initContainerWidth = () => {
  uni.getSystemInfo({
    success: (res) => {
      containerWidth.value = res.windowWidth
    }
  })
}

const getItemStyle = (item: WaterfallItem) => {
  const style: Record<string, string> = {
    width: columnWidth.value + 'px',
    marginBottom: props.gap + 'px'
  }

  const itemKey = item.id?.toString() || ''
  const cachedHeight = itemHeights.value.get(itemKey)
  if (cachedHeight) {
    style.height = cachedHeight + 'px'
  } else if (item.height) {
    style.height = item.height + 'px'
  }

  return style
}

const onImageLoad = (e: { detail: { height: number; width: number } }, item: WaterfallItem, colIndex: number, itemIndex: number) => {
  const { width, height } = e.detail
  const aspectRatio = height / width
  const calculatedHeight = columnWidth.value * aspectRatio

  const itemKey = item.id?.toString() || `${colIndex}-${itemIndex}`
  itemHeights.value.set(itemKey, calculatedHeight)

  emit('image-load', item, colIndex, itemIndex)
}

const onItemTap = (item: WaterfallItem, column: number, index: number) => {
  emit('click', item, column, index)
}

const onItemLongPress = (item: WaterfallItem, column: number, index: number) => {
  emit('longpress', item, column, index)
}

const onScroll = (e: Event) => {
  emit('scroll', e)
}

const onScrollToLower = () => {
  emit('loadmore')
}

const onTouchStart = (e: TouchEvent) => {
  const touch = e.touches[0]
  touchState.startX = touch.clientX
  touchState.startY = touch.clientY
  touchState.moveX = touch.clientX
  touchState.moveY = touch.clientY
  touchState.startTime = Date.now()

  touchState.longPressTimer = setTimeout(() => {
    emit('longpress', {} as WaterfallItem, -1, -1)
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

const scrollToTop = () => {
  scrollTop.value = 0
}

const scrollToBottom = () => {
  scrollTop.value = 999999
}

defineExpose({
  scrollToTop,
  scrollToBottom
})

onMounted(() => {
  initContainerWidth()
  nextTick(() => {
    initContainerWidth()
  })
})

watch(() => props.items, () => {
  nextTick(() => {
    // force update
  })
}, { deep: true })
</script>

<style lang="scss" scoped>
.mobile-waterfall {
  width: 100%;
  height: 100%;

  &__scroll {
    width: 100%;
    height: 100%;
  }

  &__columns {
    display: flex;
    padding: 0 v-bind('gap + "px"');
  }

  &__column {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 0 v-bind('(gap / 2) + "px"');
  }

  &__item {
    overflow: hidden;
    background-color: #fff;
    border-radius: 12rpx;
    box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
    transition: transform 0.2s ease;

    &:active {
      transform: scale(0.98);
    }
  }

  &__image {
    width: 100%;
    display: block;
  }

  &__placeholder {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f7f8fa;
  }

  &__title {
    padding: 16rpx;
    font-size: 28rpx;
    color: #323233;
    line-height: 1.4;
  }
}
</style>
