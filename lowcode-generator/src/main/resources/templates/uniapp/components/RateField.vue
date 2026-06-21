<template>
  <view class="rate-field">
    <view
      v-for="index in count"
      :key="index"
      class="rate-field__star"
      :class="{
        'is-active': index <= currentValue,
        'is-half': allowHalf && index === Math.ceil(currentValue) && currentValue % 1 !== 0,
        'is-disabled': disabled
      }"
      @touchstart.prevent="onTouchStart(index, $event)"
      @touchmove.prevent="onTouchMove($event)"
      @touchend="onTouchEnd"
    >
      <view class="rate-field__star-inner">
        <text class="rate-field__star-icon rate-field__star-icon--bg">☆</text>
        <text
          class="rate-field__star-icon rate-field__star-icon--filled"
          :style="{ width: getStarWidth(index) }"
        >★</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface RateProps {
  count?: number
  allowHalf?: boolean
  allowClear?: boolean
  disabled?: boolean
  modelValue?: number
}

interface RateEmits {
  (e: 'update:modelValue', value: number): void
}

const props = withDefaults(defineProps<RateProps>(), {
  count: 5,
  allowHalf: false,
  allowClear: true,
  disabled: false,
  modelValue: 0
})

const emit = defineEmits<RateEmits>()

const currentValue = ref(props.modelValue)
const touchStartValue = ref(0)
const isTouching = ref(false)
const touchStarIndex = ref(0)
const touchIsHalf = ref(false)

watch(() => props.modelValue, (val) => {
  currentValue.value = val
})

const getStarWidth = (index: number): string => {
  if (index <= Math.floor(currentValue.value)) return '100%'
  if (index === Math.ceil(currentValue.value) && currentValue.value % 1 !== 0) {
    return `${(currentValue.value % 1) * 100}%`
  }
  return '0%'
}

const getStarIndexFromTouch = (clientX: number): { index: number; isHalf: boolean } => {
  const query = uni.createSelectorQuery().in(getCurrentInstance())
  return new Promise<{ index: number; isHalf: boolean }>((resolve) => {
    query.selectAll('.rate-field__star').boundingClientRect((rects: any[]) => {
      if (!rects || !rects.length) {
        resolve({ index: 1, isHalf: false })
        return
      }
      for (let i = 0; i < rects.length; i++) {
        const rect = rects[i]
        if (clientX >= rect.left && clientX <= rect.right) {
          const isHalf = props.allowHalf && (clientX - rect.left) < rect.width / 2
          resolve({ index: i + 1, isHalf })
          return
        }
      }
      resolve({ index: rects.length, isHalf: false })
    }).exec()
  }) as any
}

const onTouchStart = async (index: number, e: TouchEvent) => {
  if (props.disabled) return
  const touch = e.touches[0]
  touchStartValue.value = currentValue.value
  isTouching.value = true
  const result = await getStarIndexFromTouch(touch.clientX)
  touchStarIndex.value = result.index
  touchIsHalf.value = result.isHalf
  currentValue.value = result.isHalf ? result.index - 0.5 : result.index
}

const onTouchMove = async (e: TouchEvent) => {
  if (props.disabled || !isTouching.value) return
  const touch = e.touches[0]
  const result = await getStarIndexFromTouch(touch.clientX)
  touchStarIndex.value = result.index
  touchIsHalf.value = result.isHalf
  currentValue.value = result.isHalf ? result.index - 0.5 : result.index
}

const onTouchEnd = () => {
  if (props.disabled) return
  isTouching.value = false
  if (props.allowClear && currentValue.value === touchStartValue.value) {
    currentValue.value = 0
  }
  emit('update:modelValue', currentValue.value)
}
</script>

<style lang="scss" scoped>
.rate-field {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;

  &__star {
    position: relative;
    font-size: 48rpx;
    line-height: 1;
    cursor: pointer;
    transition: transform 0.15s ease;

    &:active {
      transform: scale(1.2);
    }

    &.is-disabled {
      opacity: 0.5;
      cursor: not-allowed;

      &:active {
        transform: none;
      }
    }
  }

  &__star-inner {
    position: relative;
    display: inline-block;
  }

  &__star-icon {
    font-style: normal;

    &--bg {
      color: #e8e8e8;
    }

    &--filled {
      position: absolute;
      left: 0;
      top: 0;
      overflow: hidden;
      color: #ffd700;
      white-space: nowrap;
    }
  }
}
</style>
