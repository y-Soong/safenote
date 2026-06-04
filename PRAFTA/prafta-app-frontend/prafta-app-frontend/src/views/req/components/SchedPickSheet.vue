<!--
  SchedPickSheet.vue — 스케줄 선택 라디오 단일 선택 시트 (선택 즉시 적용)
  - 작업 ID: PRAFTA-APP-007 F2
  - RequestSortSheet.vue / BaseBottomSheet.vue 패턴 미러링
  - props:
      modelValue (v-model 열림 상태)
      options    스케줄 옵션 배열 [{ schCd, schNo, baseYn, label }]
      selected   현재 선택된 schCd
      loading / error  조회 상태
  - emits:
      update:modelValue  열림 토글
      apply              선택한 옵션 { schCd, label } 적용 (즉시 닫힘)
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="스케줄 선택"
    :show-footer="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <!-- 로딩 -->
    <p v-if="loading" class="sched-pick__state">스케줄을 불러오는 중...</p>

    <!-- 에러 -->
    <p v-else-if="error" class="sched-pick__state sched-pick__state--error">
      스케줄을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.
    </p>

    <!-- 빈 결과 -->
    <p v-else-if="!options.length" class="sched-pick__state">
      선택 가능한 스케줄이 없어요. 관리자에게 문의해 주세요.
    </p>

    <!-- 목록 -->
    <ul v-else class="sched-pick__list">
      <li v-for="opt in options" :key="opt.schCd" class="sched-pick__item" @click="onPick(opt)">
        <span class="sched-pick__main">
          <span class="sched-pick__top">
            <span class="sched-pick__no">{{ opt.schNo }}</span>
            <span v-if="opt.baseYn === 'Y'" class="sched-pick__chip">기본</span>
          </span>
          <span class="sched-pick__label">{{ opt.label }}</span>
        </span>
        <span class="sched-pick__radio" aria-hidden="true">
          <span
            class="sched-pick__radio-dot"
            :class="{ 'sched-pick__radio-dot--on': selected === opt.schCd }"
          ></span>
        </span>
      </li>
    </ul>
  </BaseBottomSheet>
</template>

<script setup>
import BaseBottomSheet from './BaseBottomSheet.vue'

defineProps({
  modelValue: { type: Boolean, default: false },
  options: { type: Array, default: () => [] },
  selected: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  error: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'apply'])

const onPick = (opt) => {
  // 선택 즉시 적용 + 자동 닫힘 (RequestSortSheet 패턴)
  emit('apply', { schCd: opt.schCd, label: opt.label })
  emit('update:modelValue', false)
}
</script>

<style scoped>
.sched-pick__state {
  margin: 0;
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.sched-pick__state--error {
  color: var(--color-danger);
}

.sched-pick__list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.sched-pick__item {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: var(--space-sm);
  padding: 14px 0;
  cursor: pointer;
  border-bottom: 0.5px solid var(--color-border-light);
  min-height: 44px;
}

.sched-pick__main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.sched-pick__top {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.sched-pick__no {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.sched-pick__chip {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-primary);
  background: var(--color-primary-tint);
  border-radius: var(--radius-full);
  padding: 1px 8px;
  line-height: 1.6;
}

.sched-pick__label {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}

.sched-pick__radio {
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  border: 1.5px solid var(--color-border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.sched-pick__radio-dot {
  width: 10px;
  height: 10px;
  border-radius: var(--radius-full);
  background: transparent;
}
.sched-pick__radio-dot--on {
  background: var(--color-primary);
}
.sched-pick__radio:has(.sched-pick__radio-dot--on) {
  border-color: var(--color-primary);
}
</style>
