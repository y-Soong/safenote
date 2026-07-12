<!--
  LeaveAppliedCard.vue — 신청형 휴가 카드 (LEAVE_TYPE='01' 타입별 잔여/한도)
  - 작업: 연차 개편(표시) A3. 법정/관리자부여 그룹과 분리된 "신청형 휴가" 별도 섹션.
  - 참조 패턴: LeaveBalanceCard.vue(.card 토큰/마크업). 토큰은 부모(.my-leave-view)에서 상속.
  - types: [{ leaveCd, leaveNm, maxAplyDays, usedDays, remainDays }] (서버 권위값, 재계산 없음).
  - 표시 전용(비즈니스 로직 없음). 각 행: "타입명 · 잔여 N일 / 한도 N일".
  - 항목 0개면 부모에서 v-if 로 미노출(이 카드는 1개 이상일 때만 렌더).
-->
<template>
  <div class="card">
    <p class="card__label">신청형 휴가</p>

    <ul class="rows">
      <li v-for="t in types" :key="t.leaveCd" class="row">
        <span class="row__nm">{{ t.leaveNm }}</span>
        <span class="row__vals">
          <span class="row__remain" :class="{ 'row__remain--zero': isZero(t.remainDays) }">
            잔여 {{ fmtDays(t.remainDays) }}
          </span>
          <span class="row__total">/ 한도 {{ fmtDays(t.maxAplyDays) }}</span>
        </span>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { formatLeaveDays } from '@/utils/leaveFormat'

const props = defineProps({
  // 신청형 휴가('01') 타입 배열. [{ leaveCd, leaveNm, maxAplyDays, usedDays, remainDays }]
  types: {
    type: Array,
    default: () => [],
  },
  // LC-11: 1일 환산시간(분) — "N일 H시간 M분" 표기 분모(서버 권위). 미제공 시 480 폴백.
  convMinutes: {
    type: Number,
    default: 480,
  },
})

// LC-11: 소수점 노출 금지 — "N일 H시간 M분" 표기(서버 권위값 그대로, 표시만 교체).
const fmtDays = (v) => formatLeaveDays(Number(v ?? 0), props.convMinutes)

const isZero = (v) => Number(v ?? 0) === 0
</script>

<style scoped>
.card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 16px;
}
.card__label {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.rows {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-sm);
}
.row__nm {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.row__vals {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-variant-numeric: tabular-nums;
}
.row__remain {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-primary);
}
.row__remain--zero {
  color: var(--color-text-primary);
}
.row__total {
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-secondary);
}
</style>
