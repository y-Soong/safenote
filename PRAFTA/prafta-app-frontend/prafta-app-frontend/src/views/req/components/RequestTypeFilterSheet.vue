<!--
  RequestTypeFilterSheet.vue — 요청 유형 다중 선택 (SYS032 7종 + LC_MOVE 1종, 8종)
  - 작업 ID: PRAFTA-APP-006-5 / PRAFTA-APP-029-5 (스케줄수정 '10' 추가)
  - PRAFTA-내승인요청연차통합-2: 연차 이동(LC_MOVE) 옵션 추가.
    LC_MOVE 는 SYS032 예약 충돌(07~09) 회피 목적의 합성 코드 — SYS032 DB 미등록,
    향후 SYS032 서버 조회로 전환 시에도 LC_MOVE 는 별도 하드코딩 유지(AppReq06ServiceImpl 참조).
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="요청 유형"
    :show-footer="true"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <ul class="req-type-list">
      <li
        v-for="opt in OPTIONS"
        :key="opt.code"
        class="req-type-list__item"
        @click="toggle(opt.code)"
      >
        <span class="req-type-list__label">{{ opt.label }}</span>
        <span class="req-type-list__check" aria-hidden="true">
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
      <div class="req-type-footer">
        <button type="button" class="req-type-footer__reset" @click="onResetInternal">
          초기화
        </button>
        <button
          type="button"
          class="req-type-footer__apply"
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
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  selected: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'apply'])

// TODO(developer): 라벨 단일출처(SYS032) FU — 현재 01~06/10 이 하드코딩 라벨이므로 일관성 위해 유지.
//   추후 SYS032 서버 조회로 OPTIONS 전체를 치환하더라도 LC_MOVE 는 SYS032 비등록 코드이므로 별도 유지 필요.
const OPTIONS = [
  { code: '01', label: '근태 생성' },
  { code: '02', label: '근태 수정' },
  { code: '03', label: '초과근무 생성' },
  { code: '04', label: '초과근무 수정' },
  { code: '05', label: '연차 사용' },
  { code: '06', label: '연차 수정' },
  { code: '10', label: '스케줄수정' },
  { code: 'LC_MOVE', label: '연차 이동' },
]

const localSelected = ref([...props.selected])

watch(
  () => props.modelValue,
  (open) => {
    if (open) localSelected.value = [...props.selected]
  },
)

const toggle = (code) => {
  const idx = localSelected.value.indexOf(code)
  if (idx >= 0) localSelected.value.splice(idx, 1)
  else localSelected.value.push(code)
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
.req-type-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.req-type-list__item {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  padding: 14px 0;
  cursor: pointer;
  border-bottom: 0.5px solid var(--color-border-light);
  min-height: 44px;
}

.req-type-list__label {
  font-size: 14px;
  color: var(--color-text-primary);
}

.req-type-list__check {
  color: var(--color-primary);
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.req-type-footer {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 8px;
  padding: 4px 0;
}

.req-type-footer__reset {
  height: 48px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.req-type-footer__apply {
  height: 48px;
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
.req-type-footer__apply:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
