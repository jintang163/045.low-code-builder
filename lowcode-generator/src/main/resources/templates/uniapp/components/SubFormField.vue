<template>
  <view class="sub-form-field">
    <view class="sub-form-field__header">
      <text class="sub-form-field__title">{{ title }}</text>
      <text class="sub-form-field__count">{{ innerValue.length }}/{{ maxRows }}</text>
    </view>

    <view class="sub-form-field__list">
      <view
        v-for="(row, rowIndex) in innerValue"
        :key="rowIndex"
        class="sub-form-field__row"
      >
        <view class="sub-form-field__row-index">
          <text>{{ rowIndex + 1 }}</text>
        </view>
        <view class="sub-form-field__row-content" @touchstart.prevent="onViewRow(rowIndex)">
          <view
            v-for="col in columns"
            :key="col.key || col.dataIndex"
            class="sub-form-field__cell"
          >
            <text class="sub-form-field__cell-label">{{ col.title }}:</text>
            <text class="sub-form-field__cell-value">{{ getCellValue(row, col) }}</text>
          </view>
        </view>
        <view v-if="!disabled && showDeleteButton" class="sub-form-field__row-delete" @touchstart.prevent="onDeleteRow(rowIndex)">
          <text>✕</text>
        </view>
      </view>

      <view v-if="innerValue.length === 0" class="sub-form-field__empty">
        <text>暂无数据</text>
      </view>
    </view>

    <view v-if="!disabled && showAddButton" class="sub-form-field__add" @touchstart.prevent="onAddRow">
      <text class="sub-form-field__add-icon">+</text>
      <text class="sub-form-field__add-text">添加</text>
    </view>

    <uni-popup ref="popupRef" type="bottom">
      <view class="sub-form-field__popup">
        <view class="sub-form-field__popup-header">
          <view class="sub-form-field__popup-cancel" @touchstart.prevent="onCancelEdit">
            <text>取消</text>
          </view>
          <text class="sub-form-field__popup-title">{{ editingIndex >= 0 ? '编辑' : '新增' }}</text>
          <view class="sub-form-field__popup-confirm" @touchstart.prevent="onConfirmEdit">
            <text>确定</text>
          </view>
        </view>
        <scroll-view scroll-y class="sub-form-field__popup-body">
          <view
            v-for="col in columns"
            :key="col.key || col.dataIndex"
            class="sub-form-field__popup-field"
          >
            <text class="sub-form-field__popup-label">{{ col.title }}</text>
            <input
              class="sub-form-field__popup-input"
              :value="getEditingValue(col)"
              :placeholder="`请输入${col.title}`"
              :disabled="disabled"
              @input="onEditingInput(col, $event)"
            />
          </view>
        </scroll-view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

export interface SubFormColumn {
  key?: string
  title: string
  dataIndex: string
}

interface SubFormProps {
  title?: string
  columns?: SubFormColumn[]
  modelValue?: Record<string, any>[]
  showAddButton?: boolean
  showDeleteButton?: boolean
  minRows?: number
  maxRows?: number
  disabled?: boolean
}

interface SubFormEmits {
  (e: 'update:modelValue', value: Record<string, any>[]): void
}

const props = withDefaults(defineProps<SubFormProps>(), {
  title: '子表单',
  columns: () => [],
  modelValue: () => [],
  showAddButton: true,
  showDeleteButton: true,
  minRows: 0,
  maxRows: 10,
  disabled: false
})

const emit = defineEmits<SubFormEmits>()

const innerValue = ref<Record<string, any>[]>([...props.modelValue])
const popupRef = ref()
const editingIndex = ref(-1)
const editingRow = ref<Record<string, any>>({})

watch(() => props.modelValue, (val) => {
  innerValue.value = [...val]
}, { deep: true })

const getCellValue = (row: Record<string, any>, col: SubFormColumn): string => {
  const key = col.key || col.dataIndex
  const val = row[key]
  return val !== undefined && val !== null && val !== '' ? String(val) : '-'
}

const getEditingValue = (col: SubFormColumn): string => {
  const key = col.key || col.dataIndex
  const val = editingRow.value[key]
  return val !== undefined && val !== null ? String(val) : ''
}

const emitValue = () => {
  emit('update:modelValue', [...innerValue.value])
}

const onAddRow = () => {
  if (props.disabled) return
  if (innerValue.value.length >= props.maxRows) {
    uni.showToast({ title: `最多添加${props.maxRows}条`, icon: 'none' })
    return
  }
  editingIndex.value = -1
  editingRow.value = {}
  popupRef.value?.open()
}

