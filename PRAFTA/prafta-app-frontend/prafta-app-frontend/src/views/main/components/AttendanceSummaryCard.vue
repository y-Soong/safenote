<!--
  AttendanceSummaryCard.vue — 근태 조회 카드 (잔여연차 + 승인요청 2분할 KPI)
  - 상세 요청서 §3.3, §4.3
  - attd §8.1 (연차 타입), §9.4 (휴가 신청)
  - 요청승인관리 재기획서 §1.2 (요청 승인 단일 출처)
  - 잔여연차: 통합 표시(법정/법정외 분류는 신청 폼에서)
  - 승인요청: REQ_STATUS = 'REQUESTED' 본인 등록건만 (상세 §10 결정사항 10)
-->
<template>
  <div class="card">
    <div class="head-row">
      <p class="card-title">근태 조회</p>
      <button
        type="button"
        class="head-chev"
        aria-label="자세히 보기"
        @click="$emit('click:detail')"
      >
        <svg class="icon" width="18" height="18" aria-hidden="true">
          <use href="#i-chev" />
        </svg>
      </button>
    </div>

    <div class="kpi-grid" :class="{ 'kpi-grid--single': !showLeave }">
      <!-- 잔여 연차 — 소정-12(UI-E): 연차 기능 미노출 회사(자동부여 off + 부여이력 0)는 숨김 -->
      <div
        v-if="showLeave"
        class="kpi"
        role="button"
        tabindex="0"
        @click="$emit('click:leave')"
        @keydown.enter="$emit('click:leave')"
        @keydown.space.prevent="$emit('click:leave')"
      >
        <div class="kpi-top">
          <span class="kpi-lbl">잔여 연차</span>
          <svg class="icon kpi-top__chev" width="14" height="14" aria-hidden="true">
            <use href="#i-chev" />
          </svg>
        </div>
        <div class="kpi-vals">
          <span class="kpi-num">{{ formattedRemaining }}</span>
          <span class="kpi-unit">/ {{ formattedGranted }}일</span>
        </div>
      </div>

      <!-- 승인 요청 -->
      <div
        class="kpi"
        role="button"
        tabindex="0"
        @click="$emit('click:approval')"
        @keydown.enter="$emit('click:approval')"
        @keydown.space.prevent="$emit('click:approval')"
      >
        <div class="kpi-top">
          <span class="kpi-lbl">승인 요청</span>
          <svg class="icon kpi-top__chev" width="14" height="14" aria-hidden="true">
            <use href="#i-chev" />
          </svg>
        </div>
        <div class="kpi-vals">
          <span class="kpi-num kpi-num--warn">{{ pendingCount }}</span>
          <span class="kpi-unit">건 대기</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 이번 연도 잔여 연차 일수
  remainingDays: {
    type: Number,
    default: 0,
  },
  // 이번 연도 총 부여 일수
  grantedDays: {
    type: Number,
    default: 0,
  },
  // 본인이 등록한 요청 중 REQ_STATUS = 'REQUESTED' 건수
  pendingCount: {
    type: Number,
    default: 0,
  },
  // 소정-12(UI-E): 잔여 연차 KPI 노출 여부. false 면 승인 요청 KPI 만 남는다.
  //   기본 true — 판정 실패/미판정은 "보이는 쪽" 폴백(연차 기능이 조용히 사라지지 않게).
  showLeave: {
    type: Boolean,
    default: true,
  },
})

defineEmits(['click:detail', 'click:leave', 'click:approval'])

// 0.5 단위 표시 처리 (정수면 정수로, 소수면 1자리)
// TODO(developer): 정책서에 따라 시간 단위 휴가 표시 검토 (attd §8.4)
const trimDays = (v) => {
  if (v == null) return '0'
  const n = Number(v)
  if (Number.isInteger(n)) return String(n)
  return n.toFixed(1)
}

const formattedRemaining = computed(() => trimDays(props.remainingDays))
const formattedGranted = computed(() => trimDays(props.grantedDays))
</script>

<style scoped>
.card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--color-border);
  padding: 16px;
  margin-bottom: 12px;
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
  margin-bottom: 14px;
}

.head-chev {
  background: transparent;
  border: 0;
  padding: 4px;
  /* hit area 확장 — 상세 §2.2 (최소 44×44px) */
  min-width: 44px;
  min-height: 44px;
  margin: -10px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-family: inherit;
}

.kpi-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
/* 소정-12(UI-E): 잔여 연차 KPI 숨김 회사 — 남은 KPI 1개가 폭을 전부 사용한다. */
.kpi-grid--single {
  grid-template-columns: 1fr;
}

.kpi {
  background: #ffffff;
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px 12px;
  min-height: 76px;
  cursor: pointer;
}
.kpi:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.kpi-top__chev {
  color: var(--color-text-tertiary);
}

.kpi-lbl {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
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
  color: var(--color-text-primary);
}
.kpi-num--warn {
  color: var(--color-warning);
}

.kpi-unit {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
