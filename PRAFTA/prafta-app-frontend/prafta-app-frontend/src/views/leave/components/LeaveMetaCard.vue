<!--
  LeaveMetaCard.vue — 메타 카드 (입사일 / 근속 / 사용률)
  - 작업: prafta-app-005 슬롯 G (UI 명세: UI-A005)
  - 시안 §4.6 / 3행 + 구분선. 경력 인정(>0)이면 입사일 행 아래 보조 라벨.
  - props.user: { hireDate(YYYYMMDD), serviceMonths, serviceCreditMonths }, usageRate(정수)
  - 표시 전용(단순 포맷). 근속/사용률 등 값 산출 책임은 서버(serviceMonths/usageRate). 토큰은 부모 상속.
-->
<template>
  <div class="meta">
    <!-- 입사일 -->
    <div class="meta__row">
      <span class="meta__k">입사일</span>
      <span class="meta__v">{{ hireDateText }}</span>
    </div>
    <!-- 경력 인정 보조 (creditMonths > 0일 때만) -->
    <div v-if="hasCredit" class="meta__row meta__row--sub">
      <span class="meta__k meta__k--sub">경력 인정</span>
      <span class="meta__v meta__v--sub">{{ creditMonths }}개월</span>
    </div>
    <!-- 근속 -->
    <div class="meta__row">
      <span class="meta__k">근속</span>
      <span class="meta__v">{{ tenureText }}</span>
    </div>
    <!-- 사용률 -->
    <div class="meta__row">
      <span class="meta__k">사용률</span>
      <span class="meta__v">{{ usageRate }}%</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

import { formatYmdDisplay } from '@/utils/approvalFormat'

const props = defineProps({
  // { hireDate(YYYYMMDD), serviceMonths, serviceCreditMonths }
  user: {
    type: Object,
    default: null,
  },
  // 사용률(정수 %) — 현재 토글 그룹 기준(Q5 확정에 따름)
  usageRate: {
    type: Number,
    default: 0,
  },
})

const creditMonths = computed(() => Number(props.user?.serviceCreditMonths ?? 0))
const hasCredit = computed(() => creditMonths.value > 0)

// YYYYMMDD → YYYY.MM.DD (D1 점 통일). 형식 미충족 시 원본/대시 폴백 유지.
const hireDateText = computed(() => {
  const raw = String(props.user?.hireDate ?? '')
  if (raw.length !== 8) return raw || '-'
  return formatYmdDisplay(raw)
})

// 개월수 → "N년 M개월" (0년이면 "M개월", 0개월이면 "N년")
const tenureText = computed(() => {
  const m = Number(props.user?.serviceMonths ?? 0)
  if (m <= 0) return '0개월'
  const years = Math.floor(m / 12)
  const months = m % 12
  if (years === 0) return `${months}개월`
  if (months === 0) return `${years}년`
  return `${years}년 ${months}개월`
})
</script>

<style scoped>
.meta {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
}
.meta__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}
.meta__row + .meta__row {
  border-top: 1px solid var(--color-border-light);
}
.meta__row--sub {
  padding: 4px 0 6px;
  border-top: 0;
}
.meta__k {
  color: var(--color-text-secondary);
}
.meta__k--sub {
  font-size: 12px;
  color: var(--color-text-tertiary);
  padding-left: var(--space-sm);
}
.meta__v {
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.meta__v--sub {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-tertiary);
}
</style>
