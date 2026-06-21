<template>
  <view class="steps-field" :class="`is-${direction}`">
    <view
      v-for="(item, index) in items"
      :key="index"
      class="steps-field__item"
      :class="[`is-${getItemStatus(index)}`, { 'is-last': index === items.length - 1 }]"
    >
      <view class="steps-field__indicator">
        <view class="steps-field__circle">
          <text v-if="getItemStatus(index) === 'finish'" class="steps-field__check">✓</text>
          <text v-else-if="getItemStatus(index) === 'error'" class="steps-field__cross">✕</text>
          <text v-else class="steps-field__number">{{ index + 1 }}</text>
        </view>
        <view v-if="index < items.length - 1" class="steps-field__line"></view>
      </view>
      <view class="steps-field__content">
        <view class="steps-field__title">{{ item.title }}</view>
        <view v-if="item.description" class="steps-field__description">{{ item.description }}</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface StepItem {
  title: string
  description?: string
}

interface StepsProps {
  current?: number
  items?: StepItem[]
  direction?: 'horizontal' | 'vertical'
  status?: 'process' | 'wait' | 'finish' | 'error'
}

const props = withDefaults(defineProps<StepsProps>(), {
  current: 0,
  items: () => [],
  direction: 'horizontal',
  status: 'process'
})

const getItemStatus = (index: number): 'wait' | 'process' | 'finish' | 'error' => {
  if (index < props.current) return 'finish'
  if (index === props.current) return props.status
  return 'wait'
}
</script>

<style lang="scss" scoped>
.steps-field {
  display: flex;
  width: 100%;
  box-sizing: border-box;

  &.is-horizontal {
    flex-direction: row;
    overflow-x: auto;

    .steps-field__item {
      flex: 1;
      flex-direction: column;
      align-items: center;
    }

    .steps-field__indicator {
      flex-direction: row;
      align-items: center;
      width: 100%;
    }

    .steps-field__line {
      flex: 1;
      height: 2rpx;
      margin: 0 8rpx;
    }

    .steps-field__content {
      text-align: center;
      margin-top: 12rpx;
      padding: 0 8rpx;
    }
  }

  &.is-vertical {
    flex-direction: column;
    padding-left: 16rpx;

    .steps-field__item {
      flex-direction: row;
      align-items: flex-start;
      min-height: 80rpx;
    }

    .steps-field__indicator {
      flex-direction: column;
      align-items: center;
    }

    .steps-field__line {
      flex: 1;
      width: 2rpx;
      margin: 8rpx 0;
      min-height: 32rpx;
    }

    .steps-field__content {
      margin-left: 20rpx;
      padding-bottom: 32rpx;
    }
  }
}

.steps-field__item {
  display: flex;
  position: relative;

  &.is-last {
    .steps-field__line {
      display: none;
    }
  }
}

.steps-field__indicator {
  display: flex;
  flex-shrink: 0;
}

.steps-field__circle {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  flex-shrink: 0;
  transition: all 0.3s ease;

  .is-wait & {
    background-color: #fff;
    border: 2rpx solid #c8c9cc;
    color: #c8c9cc;
  }

  .is-process & {
    background-color: #1989fa;
    border: 2rpx solid #1989fa;
    color: #fff;
  }

  .is-finish & {
    background-color: #1989fa;
    border: 2rpx solid #1989fa;
    color: #fff;
  }

  .is-error & {
    background-color: #ee0a24;
    border: 2rpx solid #ee0a24;
    color: #fff;
  }
}

.steps-field__check,
.steps-field__cross,
.steps-field__number {
  line-height: 1;
}

.steps-field__line {
  background-color: #c8c9cc;
  transition: background-color 0.3s ease;

  .is-finish & {
    background-color: #1989fa;
  }

  .is-error & {
    background-color: #ee0a24;
  }
}

.steps-field__content {
  flex: 1;
  min-width: 0;
}

.steps-field__title {
  font-size: 28rpx;
  color: #323233;
  line-height: 1.4;
  word-break: break-all;

  .is-wait & {
    color: #969799;
  }

  .is-process & {
    color: #323233;
    font-weight: 500;
  }

  .is-finish & {
    color: #323233;
  }

  .is-error & {
    color: #ee0a24;
  }
}

.steps-field__description {
  font-size: 24rpx;
  color: #969799;
  line-height: 1.4;
  margin-top: 4rpx;

  .is-process & {
    color: #646566;
  }

  .is-error & {
    color: #ee0a24;
  }
}
</style>
