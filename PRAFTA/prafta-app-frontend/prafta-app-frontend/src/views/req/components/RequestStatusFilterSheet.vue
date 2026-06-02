<!--
  RequestStatusFilterSheet.vue — 상태 다중 선택 시트
  - 작업 ID: PRAFTA-APP-006-3
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="상태"
    :show-footer="true"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <ul class="req-status-list">
      <li
        v-for="opt in OPTIONS"
        :key="opt.code"
        class="req-status-list__item"
        :class="{ 'req-status-list__item--on': localSelected.includes(opt.code) }"
        @click="toggle(opt.code)"
      >
        <span class="req-status-list__dot" :class="opt.dotClass" aria-hidden="true"></span>
        <span class="req-status-list__label">{{ opt.label }}</span>
        <span class="req-status-list__check" aria-hidden="true">
          <svg
            v-if="localSelected.includes(opt.code)"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </span>
      </li>
    </ul>

    <template #footer>
      <div class="req-status-footer">
        <button type="button" class="req-status-footer__reset" @click="onResetInternal">
          초기화
        </button>
        <button
          type="button"
          class="req-status-footer__apply"
          :disabled="localSelected.length === 0"
          @click="onApply"
        >
          {{ localSelected.length }}개 선택 적용
        </button>
      </div>
    </template>
  </BaseBottomSheet>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseBottomSheet from './BaseBottomSheet.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  selected: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'apply'])

const OPTIONS = [
  { code: '01', label: '대기', dotClass: 'req-status-list__dot--warning' },
  { code: '02', label: '승인', dotClass: 'req-status-list__dot--primary' },
  { code: '03', label: '반려', dotClass: 'req-status-list__dot--danger' },
  { code: '04', label: '취소', dotClass: 'req-status-list__dot--neutral' },
]

const localSelected = ref([...props.selected])

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      // 열릴 때 상위 selected 와 동기화
      localSelected.value = [...props.selected]
    }
  },
)

const toggle = (code) => {
  const idx = localSelected.value.indexOf(code)
  if (idx >= 0) {
    localSelected.value.splice(idx, 1)
  } else {
    localSelected.value.push(code)
  }
}

const onResetInternal = () => {
  localSelected.value = []
}

const onApply = () => {
  emit('apply', [...localSelected.value])
  emit('update:modelValue', false)
}
</script>

<style scoped>
.req-status-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.req-status-list__item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 14px 0;
  cursor: pointer;
  border-bottom: 0.5px solid var(--color-border-light);
  min-height: 44px;
}

.req-status-list__dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  background: currentColor;
}
.req-status-list__dot--warning {
  color: var(--color-warning);
}
.req-status-list__dot--primary {
  color: var(--color-primary);
}
.req-status-list__dot--danger {
  color: var(--color-danger);
}
.req-status-list__dot--neutral {
  color: var(--color-text-tertiary);
}

.req-status-list__label {
  font-size: 14px;
  color: var(--color-text-primary);
}

.req-status-list__check {
  color: var(--color-primary);
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.req-status-footer {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 8px;
  padding: 4px 0;
}

.req-status-footer__reset {
  height: 48px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.req-status-footer__apply {
  height: 48px;
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
.req-status-footer__apply:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