const onViewRow = (index: number) => {
  if (props.disabled) return
  editingIndex.value = index
  editingRow.value = { ...innerValue.value[index] }
  popupRef.value?.open()
}

const onDeleteRow = (index: number) => {
  if (props.disabled) return
  if (innerValue.value.length <= props.minRows) {
    uni.showToast({ title: `最少保留${props.minRows}条`, icon: 'none' })
    return
  }
  uni.showModal({
    title: '提示',
    content: '确定删除该条数据？',
    success: (res: any) => {
      if (res.confirm) {
        innerValue.value.splice(index, 1)
        emitValue()
      }
    }
  })
}

const onEditingInput = (col: SubFormColumn, e: any) => {
  const key = col.key || col.dataIndex
  editingRow.value[key] = e.detail.value
}

const onConfirmEdit = () => {
  if (editingIndex.value >= 0) {
    innerValue.value[editingIndex.value] = { ...editingRow.value }
  } else {
    innerValue.value.push({ ...editingRow.value })
  }
  emitValue()
  popupRef.value?.close()
}

const onCancelEdit = () => {
  popupRef.value?.close()
}
</script>

<style lang="scss" scoped>
.sub-form-field {
  width: 100%;
  box-sizing: border-box;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16rpx 0;
  }

  &__title {
    font-size: 30rpx;
    color: #323233;
    font-weight: 500;
  }

  &__count {
    font-size: 24rpx;
    color: #969799;
  }

  &__list {
    width: 100%;
  }

  &__row {
    display: flex;
    align-items: flex-start;
    padding: 20rpx 24rpx;
    background-color: #f7f8fa;
    border-radius: 8rpx;
    margin-bottom: 16rpx;

    &:active {
      background-color: #f0f1f3;
    }
  }

  &__row-index {
    width: 48rpx;
    height: 48rpx;
    border-radius: 50%;
    background-color: #1989fa;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24rpx;
    flex-shrink: 0;
    margin-right: 16rpx;
  }

  &__row-content {
    flex: 1;
    min-width: 0;
  }

  &__cell {
    display: flex;
    align-items: baseline;
    margin-bottom: 8rpx;

    &:last-child {
      margin-bottom: 0;
    }
  }

  &__cell-label {
    font-size: 24rpx;
    color: #969799;
    flex-shrink: 0;
    margin-right: 8rpx;
  }

  &__cell-value {
    font-size: 26rpx;
    color: #323233;
    word-break: break-all;
  }

  &__row-delete {
    flex-shrink: 0;
    width: 48rpx;
    height: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-left: 16rpx;
    color: #ee0a24;
    font-size: 28rpx;

    &:active {
      opacity: 0.7;
    }
  }

  &__empty {
    padding: 48rpx 0;
    text-align: center;

    text {
      font-size: 28rpx;
      color: #c8c9cc;
    }
  }

  &__add {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    padding: 20rpx;
    border: 2rpx dashed #c8c9cc;
    border-radius: 8rpx;
    margin-top: 8rpx;

    &:active {
      background-color: #f7f8fa;
      border-color: #1989fa;
    }
  }

  &__add-icon {
    font-size: 36rpx;
    color: #1989fa;
  }

  &__add-text {
    font-size: 28rpx;
    color: #1989fa;
  }

  &__popup {
    background-color: #fff;
    border-radius: 24rpx 24rpx 0 0;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
  }

  &__popup-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx 32rpx;
    border-bottom: 1rpx solid #ebedf0;
  }

  &__popup-cancel,
  &__popup-confirm {
    font-size: 28rpx;
    padding: 8rpx 16rpx;

    &:active {
      opacity: 0.7;
    }
  }

  &__popup-cancel {
    color: #969799;
  }

  &__popup-confirm {
    color: #1989fa;
    font-weight: 500;
  }

  &__popup-title {
    font-size: 32rpx;
    color: #323233;
    font-weight: 500;
  }

  &__popup-body {
    flex: 1;
    padding: 24rpx 32rpx;
    max-height: 60vh;
  }

  &__popup-field {
    margin-bottom: 24rpx;
  }

  &__popup-label {
    display: block;
    font-size: 28rpx;
    color: #646566;
    margin-bottom: 12rpx;
  }

  &__popup-input {
    width: 100%;
    height: 72rpx;
    padding: 0 20rpx;
    background-color: #f7f8fa;
    border: 2rpx solid #e8e8e8;
    border-radius: 8rpx;
    font-size: 28rpx;
    color: #323233;
    box-sizing: border-box;

    &:focus {
      border-color: #1989fa;
    }
  }
}
</style>
