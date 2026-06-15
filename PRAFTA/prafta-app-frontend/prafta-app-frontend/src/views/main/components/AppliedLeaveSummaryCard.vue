<!--
  AppliedLeaveSummaryCard.vue — 메인 홈 "신청형 휴가" 요약 카드 (LEAVE_TYPE='01')
  - 작업: 연차 개편(표시) A3. 법정/관리자부여(AttendanceSummaryCard 잔여연차)와 분리된 별도 카드.
  - 참조 패턴: AttendanceSummaryCard.vue(.card 토큰/마크업). 토큰은 부모(.home-view)에서 상속.
  - 홈은 요약이므로 간결히: 보유 타입 수 + 총잔여. 상세(타입별)는 연차 현황 화면에서.
  - 서버 권위값(typeCount/remainingDays)을 그대로 렌더(재계산 없음). 표시 전용.
  - typeCount===0 이면 부모에서 v-if 로 미노출.
-->
<template>
  <div class="card" role="button" tabindex="0" @click="$emit('click:detail')" @keydown.enter="$emit('click:detail')" @keydown.space.prevent="$emit('click:detail')">
    <div class="head-row">
      <p class="card-title">신청형 휴가</p>
      <svg class="icon head-chev" width="18" height="18" aria-hidden="true">
        <use href="#i-chev" />
      </svg>
    </div>

    <div class="kpi-vals">
      <span class="kpi-num">{{ formattedRemaining }}</span>
      <span class="kpi-unit">일 잔여</span>
      <span class="kpi-meta">· {{ typeCount }}종</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 신청형 휴가('01') 보유 타입 수
  typeCount: {
    type: Number,
    default: 0,
  },
  // 신청형 휴가('01') 총잔여 합(서버 산출)
  remainingDays: {
    type: Number,
    default: 0,
  },
})

defineEmits(['click:detail'])

// 0.5 단위 표시 (정수면 정수, 소수면 1자리) — AttendanceSummaryCard.trimDays 동일.
const trimDays = (v) => {
  if (v == null) return '0'
  const n = Number(v)
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}

const formattedRemaining = computed(() => trimDays(props.remainingDays))
</script>

<style scoped>
.card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--color-border);
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
}
.card:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.head-chev {
  color: var(--color-text-tertiary);
}

.kpi-vals {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.kpi-num {
  font-size: 26px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: var(--color-primary);
}
.kpi-unit {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.kpi-meta {
  margin-left: 4px;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
