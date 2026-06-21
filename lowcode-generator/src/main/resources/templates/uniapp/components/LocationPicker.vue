<template>
  <view class="location-picker">
    <view class="location-picker__trigger" @touchstart.prevent="onPickLocation">
      <view class="location-picker__content">
        <view v-if="innerValue.address" class="location-picker__info">
          <view class="location-picker__address">
            <text class="location-picker__icon">📍</text>
            <text class="location-picker__address-text">{{ innerValue.address }}</text>
          </view>
          <view v-if="showCoordinate" class="location-picker__coordinate">
            <text class="location-picker__coord-item">经度: {{ innerValue.longitude }}</text>
            <text class="location-picker__coord-item">纬度: {{ innerValue.latitude }}</text>
          </view>
        </view>
        <view v-else class="location-picker__placeholder">
          <text>{{ placeholder }}</text>
        </view>
      </view>
      <view class="location-picker__arrow">
        <text>▸</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

interface LocationValue {
  address: string
  longitude: number
  latitude: number
}

interface LocationPickerProps {
  placeholder?: string
  showCoordinate?: boolean
  modelValue?: LocationValue
}

interface LocationPickerEmits {
  (e: 'update:modelValue', value: LocationValue): void
}

const props = withDefaults(defineProps<LocationPickerProps>(), {
  placeholder: '请选择位置',
  showCoordinate: true,
  modelValue: () => ({ address: '', longitude: 0, latitude: 0 })
})

const emit = defineEmits<LocationPickerEmits>()

const innerValue = reactive<LocationValue>({
  address: props.modelValue?.address || '',
  longitude: props.modelValue?.longitude || 0,
  latitude: props.modelValue?.latitude || 0
})

watch(() => props.modelValue, (val) => {
  if (val) {
    innerValue.address = val.address || ''
    innerValue.longitude = val.longitude || 0
    innerValue.latitude = val.latitude || 0
  }
}, { deep: true })

const onPickLocation = () => {
  uni.chooseLocation({
    latitude: innerValue.latitude || undefined,
    longitude: innerValue.longitude || undefined,
    success: (res: any) => {
      innerValue.address = res.address || res.name || ''
      innerValue.longitude = res.longitude
      innerValue.latitude = res.latitude
      emit('update:modelValue', {
        address: innerValue.address,
        longitude: innerValue.longitude,
        latitude: innerValue.latitude
      })
    },
    fail: (err: any) => {
      if (err.errMsg?.indexOf('auth deny') > -1 || err.errMsg?.indexOf('authorize') > -1) {
        uni.showModal({
          title: '提示',
          content: '需要授权位置权限才能选择位置',
          success: (modalRes: any) => {
            if (modalRes.confirm) {
              uni.openSetting({})
            }
          }
        })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.location-picker {
  width: 100%;
  box-sizing: border-box;

  &__trigger {
    display: flex;
    align-items: center;
    min-height: 80rpx;
    padding: 16rpx 24rpx;
    background-color: #f7f8fa;
    border-radius: 8rpx;
    border: 2rpx solid #e8e8e8;
    transition: border-color 0.2s ease;

    &:active {
      border-color: #1989fa;
    }
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__info {
    display: flex;
    flex-direction: column;
    gap: 8rpx;
  }

  &__address {
    display: flex;
    align-items: flex-start;
    gap: 8rpx;
  }

  &__icon {
    font-size: 32rpx;
    flex-shrink: 0;
    line-height: 1.4;
  }

  &__address-text {
    font-size: 28rpx;
    color: #323233;
    line-height: 1.4;
    word-break: break-all;
  }

  &__coordinate {
    display: flex;
    gap: 24rpx;
    padding-left: 40rpx;
  }

  &__coord-item {
    font-size: 24rpx;
    color: #969799;
  }

  &__placeholder {
    text {
      font-size: 28rpx;
      color: #c8c9cc;
    }
  }

  &__arrow {
    flex-shrink: 0;
    margin-left: 16rpx;
    color: #c8c9cc;
    font-size: 28rpx;
  }
}
</style>
