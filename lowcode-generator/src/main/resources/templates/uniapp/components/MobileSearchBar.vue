<template>
  <view
    class="mobile-search-bar"
    :class="{ 'is-focused': isFocused, 'is-round': shape === 'round' }"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <view class="mobile-search-bar__inner" :style="{ backgroundColor: background}>
      <view class="mobile-search-bar__input-wrapper" :style="{ borderRadius: radius }">
        <view class="mobile-search-bar__icon">
          <slot name="icon">
            <text class="iconfont">🔍</text>
          </slot>
        </view>
        <input
          v-model="inputValue"
          class="mobile-search-bar__input"
          :type="type"
          :placeholder="placeholder"
          :placeholder-style="placeholderStyle"
          :placeholder-class="placeholderClass"
          :disabled="disabled"
          :maxlength="maxlength"
          :focus="autofocus"
          :confirm-type="confirmType"
          :cursor-spacing="cursorSpacing"
          :adjust-position="adjustPosition"
          @focus="onFocus"
          @blur="onBlur"
          @input="onInput"
          @confirm="onConfirm"
          @search="onSearch"
        />
        <view
          v-if="clearable && inputValue"
          class="mobile-search-bar__clear"
          @tap="onClear"
        >
          <text class="iconfont">✕</text>
        </view>
      </view>
      <view
        v-if="showAction"
        class="mobile-search-bar__action"
        @tap="onActionClick"
      >
        <slot name="action">
          <text>{{ actionText }}</text>
        </slot>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'

interface SearchBarProps {
  modelValue?: string
  placeholder?: string
  placeholderStyle?: string
  placeholderClass?: string
  type?: 'text' | 'number' | 'idcard' | 'digit'
  disabled?: boolean
  maxlength?: number
  clearable?: boolean
  autofocus?: boolean
  showAction?: boolean
  actionText?: string
  background?: string
  shape?: 'square' | 'round'
  radius?: string
  confirmType?: 'send' | 'search' | 'next' | 'go' | 'done'
  cursorSpacing?: number
  adjustPosition?: boolean
}

interface SearchBarEmits {
  (e: 'update:modelValue', value: string): void
  (e: 'search', value: string): void
  (e: 'focus', e: Event): void
  (e: 'blur', e: Event): void
  (e: 'clear'): void
  (e: 'action-click'): void
  (e: 'input', value: string): void
  (e: 'change', value: string): void
  (e: 'swipe', direction: 'left' | 'right' | 'up' | 'down'): void
  (e: 'longpress'): void
}

const props = withDefaults(defineProps<SearchBarProps>(), {
  modelValue: '',
  placeholder: '请输入搜索内容',
  placeholderStyle: '',
  placeholderClass: '',
  type: 'text',
  disabled: false,
  maxlength: -1,
  clearable: true,
  autofocus: false,
  showAction: false,
  actionText: '取消',
  background: '#f7f8fa',
  shape: 'square',
  radius: '8rpx',
  confirmType: 'search',
  cursorSpacing: 0,
  adjustPosition: true
})

const emit = defineEmits<SearchBarEmits>()

const inputValue = ref(props.modelValue)
const isFocused = ref(false)

const touchState = reactive({
  startX: 0,
  startY: 0,
  moveX: 0,
  moveY: 0,
  startTime: 0,
  longPressTimer: null as ReturnType<typeof setTimeout> | null
})

watch(() => props.modelValue, (newVal) => {
  inputValue.value = newVal
})

watch(inputValue, (newVal) => {
  emit('update:modelValue', newVal)
  emit('input', newVal)
  emit('change', newVal)
})

const onFocus = (e: Event) => {
  isFocused.value = true
  emit('focus', e)
}

const onBlur = (e: Event) => {
  isFocused.value = false
  emit('blur', e)
}

const onInput = (e: { detail: { value: string } }) => {
  inputValue.value = e.detail.value
}

const onConfirm = (e: { detail: { value: string } }) => {
  emit('search', e.detail.value)
}

const onSearch = (e: { detail: { value: string } }) => {
  emit('search', e.detail.value)
}

const onClear = () => {
  inputValue.value = ''
  emit('clear')
}

const onActionClick = () => {
  emit('action-click')
}

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
.mobile-search-bar {
  width: 100%;
  padding: 16rpx 24rpx;
  box-sizing: border-box;
  background-color: #fff;

  &__inner {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  &__input-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    height: 72rpx;
    padding: 0 24rpx;
    background-color: #f7f8fa;
    transition: background-color 0.2s ease;
  }

  &__icon {
    margin-right: 16rpx;
    font-size: 28rpx;
    color: #969799;
  }

  &__input {
    flex: 1;
    height: 100%;
    font-size: 28rpx;
    color: #323233;
  }

  &__clear {
    width: 36rpx;
    height: 36rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-left: 16rpx;
    background-color: #c8c9cc;
    border-radius: 50%;
    font-size: 20rpx;
    color: #fff;
  }

  &__action {
    padding: 0 8rpx;
    font-size: 28rpx;
    color: #1989fa;
    white-space: nowrap;
  }

  &.is-focused {
    .mobile-search-bar__input-wrapper {
      background-color: #fff;
    }
  }

  &.is-round {
    .mobile-search-bar__input-wrapper {
      border-radius: 36rpx;
    }
  }
}
</style>
