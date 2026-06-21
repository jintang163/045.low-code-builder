<template>
  <view class="signature-field">
    <view class="signature-field__canvas-wrapper">
      <canvas
        canvas-id="signatureCanvas"
        :id="canvasId"
        class="signature-field__canvas"
        :style="canvasStyle"
        @touchstart="onTouchStart"
        @touchmove="onTouchMove"
        @touchend="onTouchEnd"
        disable-scroll
      ></canvas>
      <view v-if="isEmpty" class="signature-field__placeholder">
        <text>请在此处签名</text>
      </view>
    </view>
    <view class="signature-field__actions">
      <view class="signature-field__btn signature-field__btn--clear" @touchstart.prevent="onClear">
        <text>清除</text>
      </view>
      <view class="signature-field__btn signature-field__btn--confirm" @touchstart.prevent="onConfirm">
        <text>确认</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, getCurrentInstance } from 'vue'

interface SignatureProps {
  width?: number
  height?: number
  penColor?: string
  backgroundColor?: string
  penWidth?: number
  modelValue?: string
}

interface SignatureEmits {
  (e: 'update:modelValue', value: string): void
}

const props = withDefaults(defineProps<SignatureProps>(), {
  width: 400,
  height: 200,
  penColor: '#000000',
  backgroundColor: '#ffffff',
  penWidth: 2,
  modelValue: ''
})

const emit = defineEmits<SignatureEmits>()

const canvasId = 'signatureCanvas'
const isEmpty = ref(true)
const isDrawing = ref(false)
const lastX = ref(0)
const lastY = ref(0)
let ctx: UniApp.CanvasContext | null = null

const canvasStyle = computed(() => ({
  width: `${props.width}rpx`,
  height: `${props.height}rpx`,
  backgroundColor: props.backgroundColor
}))

const initCanvas = () => {
  ctx = uni.createCanvasContext(canvasId, getCurrentInstance())
  if (ctx) {
    ctx.setFillStyle(props.backgroundColor)
    ctx.fillRect(0, 0, props.width, props.height)
    ctx.draw(true)
  }
}

onMounted(() => {
  nextTick(() => {
    setTimeout(() => {
      initCanvas()
    }, 300)
  })
})

const getTouchPos = (e: TouchEvent) => {
  const touch = e.touches[0] || e.changedTouches[0]
  return new Promise<{ x: number; y: number }>((resolve) => {
    const query = uni.createSelectorQuery().in(getCurrentInstance())
    query.select(`#${canvasId}`).boundingClientRect((rect: any) => {
      if (rect) {
        resolve({
          x: touch.clientX - rect.left,
          y: touch.clientY - rect.top
        })
      } else {
        resolve({ x: touch.clientX, y: touch.clientY })
      }
    }).exec()
  })
})

const onTouchStart = async (e: TouchEvent) => {
  isDrawing.value = true
  const pos = await getTouchPos(e)
  lastX.value = pos.x
  lastY.value = pos.y
}

const onTouchMove = async (e: TouchEvent) => {
  if (!isDrawing.value || !ctx) return
  const pos = await getTouchPos(e)

  ctx.beginPath()
  ctx.setStrokeStyle(props.penColor)
  ctx.setLineWidth(props.penWidth)
  ctx.setLineCap('round')
  ctx.setLineJoin('round')
  ctx.moveTo(lastX.value, lastY.value)
  ctx.lineTo(pos.x, pos.y)
  ctx.stroke()
  ctx.draw(true)

  lastX.value = pos.x
  lastY.value = pos.y
  isEmpty.value = false
}

const onTouchEnd = () => {
  isDrawing.value = false
}

const onClear = () => {
  if (!ctx) return
  ctx.setFillStyle(props.backgroundColor)
  ctx.fillRect(0, 0, props.width, props.height)
  ctx.draw(true)
  isEmpty.value = true
  emit('update:modelValue', '')
}

const onConfirm = () => {
  uni.canvasToTempFilePath({
    canvasId,
    success: (res: any) => {
      const base64 = res.tempFilePath
      emit('update:modelValue', base64)
    },
    fail: () => {
      uni.showToast({ title: '签名保存失败', icon: 'none' })
    }
  }, getCurrentInstance())
}
</script>

<style lang="scss" scoped>
.signature-field {
  width: 100%;
  box-sizing: border-box;

  &__canvas-wrapper {
    position: relative;
    border: 2rpx solid #e8e8e8;
    border-radius: 8rpx;
    overflow: hidden;
  }

  &__canvas {
    display: block;
  }

  &__placeholder {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: none;

    text {
      font-size: 28rpx;
      color: #c8c9cc;
    }
  }

  &__actions {
    display: flex;
    justify-content: flex-end;
    gap: 16rpx;
    margin-top: 16rpx;
  }

  &__btn {
    padding: 12rpx 32rpx;
    border-radius: 8rpx;
    font-size: 28rpx;
    text-align: center;
    transition: opacity 0.2s ease;

    &:active {
      opacity: 0.7;
    }

    &--clear {
      background-color: #f7f8fa;
      color: #646566;
      border: 2rpx solid #e8e8e8;
    }

    &--confirm {
      background-color: #1989fa;
      color: #fff;
      border: 2rpx solid #1989fa;
    }
  }
}
</style>
