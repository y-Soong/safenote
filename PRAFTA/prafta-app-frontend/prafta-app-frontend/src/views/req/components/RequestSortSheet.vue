<!--
  RequestSortSheet.vue — 정렬 라디오 단일 선택 (선택 즉시 적용)
  - 작업 ID: PRAFTA-APP-006-6
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="정렬"
    :show-footer="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <ul class="req-sort-list">
      <li
        v-for="opt in OPTIONS"
        :key="opt.code"
        class="req-sort-list__item"
        @click="onPick(opt.code)"
      >
        <span class="req-sort-list__label">{{ opt.label }}</span>
        <span class="req-sort-list__radio" aria-hidden="true">
          <span
            class="req-sort-list__radio-dot"
            :class="{ 'req-sort-list__radio-dot--on': selected === opt.code }"
          ></span>
        </span>
      </li>
    </ul>
  </BaseBottomSheet>
</template>

<script setup>
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'

defineProps({
  modelValue: { type: Boolean, default: false },
  selected: { type: String, default: 'PENDING_FIRST' },
})
const emit = defineEmits(['update:modelValue', 'apply'])

const OPTIONS = [
  { code: 'PENDING_FIRST', label: '대기 우선 (기본)' },
  { code: 'RECENT', label: '최근 요청순' },
  { code: 'TARGET_DATE', label: '대상일자 가까운순' },
]

const onPick = (code) => {
  // 선택 즉시 적용 + 자동 닫힘 (시안 §4.4.4)
  emit('apply', code)
  emit('update:modelValue', false)
}
</script>

<style scoped>
.req-sort-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.req-sort-list__item {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  padding: 14px 0;
  cursor: pointer;
  border-bottom: 0.5px solid var(--color-border-light);
  min-height: 44px;
}

.req-sort-list__label {
  font-size: 14px;
  color: var(--color-text-primary);
}

.req-sort-list__radio {
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  border: 1.5px solid var(--color-border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.req-sort-list__radio-dot {
  width: 10px;
  height: 10px;
  border-radius: var(--radius-full);
  background: transparent;
}
.req-sort-list__radio-dot--on {
  background: var(--color-primary);
}
.req-sort-list__radio:has(.req-sort-list__radio-dot--on) {
  border-color: var(--color-primary);
}
</style>
