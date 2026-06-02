<!--
  LeaveGroupToggle.vue — 그룹 토글 (전체 / 법정 / 법정 외) 세그먼트
  - 작업: prafta-app-005 슬롯 C (UI 명세: UI-A005)
  - 시안 §4.2 / 참조: views/attd/MyAttendanceView.vue (.attd-seg 세그먼트 패턴)
  - 단일 선택. modelValue: 'TOTAL' | 'STATUTORY' | 'NON_STATUTORY'. 토글은 UI 상태(허용 범위).
  - 토큰은 부모(.my-leave-view) 루트에서 상속.
-->
<template>
  <div class="seg" role="tablist" aria-label="연차 그룹">
    <button
      v-for="opt in options"
      :key="opt.key"
      type="button"
      role="tab"
      class="seg__item"
      :class="{ 'seg__item--on': modelValue === opt.key }"
      :aria-selected="modelValue === opt.key"
      @click="onSelect(opt.key)"
    >
      {{ opt.label }}
    </button>
  </div>
</template>

<script setup>
defineProps({
  // 'TOTAL' | 'STATUTORY' | 'NON_STATUTORY'
  modelValue: {
    type: String,
    default: 'TOTAL',
  },
})

const emit = defineEmits(['update:modelValue'])

// 토글 항목 (고정 UI 정의)
const options = [
  { key: 'TOTAL', label: '전체' },
  { key: 'STATUTORY', label: '법정' },
  { key: 'NON_STATUTORY', label: '법정 외' },
]

const onSelect = (key) => {
  emit('update:modelValue', key)
}
</script>

<style scoped>
.seg {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding: 3px;
  background: var(--color-border-light);
  border-radius: var(--radius-md);
}
.seg__item {
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.seg__item--on {
  background: var(--color-surface);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-sm);
}
</style>
